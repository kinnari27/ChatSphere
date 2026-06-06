package com.chatsphere.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chatsphere.presentation.auth.LoginScreen
import com.chatsphere.presentation.auth.RegisterScreen
import com.chatsphere.presentation.chats.ChatListScreen
import com.chatsphere.presentation.chats.ChatScreen
import com.chatsphere.presentation.groups.CreateGroupScreen
import com.chatsphere.presentation.profile.ProfileScreen
import com.chatsphere.presentation.settings.SettingsScreen

@Composable
fun ChatSphereNavHost() {
    val navController = rememberNavController()
    val tabs = listOf(Screen.Chats, Screen.Groups, Screen.Profile, Screen.Settings)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBar = currentRoute in tabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onAuthenticated = {
                        navController.navigate(Screen.Chats.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                    },
                    onRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onAuthenticated = {
                        navController.navigate(Screen.Chats.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                    },
                    onLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.Chats.route) {
                ChatListScreen(onChatSelected = { navController.navigate("chat/$it") })
            }
            composable("chat/{conversationId}") {
                ChatScreen()
            }
            composable(Screen.Groups.route) { CreateGroupScreen() }
            composable(Screen.Profile.route) { ProfileScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

private sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Login : Screen("login", "Login", Icons.Outlined.Chat)
    data object Register : Screen("register", "Register", Icons.Outlined.Add)
    data object Chats : Screen("chats", "Chats", Icons.Outlined.Chat)
    data object Groups : Screen("groups", "Groups", Icons.Outlined.Add)
    data object Profile : Screen("profile", "Profile", Icons.Outlined.AccountCircle)
    data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)
}
