package com.danzucker.networklocationtracker.core.data

import com.danzucker.networklocationtracker.core.domain.ServerPinger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.InetSocketAddress
import java.net.Socket

/** Pluggable seam so tests can drive reachability without real network calls. */
fun interface ReachabilityCheck {
    suspend fun isReachable(address: String, timeoutMs: Int): Boolean
}

/**
 * Default reachability uses a TCP connect to port 443. The class name is "IcmpServerPinger" for
 * historical reasons, but on Android we can't actually use ICMP — `InetAddress.isReachable()`
 * silently falls back to TCP echo (port 7), which is closed on every modern server, so it always
 * returns false. Port 443 (HTTPS) is the most reliable "is the internet alive" probe: it's open
 * on 8.8.8.8 and effectively every reachable server, and it works through HTTPS-only networks
 * that block ICMP/UDP.
 */
private val defaultReachability = ReachabilityCheck { address, timeoutMs ->
    try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(address, 443), timeoutMs)
            socket.isConnected
        }
    } catch (_: Exception) {
        false
    }
}

class IcmpServerPinger(
    private val reachability: ReachabilityCheck = defaultReachability,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ServerPinger {

    override fun pingServer(
        serverAddress: String,
        initialTimeOutMs: Int,
        maxAttempts: Int
    ): Flow<Boolean> = flow {
        var timeout = initialTimeOutMs
        repeat(maxAttempts) { attempt ->
            if (reachability.isReachable(serverAddress, timeout)) {
                emit(true)
                return@flow
            }
            timeout *= 2
            if (attempt < maxAttempts - 1) {
                delay(timeout.toLong())
            }
        }
        emit(false)
    }.flowOn(dispatcher)
}
