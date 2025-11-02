package com.example.mavmart

import androidx.compose.foundation.background
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
import androidx.compose.runtime.snapshots.SnapshotStateList // NEW
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private enum class HomeTab { Listings, MyListings, Profile, Cart }

/* ================== Cart data + repository ================== */ // NEW

// NEW
private data class CartItem(
    val id: Long,
    val listing: Listing,
    var quantity: Int
)

// NEW
private object CartRepository {
    private val carts = mutableMapOf<Long, SnapshotStateList<CartItem>>()
    private var nextId = 1L

    private fun cartFor(userId: Long): SnapshotStateList<CartItem> {
        return carts.getOrPut(userId) { mutableStateListOf() }
    }

    fun getItems(userId: Long): SnapshotStateList<CartItem> = cartFor(userId)

    fun add(userId: Long, listing: Listing, qty: Int = 1) {
        val cart = cartFor(userId)
        val existing = cart.firstOrNull { it.listing.id == listing.id }
        if (existing != null) {
            existing.quantity += qty.coerceAtLeast(1)
            val idx = cart.indexOf(existing)
            if (idx >= 0) cart[idx] = cart[idx] // nudge for recomposition
        } else {
            cart.add(
                CartItem(
                    id = nextId++,
                    listing = listing,
                    quantity = qty.coerceAtLeast(1)
                )
            )
        }
    }

    fun updateQuantity(userId: Long, cartItemId: Long, quantity: Int) {
        val cart = cartFor(userId)
        val idx = cart.indexOfFirst { it.id == cartItemId }
        if (idx >= 0) {
            if (quantity <= 0) {
                cart.removeAt(idx)
            } else {
                cart[idx] = cart[idx].copy(quantity = quantity)
            }
        }
    }

    fun remove(userId: Long, cartItemId: Long) {
        val cart = cartFor(userId)
        cart.removeAll { it.id == cartItemId }
    }

    fun clear(userId: Long) {
        cartFor(userId).clear()
    }

    fun totalCents(userId: Long): Int {
        return cartFor(userId).sumOf { it.listing.priceCents * it.quantity }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentUserId: Long,
    onLogout: () -> Unit
) {
    val brandPrimary = Color(0xFF0A2647)
    val brandOrange  = Color(0xFFFF8C00)
    val background   = Color(0xFFF8F9FA)

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
                actions = { TextButton(onClick = onLogout, colors = ButtonDefaults.textButtonColors(contentColor = brandPrimary)) { Text("Logout") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Box {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = tab == HomeTab.Listings,
                        onClick = { tab = HomeTab.Listings },
                        icon = { Icon(Icons.AutoMirrored.Outlined.ViewList, contentDescription = "Listings") },
                        label = { Text("Listings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = brandPrimary,
                            selectedTextColor = brandPrimary
                        )
                    )
                    NavigationBarItem(
                        selected = tab == HomeTab.MyListings,
                        onClick = { tab = HomeTab.MyListings },
                        icon = { Icon(Icons.AutoMirrored.Outlined.List,     contentDescription = "My Listings") },
                        label = { Text("My Listings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = brandPrimary,
                            selectedTextColor = brandPrimary
                        )
                    )

                    Spacer(Modifier.weight(1f)) // center gap for FAB

                    NavigationBarItem(
                        selected = tab == HomeTab.Profile,
                        onClick = { tab = HomeTab.Profile },
                        icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = brandPrimary,
                            selectedTextColor = brandPrimary
                        )
                    )
                    NavigationBarItem(
                        selected = tab == HomeTab.Cart,
                        onClick = { tab = HomeTab.Cart },
                        icon = { Icon(Icons.Outlined.ShoppingCart, contentDescription = "Cart") },
                        label = { Text("Cart") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = brandPrimary,
                            selectedTextColor = brandPrimary
                        )
                    )
                }

                // Centered FAB docked over the bar for listing tabs
                if (tab == HomeTab.Listings || tab == HomeTab.MyListings) {
                    FloatingActionButton(
                        onClick = { showCreate = true },
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = brandPrimary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Create Listing")
                    }
                }
            }
        }
    ) { inner ->
        Box(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .background(background)
        ) {
            when (tab) {
                HomeTab.Listings -> {
                    ListingsFeed( // CHANGED
                        listings = items,
                        brandPrimary = brandPrimary,
                        showAddToCart = true, // NEW
                        onAddToCart = { listing -> CartRepository.add(currentUserId, listing, 1) } // NEW
                    )
                }
                HomeTab.MyListings -> {
                    ListingsFeed( // CHANGED
                        listings = items,
                        brandPrimary = brandPrimary,
                        showAddToCart = false, // NEW
                        onAddToCart = {} // NEW
                    )
                }

                HomeTab.Profile -> ProfileScreen(currentUserId, brandPrimary, background)

                HomeTab.Cart -> {
                    CartScreen( // NEW
                        currentUserId = currentUserId,
                        brandPrimary = brandPrimary,
                        background = background
                    )
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
            },
            brandPrimary = brandPrimary
        )
    }
}

/* ================== Profile ================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(currentUserId: Long, brandPrimary: Color = Color(0xFF0A2647), background: Color = Color(0xFFF8F9FA)) {
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
                .background(background)
                .padding(20.dp)
        ) {
            if (user == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("User not found", color = Color.Black)
                }
            } else {
                val u = user!!
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("USER ID: ${u.id}",  color = brandPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("NAME: ${u.first} ${u.last}", color = brandPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("EMAIL: ${u.email}", color = brandPrimary, style = MaterialTheme.typography.titleMedium)
                    Text("ROLE: ${u.role.name}", color = brandPrimary, style = MaterialTheme.typography.titleMedium)

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showEdit = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = brandPrimary, contentColor = Color.White)
                    ) {
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
                db.updateUser(updated)
                user = db.getUserById(currentUserId)
                showEdit = false
            },
            brandPrimary = brandPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit,
    brandPrimary: Color = Color(0xFF0A2647)
) {
    var first by remember { mutableStateOf(user.first) }
    var last by remember { mutableStateOf(user.last) }
    var email by remember { mutableStateOf(user.email) }
    var password by remember { mutableStateOf(user.password) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", color = brandPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = first, onValueChange = { first = it },
                    label = { Text("First name") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandPrimary,
                        focusedLabelColor = brandPrimary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                OutlinedTextField(
                    value = last, onValueChange = { last = it },
                    label = { Text("Last name") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandPrimary,
                        focusedLabelColor = brandPrimary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandPrimary,
                        focusedLabelColor = brandPrimary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandPrimary,
                        focusedLabelColor = brandPrimary,
                        unfocusedBorderColor = Color.LightGray
                    )
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
                enabled = first.isNotBlank() && last.isNotBlank() && email.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = brandPrimary)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = brandPrimary)
            ) { Text("Cancel") }
        }
    )
}

/* ================== Listings feed ================== */

// CHANGED signature: added showAddToCart and onAddToCart
@Composable
private fun ListingsFeed(
    listings: List<Listing>,
    brandPrimary: Color,
    showAddToCart: Boolean,                 // NEW
    onAddToCart: (Listing) -> Unit          // NEW
) {
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
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp)
                            .padding(16.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = brandPrimary)

