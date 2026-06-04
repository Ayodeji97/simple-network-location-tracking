package com.danzucker.networklocationtracker.fakes

import com.danzucker.networklocationtracker.core.domain.ServerPinger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Returns a single-emit flow matching the real pinger's contract. Behavior controllable per-call:
 * either a fixed result or a function of the attempt index.
 */
class FakeServerPinger(
    var nextResult: Boolean = true,
    var throwOnCall: Throwable? = null,
) : ServerPinger {

    var callCount = 0
        private set

    override fun pingServer(
        serverAddress: String,
        initialTimeOutMs: Int,
        maxAttempts: Int
    ): Flow<Boolean> = flow {
        callCount++
        throwOnCall?.let { throw it }
        emit(nextResult)
    }
}
