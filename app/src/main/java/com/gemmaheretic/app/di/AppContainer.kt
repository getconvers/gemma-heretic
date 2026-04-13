package com.gemmaheretic.app.di

import android.content.Context
import com.gemmaheretic.app.data.local.database.AppDatabase
import com.gemmaheretic.app.data.local.preferences.AppPreferences
import com.gemmaheretic.app.data.repository.ChatRepository
import com.gemmaheretic.app.data.repository.EndpointRepository

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    val preferences = AppPreferences(context)

    val endpointRepository = EndpointRepository(
        endpointDao = database.endpointDao()
    )

    val chatRepository = ChatRepository(
        chatSessionDao = database.chatSessionDao(),
        chatMessageDao = database.chatMessageDao()
    )

    val favoriteModelDao = database.favoriteModelDao()
}
