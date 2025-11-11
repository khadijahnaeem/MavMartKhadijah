package com.example.mavmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mavmart.ui.theme.MavMartTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MavMartTheme {
                val brandPrimary = Color(0xFF0A2647)    // deep navy blue
                val backgroundColor = Color(0xFFF8F9FA) // very light gray background

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = backgroundColor,
                    contentColor = brandPrimary
                ) {
                    val nav = rememberNavController()

                    NavHost(navController = nav, startDestination = "login") {
                        composable("login") {
                            LoginScreen(
                                onUser = { nav.navigate("login/auth") },
                                onAdmin = { nav.navigate("login/auth") },
                                onRegister = { nav.navigate("register") }
                            )
                        }
                        // Single auth screen handles role at button click (User/Admin)
                        composable("login/auth") {
                            AuthLoginScreen(
                                onBack = { nav.popBackStack() },
                                onSubmit = { role, email, password ->
                                    // TODO: route based on role (Role.User / Role.Admin)
                                    nav.popBackStack()
                                }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onBack = { nav.popBackStack() },
                                onSubmit = { _, _, _, _, _ -> nav.popBackStack() } // Register creates a User inside RegisterScreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onUser: () -> Unit,
    onAdmin: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandPrimary = Color(0xFF0A2647)    // deep navy blue
    val brandSecondary = Color(0xFF144272)  // soft dark blue
    val brandAccent = Color(0xFFFFF3D9)     // pale cream accent
    val brandOrange = Color(0xFFFF8C00)     // vivid orange
    val backgroundColor = Color(0xFFF8F9FA) // light neutral background

    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = brandPrimary
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = brandOrange
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(brandAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MM",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = brandPrimary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "MavMart",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Choose how you want to sign in",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Center card area for login role selection (User/Admin only)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.15.sp
                        ),
                        color = brandPrimary
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onUser,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        Text("Login as User", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandPrimary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) {
                        Text("Login as Admin", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Tip: Use your UTA email if you are a current student.",
                        style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.05.sp),
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom register button
            Button(
                onClick = onRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .heightIn(min = 48.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = brandOrange,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Text("Register", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    MavMartTheme { LoginScreen({}, {}, {}) }
}
