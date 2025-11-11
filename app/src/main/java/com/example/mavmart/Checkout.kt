package com.example.mavmart




import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderPlaced: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }
    var showCvc by remember { mutableStateOf(false) }

    var cardError by remember { mutableStateOf<String?>(null) }
    var expiryError by remember { mutableStateOf<String?>(null) }
    var cvcError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var valid = true

        if (cardNumber.filter { it.isDigit() }.length != 16) {
            cardError = "Card number must be 16 digits"
            valid = false
        } else cardError = null

        if (!isValidExpiry(expiry)) {
            expiryError = "Invalid expiry date (MM/YY)"
            valid = false
        } else expiryError = null

        if (cvc.length != 3 || cvc.any { !it.isDigit() }) {
            cvcError = "CVC must be 3 digits"
            valid = false
        } else cvcError = null

        return valid
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = cardNumber,
                onValueChange = { cardNumber = it },
                label = { Text("Card Number") },
                isError = cardError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { cardError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = expiry,
                onValueChange = { expiry = it },
                label = { Text("Expiry (MM/YY)") },
                isError = expiryError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { expiryError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cvc,
                onValueChange = { cvc = it },
                label = { Text("CVC") },
                isError = cvcError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = if (showCvc) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showCvc = !showCvc }) {
                        Text(if (showCvc) "Hide" else "Show")
                    }
                },
                supportingText = { cvcError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (validate()) {
                        focusManager.clearFocus()
                        onOrderPlaced()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Place Order")
            }
        }
    }
}

fun isValidExpiry(expiry: String): Boolean {
    val regex = Regex("^(0[1-9]|1[0-2])/[0-9]{2}$")
    if (!regex.matches(expiry)) return false

    val (monthStr, yearStr) = expiry.split("/")
    val month = monthStr.toInt()
    val year = "20$yearStr".toInt()

    val now = java.util.Calendar.getInstance()
    val currentYear = now.get(java.util.Calendar.YEAR)
    val currentMonth = now.get(java.util.Calendar.MONTH) + 1

    if (year < currentYear) return false
    if (year == currentYear && month < currentMonth) return false

    return true
}

