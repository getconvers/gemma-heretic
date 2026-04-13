package com.gemmaheretic.app.ui.navigation

sealed class NavRoutes(val route: String) {
    data object ChatList : NavRoutes("chat_list")
    data object Chat : NavRoutes("chat/{sessionId}") {
        fun withId(sessionId: Long) = "chat/$sessionId"
        const val ARG_SESSION_ID = "sessionId"
    }
    data object NewChat : NavRoutes("new_chat")
    data object Endpoints : NavRoutes("endpoints")
    data object Models : NavRoutes("models")
    data object Settings : NavRoutes("settings")
    data object About : NavRoutes("about")
}
