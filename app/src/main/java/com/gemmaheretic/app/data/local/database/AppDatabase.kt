package com.gemmaheretic.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gemmaheretic.app.data.local.dao.*
import com.gemmaheretic.app.data.local.entity.*

@Database(
    entities = [
        EndpointEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        FavoriteModelEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun endpointDao(): EndpointDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun favoriteModelDao(): FavoriteModelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gemma_heretic.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
