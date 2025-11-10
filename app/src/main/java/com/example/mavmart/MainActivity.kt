package com.example.mavmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mavmart.ui.theme.MavMartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDark by rememberSaveable { mutableStateOf(false) }

            MavMartTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    val nav = rememberNavController()

                    NavHost(navController = nav, startDestination = "login") {
                        composable("login") {
                            LoginScreen(
                                onUser = { nav.navigate("login/user") },
                                onAdmin = { nav.navigate("login/admin") },
                                onRegister = { nav.navigate("register") }
                            )
                        }

                        composable("login/user") {
                            UserLoginScreen(
                                onBack = { nav.popBackStack() },
                                onSuccess = { userId ->
                                    nav.navigate("home/$userId") {
                                        popUpTo(nav.graph.startDestinationId) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable("login/admin") {
                            AdminLoginScreen(
                                onBack = { nav.popBackStack() },
                                onSuccess = {
                                    nav.navigate("admin") {
                                        popUpTo(nav.graph.startDestinationId) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable("register") {
                            RegisterScreen(onBack = { nav.popBackStack() })
                        }

                        composable(
                            route = "home/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
                            HomeScreen(
                                currentUserId = userId,
                                onLogout = {
                                    nav.navigate("login") {
                                        popUpTo(nav.graph.startDestinationId) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onOpenListing = { listingId -> nav.navigate("listing/$userId/$listingId") },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                        }

                        composable("admin") {
                            AdminDashboardScreen(
                                onLogout = {
                                    nav.navigate("login") {
                                        popUpTo(nav.graph.startDestinationId) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(
                            route = "listing/{userId}/{listingId}",
                            arguments = listOf(
                                navArgument("userId") { type = NavType.LongType },
                                navArgument("listingId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
                            val listingId = backStackEntry.arguments?.getLong("listingId") ?: 0L
                            ListingDetailsScreen(
                                currentUserId = userId,
                                listingId = listingId,
                                onBack = { nav.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}