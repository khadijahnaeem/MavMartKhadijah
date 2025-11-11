package com.example.mavmart

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.Locale

private enum class HomeTab { Listings, MyListings, Profile, Cart }

/* ================== Cart data + repository ================== */
private data class CartItem(
    val id: Long,
    val listing: Listing,
    var quantity: Int
)

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
            if (idx >= 0) cart[idx] = cart[idx]
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
    onLogout: () -> Unit,
    onOpenListing: (Long) -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    var tab by remember { mutableStateOf(HomeTab.Listings) }
    var showCreate by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }

    var items by remember { mutableStateOf(emptyList<Listing>()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(tab, currentUserId) {
        items = when (tab) {
            HomeTab.Listings   -> db.getAllListingsVisible()
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
                        color = cs.primary
                    )
                },
                actions = {
                    if (tab == HomeTab.Listings) {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        Spacer(Modifier.width(4.dp))
                    }

                    if (tab == HomeTab.Profile) {
                        TextButton(
                            onClick = onToggleTheme,
                            colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = if (isDark) "Switch to light" else "Switch to dark"
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (isDark) "Light Mode" else "Dark Mode")
                        }

                        Spacer(Modifier.width(8.dp))

                        TextButton(
                            onClick = onLogout,
                            colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
                        ) {
                            Text("Logout")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Box {
                NavigationBar(containerColor = cs.surface) {
                    NavigationBarItem(
                        selected = tab == HomeTab.Listings,
                        onClick = { tab = HomeTab.Listings },
                        icon = { Icon(Icons.AutoMirrored.Outlined.ViewList, contentDescription = "Listings") },
                        label = { Text("Listings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = cs.primary,
                            selectedTextColor = cs.primary
                        )
                    )
                    NavigationBarItem(
                        selected = tab == HomeTab.MyListings,
                        onClick = { tab = HomeTab.MyListings },
                        icon = { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "My Listings") },
                        label = { Text("My Listings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = cs.primary,
                            selectedTextColor = cs.primary
                        )
                    )

                    Spacer(Modifier.weight(1f))

                    NavigationBarItem(
                        selected = tab == HomeTab.Profile,
                        onClick = { tab = HomeTab.Profile },
                        icon = { Icon(Icons.Outlined.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = cs.primary,
                            selectedTextColor = cs.primary
                        )
                    )
                    NavigationBarItem(
                        selected = tab == HomeTab.Cart,
                        onClick = { tab = HomeTab.Cart },
                        icon = { Icon(Icons.Outlined.ShoppingCart, contentDescription = "Cart") },
                        label = { Text("Cart") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = cs.primary,
                            selectedTextColor = cs.primary
                        )
                    )
                }

                if (tab == HomeTab.Listings || tab == HomeTab.MyListings) {
                    FloatingActionButton(
                        onClick = { showCreate = true },
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = cs.primary,
                        contentColor = cs.onPrimary
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Create Listing")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .background(cs.background)
        ) {
            when (tab) {
                HomeTab.Listings -> {
                    ListingsFeed(
                        listings = items,
                        showAddToCart = true,
                        currentUserId = currentUserId,
                        onAddToCart = { listing ->
                            CartRepository.add(currentUserId, listing, 1)
                            scope.launch { snackbarHostState.showSnackbar("Added to cart", withDismissAction = true) }
                        },
                        onOpenListing = onOpenListing
                    )
                }
                HomeTab.MyListings -> {
                    ListingsFeed(
                        listings = items,
                        showAddToCart = false,
                        currentUserId = currentUserId,
                        onAddToCart = {},
                        onOpenListing = onOpenListing
                    )
                }
                HomeTab.Profile ->
                    ProfileScreen(
                        currentUserId = currentUserId
                    )
                HomeTab.Cart -> {
                    CartScreen(
                        currentUserId = currentUserId
                    )
                }
            }
        }
    }

    if (showCreate) {
        AddListingDialog(
            onDismiss = { showCreate = false },
            onSave = { title, description, category, priceDollars, condition, photos ->
                val priceCents = (priceDollars.toDoubleOrNull()?.times(100))?.toInt() ?: 0
                val newListing = Listing(
                    id = 0L,
                    sellerId = currentUserId,
                    title = title.trim(),
                    description = description.ifBlank { null },
                    category = category,
                    priceCents = priceCents,
                    condition = condition,
                    photos = photos,
                    status = ListingStatus.ACTIVE,
                    createdAt = System.currentTimeMillis()
                )
                db.insertListing(newListing)
                items = if (tab == HomeTab.MyListings) {
                    db.getListingsForSeller(currentUserId)
                } else {
                    db.getAllListingsVisible()
                }
                showCreate = false
            }
        )
    }

    if (showSearch) {
        SearchScreen(
            items = items,
            onClose = { showSearch = false },
            onOpenListing = { id ->
                showSearch = false
                onOpenListing(id)
            }
        )
    }
}

/* =============== Search screen =============== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    items: List<Listing>,
    onClose: () -> Unit,
    onOpenListing: (Long) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    val categories = ListingCategory.entries
    var selectedCategories by remember { mutableStateOf(setOf<ListingCategory>()) }

    // Price bounds from items
    val (minCents, maxCents) = remember(items) {
        val prices = items.map { it.priceCents }
        val min = prices.minOrNull() ?: 0
        val max = prices.maxOrNull() ?: 0
        min to max
    }
    val safeMin = minCents
    val safeMax = if (maxCents <= minCents) minCents + 100 else maxCents

    // Slider in dollars
    val initialRange = remember(safeMin, safeMax) {
        (safeMin / 100f)..(safeMax / 100f)
    }
    var priceRange by remember(safeMin, safeMax) { mutableStateOf(initialRange) }

    fun inPriceRange(priceCents: Int): Boolean {
        val minSel = (priceRange.start * 100).toInt()
        val maxSel = (priceRange.endInclusive * 100).toInt()
        return priceCents in minSel..maxSel
    }

    val filtered = remember(query, selectedCategories, items, priceRange) {
        items.filter { listing ->
            val matchesQuery = query.isBlank() ||
                    listing.title.contains(query, ignoreCase = true) ||
                    (listing.description?.contains(query, ignoreCase = true) ?: false)
            val matchesCategory =
                selectedCategories.isEmpty() || selectedCategories.contains(listing.category)
            val matchesPrice = inPriceRange(listing.priceCents)
            matchesQuery && matchesCategory && matchesPrice
        }
    }

    Surface(Modifier.fillMaxSize(), color = cs.surface) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Search Listings", color = cs.primary) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = cs.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search listings…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedLabelColor = cs.primary
                    ),
                    trailingIcon = {
                        TextButton(onClick = { showFilters = !showFilters }) {
                            Text(if (showFilters) "Hide Filters" else "Filters", color = cs.primary)
                        }
                    }
                )

                if (showFilters) {
                    Spacer(Modifier.height(8.dp))
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Filter by Category", color = cs.primary, style = MaterialTheme.typography.titleSmall)
                            categories.forEach { cat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = selectedCategories.contains(cat),
                                        onCheckedChange = { checked ->
                                            selectedCategories =
                                                if (checked) selectedCategories + cat else selectedCategories - cat
                                        }
                                    )
                                    Text(cat.label, color = cs.onSurface)
                                }
                            }

                            HorizontalDivider()

                            // Price Range
                            Text("Price Range", color = cs.primary, style = MaterialTheme.typography.titleSmall)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$" + String.format(Locale.US, "%.2f", priceRange.start), color = cs.onSurface)
                                Text("$" + String.format(Locale.US, "%.2f", priceRange.endInclusive), color = cs.onSurface)
                            }

                            // Slider (in dollars)
                            RangeSlider(
                                value = priceRange,
                                onValueChange = { range ->
                                    val clampedStart = range.start.coerceIn(safeMin / 100f, safeMax / 100f)
                                    val clampedEnd = range.endInclusive.coerceIn(safeMin / 100f, safeMax / 100f)
                                    priceRange = clampedStart..clampedEnd
                                },
                                valueRange = (safeMin / 100f)..(safeMax / 100f),
                                steps = 10,
                                colors = SliderDefaults.colors(
                                    thumbColor = cs.primary,
                                    activeTrackColor = cs.primary
                                )
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No listings found", color = cs.onSurface.copy(alpha = 0.7f))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filtered) { item ->
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenListing(item.id) }
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(item.title, color = cs.primary, style = MaterialTheme.typography.titleMedium)
                                    item.description?.takeIf { it.isNotBlank() }?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text(it, color = cs.onSurface)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(formatCents(item.priceCents), color = cs.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ================== Profile ================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(currentUserId: Long) {
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }
    val cs = MaterialTheme.colorScheme

    var user by remember { mutableStateOf<User?>(null) }
    var showEdit by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId) {
        user = db.getUserById(currentUserId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .padding(20.dp)
    ) {
        if (user == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("User not found", color = cs.onSurface)
            }
        } else {
            val u = user!!
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("USER ID: ${u.id}",  color = cs.primary, style = MaterialTheme.typography.titleMedium)
                Text("NAME: ${u.first} ${u.last}", color = cs.primary, style = MaterialTheme.typography.titleMedium)
                Text("EMAIL: ${u.email}", color = cs.primary, style = MaterialTheme.typography.titleMedium)
                Text("ROLE: ${u.role.name}", color = cs.primary, style = MaterialTheme.typography.titleMedium)

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showEdit = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.primary,
                        contentColor = cs.onPrimary
                    )
                ) { Text("EDIT PROFILE") }
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
    val cs = MaterialTheme.colorScheme
    var first by remember { mutableStateOf(user.first) }
    var last by remember { mutableStateOf(user.last) }
    var email by remember { mutableStateOf(user.email) }
    var password by remember { mutableStateOf(user.password) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", color = cs.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = first, onValueChange = { first = it },
                    label = { Text("First name") },
                    textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedLabelColor = cs.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                OutlinedTextField(
                    value = last, onValueChange = { last = it },
                    label = { Text("Last name") },
                    textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedLabelColor = cs.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") },
                    textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedLabelColor = cs.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") },
                    textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedLabelColor = cs.primary,
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
                colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
            ) { Text("Cancel") }
        }
    )
}

/* ================== Listings feed ================== */
@Composable
private fun ListingsFeed(
    listings: List<Listing>,
    showAddToCart: Boolean,
    currentUserId: Long,
    onAddToCart: (Listing) -> Unit,
    onOpenListing: (Long) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    if (listings.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No listings yet.", color = cs.onSurface)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp)
                        .clickable { onOpenListing(item.id) },
                    colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = cs.primary)

                        item.description?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
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
                                color = cs.primary
                            )

                            if (showAddToCart && item.sellerId != currentUserId) {
                                Button(
                                    onClick = { onAddToCart(item) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = cs.primary,
                                        contentColor = cs.onPrimary
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

/* ============ Listing details screen ============ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailsScreen(
    currentUserId: Long,
    listingId: Long,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var listing by remember { mutableStateOf<Listing?>(null) }
    var seller by remember { mutableStateOf<User?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(listingId) {
        val l = db.getListingById(listingId)
        listing = l
        seller = l?.let { db.getUserById(it.sellerId) }
    }

    val sellerEnabled = seller?.enabled ?: true
    if (!sellerEnabled) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This listing is no longer available.")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listing Details", color = cs.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = cs.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            val l = listing
            if (l != null) {
                val isOwner = l.sellerId == currentUserId
                if (isOwner) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cs.primary,
                                contentColor = cs.onPrimary
                            )
                        ) { Text("DELETE LISTING") }

                        Button(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cs.primary,
                                contentColor = cs.onPrimary
                            )
                        ) { Text("EDIT LISTING") }
                    }
                } else {
                    Button(
                        onClick = {
                            CartRepository.add(currentUserId, l, 1)
                            scope.launch { snackbarHostState.showSnackbar("Added to cart", withDismissAction = true) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary
                        )
                    ) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("ADD TO CART")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        val l = listing
        if (l == null) {
            Box(Modifier.padding(inner).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (l.photos.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(l.photos.size) { i ->
                            AsyncImage(
                                model = l.photos[i],
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        }
                    }
                }

                Text(l.title, style = MaterialTheme.typography.headlineSmall, color = cs.onBackground)
                Text(formatCents(l.priceCents), style = MaterialTheme.typography.titleMedium, color = cs.onBackground)

                Text("Category: ${l.category.label}", color = cs.onBackground)
                Text("Condition: ${l.condition.label}", color = cs.onBackground)

                val sellerName = seller?.let { "${it.first} ${it.last}" } ?: "Unknown"
                Text("Seller: $sellerName", color = cs.onBackground)

                l.description?.takeIf { it.isNotBlank() }?.let {
                    HorizontalDivider()
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = cs.onBackground)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete listing?", color = cs.onSurface) },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        listing?.let { db.deleteListing(it.id) }
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = cs.onSurface)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = cs.onSurface)
                ) { Text("Cancel") }
            }
        )
    }
    if (showEditDialog && listing != null) {
        EditListingDialog(
            listing = listing!!,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                db.updateListing(updated)
                listing = db.getListingById(updated.id)
                showEditDialog = false
                scope.launch { snackbarHostState.showSnackbar("Listing updated", withDismissAction = true) }
            }
        )
    }
}

/* ================== Cart screen ================== */

@Composable
private fun CartScreen(currentUserId: Long) {
    val cs = MaterialTheme.colorScheme
    val cartItems = remember(currentUserId) { CartRepository.getItems(currentUserId) }
    val totalCents by remember(cartItems) {
        derivedStateOf { CartRepository.totalCents(currentUserId) }
    }

    if (cartItems.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your cart is empty.", color = cs.onSurface)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.surface)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cartItems, key = { it.id }) { cartItem ->
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = cs.surface),
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
                            Text(cartItem.listing.title, style = MaterialTheme.typography.titleMedium, color = cs.primary)
                            Spacer(Modifier.height(4.dp))
                            Text(formatCents(cartItem.listing.priceCents), color = cs.onSurface)
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
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.primary)
                            ) { Text("-") }

                            Text("${cartItem.quantity}", color = cs.onSurface, style = MaterialTheme.typography.titleSmall)

                            Button(
                                onClick = {
                                    CartRepository.updateQuantity(currentUserId, cartItem.id, cartItem.quantity + 1)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)
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
            color = cs.surface
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total", color = cs.primary, style = MaterialTheme.typography.titleMedium)
                    Text(formatCents(totalCents), color = cs.primary, style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { CartRepository.clear(currentUserId) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.primary)
                    ) { Text("CLEAR") }

                    Button(
                        onClick = {
                            CartRepository.clear(currentUserId)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary, contentColor = cs.onPrimary)
                    ) { Text("CHECK OUT") }
                }
            }
        }
    }
}

/* ================== Create/Edit listing dialog + pickers ================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddListingDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: ListingCategory,
        priceDollars: String,
        condition: ItemCondition,
        photos: List<String>
    ) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    var category by remember { mutableStateOf(ListingCategory.GENERAL) }
    var condition by remember { mutableStateOf(ItemCondition.GOOD) }

    var selectedPhotos by remember { mutableStateOf<List<String>>(emptyList()) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedPhotos = uris.map { it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Listing", color = cs.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") },
                    textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedLabelColor = cs.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedLabelColor = cs.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                CategoryPicker(value = category, onChange = { category = it })
                ConditionPicker(value = condition, onChange = { condition = it })

                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Price (e.g., 12.34)") },
                    textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedLabelColor = cs.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                // Add photos
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { photoPicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cs.primary,
                        contentColor = cs.onPrimary
                    )
                ) { Text("Add photos") }

                if (selectedPhotos.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(selectedPhotos) { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, description, category, price, condition, selectedPhotos) },
                enabled = title.isNotBlank() && price.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
            ) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditListingDialog(
    listing: Listing,
    onDismiss: () -> Unit,
    onSave: (Listing) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var title by remember { mutableStateOf(listing.title) }
    var description by remember { mutableStateOf(listing.description ?: "") }
    var category by remember { mutableStateOf(listing.category) }
    var condition by remember { mutableStateOf(listing.condition) }
    var price by remember { mutableStateOf((listing.priceCents / 100.0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit listing", color = cs.primary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title") },
                    textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedLabelColor = cs.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedLabelColor = cs.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                CategoryPicker(value = category, onChange = { category = it })
                ConditionPicker(value = condition, onChange = { condition = it })

                OutlinedTextField(
                    value = price, onValueChange = { price = it },
                    label = { Text("Price (e.g., 12.34)") },
                    textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = cs.primary,
                        focusedLabelColor = cs.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = (price.toDoubleOrNull()?.times(100))?.toInt() ?: listing.priceCents
                    onSave(
                        listing.copy(
                            title = title.trim(),
                            description = description.ifBlank { null },
                            category = category,
                            condition = condition,
                            priceCents = cents
                        )
                    )
                },
                enabled = title.isNotBlank() && price.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
            ) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    value: ListingCategory,
    onChange: (ListingCategory) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = cs.primary,
                focusedLabelColor = cs.primary,
                unfocusedBorderColor = Color.LightGray
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ListingCategory.entries.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label, color = cs.onSurface) },
                    onClick = { onChange(opt); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConditionPicker(
    value: ItemCondition,
    onChange: (ItemCondition) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Condition") },
            textStyle = LocalTextStyle.current.copy(color = cs.onSurface),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = cs.primary,
                focusedLabelColor = cs.primary,
                unfocusedBorderColor = Color.LightGray
            )
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ItemCondition.entries.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label, color = cs.onSurface) },
                    onClick = { onChange(opt); expanded = false }
                )
            }
        }
    }
}

/* Money formatter */
private fun formatCents(cents: Int): String {
    val dollars = cents / 100
    val pennies = cents % 100
    return "$$dollars.${pennies.toString().padStart(2, '0')}"
}