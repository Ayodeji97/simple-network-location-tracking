package com.danzucker.networklocationtracker.core.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IcmpServerPingerTest {

    @Test
    fun `succeeds on first attempt without any delay`() = runTest {
        val pinger = IcmpServerPinger(
            reachability = { _, _ -> true },
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val startTime = currentTime
        pinger.pingServer("addr", 1000, 3).test {
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
        advanceUntilIdle()
        assertThat(currentTime - startTime).isEqualTo(0L)
    }

    @Test
    fun `succeeds on second attempt after initial backoff of 2x timeout`() = runTest {
        var attempt = 0
        val pinger = IcmpServerPinger(
            reachability = { _, _ -> (attempt++) >= 1 },
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val startTime = currentTime
        pinger.pingServer("addr", 1000, 3).test {
            assertThat(awaitItem()).isTrue()
            awaitComplete()
        }
        advanceUntilIdle()
        // After first failure timeout doubles (1000 → 2000), delay is 2000ms before retry.
        assertThat(currentTime - startTime).isEqualTo(2000L)
    }

    @Test
    fun `emits false after exhausting maxAttempts`() = runTest {
        val pinger = IcmpServerPinger(
            reachability = { _, _ -> false },
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        pinger.pingServer("addr", 1000, 3).test {
            assertThat(awaitItem()).isFalse()
            awaitComplete()
        }
        advanceUntilIdle()
        // Timeouts: attempt1 fails (t=1000 → 2000), delay 2000, attempt2 fails (2000 → 4000),
        // delay 4000, attempt3 fails, no delay after last. Total delay = 2000 + 4000 = 6000ms.
        assertThat(currentTime).isEqualTo(6000L)
    }
}
