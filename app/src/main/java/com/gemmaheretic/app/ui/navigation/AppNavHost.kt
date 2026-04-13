package com.gemmaheretic.app.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.gemmaheretic.app.GemmaHereticApp
import com.gemmaheretic.app.ui.about.AboutScreen
import com.gemmaheretic.app.ui.chat.ChatScreen
import com.gemmaheretic.app.ui.chat.ChatViewModel
import com.gemmaheretic.app.ui.endpoints.EndpointsScreen
import com.gemmaheretic.app.ui.endpoints.EndpointsViewModel
import com.gemmaheretic.app.ui.history.HistoryScreen
import com.gemmaheretic.app.ui.history.HistoryViewModel
import com.gemmaheretic.app.ui.history.NewChatSheet
import com.gemmaheretic.app.ui.models.ModelsScreen
import com.gemmaheretic.app.ui.models.ModelsViewModel
import com.gemmaheretic.app.ui.settings.SettingsScreen
import com.gemmaheretic.app.ui.settings.SettingsViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    isDarkTheme: Boolean,
    onDarkModeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as GemmaHereticApp
    val container = app.container

    NavHost(
        navController = navController,
        startDestination = NavRoutes.ChatList.route
    ) {
        composable(NavRoutes.ChatList.route) {
            val viewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.Factory(
                    container.chatRepository,
                    container.endpointRepository,
                    container.preferences
                )
            )
            val uiState by viewModel.uiState.collectAsState()
            var showNewChat by remember { mutableStateOf(false) }

            HistoryScreen(
                viewModel = viewModel,
                onOpenChat = { sessionId ->
                    navController.navigate(NavRoutes.Chat.withId(sessionId))
                },
                onNewChat = { showNewChat = true },
                onNavigateToEndpoints = { navController.navigate(NavRoutes.Endpoints.route) },
                onNavigateToModels = { navController.navigate(NavRoutes.Models.route) },
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
                onNavigateToAbout = { navController.navigate(NavRoutes.About.route) }
            )

            if (showNewChat && uiState.endpoints.isNotEmpty()) {
                NewChatSheet(
                    endpoints = uiState.endpoints,
                    lastEndpointId = uiState.lastEndpointId,
                    lastModel = uiState.lastModel,
                    endpointRepository = container.endpointRepository,
                    onCreateChat = { endpointId, endpointName, modelName ->
                        viewModel.createNewSession(endpointId, endpointName, modelName)
                    },
                    onChatCreated = { sessionId ->
                        showNewChat = false
                        navController.navigate(NavRoutes.Chat.withId(sessionId))
                    },
                    onDismiss = { showNewChat = false }
                )
            } else if (showNewChat && uiState.endpoints.isEmpty()) {
                showNewChat = false
                navController.navigate(NavRoutes.Endpoints.route)
            }
        }

        composable(
            route = NavRoutes.Chat.route,
            arguments = listOf(navArgument(NavRoutes.Chat.ARG_SESSION_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong(NavRoutes.Chat.ARG_SESSION_ID) ?: return@composable
            val viewModel: ChatViewModel = viewModel(
                key = "chat_$sessionId",
                factory = ChatViewModel.Factory(
                    container.chatRepository,
                    container.endpointRepository,
                    container.preferences,
                    sessionId
                )
            )

            ChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
                isDarkTheme = isDarkTheme
            )
        }

        composable(NavRoutes.Endpoints.route) {
            val viewModel: EndpointsViewModel = viewModel(
                factory = EndpointsViewModel.Factory(container.endpointRepository)
            )
            EndpointsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Models.route) {
            val viewModel: ModelsViewModel = viewModel(
                factory = ModelsViewModel.Factory(
                    container.endpointRepository,
                    container.favoriteModelDao
                )
            )
            ModelsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(container.preferences)
            )
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onDarkModeChange = onDarkModeChange
            )
        }

        composable(NavRoutes.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
