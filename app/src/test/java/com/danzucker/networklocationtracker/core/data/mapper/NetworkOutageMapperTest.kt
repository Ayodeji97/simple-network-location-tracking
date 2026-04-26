package com.danzucker.networklocationtracker.core.data.mapper

import android.location.Location
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutage
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds
import org.junit.jupiter.api.Test

class NetworkOutageMapperTest {

    private fun loc(lat: Double, lon: Double): Location = Location("test").apply {
        latitude = lat
        longitude = lon
    }

    @Test
    fun `completed outage round-trips through entity`() {
        val original = NetworkOutage(
            id = 42L,
            startTime = Instant.fromEpochMilliseconds(1_700_000_000_000),
            endTime = Instant.fromEpochMilliseconds(1_700_000_004_500),
            startLocation = loc(40.7128, -74.0060),
            endLocation = loc(40.7135, -74.0082),
            duration = 4_500.milliseconds,
            isServerReachable = true,
        )

        val roundTripped = original.toNetworkOutageEntity().toNetworkOutage()

        assertThat(roundTripped.id).isEqualTo(original.id)
        assertThat(roundTripped.startTime).isEqualTo(original.startTime)
        assertThat(roundTripped.endTime).isEqualTo(original.endTime)
        assertThat(roundTripped.startLocation.latitude).isEqualTo(original.startLocation.latitude)
        assertThat(roundTripped.startLocation.longitude).isEqualTo(original.startLocation.longitude)
        assertThat(roundTripped.endLocation?.latitude).isEqualTo(original.endLocation?.latitude)
        assertThat(roundTripped.duration).isEqualTo(original.duration)
        assertThat(roundTripped.isServerReachable).isEqualTo(original.isServerReachable)
    }

    @Test
    fun `ongoing outage with DISTANT_PAST endTime round-trips`() {
        val original = NetworkOutage(
            id = 1L,
            startTime = Instant.fromEpochMilliseconds(1_700_000_000_000),
            endTime = Instant.DISTANT_PAST,
            startLocation = loc(10.0, 20.0),
            endLocation = null,
            duration = kotlin.time.Duration.ZERO,
            isServerReachable = false,
        )

        val entity = original.toNetworkOutageEntity()
        assertThat(entity.endTime).isEqualTo(ONGOING_OUTAGE_SENTINEL)
        assertThat(entity.endLatitude).isNull()
        assertThat(entity.endLongitude).isNull()

        val back = entity.toNetworkOutage()
        assertThat(back.endTime).isEqualTo(Instant.DISTANT_PAST)
        assertThat(back.endLocation).isNull()
    }
}
