package com.danzucker.networklocationtracker.fakes

import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkChecker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeNetworkChecker : NetworkChecker {
    private val flow = MutableSharedFlow<Boolean>(replay = 1, extraBufferCapacity = 8)

    suspend fun emit(connected: Boolean) {
        flow.emit(connected)
    }

    override fun isDeviceConnected(): Flow<Boolean> = flow
}
