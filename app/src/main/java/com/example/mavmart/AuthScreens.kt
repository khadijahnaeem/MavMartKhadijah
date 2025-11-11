package com.example.mavmart

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/* ------------ Landing screen (three buttons) ------------ */

@Composable
fun LoginScreen(
    onUser: () -> Unit,
    onAdmin: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxSize(),
        color = cs.surface,            // page: white (light) / black (dark)
        contentColor = cs.onSurface
    ) {
        Column(Modifier.fillMaxSize()) {

            // Banner: orange (light) / navy (dark)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = cs.background,
                contentColor = cs.onBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LogoImage(Modifier.size(52.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("MavMart", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                        Text("Choose how you want to sign in", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cs.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onUser,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,   // navy (light) / orange (dark)
                            contentColor = cs.onPrimary
                        )
                    ) { Text("Login as User", fontSize = 16.sp, fontWeight = FontWeight.Medium) }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary
                        )
                    ) { Text("Login as Admin", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
                }
            }

            Spacer(Modifier.weight(1f))

            Surface(color = cs.background) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onRegister,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,      // navy (light) / orange (dark)
                            contentColor = cs.onPrimary,
                            disabledContainerColor = cs.primary.copy(alpha = .30f),
                            disabledContentColor = cs.onPrimary.copy(alpha = .65f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) { Text("Register", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
fun LogoImage(modifier: Modifier = Modifier, contentDescription: String? = null) {
    Image(
        painter = painterResource(R.drawable.logo_1),
        contentDescription = contentDescription,
        modifier = modifier
    )
}

/* Keep last email typed so we can fetch the user on success */
private var RoleLoginForm_lastEmail: String? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleLoginForm(
    title: String,
    onBack: () -> Unit,
    validateEmail: ((String) -> String?)? = null,
    authenticate: (email: String, password: String) -> Boolean,
    onSuccess: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = cs.onPrimary,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = cs.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.primary)
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            // Orange banner with navy Login button – same pattern as Register
            Surface(color = cs.background) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            val msg = validateEmail?.invoke(email)
                            if (msg != null) {
                                scope.launch { snack.showSnackbar(msg, withDismissAction = true) }
                            } else if (authenticate(email.trim().lowercase(), password)) {
                                onSuccess()
                            } else {
                                scope.launch {
                                    snack.showSnackbar("Invalid email or password.", withDismissAction = true)
                                }
                            }
                        },
                        enabled = email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor   = cs.onPrimary,
                            disabledContainerColor = cs.primary.copy(alpha = .30f),
                            disabledContentColor   = cs.onPrimary.copy(alpha = .65f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) { Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }


    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(cs.surface)  // page: white / black
        ) {
            // Banner: orange / navy
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = cs.background,
                contentColor = cs.onBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LogoImage(Modifier.size(52.dp))
                    Spacer(Modifier.width(14.dp))
                    Column { Text("MavMart", fontSize = 26.sp, fontWeight = FontWeight.SemiBold) }
                }
            }

            Spacer(Modifier.height(20.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.15.sp
                        ),
                        color = cs.onSurface
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            RoleLoginForm_lastEmail = it.trim().lowercase()
                        },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cs.primary,
                            focusedLabelColor = cs.primary,
                            unfocusedBorderColor = cs.outline,
                            cursorColor = cs.primary
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    imageVector = if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPass) "Hide password" else "Show password",
                                    tint = cs.primary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cs.primary,
                            focusedLabelColor = cs.primary,
                            unfocusedBorderColor = cs.outline,
                            cursorColor = cs.primary
                        )
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

/* ------------ User / Admin wrappers ------------ */

@Composable
fun UserLoginScreen(
    onBack: () -> Unit,
    onSuccess: (Long) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }

    RoleLoginForm(
        title = "User Login",
        onBack = onBack,
        validateEmail = null,
        authenticate = { email, password ->
            db.validateLogin(email, password, expectedRole = Role.User) != null
        },
        onSuccess = {
            val email = RoleLoginForm_lastEmail ?: return@RoleLoginForm onBack()
            val id = db.findUserByEmail(email)?.id
            if (id != null) onSuccess(id) else onBack()
        }
    )
}

@Composable
fun AdminLoginScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    RoleLoginForm(
        title = "Admin Login",
        onBack = onBack,
        validateEmail = { raw ->
            val e = raw.trim().lowercase()
            if (e.endsWith("@mavmart.com")) null else "Admin email should end with @mavmart.com"
        },
        authenticate = { _, _ -> true },
        onSuccess = onSuccess
    )
}

/* ------------ Register (writes to SQLite) ------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }

    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register", color = cs.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = cs.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.primary)
            )
        },
        bottomBar = {
            // Sticky bar: orange (light) / navy (dark), white text for readability
            Button(
                onClick = {
                    val user = User(
                        id = 0L,
                        first = first.trim(),
                        last = last.trim(),
                        email = email.trim().lowercase(),
                        password = password,
                        role = Role.User
                    )
                    db.insertUser(user)
                    onBack()
                },
                enabled = first.isNotBlank() && last.isNotBlank() &&
                        email.isNotBlank() && password.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary
                )
            ) { Text("Create Account") }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(cs.surface)
        ) {
            // Banner block (orange / navy)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = cs.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LogoImage(Modifier.size(52.dp))
                    Spacer(Modifier.width(14.dp))
                    Column { Text("MavMart", fontSize = 26.sp, fontWeight = FontWeight.SemiBold) }
                }
            }

            Spacer(Modifier.height(20.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create your account",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.15.sp
                        ),
                        color = cs.primary
                    )

                    OutlinedTextField(
                        value = first, onValueChange = { first = it },
                        label = { Text("First Name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cs.primary,
                            focusedLabelColor = cs.primary,
                            unfocusedBorderColor = cs.outline,
                            cursorColor = cs.primary
                        )
                    )

                    OutlinedTextField(
                        value = last, onValueChange = { last = it },
                        label = { Text("Last Name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cs.primary,
                            focusedLabelColor = cs.primary,
                            unfocusedBorderColor = cs.outline,
                            cursorColor = cs.primary
                        )
                    )

                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cs.primary,
                            focusedLabelColor = cs.primary,
                            unfocusedBorderColor = cs.outline,
                            cursorColor = cs.primary
                        )
                    )

                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cs.primary,
                            focusedLabelColor = cs.primary,
                            unfocusedBorderColor = cs.outline,
                            cursorColor = cs.primary
                        )
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}