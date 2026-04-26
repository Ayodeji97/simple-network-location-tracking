package com.danzucker.networklocationtracker.core.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import com.danzucker.networklocationtracker.core.data.dao.NetworkOutageDao
import com.danzucker.networklocationtracker.core.data.mapper.toNetworkOutage
import com.danzucker.networklocationtracker.core.data.mapper.toNetworkOutageEntity
import com.danzucker.networklocationtracker.core.data.mapper.toNetworkOutages
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutage
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutageLocalDataSource
import com.danzucker.networklocationtracker.core.domain.util.DataError
import com.danzucker.networklocationtracker.core.domain.util.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomNetworkOutageLocalDataSource(
    private val networkOutageDao: NetworkOutageDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : NetworkOutageLocalDataSource {

    override fun getNetworkOutages(): Flow<List<NetworkOutage>> {
        return networkOutageDao.getNetworkOutages()
            .map { it.toNetworkOutages() }
    }

    override suspend fun getOngoingNetworkOutage(
        id: Long
    ): Result<NetworkOutage?, DataError.Local> = withDataErrorHandling {
        withContext(dispatcher) {
            Result.Success(networkOutageDao.getOngoingNetworkOutage(id)?.toNetworkOutage())
        }
    }

    override suspend fun getOngoingNetworkOutage(): Result<NetworkOutage?, DataError.Local> =
        withDataErrorHandling {
            withContext(dispatcher) {
                Result.Success(networkOutageDao.getOngoingNetworkOutage()?.toNetworkOutage())
            }
        }

    override suspend fun insertNetworkOutage(
        networkOutage: NetworkOutage
    ): Result<Long, DataError.Local> = withDataErrorHandling {
        withContext(dispatcher) {
            val rowId = networkOutageDao.upsertNetworkOutage(networkOutage.toNetworkOutageEntity())
            // Room returns -1 when an existing row is updated; fall back to the existing id.
            val id = if (rowId == -1L) networkOutage.id else rowId
            Result.Success(id)
        }
    }

    override suspend fun deleteNetworkOutage(id: Long) {
        withContext(dispatcher) { networkOutageDao.deleteNetworkOutage(id) }
    }

    override suspend fun deleteAllNetworkOutages() {
        withContext(dispatcher) { networkOutageDao.deleteAllNetworkOutages() }
    }

    private suspend fun <T> withDataErrorHandling(
        block: suspend () -> Result<T, DataError.Local>
    ): Result<T, DataError.Local> {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteFullException) {
            Result.Error(DataError.Local.DISK_FULL)
        } catch (e: SQLiteException) {
            Result.Error(DataError.Local.DATABASE_ERROR)
        } catch (e: Exception) {
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}
