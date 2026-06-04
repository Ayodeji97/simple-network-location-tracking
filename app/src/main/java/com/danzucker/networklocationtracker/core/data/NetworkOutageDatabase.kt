package com.danzucker.networklocationtracker.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.danzucker.networklocationtracker.core.data.dao.NetworkOutageDao
import com.danzucker.networklocationtracker.core.data.entity.NetworkOutageEntity

@Database(
    entities = [
        NetworkOutageEntity::class,
    ],
    version = 2,
)
abstract class NetworkOutageDatabase : RoomDatabase() {
    abstract val networkOutageDao: NetworkOutageDao

    companion object {
        const val NETWORK_OUTAGE_DATABASE_NAME = "network_outage_database"
    }
}
