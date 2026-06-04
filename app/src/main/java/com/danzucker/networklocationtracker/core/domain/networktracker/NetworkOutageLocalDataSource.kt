package com.danzucker.networklocationtracker.core.domain.networktracker

import com.danzucker.networklocationtracker.core.domain.util.DataError
import com.danzucker.networklocationtracker.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface NetworkOutageLocalDataSource {
    fun getNetworkOutages(): Flow<List<NetworkOutage>>
    suspend fun getOngoingNetworkOutage(id: Long): Result<NetworkOutage?, DataError.Local>
    suspend fun getOngoingNetworkOutage(): Result<NetworkOutage?, DataError.Local>
    suspend fun insertNetworkOutage(networkOutage: NetworkOutage): Result<Long, DataError.Local>
    suspend fun deleteNetworkOutage(id: Long)
    suspend fun deleteAllNetworkOutages()
}
