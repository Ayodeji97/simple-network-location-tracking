package com.danzucker.networklocationtracker

import android.app.Application
import com.danzucker.networklocationtracker.di.databaseModule
import com.danzucker.networklocationtracker.di.dataModule
import com.danzucker.networklocationtracker.di.trackerModule
import com.danzucker.networklocationtracker.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NetworkTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@NetworkTrackerApp)
            modules(
                databaseModule,
                dataModule,
                trackerModule,
                viewModelModule,
            )
        }
    }
}
