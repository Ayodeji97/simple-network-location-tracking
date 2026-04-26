package com.danzucker.networklocationtracker.core.domain

import kotlinx.coroutines.flow.Flow

interface ServerPinger {
    fun pingServer(
        serverAddress: String,
        initialTimeOutMs: Int,
        maxAttempts: Int
    ): Flow<Boolean>
}