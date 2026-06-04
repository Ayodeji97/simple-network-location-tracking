package com.danzucker.networklocationtracker.di

import androidx.room.Room
import com.danzucker.networklocationtracker.core.data.NetworkOutageDatabase
import com.danzucker.networklocationtracker.core.data.NetworkOutageDatabase.Companion.NETWORK_OUTAGE_DATABASE_NAME
import com.danzucker.networklocationtracker.core.data.RoomNetworkOutageLocalDataSource
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutageLocalDataSource
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidApplication(),
            NetworkOutageDatabase::class.java,
            NETWORK_OUTAGE_DATABASE_NAME,
        )
            // Schema v1 had no address columns. No real user data to preserve — wipe on upgrade.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    single { get<NetworkOutageDatabase>().networkOutageDao }

    single<NetworkOutageLocalDataSource> { RoomNetworkOutageLocalDataSource(get()) }
}
