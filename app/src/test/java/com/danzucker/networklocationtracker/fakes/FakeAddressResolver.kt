package com.danzucker.networklocationtracker.fakes

import com.danzucker.networklocationtracker.core.domain.AddressResolver

class FakeAddressResolver(
    private val result: String? = null,
    var throwOnCall: Throwable? = null,
) : AddressResolver {

    var callCount = 0
        private set

    override suspend fun resolve(latitude: Double, longitude: Double): String? {
        callCount++
        throwOnCall?.let { throw it }
        return result
    }
}
