package com.danzucker.networklocationtracker.networktrackerlist

import com.danzucker.networklocationtracker.core.data.util.toReadableDateTime
import com.danzucker.networklocationtracker.core.data.util.toReadableDuration
import com.danzucker.networklocationtracker.core.data.util.toReadableTime
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutage
import kotlinx.datetime.Instant

data class NetworkOutageUi(
    val id: Long,
    val startTimeLabel: String,
    val endTimeLabel: String?,
    val durationLabel: String?,
    val startCoordinates: String,
    val endCoordinates: String?,
    val startAddress: String?,
    val endAddress: String?,
    val isServerReachable: Boolean,
    val isOngoing: Boolean,
)

fun NetworkOutage.toNetworkOutageUi(): NetworkOutageUi {
    val ongoing = endTime == Instant.DISTANT_PAST
    return NetworkOutageUi(
        id = id,
        startTimeLabel = startTime.toReadableDateTime(),
        endTimeLabel = if (ongoing) null else endTime.toReadableTime(),
        durationLabel = if (ongoing) null else duration.toReadableDuration(),
        startCoordinates = startLocation.formatCoords(),
        endCoordinates = endLocation?.formatCoords(),
        startAddress = startAddress,
        endAddress = endAddress,
        isServerReachable = isServerReachable,
        isOngoing = ongoing,
    )
}

private fun android.location.Location.formatCoords(): String =
    "%.4f, %.4f".format(latitude, longitude)
