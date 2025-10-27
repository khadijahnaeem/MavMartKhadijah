package com.example.mavmart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit
) {
    val brandPrimary = Color(0xFF0A2647)
    val context = LocalContext.current
    val db = remember { AppDatabase.get(context) }

    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Users", "Listings")

    var users by remember { mutableStateOf(emptyList<User>()) }
    var listings by remember { mutableStateOf(emptyList<Listing>()) }

    LaunchedEffect(Unit) {
        users = db.getAllUsers()
        listings = db.getAllListings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", color = brandPrimary) },
                actions = {
                    TextButton(onClick = onLogout) { Text("Logout", color = brandPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = i == tabIndex,
                        onClick = { tabIndex = i },
                        text = { Text(title) }
                    )
                }
            }

            when (tabIndex) {
                0 -> UsersTab(users)
                1 -> ListingsTab(listings)
            }
        }
    }
}

@Composable
private fun UsersTab(users: List<User>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(users) { u ->
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "${u.first} ${u.last}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = u.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text(u.role.name, color = Color.Black) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = Color.Black
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingsTab(listings: List<Listing>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(listings) { item ->
            ElevatedCard {
                Column(Modifier.padding(16.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatCents(item.priceCents),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF0A2647)
                    )
                    item.description?.let { desc ->
                        Spacer(Modifier.height(6.dp))
                        Text(desc, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                    }
                    Spacer(Modifier.height(8.dp))
                    InfoChipsRow(item)
                }
            }
        }
    }
}

@Composable
private fun InfoChipsRow(item: Listing) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = {}, label = { Text(item.categoryLabel(), color = Color.Black) })
        AssistChip(onClick = {}, label = { Text(item.conditionLabel(), color = Color.Black) })
        AssistChip(onClick = {}, label = { Text(item.status.name, color = Color.Black) })
    }
}

/* Show labels added to enums */
private fun Listing.categoryLabel(): String =
    when (category) {
        ListingCategory.GENERAL -> ListingCategory.GENERAL.label
        ListingCategory.ENGINEERING -> ListingCategory.ENGINEERING.label
        ListingCategory.PRE_MED -> ListingCategory.PRE_MED.label
        ListingCategory.HUMANITIES_ART -> ListingCategory.HUMANITIES_ART.label
    }

private fun Listing.conditionLabel(): String =
    when (condition) {
        ItemCondition.NEW -> ItemCondition.NEW.label
        ItemCondition.LIKE_NEW -> ItemCondition.LIKE_NEW.label
        ItemCondition.GOOD -> ItemCondition.GOOD.label
        ItemCondition.FAIR -> ItemCondition.FAIR.label
        ItemCondition.POOR -> ItemCondition.POOR.label
    }

/* Money formatter */
private fun formatCents(cents: Int): String {
    val dollars = cents / 100
    val pennies = cents % 100
    return "$$dollars.${pennies.toString().padStart(2, '0')}"
}