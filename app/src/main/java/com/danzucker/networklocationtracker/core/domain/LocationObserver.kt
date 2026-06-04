package com.danzucker.networklocationtracker.core.domain

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationObserver {
    fun observeLocation(interval: Long): Flow<Location>
}