package com.danzucker.networklocationtracker.fakes

import android.location.Location
import com.danzucker.networklocationtracker.core.domain.LocationObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeLocationObserver : LocationObserver {
    private val flow = MutableSharedFlow<Location>(replay = 1, extraBufferCapacity = 8)

    suspend fun emit(location: Location) {
        flow.emit(location)
    }

    override fun observeLocation(interval: Long): Flow<Location> = flow
}

fun fakeLocation(lat: Double = 0.0, lon: Double = 0.0, provider: String = "test"): Location =
    Location(provider).apply {
        latitude = lat
        longitude = lon
    }
