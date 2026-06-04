package com.danzucker.networklocationtracker.core.domain

/**
 * Converts raw coordinates into a human-readable "Street, City, Country" string.
 * Implementations may return `null` when the platform is offline, the coordinates can't be
 * resolved, or the geocoding backend is unavailable — callers should treat that as "unknown".
 */
interface AddressResolver {
    suspend fun resolve(latitude: Double, longitude: Double): String?
}
