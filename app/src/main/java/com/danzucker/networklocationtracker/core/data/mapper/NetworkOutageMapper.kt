package com.danzucker.networklocationtracker.core.data.mapper

import android.location.Location
import com.danzucker.networklocationtracker.core.data.entity.NetworkOutageEntity
import com.danzucker.networklocationtracker.core.data.util.instantToMillis
import com.danzucker.networklocationtracker.core.data.util.millisToInstant
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutage
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

/**
 * Sentinel stored in `endTime` to mark an outage that hasn't ended yet. -1L is safe because
 * `toEpochMilliseconds()` never returns -1 for any real wall-clock instant (epoch +/- 1 ms is
 * billions of entries away from any plausible outage time).
 */
internal const val ONGOING_OUTAGE_SENTINEL = -1L

fun NetworkOutageEntity.toNetworkOutage(): NetworkOutage {
    return NetworkOutage(
        id = id,
        startTime = startTime.millisToInstant(),
        endTime = if (endTime == ONGOING_OUTAGE_SENTINEL) Instant.DISTANT_PAST else endTime.millisToInstant(),
        startLocation = startLatitude?.let { lat ->
            startLongitude?.let { lon ->
                Location("start").apply {
                    latitude = lat
                    longitude = lon
                }
            }
        },
        endLocation = endLatitude?.let { lat ->
            endLongitude?.let { lon ->
                Location("end").apply {
                    latitude = lat
                    longitude = lon
                }
            }
        },
        duration = durationMillis.milliseconds,
        isServerReachable = isServerReachable,
        startAddress = startAddress,
        endAddress = endAddress,
    )
}

fun NetworkOutage.toNetworkOutageEntity(): NetworkOutageEntity {
    return NetworkOutageEntity(
        id = id,
        startTime = startTime.instantToMillis(),
        endTime = if (endTime == Instant.DISTANT_PAST) ONGOING_OUTAGE_SENTINEL else endTime.instantToMillis(),
        startLatitude = startLocation?.latitude,
        startLongitude = startLocation?.longitude,
        endLatitude = endLocation?.latitude,
        endLongitude = endLocation?.longitude,
        durationMillis = duration.inWholeMilliseconds,
        isServerReachable = isServerReachable,
        startAddress = startAddress,
        endAddress = endAddress,
    )
}

fun List<NetworkOutageEntity>.toNetworkOutages(): List<NetworkOutage> = map { it.toNetworkOutage() }
