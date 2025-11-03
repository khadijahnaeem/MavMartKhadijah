package com.example.mavmart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private enum class HomeTab { Listings, MyListings, Profile, Cart }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUserId: Long,
    onLogout: () -> Unit
) {
    val brandPrimary = Color(0xFF0A2647)
    var tab by remember { mutableStateOf(HomeTab.Listings) }   // Start on “Listings”
    var showCreate by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }

    var items by remember { mutableStateOf(emptyList<Listing>()) }

    // Load listings for the active tab
    LaunchedEffect(tab, currentUserId) {
        items = when (tab) {
            HomeTab.Listings   -> db.getAllListings()
            HomeTab.MyListings -> db.getListingsForSeller(currentUserId)
            else               -> emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (tab) {
                            HomeTab.Listings   -> "Listings"
                            HomeTab.MyListings -> "My Listings"
                            HomeTab.Profile    -> "Profile"
                            HomeTab.Cart       -> "Cart"
                        },
                        color = brandPrimary
                    )
                },
                actions = { TextButton(onClick = onLogout) { Text("Logout", color = brandPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Box {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == HomeTab.Listings,
                        onClick = { tab = HomeTab.Listings },
                        icon = { Icon(Icons.AutoMirrored.Outlined.ViewList, contentDescription = "Listings") },
                        label = { Text("Listings") }
                    )
                    NavigationBarItem(
                        selected = tab == HomeTab.MyListings,
                        onClick = { tab = HomeTab.MyListings },
                        icon = { Icon(Icons.AutoMirrored.Outlined.List,     contentDescription = "My Listings") },
                        label = { Text("My Listings") }
                    )

                    Spacer(Modifier.weight(1f)) // center gap for FAB

                    NavigationBarItem(
                        selected = tab == HomeTab.Profile,
                        onClick = { tab = HomeTab.Profile },
                        icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
                        label = { Text("Profile") }
                    )
                    NavigationBarItem(
                        selected = tab == HomeTab.Cart,
                        onClick = { tab = HomeTab.Cart },
                        icon = { Icon(Icons.Outlined.ShoppingCart, contentDescription = "Cart") },
                        label = { Text("Cart") }
                    )
                }

                // Centered FAB docked over the bar for listing tabs
                if (tab == HomeTab.Listings || tab == HomeTab.MyListings) {
                    FloatingActionButton(
                        onClick = { showCreate = true },
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Create Listing")
                    }
                }
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner).fillMaxSize()) {
            when (tab) {
                HomeTab.Listings,
                HomeTab.MyListings -> ListingsFeed(items, brandPrimary)

                HomeTab.Profile -> ProfileScreen(currentUserId)
                HomeTab.Cart -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Cart (coming soon)", color = Color.Black)
                    }
                }
            }
        }
    }

    // Create listing dialog (only for listing tabs)
    if (showCreate) {
        AddListingDialog(
            onDismiss = { showCreate = false },
            onSave = { title, description, category, priceDollars, condition ->
                val priceCents = (priceDollars.toDoubleOrNull()?.times(100))?.toInt() ?: 0
                val newListing = Listing(
                    id = 0L,
                    sellerId = currentUserId,
                    title = title.trim(),
                    description = description.ifBlank { null },
                    category = category,
                    priceCents = priceCents,
                    condition = condition,
                    photos = emptyList(),
                    status = ListingStatus.ACTIVE,
                    createdAt = System.currentTimeMillis()
                )
                db.insertListing(newListing)
                // Refresh whichever list we’re on
                items = if (tab == HomeTab.MyListings) {
                    db.getListingsForSeller(currentUserId)
                } else {
                    db.getAllListings()
                }
                showCreate = false
            }
        )
    }
}

/* ================== Profile ================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(currentUserId: Long) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }

    var user by remember { mutableStateOf<User?>(null) }
    var showEdit by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        user = db.getUserById(currentUserId)
    }

    Scaffold { inner ->
        Box(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(20.dp)
        ) {
            if (user == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("User not found", color = Color.Black)
                }
            } else {
                val u = user!!
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("USER ID: ${u.id}",  color = Color.Black, style = MaterialTheme.typography.titleMedium)
                    Text("NAME: ${u.first} ${u.last}", color = Color.Black, style = MaterialTheme.typography.titleMedium)
                    Text("EMAIL: ${u.email}", color = Color.Black, style = MaterialTheme.typography.titleMedium)
                    Text("ROLE: ${u.role.name}", color = Color.Black, style = MaterialTheme.typography.titleMedium)

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { showEdit = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("EDIT PROFILE")
                    }
                }
            }
        }
    }

    if (showEdit && user != null) {
        EditProfileDialog(
            user = user!!,
            onDismiss = { showEdit = false },
            onSave = { updated ->
                // use the db captured from the composable scope
                db.updateUser(updated)
                user = db.getUserById(currentUserId)
                showEdit = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var first by remember { mutableStateOf(user.first) }
    var last by remember { mutableStateOf(user.last) }
    var email by remember { mutableStateOf(user.email) }
    var password by remember { mutableStateOf(user.password) } 

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = first, onValueChange = { first = it },
                    label = { Text("First name", color = Color.Black) },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = last, onValueChange = { last = it },
                    label = { Text("Last name", color = Color.Black) },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email", color = Color.Black) },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password", color = Color.Black) },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        user.copy(
                            first = first.trim(),
                            last = last.trim(),
                            email = email.trim().lowercase(),
                            password = password
                        )
                    )
                },
                enabled = first.isNotBlank() && last.isNotBlank() && email.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/* ================== Listings feed ================== */

@Composable
private fun ListingsFeed(listings: List<Listing>, brandPrimary: Color) {
    if (listings.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No listings yet.", color = Color.Black)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listings) { item ->
                ElevatedCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp)
                            .padding(16.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = Color.Black)

                        item.description?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                        }

                        Spacer(Modifier.weight(1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                formatCents(item.priceCents),
                                style = MaterialTheme.typography.titleSmall,
                                color = brandPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ================== Create listing dialog + pickers ================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddListingDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: ListingCategory,
        priceDollars: String,
        condition: ItemCondition
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    var category by remember { mutableStateOf(ListingCategory.GENERAL) }
    var condition by remember { mutableStateOf(ItemCondition.GOOD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Listing", color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title", color = Color.Black) },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)", color = Color.Black) },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )

                CategoryPicker(value = category, onChange = { category = it })
                ConditionPicker(value = condition, onChange = { condition = it })

                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Price (e.g., 12.34)", color = Color.Black) },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, description, category, price, condition) },
                enabled = title.isNotBlank() && price.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(value: ListingCategory, onChange: (ListingCategory) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value.label, 
            onValueChange = {},
            readOnly = true,
            label = { Text("Category", color = Color.Black) },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ListingCategory.entries.forEach  { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label, color = Color.Black) },
                    onClick = { onChange(opt); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionPicker(value: ItemCondition, onChange: (ItemCondition) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value.label, 
            onValueChange = {},
            readOnly = true,
            label = { Text("Condition", color = Color.Black) },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ItemCondition.entries.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label, color = Color.Black) },
                    onClick = { onChange(opt); expanded = false }
                )
            }
        }
    }
}

/* ================ helper ================ */

private fun formatCents(cents: Int): String {
    val dollars = cents / 100
    val pennies = cents % 100
    return "$$dollars.${pennies.toString().padStart(2, '0')}"

}
