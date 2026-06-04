package com.danzucker.networklocationtracker.core.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.danzucker.networklocationtracker.core.domain.AddressResolver
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class GeocoderAddressResolver(
    context: Context,
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault()),
) : AddressResolver {

    override suspend fun resolve(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    resolveAsync(latitude, longitude)
                } else {
                    resolveBlocking(latitude, longitude)
                }
            } catch (_: Exception) {
                null
            }
        }

    private suspend fun resolveAsync(latitude: Double, longitude: Double): String? =
        suspendCancellableCoroutine { cont: CancellableContinuation<String?> ->
            geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                if (cont.isActive) cont.resume(formatFirst(addresses))
            }
        }

    @Suppress("DEPRECATION")
    private fun resolveBlocking(latitude: Double, longitude: Double): String? {
        val addresses = geocoder.getFromLocation(latitude, longitude, 1) ?: return null
        return formatFirst(addresses)
    }

    private fun formatFirst(addresses: List<Address>?): String? {
        val first = addresses?.firstOrNull() ?: return null
        // Street (if present) + city + country. locality ≈ city; thoroughfare ≈ street name.
        val parts = listOfNotNull(
            first.thoroughfare,
            first.locality ?: first.subAdminArea ?: first.adminArea,
            first.countryName,
        ).filter { it.isNotBlank() }
        return if (parts.isEmpty()) null else parts.joinToString(", ")
    }
}
