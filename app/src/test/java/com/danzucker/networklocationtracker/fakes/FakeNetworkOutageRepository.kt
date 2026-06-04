package com.danzucker.networklocationtracker.fakes

import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutage
import com.danzucker.networklocationtracker.core.domain.networktracker.NetworkOutageRepository
import com.danzucker.networklocationtracker.core.domain.util.DataError
import com.danzucker.networklocationtracker.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant

class FakeNetworkOutageRepository(
    initial: List<NetworkOutage> = emptyList(),
) : NetworkOutageRepository {

    private val _outages = MutableStateFlow(initial)
    val outages get() = _outages.value

    private var nextId: Long = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    var insertError: DataError.Local? = null
    var readError: DataError.Local? = null

    override fun getNetworkOutages(): Flow<List<NetworkOutage>> = _outages.asStateFlow()

    override suspend fun getOngoingNetworkOutage(id: Long): Result<NetworkOutage?, DataError.Local> {
        readError?.let { return Result.Error(it) }
        return Result.Success(_outages.value.firstOrNull { it.id == id })
    }

    override suspend fun getOngoingNetworkOutage(): Result<NetworkOutage?, DataError.Local> {
        readError?.let { return Result.Error(it) }
        return Result.Success(_outages.value.firstOrNull { it.endTime == Instant.DISTANT_PAST })
    }

    override suspend fun insertNetworkOutage(networkOutage: NetworkOutage): Result<Long, DataError.Local> {
        insertError?.let { return Result.Error(it) }
        val existingIndex = _outages.value.indexOfFirst { it.id == networkOutage.id && it.id != 0L }
        val newId = if (networkOutage.id == 0L) nextId++ else networkOutage.id
        val toStore = networkOutage.copy(id = newId)
        _outages.value = if (existingIndex >= 0) {
            _outages.value.toMutableList().also { it[existingIndex] = toStore }
        } else {
            _outages.value + toStore
        }
        return Result.Success(newId)
    }

    override suspend fun deleteNetworkOutage(id: Long) {
        _outages.value = _outages.value.filterNot { it.id == id }
    }

    override suspend fun deleteAllNetworkOutages() {
        _outages.value = emptyList()
    }
}
