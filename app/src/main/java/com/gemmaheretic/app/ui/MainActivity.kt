package com.gemmaheretic.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.gemmaheretic.app.GemmaHereticApp
import com.gemmaheretic.app.ui.navigation.AppNavHost
import com.gemmaheretic.app.ui.theme.GemmaHereticTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GemmaHereticApp
        // Read initial dark mode preference synchronously (fast DataStore read)
        val initialDarkMode = runBlocking {
            try { app.container.preferences.darkMode.first() } catch (_: Exception) { "dark" }
        }

        setContent {
            var darkModeSetting by remember { mutableStateOf(initialDarkMode) }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (darkModeSetting) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }

            GemmaHereticTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    isDarkTheme = isDark,
                    onDarkModeChange = { darkModeSetting = it }
                )
            }
        }
    }
}
