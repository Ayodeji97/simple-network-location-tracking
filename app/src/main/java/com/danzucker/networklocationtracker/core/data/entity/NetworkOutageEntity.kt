package com.danzucker.networklocationtracker.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.danzucker.networklocationtracker.core.data.NetworkOutageDatabase.Companion.NETWORK_OUTAGE_DATABASE_NAME

@Entity(tableName = NETWORK_OUTAGE_DATABASE_NAME)
data class NetworkOutageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTime: Long,
    val endTime: Long,
    val startLatitude: Double?,
    val startLongitude: Double?,
    val endLatitude: Double?,
    val endLongitude: Double?,
    val durationMillis: Long,
    val isServerReachable: Boolean,
    val startAddress: String? = null,
    val endAddress: String? = null,
)
