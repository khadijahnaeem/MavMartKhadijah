package com.example.mavmart

/* ========= Roles ========= */

enum class Role { User, Admin }

/* ========= Listings enums (names must match DB values) ======== */

enum class ListingCategory(val label: String) {
    GENERAL("General"),
    ENGINEERING("Engineering"),
    PRE_MED("Pre Med"),
    HUMANITIES_ART("Humanities/Art")
}

enum class ItemCondition(val label: String) {
    NEW("NEW"),
    LIKE_NEW("LIKE NEW"),
    GOOD("GOOD"),
    FAIR("FAIR"),
    POOR("POOR")
}

enum class ListingStatus {
    ACTIVE, SOLD, ARCHIVED
}

/* ========= Data classes ========= */

data class User(
    val id: Long = 0L,
    val first: String,
    val last: String,
    val email: String,
    val password: String,
    val role: Role = Role.User
)

data class Listing(
    val id: Long = 0L,
    val sellerId: Long,
    val title: String,
    val description: String?,
    val category: ListingCategory,
    val priceCents: Int,
    val condition: ItemCondition,
    val photos: List<String>,
    val status: ListingStatus = ListingStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
)