package com.refreshsoda.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.refreshsoda.app.data.model.*

@Database(
    entities = [
        ShopBrand::class,
        UserProfile::class,
        ProductCategory::class,
        Product::class,
        SaleTransaction::class,
        CustomerKhata::class,
        KhataRepayment::class,
        Expense::class,
        DayClosing::class,
        PaytmSettlement::class,
        AuditLog::class,
        UserAccount::class,
        SupplierPurchase::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL PRIMARY KEY, `name` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL DEFAULT 0)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sales` ADD COLUMN `itemsDetails` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (`id` TEXT NOT NULL PRIMARY KEY, `shopName` TEXT NOT NULL, `ownerName` TEXT NOT NULL, `phone` TEXT NOT NULL, `upiId` TEXT NOT NULL, `address` TEXT NOT NULL)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_accounts` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `emailOrPhone` TEXT NOT NULL,
                        `password` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `shopName` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `lastLoginAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `products` ADD COLUMN `costPrice` REAL NOT NULL DEFAULT 0.0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `supplier_purchases` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `brand` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `billNumber` TEXT NOT NULL,
                        `billDate` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `paidAmount` REAL NOT NULL,
                        `paymentStatus` TEXT NOT NULL,
                        `paymentMode` TEXT NOT NULL,
                        `totalBoxes` INTEGER NOT NULL,
                        `itemsSummary` TEXT NOT NULL,
                        `billImageUri` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `brands` (`id` TEXT NOT NULL PRIMARY KEY, `name` TEXT NOT NULL)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "refresh_soda_shop_db"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_4_6,
                    MIGRATION_6_7
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}