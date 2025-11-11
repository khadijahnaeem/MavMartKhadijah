package com.example.mavmart

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

class AppDatabase private constructor(ctx: Context) :
    SQLiteOpenHelper(ctx, Db.DB_NAME, null, Db.DB_VERSION) {

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context.applicationContext).also { INSTANCE = it }
            }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(Db.Users.CREATE)
        db.execSQL(Db.Users.CREATE_INDEX_EMAIL)

        db.execSQL(Db.Listings.CREATE)
        db.execSQL(Db.Listings.CREATE_INDEX_SELLER)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Your old policy: destructive for anything before 6
        if (oldVersion < 6) {
            db.execSQL("DROP TABLE IF EXISTS ${Db.Listings.TABLE}")
            db.execSQL("DROP TABLE IF EXISTS ${Db.Users.TABLE}")
            onCreate(db)
            return
        }
        // v6 -> v7: add enabled column to users
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE ${Db.Users.TABLE} ADD COLUMN ${Db.Users.COL_ENABLED} INTEGER NOT NULL DEFAULT 1")
            // ensure no nulls even if ALTER didn't apply default (some sqlite builds)
            db.execSQL("UPDATE ${Db.Users.TABLE} SET ${Db.Users.COL_ENABLED}=1 WHERE ${Db.Users.COL_ENABLED} IS NULL")
        }
    }

    /* ================= Users ================= */

    fun insertUser(user: User): Long {
        val cv = ContentValues().apply {
            put(Db.Users.COL_FIRST, user.first)
            put(Db.Users.COL_LAST,  user.last)
            put(Db.Users.COL_EMAIL, user.email)
            put(Db.Users.COL_PASSWORD, user.password)
            put(Db.Users.COL_ROLE, user.role.name)
            put(Db.Users.COL_ENABLED, if (user.enabled) 1 else 0)
        }
        return writableDatabase.insert(Db.Users.TABLE, null, cv)
    }

    fun getAllUsers(): List<User> {
        val out = mutableListOf<User>()
        val c = readableDatabase.query(
            Db.Users.TABLE,
            arrayOf(
                Db.Users.COL_ID,
                Db.Users.COL_FIRST,
                Db.Users.COL_LAST,
                Db.Users.COL_EMAIL,
                Db.Users.COL_PASSWORD,
                Db.Users.COL_ROLE,
                Db.Users.COL_ENABLED
            ),
            null, null, null, null,
            "${Db.Users.COL_FIRST} COLLATE NOCASE ASC, ${Db.Users.COL_LAST} COLLATE NOCASE ASC"
        )
        c.use {
            while (it.moveToNext()) {
                out += User(
                    id = it.getLong(0),
                    first = it.getString(1),
                    last = it.getString(2),
                    email = it.getString(3),
                    password = it.getString(4),
                    role = Role.valueOf(it.getString(5)),
                    enabled = it.getInt(6) != 0
                )
            }
        }
        return out
    }

    fun findUserByEmail(email: String): User? {
        val c = readableDatabase.query(
            Db.Users.TABLE,
            arrayOf(
                Db.Users.COL_ID,
                Db.Users.COL_FIRST,
                Db.Users.COL_LAST,
                Db.Users.COL_EMAIL,
                Db.Users.COL_PASSWORD,
                Db.Users.COL_ROLE,
                Db.Users.COL_ENABLED
            ),
            "${Db.Users.COL_EMAIL}=?",
            arrayOf(email),
            null, null, null
        )
        c.use {
            if (it.moveToFirst()) {
                return User(
                    id = it.getLong(0),
                    first = it.getString(1),
                    last = it.getString(2),
                    email = it.getString(3),
                    password = it.getString(4),
                    role = Role.valueOf(it.getString(5)),
                    enabled = it.getInt(6) != 0
                )
            }
        }
        return null
    }

    fun getUserById(id: Long): User? {
        val c = readableDatabase.query(
            Db.Users.TABLE,
            arrayOf(
                Db.Users.COL_ID,
                Db.Users.COL_FIRST,
                Db.Users.COL_LAST,
                Db.Users.COL_EMAIL,
                Db.Users.COL_PASSWORD,
                Db.Users.COL_ROLE,
                Db.Users.COL_ENABLED
            ),
            "${Db.Users.COL_ID}=?",
            arrayOf(id.toString()),
            null, null, null
        )
        c.use {
            return if (it.moveToFirst()) {
                User(
                    id = it.getLong(0),
                    first = it.getString(1),
                    last = it.getString(2),
                    email = it.getString(3),
                    password = it.getString(4),
                    role = Role.valueOf(it.getString(5)),
                    enabled = it.getInt(6) != 0
                )
            } else null
        }
    }
    fun updateUser(u: User): Int {
        val values = ContentValues().apply {
            put(Db.Users.COL_FIRST, u.first)
            put(Db.Users.COL_LAST,  u.last)
            put(Db.Users.COL_EMAIL, u.email)
            put(Db.Users.COL_PASSWORD, u.password)
            put(Db.Users.COL_ROLE, u.role.name)
            put(Db.Users.COL_ENABLED, if (u.enabled) 1 else 0)
        }
        return writableDatabase.update(
            Db.Users.TABLE,
            values,
            "${Db.Users.COL_ID}=?",
            arrayOf(u.id.toString())
        )
    }
    fun setUserEnabled(userId: Long, enabled: Boolean): Int {
        val cv = ContentValues().apply {
            put(Db.Users.COL_ENABLED, if (enabled) 1 else 0)
        }
        return writableDatabase.update(
            Db.Users.TABLE,
            cv,
            "${Db.Users.COL_ID}=?",
            arrayOf(userId.toString())
        )
    }

    fun validateLogin(email: String, password: String, expectedRole: Role? = null): User? {
        val u = findUserByEmail(email) ?: return null
        if (!u.enabled) return null
        if (u.password != password) return null
        if (expectedRole != null && u.role != expectedRole) return null
        return u
    }

    /* ================= Listings ================= */

    private fun photosToJson(photos: List<String>): String = JSONArray(photos).toString()

    private fun jsonToPhotos(json: String): List<String> =
        try {
            val arr = JSONArray(json)
            List(arr.length()) { i -> arr.optString(i) }.filter { it.isNotBlank() }
        } catch (_: Exception) { emptyList() }

    fun insertListing(listing: Listing): Long {
        val cv = ContentValues().apply {
            put(Db.Listings.COL_SELLER_ID, listing.sellerId)
            put(Db.Listings.COL_TITLE, listing.title)
            put(Db.Listings.COL_DESC, listing.description)
            put(Db.Listings.COL_CATEGORY, listing.category.name)
            put(Db.Listings.COL_PRICE_CENTS, listing.priceCents)
            put(Db.Listings.COL_CONDITION, listing.condition.name)
            put(Db.Listings.COL_PHOTOS_JSON, photosToJson(listing.photos))
            put(Db.Listings.COL_STATUS, listing.status.name)
            put(Db.Listings.COL_CREATED_AT, listing.createdAt)
        }
        return writableDatabase.insert(Db.Listings.TABLE, null, cv)
    }

    fun getAllListings(): List<Listing> {
        val out = mutableListOf<Listing>()
        val c = readableDatabase.query(
            Db.Listings.TABLE,
            arrayOf(
                Db.Listings.COL_ID,
                Db.Listings.COL_SELLER_ID,
                Db.Listings.COL_TITLE,
                Db.Listings.COL_DESC,
                Db.Listings.COL_CATEGORY,
                Db.Listings.COL_PRICE_CENTS,
                Db.Listings.COL_CONDITION,
                Db.Listings.COL_PHOTOS_JSON,
                Db.Listings.COL_STATUS,
                Db.Listings.COL_CREATED_AT
            ),
            null, null, null, null,
            "${Db.Listings.COL_CREATED_AT} DESC"
        )
        c.use {
            while (it.moveToNext()) {
                out += Listing(
                    id = it.getLong(0),
                    sellerId = it.getLong(1),
                    title = it.getString(2),
                    description = it.getString(3),
                    category = ListingCategory.valueOf(it.getString(4)),
                    priceCents = it.getInt(5),
                    condition = ItemCondition.valueOf(it.getString(6)),
                    photos = jsonToPhotos(it.getString(7)),
                    status = ListingStatus.valueOf(it.getString(8)),
                    createdAt = it.getLong(9)
                )
            }
        }
        return out
    }
