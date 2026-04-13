package com.gemmaheretic.app.data.local.dao

import androidx.room.*
import com.gemmaheretic.app.data.local.entity.FavoriteModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteModelDao {
    @Query("SELECT * FROM favorite_models ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteModelEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_models WHERE name = :name)")
    suspend fun isFavorite(name: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(model: FavoriteModelEntity)

    @Query("DELETE FROM favorite_models WHERE name = :name")
    suspend fun removeFavorite(name: String)
}
