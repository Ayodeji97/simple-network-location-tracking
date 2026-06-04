package com.danzucker.networklocationtracker.di

import com.danzucker.networklocationtracker.networktrackerlist.NetworkTrackerListViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::NetworkTrackerListViewModel)
}