// NEW
    fun getAllListingsVisible(): List<Listing> {
        val sql = """
        SELECT l.${Db.Listings.COL_ID},
               l.${Db.Listings.COL_SELLER_ID},
               l.${Db.Listings.COL_TITLE},
               l.${Db.Listings.COL_DESC},
               l.${Db.Listings.COL_CATEGORY},
               l.${Db.Listings.COL_PRICE_CENTS},
               l.${Db.Listings.COL_CONDITION},
               l.${Db.Listings.COL_PHOTOS_JSON},
               l.${Db.Listings.COL_STATUS},
               l.${Db.Listings.COL_CREATED_AT}
        FROM ${Db.Listings.TABLE} l
        JOIN ${Db.Users.TABLE} u
          ON u.${Db.Users.COL_ID} = l.${Db.Listings.COL_SELLER_ID}
        WHERE u.${Db.Users.COL_ENABLED} = 1
        ORDER BY l.${Db.Listings.COL_CREATED_AT} DESC
    """.trimIndent()

        val out = mutableListOf<Listing>()
        val c = readableDatabase.rawQuery(sql, null)
        c.use {
            while (it.moveToNext()) {
                out += Listing(
                    id = it.getLong(0),
                    sellerId = it.getLong(1),
                    title = it.getString(2),
                    description = it.getString(3),
                    category = ListingCategory.valueOf(it.getString(4)),
                    priceCents = it.getInt(5),
                    condition = ItemCondition.valueOf(it.getString(6)),
                    photos = jsonToPhotos(it.getString(7)),
                    status = ListingStatus.valueOf(it.getString(8)),
                    createdAt = it.getLong(9)
                )
            }
        }
        return out
    }

    fun getListingsForSeller(sellerId: Long): List<Listing> {
        val out = mutableListOf<Listing>()
        val c = readableDatabase.query(
            Db.Listings.TABLE,
            arrayOf(
                Db.Listings.COL_ID,
                Db.Listings.COL_SELLER_ID,
                Db.Listings.COL_TITLE,
                Db.Listings.COL_DESC,
                Db.Listings.COL_CATEGORY,
                Db.Listings.COL_PRICE_CENTS,
                Db.Listings.COL_CONDITION,
                Db.Listings.COL_PHOTOS_JSON,
                Db.Listings.COL_STATUS,
                Db.Listings.COL_CREATED_AT
            ),
            "${Db.Listings.COL_SELLER_ID}=?",
            arrayOf(sellerId.toString()),
            null, null,
            "${Db.Listings.COL_CREATED_AT} DESC"
        )
        c.use {
            while (it.moveToNext()) {
                out += Listing(
                    id = it.getLong(0),
                    sellerId = it.getLong(1),
                    title = it.getString(2),
                    description = it.getString(3),
                    category = ListingCategory.valueOf(it.getString(4)),
                    priceCents = it.getInt(5),
                    condition = ItemCondition.valueOf(it.getString(6)),
                    photos = jsonToPhotos(it.getString(7)),
                    status = ListingStatus.valueOf(it.getString(8)),
                    createdAt = it.getLong(9)
                )
            }
        }
        return out
    }

    fun getListingById(id: Long): Listing? {
        val c = readableDatabase.query(
            Db.Listings.TABLE,
            arrayOf(
                Db.Listings.COL_ID,
                Db.Listings.COL_SELLER_ID,
                Db.Listings.COL_TITLE,
                Db.Listings.COL_DESC,
                Db.Listings.COL_CATEGORY,
                Db.Listings.COL_PRICE_CENTS,
                Db.Listings.COL_CONDITION,
                Db.Listings.COL_PHOTOS_JSON,
                Db.Listings.COL_STATUS,
                Db.Listings.COL_CREATED_AT
            ),
            "${Db.Listings.COL_ID}=?",
            arrayOf(id.toString()),
            null, null, null
        )
        c.use {
            return if (it.moveToFirst()) {
                Listing(
                    id = it.getLong(0),
                    sellerId = it.getLong(1),
                    title = it.getString(2),
                    description = it.getString(3),
                    category = ListingCategory.valueOf(it.getString(4)),
                    priceCents = it.getInt(5),
                    condition = ItemCondition.valueOf(it.getString(6)),
                    photos = jsonToPhotos(it.getString(7)),
                    status = ListingStatus.valueOf(it.getString(8)),
                    createdAt = it.getLong(9)
                )
            } else null
        }
    }

    fun updateListing(l: Listing): Int {
        val cv = ContentValues().apply {
            put(Db.Listings.COL_SELLER_ID, l.sellerId)
            put(Db.Listings.COL_TITLE,     l.title)
            put(Db.Listings.COL_DESC,      l.description)
            put(Db.Listings.COL_CATEGORY,  l.category.name)
            put(Db.Listings.COL_PRICE_CENTS, l.priceCents)
            put(Db.Listings.COL_CONDITION, l.condition.name)
            put(Db.Listings.COL_PHOTOS_JSON, JSONArray(l.photos).toString())
            put(Db.Listings.COL_STATUS,    l.status.name)
            // keep createdAt as-is
        }
        return writableDatabase.update(
            Db.Listings.TABLE,
            cv,
            "${Db.Listings.COL_ID}=?",
            arrayOf(l.id.toString())
        )
    }

    fun deleteListing(id: Long): Int {
        return writableDatabase.delete(
            Db.Listings.TABLE,
            "${Db.Listings.COL_ID}=?",
            arrayOf(id.toString())
        )
    }
}

