package com.danzucker.networklocationtracker.networktrackerlist

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.danzucker.networklocationtracker.core.data.networktracker.NetworkWithLocationTracker
import com.danzucker.networklocationtracker.core.data.networktracker.TrackingEvent
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutage
import com.danzucker.networklocationtracker.fakes.FakeAddressResolver
import com.danzucker.networklocationtracker.fakes.FakeLocationObserver
import com.danzucker.networklocationtracker.fakes.FakeNetworkChecker
import com.danzucker.networklocationtracker.fakes.FakeNetworkOutageRepository
import com.danzucker.networklocationtracker.fakes.FakeServerPinger
import com.danzucker.networklocationtracker.fakes.fakeLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlin.time.Duration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkTrackerListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildVm(
        initialOutages: List<NetworkOutage> = emptyList(),
    ): Triple<NetworkTrackerListViewModel, FakeNetworkOutageRepository, NetworkWithLocationTracker> {
        val repo = FakeNetworkOutageRepository(initialOutages)
        val tracker = NetworkWithLocationTracker(
            locationObserver = FakeLocationObserver(),
            networkChecker = FakeNetworkChecker(),
            serverPinger = FakeServerPinger(),
            networkOutageRepository = repo,
            addressResolver = FakeAddressResolver(),
            dispatcher = dispatcher,
        )
        val vm = NetworkTrackerListViewModel(
            repository = repo,
            tracker = tracker,
            savedStateHandle = SavedStateHandle(),
        )
        return Triple(vm, repo, tracker)
    }

    @Test
    fun `initial state reflects repository contents and sets isLoading off`() = runTest(dispatcher) {
        val seeded = NetworkOutage(
            id = 7L,
            startTime = Instant.fromEpochMilliseconds(1_700_000_000_000),
            endTime = Instant.fromEpochMilliseconds(1_700_000_001_000),
            startLocation = fakeLocation(1.0, 2.0),
            endLocation = fakeLocation(1.1, 2.1),
            duration = Duration.parse("1s"),
            isServerReachable = true,
        )
        val (vm, _, _) = buildVm(initialOutages = listOf(seeded))

        vm.state.test {
            val s = awaitItem()
            assertThat(s.outages).hasSize(1)
            assertThat(s.outages.first().id).isEqualTo(7L)
            assertThat(s.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ongoing outage is exposed separately from completed outages`() = runTest(dispatcher) {
        val ongoing = NetworkOutage(
            id = 9L,
            startTime = Instant.fromEpochMilliseconds(1_700_000_000_000),
            endTime = Instant.DISTANT_PAST,
            startLocation = fakeLocation(1.0, 2.0),
            endLocation = null,
            duration = Duration.ZERO,
            isServerReachable = false,
        )
        val (vm, _, _) = buildVm(initialOutages = listOf(ongoing))

        vm.state.test {
            val s = awaitItem()
            assertThat(s.ongoingOutage).isNotNull()
            assertThat(s.ongoingOutage?.id).isEqualTo(9L)
            assertThat(s.outages).hasSize(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `OnStartTracking sets isTracking true and emits StartService event`() = runTest(dispatcher) {
        val (vm, _, _) = buildVm()

        vm.events.test {
            vm.onAction(NetworkTrackerListAction.OnStartTracking)
            assertThat(awaitItem()).isEqualTo(NetworkTrackerListEvent.StartService)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(vm.state.value.isTracking).isTrue()
    }

    @Test
    fun `OnStopTracking sets isTracking false and emits StopService event`() = runTest(dispatcher) {
        val (vm, _, _) = buildVm()
        vm.onAction(NetworkTrackerListAction.OnStartTracking)

        vm.events.test {
            // Drain the StartService event from the setup call.
            awaitItem()
            vm.onAction(NetworkTrackerListAction.OnStopTracking)
            assertThat(awaitItem()).isEqualTo(NetworkTrackerListEvent.StopService)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(vm.state.value.isTracking).isFalse()
    }

    @Test
    fun `tracker Error event is forwarded as ShowError UI event`() = runTest(dispatcher) {
        // We need a tracker whose trackingEvents we can drive. Build one with a custom SharedFlow
        // plumbed into a surrogate tracker-shaped object. Since NetworkWithLocationTracker owns
        // its own events, we instead drive via the fakes + real tracker.
        val repo = FakeNetworkOutageRepository()
        val checker = FakeNetworkChecker()
        val location = FakeLocationObserver()
        val pinger = FakeServerPinger(throwOnCall = RuntimeException("network explosion"))
        val tracker = NetworkWithLocationTracker(
            locationObserver = location,
            networkChecker = checker,
            serverPinger = pinger,
            networkOutageRepository = repo,
            addressResolver = FakeAddressResolver(),
            dispatcher = dispatcher,
        )
        tracker.startTracking(backgroundScope)
        val vm = NetworkTrackerListViewModel(repo, tracker, SavedStateHandle())

        vm.events.test {
            checker.emit(true)
            location.emit(fakeLocation())

            // The tracker's catch{} produces a TrackingEvent.Error → VM forwards as ShowError.
            // OutageStarted arrives too but the VM ignores it (repo Flow handles state).
            val event = awaitItem()
            assertThat(event).isInstanceOf(NetworkTrackerListEvent.ShowError::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isTracking survives process death via SavedStateHandle`() = runTest(dispatcher) {
        val savedState = SavedStateHandle(mapOf("is_tracking" to true))
        val repo = FakeNetworkOutageRepository()
        val tracker = NetworkWithLocationTracker(
            locationObserver = FakeLocationObserver(),
            networkChecker = FakeNetworkChecker(),
            serverPinger = FakeServerPinger(),
            networkOutageRepository = repo,
            addressResolver = FakeAddressResolver(),
            dispatcher = dispatcher,
        )
        val vm = NetworkTrackerListViewModel(repo, tracker, savedState)

        vm.state.test {
            assertThat(awaitItem().isTracking).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
