package com.danzucker.networklocationtracker.core.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.danzucker.networklocationtracker.core.data.entity.NetworkOutageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkOutageDao {

    @Upsert
    suspend fun upsertNetworkOutage(networkOutage: NetworkOutageEntity): Long

    @Query("SELECT * FROM network_outage_database ORDER BY startTime DESC")
    fun getNetworkOutages(): Flow<List<NetworkOutageEntity>>

    @Query("SELECT * FROM network_outage_database WHERE id = :id LIMIT 1")
    suspend fun getOngoingNetworkOutage(id: Long): NetworkOutageEntity?

    @Query("SELECT * FROM network_outage_database WHERE endTime = -1 LIMIT 1")
    suspend fun getOngoingNetworkOutage(): NetworkOutageEntity?

    @Query("DELETE FROM network_outage_database WHERE id = :id")
    suspend fun deleteNetworkOutage(id: Long)

    @Query("DELETE FROM network_outage_database")
    suspend fun deleteAllNetworkOutages()
}
