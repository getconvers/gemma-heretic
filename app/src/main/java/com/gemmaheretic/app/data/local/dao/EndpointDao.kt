package com.gemmaheretic.app.data.local.dao

import androidx.room.*
import com.gemmaheretic.app.data.local.entity.EndpointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EndpointDao {
    @Query("SELECT * FROM endpoints ORDER BY isDefault DESC, name ASC")
    fun getAllEndpoints(): Flow<List<EndpointEntity>>

    @Query("SELECT * FROM endpoints WHERE id = :id")
    suspend fun getEndpointById(id: Long): EndpointEntity?

    @Query("SELECT * FROM endpoints WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultEndpoint(): EndpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEndpoint(endpoint: EndpointEntity): Long

    @Update
    suspend fun updateEndpoint(endpoint: EndpointEntity)

    @Delete
    suspend fun deleteEndpoint(endpoint: EndpointEntity)

    @Query("UPDATE endpoints SET isDefault = 0")
    suspend fun clearDefaultEndpoint()

    @Transaction
    suspend fun setDefaultEndpoint(id: Long) {
        clearDefaultEndpoint()
        val endpoint = getEndpointById(id) ?: return
        updateEndpoint(endpoint.copy(isDefault = true))
    }
}
