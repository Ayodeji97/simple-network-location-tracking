package com.danzucker.networklocationtracker.di

import com.danzucker.networklocationtracker.core.data.networktracker.NetworkWithLocationTracker
import org.koin.dsl.module

val trackerModule = module {
    single { NetworkWithLocationTracker(get(), get(), get(), get(), get()) }
}
