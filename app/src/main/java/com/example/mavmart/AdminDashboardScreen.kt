package com.example.mavmart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }

    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Users", "Listings")

    var users by remember { mutableStateOf(emptyList<User>()) }
    var listings by remember { mutableStateOf(emptyList<Listing>()) }

    // detail state
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedListing by remember { mutableStateOf<Listing?>(null) }

    fun refresh() {
        users = db.getAllUsers()
        listings = db.getAllListings()
    }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", color = cs.onBackground) },
                actions = { TextButton(onClick = onLogout) { Text("Logout", color = cs.onBackground) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
                .background(cs.surface)
        ) {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = cs.background,
                contentColor = cs.onBackground,
                indicator = { pos ->
                    TabRowDefaults.PrimaryIndicator(
                        Modifier.tabIndicatorOffset(pos[tabIndex]),
                        color = cs.primary
                    )
                },
                divider = { HorizontalDivider(color = cs.outlineVariant) }
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = i == tabIndex,
                        onClick = { tabIndex = i },
                        selectedContentColor = cs.primary,
                        unselectedContentColor = cs.onSurface.copy(alpha = 0.7f),
                        text = { Text(title) }
                    )
                }
            }

            when (tabIndex) {
                0 -> UsersTab(users) { selectedUser = it }
                1 -> ListingsTab(listings) { selectedListing = it }
            }
        }
    }

    // USER dialog (toggle enable/disable)
    selectedUser?.let { u ->
        UserDetailDialog(
            user = u,
            onDismiss = { selectedUser = null },
            onSaveEnabled = { enabled ->
                db.setUserEnabled(u.id, enabled)
                users = db.getAllUsers()
                selectedUser = null
            }
        )
    }

    // LISTING dialog (show all info)
    selectedListing?.let { item ->
        ListingDetailDialog(
            listing = item,
            sellerName = remember(item.sellerId) {
                db.getUserById(item.sellerId)?.let { "${it.first} ${it.last}" } ?: "Unknown"
            },
            onDismiss = { selectedListing = null }
        )
    }
}

/* ---------- tabs ---------- */

@Composable
private fun UsersTab(
    users: List<User>,
    onUserClick: (User) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(users) { u ->
            ElevatedCard(
                modifier = Modifier.clickable { onUserClick(u) },
                colors = CardDefaults.elevatedCardColors(
                    containerColor = cs.primary,
                    contentColor   = cs.onPrimary
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "${u.first} ${u.last}",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(u.email, color = cs.onPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${u.role.name} · ${if (u.enabled) "Enabled" else "Disabled"}",
                        color = cs.onPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingsTab(
    listings: List<Listing>,
    onListingClick: (Listing) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(listings) { item ->
            ElevatedCard(
                modifier = Modifier.clickable { onListingClick(item) },
                colors = CardDefaults.elevatedCardColors(
                    containerColor = cs.primary,
                    contentColor   = cs.onPrimary
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = cs.onPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(formatCents(item.priceCents), color = cs.onPrimary)
                }
            }
        }
    }
}

/* ---------- dialogs ---------- */

@Composable
private fun UserDetailDialog(
    user: User,
    onDismiss: () -> Unit,
    onSaveEnabled: (Boolean) -> Unit
) {
    var enabled by remember { mutableStateOf(user.enabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = { onSaveEnabled(enabled) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("User Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Name: ${user.first} ${user.last}")
                Text("Email: ${user.email}")
                Text("Role: ${user.role}")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (enabled) "Enabled" else "Disabled")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        }
    )
}

@Composable
private fun ListingDetailDialog(
    listing: Listing,
    sellerName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Listing Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Title: ${listing.title}")
                Text("Price: ${formatCents(listing.priceCents)}")
                Text("Category: ${listing.category.label}")
                Text("Condition: ${listing.condition.label}")
                Text("Status: ${listing.status}")
                Text("Seller: $sellerName")
                listing.description?.let { Text("Description: $it") }
            }
        }
    )
}

private fun formatCents(cents: Int): String {
    val dollars = cents / 100
    val pennies = cents % 100
    return "$$dollars.${pennies.toString().padStart(2, '0')}"
}