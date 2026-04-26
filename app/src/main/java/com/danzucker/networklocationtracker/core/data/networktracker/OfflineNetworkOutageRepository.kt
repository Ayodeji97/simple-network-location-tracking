package com.danzucker.networklocationtracker.core.data.networktracker

import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutage
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutageLocalDataSource
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutageRepository
import com.danzucker.networklocationtracker.core.domain.util.DataError
import com.danzucker.networklocationtracker.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

class OfflineNetworkOutageRepository(
    private val localDataSource: NetworkOutageLocalDataSource
) : NetworkOutageRepository {
    override fun getNetworkOutages(): Flow<List<NetworkOutage>> {
        return localDataSource.getNetworkOutages()
    }

    override suspend fun getOngoingNetworkOutage(id: Long): Result<NetworkOutage?, DataError.Local> {
        return localDataSource.getOngoingNetworkOutage(id = id)
    }

    override suspend fun getOngoingNetworkOutage(): Result<NetworkOutage?, DataError.Local> {
        return localDataSource.getOngoingNetworkOutage()
    }

    override suspend fun insertNetworkOutage(networkOutage: NetworkOutage): Result<Long, DataError.Local> {
        return localDataSource.insertNetworkOutage(networkOutage = networkOutage)
    }

    override suspend fun deleteNetworkOutage(id: Long) {
        localDataSource.deleteNetworkOutage(id = id)
    }

    override suspend fun deleteAllNetworkOutages() {
        localDataSource.deleteAllNetworkOutages()
    }
}