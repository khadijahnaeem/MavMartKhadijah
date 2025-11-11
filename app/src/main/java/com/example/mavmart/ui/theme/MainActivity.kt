package com.example.mavmart

import android.os.Bundle
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mavmart.ui.theme.MavMartTheme
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import com.example.mavmart.ui.theme.BluePrimary
import com.example.mavmart.ui.theme.OnPrimary
import com.example.mavmart.ui.theme.BackgroundOrange
import com.example.mavmart.ui.theme.OnBackground
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDark by rememberSaveable { mutableStateOf(false) }

            MavMartTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val nav = rememberNavController()

                    NavHost(navController = nav, startDestination = "login") {

                        // LOGIN SCREEN
                        composable("login") {
                            LoginScreen(
                                onUser = { nav.navigate("login/user") },
                                onAdmin = { nav.navigate("login/admin") },
                                onRegister = { nav.navigate("register") }
                            )
                        }

                        // CHECKOUT SCREEN
                        composable("checkout") {
                            CheckoutScreen(
                                onBack = { nav.popBackStack() },
                                onOrderPlaced = { nav.navigate("order_success") }
                            )
                        }

                        // ORDER SUCCESS CONFIRMATION SCREEN


                        // ORDER SUCCESS (Auto-Dismiss After 5 Seconds, Orange Theme)
                        composable("order_success") {

                            // Auto navigate back after 5 seconds
                            LaunchedEffect(Unit) {
                                kotlinx.coroutines.delay(5000)
                                nav.popBackStack() // closes success screen and returns to previous screen
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxSize(),
                                color = BackgroundOrange
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        shape = MaterialTheme.shapes.medium,
                                        colors = CardDefaults.cardColors(containerColor = OnPrimary),
                                        modifier = Modifier
                                            .padding(32.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Order Placed Successfully!",
                                                color = BluePrimary,
                                                style = MaterialTheme.typography.headlineSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }



                        // USER LOGIN
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

                        // ADMIN LOGIN
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

                        // REGISTER
                        composable("register") {
                            RegisterScreen(onBack = { nav.popBackStack() })
                        }

                        // HOME SCREEN
                        composable(
                            route = "home/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
                            HomeScreen(
                                nav = nav,
                                currentUserId = userId,
                                onLogout = {
                                    nav.navigate("login") {
                                        popUpTo(nav.graph.startDestinationId) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onOpenListing = { listingId ->
                                    nav.navigate("listing/$userId/$listingId")
                                },
                                isDark = isDark,
                                onToggleTheme = { isDark = !isDark }
                            )
                        }

                        // ADMIN DASHBOARD
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

                        // LISTING DETAILS
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
