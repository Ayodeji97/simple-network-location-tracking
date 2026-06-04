package com.danzucker.networklocationtracker.di

import com.danzucker.networklocationtracker.core.data.GeocoderAddressResolver
import com.danzucker.networklocationtracker.core.data.IcmpServerPinger
import com.danzucker.networklocationtracker.core.data.LocationObserverImpl
import com.danzucker.networklocationtracker.core.data.networktracker.NetworkCheckerImpl
import com.danzucker.networklocationtracker.core.data.networktracker.OfflineNetworkOutageRepository
import com.danzucker.networklocationtracker.core.domain.AddressResolver
import com.danzucker.networklocationtracker.core.domain.LocationObserver
import com.danzucker.networklocationtracker.core.domain.ServerPinger
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkChecker
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutageRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    single<LocationObserver> { LocationObserverImpl(androidContext()) }
    single<NetworkChecker> { NetworkCheckerImpl(androidContext()) }
    single<ServerPinger> { IcmpServerPinger() }
    single<AddressResolver> { GeocoderAddressResolver(androidContext()) }
    singleOf(::OfflineNetworkOutageRepository) bind NetworkOutageRepository::class
}
