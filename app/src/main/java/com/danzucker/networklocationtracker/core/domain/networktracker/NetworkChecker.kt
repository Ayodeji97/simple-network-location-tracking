package com.danzucker.networklocationtracker.core.domain.networktracker

import kotlinx.coroutines.flow.Flow

interface NetworkChecker {
    fun isDeviceConnected(): Flow<Boolean>
}