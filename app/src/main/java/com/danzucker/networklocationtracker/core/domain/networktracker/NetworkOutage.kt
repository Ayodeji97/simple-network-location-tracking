package com.danzucker.networklocationtracker.core.domain.networktracker

import android.location.Location
import kotlinx.datetime.Instant
import kotlin.time.Duration

data class NetworkOutage(
    val id: Long,
    val startTime: Instant,
    val endTime: Instant,
    val startLocation: Location,
    val endLocation: Location?,
    val duration: Duration,
    val isServerReachable: Boolean,
    val startAddress: String? = null,
    val endAddress: String? = null,
)
