package com.example.mavmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mavmart.ui.theme.MavMartTheme
import androidx.compose.foundation.Image

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MavMartTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF8F9FA) // background
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

                        // User login -> navigate to Home with userId
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



                        // Admin login -> admin dashboard
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
                            RegisterScreen(
                                onBack = { nav.popBackStack() }
                            )
                        }

                        // HOME with userId argument
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
                                }
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

                    }

                }
            }
        }
    }


}