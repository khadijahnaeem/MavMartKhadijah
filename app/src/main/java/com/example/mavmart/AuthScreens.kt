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
import androidx.compose.ui.graphics.Color
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
    val brandPrimary = Color(0xFF0A2647)
    val brandAccent  = Color(0xFFFFF3D9)
    val brandOrange  = Color(0xFFFF8C00)
    val background   = Color(0xFFF8F9FA)

    Surface(modifier = modifier.fillMaxSize(), color = background, contentColor = brandPrimary) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    // USE THE LOGO HERE
                    LogoImage(
                        message = "hi",
                        from = "login-banner",
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("MavMart", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Choose how you want to sign in", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
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
                        colors = ButtonDefaults.buttonColors(containerColor = brandPrimary, contentColor = Color.White)
                    ) { Text("Login as User", fontSize = 16.sp, fontWeight = FontWeight.Medium) }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandPrimary, contentColor = Color.White)
                    ) { Text("Login as Admin", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00), contentColor = Color.White)
            ) { Text("Register", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

// logo composable
@Composable
fun LogoImage(message: String, from: String, modifier: Modifier = Modifier) {
    val image = painterResource(R.drawable.logo_1)
    Image(
        painter = image,
        contentDescription = null,
        modifier = modifier
    )
}

/* Keep last email typed so we can fetch the user object on success */
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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    val brandPrimary = Color(0xFF0A2647)
    val brandAccent  = Color(0xFFFFF3D9)
    val brandOrange  = Color(0xFFFF8C00)
    val background   = Color(0xFFF8F9FA)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = brandPrimary,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = brandPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Surface(tonalElevation = 1.dp, shadowElevation = 2.dp, color = Color.Transparent) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            val msg = validateEmail?.invoke(email)
                            if (msg != null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = msg,
                                        actionLabel = "Dismiss",
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            } else {
                                val ok = authenticate(email.trim().lowercase(), password)
                                if (ok) {
                                    onSuccess()
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Invalid email or password.",
                                            actionLabel = "Dismiss",
                                            withDismissAction = true,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            }
                        },
                        enabled = email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandPrimary, contentColor = Color.White),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                    ) { Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(background)
        ) {
            // Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = brandPrimary
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // USE THE LOGO HERE
                    LogoImage(
                        message = "hi",
                        from = "role-login",
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column { Text("MavMart", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
                }
            }

            Spacer(Modifier.height(20.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.15.sp
                        ),
                        color = brandPrimary
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
                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandPrimary,
                            focusedLabelColor = brandPrimary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    imageVector = if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPass) "Hide password" else "Show password",
                                    tint = brandPrimary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandPrimary,
                            focusedLabelColor = brandPrimary,
                            unfocusedBorderColor = Color.LightGray
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
        // Only rule: must end with @mavmart.com (any password is fine)
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
    val brandPrimary = Color(0xFF0A2647)
    val brandAccent  = Color(0xFFFFF3D9)
    val brandOrange  = Color(0xFFFF8C00)
    val background   = Color(0xFFF8F9FA)

    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }

    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register", color = brandPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = brandPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
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
                colors = ButtonDefaults.buttonColors(containerColor = brandPrimary, contentColor = Color.White)
            ) { Text("Create Account") }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(background)
        ) {
            // Banner (same vibe as login)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = brandPrimary
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // USE THE LOGO HERE
                    LogoImage(
                        message = "hi",
                        from = "register-banner",
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column { Text("MavMart", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Card (mirrors login)
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create your account",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.15.sp
                        ),
                        color = brandPrimary
                    )

                    OutlinedTextField(
                        value = first, onValueChange = { first = it },
                        label = { Text("First Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandPrimary,
                            focusedLabelColor = brandPrimary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    OutlinedTextField(
                        value = last, onValueChange = { last = it },
                        label = { Text("Last Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandPrimary,
                            focusedLabelColor = brandPrimary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandPrimary,
                            focusedLabelColor = brandPrimary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        textStyle = LocalTextStyle.current.copy(color = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = brandPrimary,
                            focusedLabelColor = brandPrimary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}