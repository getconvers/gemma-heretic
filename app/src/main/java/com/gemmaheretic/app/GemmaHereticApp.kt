package com.gemmaheretic.app

import android.app.Application
import com.gemmaheretic.app.di.AppContainer

class GemmaHereticApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