                        item.description?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                formatCents(item.priceCents),
                                style = MaterialTheme.typography.titleSmall,
                                color = brandPrimary
                            )

                            if (showAddToCart) {
                                Button( // NEW
                                    onClick = { onAddToCart(item) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = brandPrimary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Outlined.ShoppingCart, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("ADD TO CART")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ================== Cart screen ================== */ // NEW

// NEW
@Composable
private fun CartScreen(
    currentUserId: Long,
    brandPrimary: Color = Color(0xFF0A2647),
    background: Color = Color(0xFFF8F9FA)
) {
    val cartItems = remember(currentUserId) { CartRepository.getItems(currentUserId) }
    val totalCents = cartItems.sumOf { it.listing.priceCents * it.quantity }

    if (cartItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your cart is empty.", color = Color.Black)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cartItems, key = { it.id }) { cartItem ->
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(cartItem.listing.title, style = MaterialTheme.typography.titleMedium, color = brandPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(formatCents(cartItem.listing.priceCents), color = Color.Black)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val newQty = (cartItem.quantity - 1).coerceAtLeast(0)
                                    CartRepository.updateQuantity(currentUserId, cartItem.id, newQty)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = brandPrimary)
                            ) { Text("-") }

                            Text("${cartItem.quantity}", color = Color.Black, style = MaterialTheme.typography.titleSmall)

                            Button(
                                onClick = {
                                    CartRepository.updateQuantity(currentUserId, cartItem.id, cartItem.quantity + 1)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = brandPrimary, contentColor = Color.White)
                            ) { Text("+") }

                            IconButton(
                                onClick = { CartRepository.remove(currentUserId, cartItem.id) }
                            ) {
                                Icon(Icons.Outlined.ShoppingCart, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        }

        Surface(
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
            color = Color.White
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", color = brandPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(formatCents(totalCents), color = brandPrimary, style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { CartRepository.clear(currentUserId) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = brandPrimary)
                    ) { Text("CLEAR") }

                    Button(
                        onClick = {
                            // Placeholder checkout action.
                            CartRepository.clear(currentUserId)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = brandPrimary, contentColor = Color.White)
                    ) { Text("CHECK OUT") }
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
    ) -> Unit,
    brandPrimary: Color = Color(0xFF0A2647)
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    var category by remember { mutableStateOf(ListingCategory.GENERAL) }
    var condition by remember { mutableStateOf(ItemCondition.GOOD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Listing", color = brandPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandPrimary,
                        focusedLabelColor = brandPrimary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandPrimary,
                        focusedLabelColor = brandPrimary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                CategoryPicker(value = category, onChange = { category = it }, brandPrimary = brandPrimary)
                ConditionPicker(value = condition, onChange = { condition = it }, brandPrimary = brandPrimary)

                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Price (e.g., 12.34)") },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = brandPrimary,
                        focusedLabelColor = brandPrimary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, description, category, price, condition) },
                enabled = title.isNotBlank() && price.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = brandPrimary)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = brandPrimary)
            ) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(value: ListingCategory, onChange: (ListingCategory) -> Unit, brandPrimary: Color = Color(0xFF0A2647)) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = brandPrimary,
                focusedLabelColor = brandPrimary,
                unfocusedBorderColor = Color.LightGray
            )
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
private fun ConditionPicker(value: ItemCondition, onChange: (ItemCondition) -> Unit, brandPrimary: Color = Color(0xFF0A2647)) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Condition") },
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = brandPrimary,
                focusedLabelColor = brandPrimary,
                unfocusedBorderColor = Color.LightGray
            )
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
