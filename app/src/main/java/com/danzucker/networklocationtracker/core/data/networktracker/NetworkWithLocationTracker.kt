@file:OptIn(ExperimentalCoroutinesApi::class)

package com.danzucker.networklocationtracker.core.data.networktracker

import android.location.Location
import com.danzucker.networklocationtracker.core.domain.AddressResolver
import com.danzucker.networklocationtracker.core.domain.LocationObserver
import com.danzucker.networklocationtracker.core.domain.ServerPinger
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkChecker
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutage
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutageRepository
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkStatus
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkStatus.CONNECTED
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkStatus.DISCONNECTED
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkWithLocation
import com.danzucker.networklocationtracker.core.domain.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlin.time.Duration

const val SERVER_ADDRESS = "8.8.8.8"
const val INITIAL_TIMEOUT_MS = 1000
const val MAX_ATTEMPTS = 3
const val LOCATION_INTERVAL_MS = 5_000L
const val REACHABILITY_POLL_INTERVAL_MS = 30_000L

/** How long to wait for a best-effort location fix before recording an outage without one. */
const val LOCATION_FIX_TIMEOUT_MS = 10_000L

class NetworkWithLocationTracker(
    private val locationObserver: LocationObserver,
    private val networkChecker: NetworkChecker,
    private val serverPinger: ServerPinger,
    private val networkOutageRepository: NetworkOutageRepository,
    private val addressResolver: AddressResolver,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> kotlinx.datetime.Instant = Clock.System::now,
) {

    private val _trackingEvents = MutableSharedFlow<TrackingEvent>(replay = 0, extraBufferCapacity = 8)
    val trackingEvents = _trackingEvents.asSharedFlow()

    /**
     * Latest observed network status, or `null` before any observation has happened (first sample
     * pending) or after [resetStatus] is called when tracking stops.
     */
    private val _currentStatus = MutableStateFlow<NetworkStatus?>(null)
    val currentStatus = _currentStatus.asStateFlow()

    fun resetStatus() {
        _currentStatus.value = null
    }

    val networkStatusFlow: Flow<NetworkStatus> = networkChecker.isDeviceConnected()
        .flatMapLatest { isConnected ->
            if (!isConnected) {
                flowOf(DISCONNECTED)
            } else {
                reachabilityStatusFlow()
            }
        }
        .catch { e ->
            _trackingEvents.emit(TrackingEvent.Error("Network status error: ${e.message}"))
            emit(DISCONNECTED)
        }
        .distinctUntilChanged()
        .flowOn(dispatcher)

    fun startTracking(scope: CoroutineScope): Job =
        networkStatusFlow
            // Update the public status the moment connectivity changes — don't wait on a location
            // fix. The banner stays in sync even if GPS takes 30s to deliver the first sample
            // (common indoors / on cold start).
            .onEach { _currentStatus.value = it }
            .flatMapLatest { status ->
                // Capture the timestamp at the moment the status changed, not when the location
                // arrived — otherwise the outage's startTime is offset by however long the fix took.
                val timestampOfChange = clock()
                flow {
                    // Best-effort location: wait briefly for a fix, but never block recording on
                    // one. Indoors/offline a fix can be slow or never arrive — we still record the
                    // outage, just without coordinates. firstOrNull() also covers the case where the
                    // location flow completes without emitting (e.g. permission revoked).
                    val location = withTimeoutOrNull(LOCATION_FIX_TIMEOUT_MS) {
                        locationObserver.observeLocation(LOCATION_INTERVAL_MS).firstOrNull()
                    }
                    emit(NetworkWithLocation(location, status, timestampOfChange))
                }
            }
            .onEach(::handleNetworkWithLocation)
            .launchIn(scope)

    private fun reachabilityStatusFlow(): Flow<NetworkStatus> = flow {
        while (currentCoroutineContext().isActive) {
            val status = try {
                val reachable = serverPinger
                    .pingServer(SERVER_ADDRESS, INITIAL_TIMEOUT_MS, MAX_ATTEMPTS)
                    .first()
                if (reachable) CONNECTED else DISCONNECTED
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _trackingEvents.emit(TrackingEvent.Error("Network status error: ${e.message}"))
                DISCONNECTED
            }

            emit(status)
            delay(REACHABILITY_POLL_INTERVAL_MS)
        }
    }

    private suspend fun handleNetworkWithLocation(snapshot: NetworkWithLocation) {
        when (snapshot.networkStatus) {
            CONNECTED -> handleConnectionRestored(snapshot)
            DISCONNECTED -> handleConnectionLost(snapshot)
        }
    }

    private suspend fun handleConnectionRestored(snapshot: NetworkWithLocation) {
        val ongoing = when (val res = networkOutageRepository.getOngoingNetworkOutage()) {
            is Result.Success -> res.data ?: return
            is Result.Error -> {
                _trackingEvents.emit(TrackingEvent.Error("Failed to look up ongoing outage: ${res.error}"))
                return
            }
        }

        val endAddress = snapshot.location.resolveAddress()
        val completed = ongoing.copy(
            endTime = snapshot.timestamp,
            endLocation = snapshot.location,
            duration = snapshot.timestamp - ongoing.startTime,
            isServerReachable = true,
            endAddress = endAddress,
        )

        when (val res = networkOutageRepository.insertNetworkOutage(completed)) {
            is Result.Success -> _trackingEvents.emit(TrackingEvent.OutageEnded(completed))
            is Result.Error -> _trackingEvents.emit(TrackingEvent.Error("Failed to complete outage: ${res.error}"))
        }
    }

    private suspend fun handleConnectionLost(snapshot: NetworkWithLocation) {
        val existing = when (val res = networkOutageRepository.getOngoingNetworkOutage()) {
            is Result.Success -> res.data
            is Result.Error -> {
                _trackingEvents.emit(TrackingEvent.Error("Failed to look up ongoing outage: ${res.error}"))
                return
            }
        }
        if (existing != null) return

        val startAddress = snapshot.location.resolveAddress()
        val newOutage = NetworkOutage(
            id = 0L,
            startTime = snapshot.timestamp,
            endTime = kotlinx.datetime.Instant.DISTANT_PAST,
            startLocation = snapshot.location,
            endLocation = null,
            duration = Duration.ZERO,
            isServerReachable = false,
            startAddress = startAddress,
        )

        when (val res = networkOutageRepository.insertNetworkOutage(newOutage)) {
            is Result.Success -> _trackingEvents.emit(TrackingEvent.OutageStarted(newOutage.copy(id = res.data)))
            is Result.Error -> _trackingEvents.emit(TrackingEvent.Error("Failed to start outage: ${res.error}"))
        }
    }

    private suspend fun Location?.resolveAddress(): String? {
        if (this == null) return null
        return try {
            addressResolver.resolve(latitude, longitude)
        } catch (_: Exception) {
            null
        }
    }
}

sealed interface TrackingEvent {
    data class OutageStarted(val outage: NetworkOutage) : TrackingEvent
    data class OutageEnded(val outage: NetworkOutage) : TrackingEvent
    data class Error(val message: String) : TrackingEvent
}
