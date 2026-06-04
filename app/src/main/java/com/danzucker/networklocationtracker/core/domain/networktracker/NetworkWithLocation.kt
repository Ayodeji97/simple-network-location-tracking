package com.danzucker.networklocationtracker.core.domain.networktracker

import android.location.Location
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class NetworkWithLocation(
    // Nullable: an outage must be recorded even when no GPS fix is available (indoors/offline),
    // so location is best-effort and may be absent.
    val location: Location?,
    val networkStatus: NetworkStatus,
    val timestamp: Instant
)


enum class NetworkStatus {
    CONNECTED,
    DISCONNECTED
}

data class NetworkStatusInfo(
    val isConnected: Boolean,
    val isServerReachable: Boolean,
    val timestamp: Instant = Clock.System.now()
)
