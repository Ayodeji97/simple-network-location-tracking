package com.danzucker.networklocationtracker.core.data.networktracker

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkStatus
import com.danzucker.networklocationtracker.fakes.FakeAddressResolver
import com.danzucker.networklocationtracker.fakes.FakeLocationObserver
import com.danzucker.networklocationtracker.fakes.FakeNetworkChecker
import com.danzucker.networklocationtracker.fakes.FakeNetworkOutageRepository
import com.danzucker.networklocationtracker.fakes.FakeServerPinger
import com.danzucker.networklocationtracker.fakes.fakeLocation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkWithLocationTrackerTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private data class Bundle(
        val tracker: NetworkWithLocationTracker,
        val checker: FakeNetworkChecker,
        val pinger: FakeServerPinger,
        val location: FakeLocationObserver,
        val repo: FakeNetworkOutageRepository,
    )

    private fun buildTracker(
        pinger: FakeServerPinger = FakeServerPinger(nextResult = true),
        addressResolver: FakeAddressResolver = FakeAddressResolver(result = "Test St, Testville, Testland"),
        clock: () -> Instant = { Instant.fromEpochMilliseconds(1_000_000) },
    ): Bundle {
        val checker = FakeNetworkChecker()
        val location = FakeLocationObserver()
        val repo = FakeNetworkOutageRepository()
        return Bundle(
            tracker = NetworkWithLocationTracker(
                locationObserver = location,
                networkChecker = checker,
                serverPinger = pinger,
                networkOutageRepository = repo,
                addressResolver = addressResolver,
                dispatcher = dispatcher,
                clock = clock,
            ),
            checker = checker,
            pinger = pinger,
            location = location,
            repo = repo,
        )
    }

    @Test
    fun `disconnect emits OutageStarted and persists ongoing outage`() = runTest(dispatcher) {
        val b = buildTracker()
        b.tracker.startTracking(backgroundScope)

        b.tracker.trackingEvents.test {
            b.checker.emit(false)
            b.location.emit(fakeLocation(lat = 10.0, lon = 20.0))

            val event = awaitItem()
            assertThat(event).isInstanceOf(TrackingEvent.OutageStarted::class)
            val outage = (event as TrackingEvent.OutageStarted).outage
            assertThat(outage.endTime).isEqualTo(Instant.DISTANT_PAST)
            assertThat(outage.isServerReachable).isFalse()
            assertThat(b.repo.outages).hasSize(1)
            assertThat(b.repo.outages.first().endTime).isEqualTo(Instant.DISTANT_PAST)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disconnect with no location fix still records an outage`() = runTest(dispatcher) {
        val b = buildTracker()
        b.tracker.startTracking(backgroundScope)

        b.tracker.trackingEvents.test {
            b.checker.emit(false)
            // No location is ever emitted — simulates indoors/offline where no GPS fix arrives.
            // The outage must still be recorded once the best-effort timeout elapses.
            advanceTimeBy(LOCATION_FIX_TIMEOUT_MS + 1)

            val event = awaitItem()
            assertThat(event).isInstanceOf(TrackingEvent.OutageStarted::class)
            val outage = (event as TrackingEvent.OutageStarted).outage
            assertThat(outage.startLocation).isNull()
            assertThat(b.repo.outages).hasSize(1)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reconnect after disconnect emits OutageEnded with duration and endLocation`() = runTest(dispatcher) {
        var now = 1_000_000L
        val b = buildTracker(clock = { Instant.fromEpochMilliseconds(now) })
        b.tracker.startTracking(backgroundScope)

        b.tracker.trackingEvents.test {
            b.checker.emit(false)
            b.location.emit(fakeLocation(lat = 10.0, lon = 20.0))
            assertThat(awaitItem()).isInstanceOf(TrackingEvent.OutageStarted::class)

            now += 4_000
            b.checker.emit(true)
            b.location.emit(fakeLocation(lat = 11.0, lon = 21.0))

            val ended = awaitItem()
            assertThat(ended).isInstanceOf(TrackingEvent.OutageEnded::class)
            val outage = (ended as TrackingEvent.OutageEnded).outage
            assertThat(outage.isServerReachable).isTrue()
            assertThat(outage.duration.inWholeMilliseconds).isEqualTo(4_000L)
            assertThat(outage.endLocation).isNotNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `server unreachable while connected is treated as DISCONNECTED`() = runTest(dispatcher) {
        val b = buildTracker(pinger = FakeServerPinger(nextResult = false))
        b.tracker.startTracking(backgroundScope)

        b.tracker.trackingEvents.test {
            b.checker.emit(true)
            b.location.emit(fakeLocation())

            val event = awaitItem()
            assertThat(event).isInstanceOf(TrackingEvent.OutageStarted::class)
            assertThat((event as TrackingEvent.OutageStarted).outage.isServerReachable).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `server outage is detected while device connectivity stays connected`() = runTest(dispatcher) {
        val pinger = FakeServerPinger(nextResult = true)
        val b = buildTracker(pinger = pinger)
        b.tracker.startTracking(backgroundScope)

        b.tracker.trackingEvents.test {
            b.checker.emit(true)
            assertThat(b.tracker.currentStatus.value).isEqualTo(NetworkStatus.CONNECTED)

            pinger.nextResult = false
            advanceTimeBy(REACHABILITY_POLL_INTERVAL_MS)
            b.location.emit(fakeLocation(lat = 10.0, lon = 20.0))

            val event = awaitItem()
            assertThat(event).isInstanceOf(TrackingEvent.OutageStarted::class)
            assertThat((event as TrackingEvent.OutageStarted).outage.isServerReachable).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `server recovery is detected while device connectivity stays connected`() = runTest(dispatcher) {
        var now = 1_000_000L
        val pinger = FakeServerPinger(nextResult = false)
        val b = buildTracker(
            pinger = pinger,
            clock = { Instant.fromEpochMilliseconds(now) },
        )
        b.tracker.startTracking(backgroundScope)

        b.tracker.trackingEvents.test {
            b.checker.emit(true)
            b.location.emit(fakeLocation(lat = 10.0, lon = 20.0))
            assertThat(awaitItem()).isInstanceOf(TrackingEvent.OutageStarted::class)

            pinger.nextResult = true
            now += 4_000
            advanceTimeBy(REACHABILITY_POLL_INTERVAL_MS)
            b.location.emit(fakeLocation(lat = 11.0, lon = 21.0))

            val event = awaitItem()
            assertThat(event).isInstanceOf(TrackingEvent.OutageEnded::class)
            val outage = (event as TrackingEvent.OutageEnded).outage
            assertThat(outage.isServerReachable).isTrue()
            assertThat(outage.duration.inWholeMilliseconds).isEqualTo(4_000L)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `distinctUntilChanged suppresses duplicate disconnect events`() = runTest(dispatcher) {
        val b = buildTracker()
        b.tracker.startTracking(backgroundScope)

        b.tracker.trackingEvents.test {
            b.checker.emit(false)
            b.location.emit(fakeLocation(lat = 10.0))
            assertThat(awaitItem()).isInstanceOf(TrackingEvent.OutageStarted::class)

            b.checker.emit(false)
            b.location.emit(fakeLocation(lat = 11.0))
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `outage is persisted with the resolved address`() = runTest(dispatcher) {
        val resolver = FakeAddressResolver(result = "Broadway, New York, United States")
        val b = buildTracker(addressResolver = resolver)
        b.tracker.startTracking(backgroundScope)

        b.tracker.trackingEvents.test {
            b.checker.emit(false)
            b.location.emit(fakeLocation(lat = 40.71, lon = -74.00))
            assertThat(awaitItem()).isInstanceOf(TrackingEvent.OutageStarted::class)

            assertThat(b.repo.outages.first().startAddress).isEqualTo("Broadway, New York, United States")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resolver throwing is tolerated - outage still persists with null address`() = runTest(dispatcher) {
        val b = buildTracker(
            addressResolver = FakeAddressResolver(throwOnCall = RuntimeException("no network"))
        )
        b.tracker.startTracking(backgroundScope)

        b.tracker.trackingEvents.test {
            b.checker.emit(false)
            b.location.emit(fakeLocation())
            assertThat(awaitItem()).isInstanceOf(TrackingEvent.OutageStarted::class)

            assertThat(b.repo.outages).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `currentStatus updates immediately on connectivity change without waiting for location`() = runTest(dispatcher) {
        val b = buildTracker()
        b.tracker.startTracking(backgroundScope)

        b.checker.emit(false) // connectivity went away
        // No location emitted yet — but the banner should already reflect DISCONNECTED.
        assertThat(b.tracker.currentStatus.value).isEqualTo(NetworkStatus.DISCONNECTED)

        b.checker.emit(true) // connectivity is back
        assertThat(b.tracker.currentStatus.value).isEqualTo(NetworkStatus.CONNECTED)
    }

    @Test
    fun `currentStatus reflects latest sample and resetStatus clears it`() = runTest(dispatcher) {
        val b = buildTracker()
        assertThat(b.tracker.currentStatus.value).isNull()

        b.tracker.startTracking(backgroundScope)
        b.tracker.trackingEvents.test {
            b.checker.emit(false)
            b.location.emit(fakeLocation())
            assertThat(awaitItem()).isInstanceOf(TrackingEvent.OutageStarted::class)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(b.tracker.currentStatus.value).isEqualTo(NetworkStatus.DISCONNECTED)

        b.tracker.resetStatus()
        assertThat(b.tracker.currentStatus.value).isNull()
    }

    @Test
    fun `pinger error is caught and surfaced as Error plus DISCONNECTED path`() = runTest(dispatcher) {
        val b = buildTracker(pinger = FakeServerPinger(throwOnCall = RuntimeException("boom")))
        b.tracker.startTracking(backgroundScope)

        b.tracker.trackingEvents.test {
            b.checker.emit(true)
            b.location.emit(fakeLocation())

            val events = listOf(awaitItem(), awaitItem())
            assertThat(events.any { it is TrackingEvent.Error }).isTrue()
            assertThat(events.any { it is TrackingEvent.OutageStarted }).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }
}