/* ===========================================================
 *  Internal DB schema
 * ===========================================================
 */
private object Db {
    const val DB_NAME = "mavmart.db"
    const val DB_VERSION = 7   // bumped

    object Users {
        const val TABLE = "users"
        const val COL_ID = "_id"
        const val COL_FIRST = "first"
        const val COL_LAST = "last"
        const val COL_EMAIL = "email"
        const val COL_PASSWORD = "password"
        const val COL_ROLE = "role"
        const val COL_ENABLED = "enabled"   // NEW

        val CREATE = """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FIRST TEXT NOT NULL,
                $COL_LAST  TEXT NOT NULL,
                $COL_EMAIL TEXT NOT NULL UNIQUE,
                $COL_PASSWORD TEXT NOT NULL,
                $COL_ROLE TEXT NOT NULL,
                $COL_ENABLED INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent()

        const val CREATE_INDEX_EMAIL =
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON $TABLE($COL_EMAIL)"
    }

    object Listings {
        const val TABLE = "listings"
        const val COL_ID = "_id"
        const val COL_SELLER_ID = "seller_id"
        const val COL_TITLE = "title"
        const val COL_DESC = "description"
        const val COL_CATEGORY = "category"
        const val COL_PRICE_CENTS = "price_cents"
        const val COL_CONDITION = "condition"
        const val COL_PHOTOS_JSON = "photos_json"
        const val COL_STATUS = "status"
        const val COL_CREATED_AT = "created_at"

        val CREATE = """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SELLER_ID INTEGER NOT NULL,
                $COL_TITLE TEXT NOT NULL,
                $COL_DESC TEXT,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_PRICE_CENTS INTEGER NOT NULL,
                $COL_CONDITION TEXT NOT NULL,
                $COL_PHOTOS_JSON TEXT NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COL_SELLER_ID) REFERENCES ${Users.TABLE}(${Users.COL_ID}) ON DELETE CASCADE
            )
        """.trimIndent()

        const val CREATE_INDEX_SELLER =
            "CREATE INDEX IF NOT EXISTS idx_listings_seller ON $TABLE($COL_SELLER_ID)"
    }
}