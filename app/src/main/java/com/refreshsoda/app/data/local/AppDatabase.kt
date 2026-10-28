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
        SubCategory::class,
        IceCreamVarietyStock::class,
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
    version = 9,
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
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `trackBoxes` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `lowStockLimit` INTEGER NOT NULL DEFAULT 2")
                db.execSQL("UPDATE `categories` SET `trackBoxes` = 0 WHERE LOWER(`name`) LIKE '%soda%'")
                db.execSQL(
                    "UPDATE `products` SET `sealedBoxes` = 0 WHERE LOWER(`category`) IN " +
                        "(SELECT LOWER(`name`) FROM `categories` WHERE `trackBoxes` = 0)"
                )
                db.execSQL("ALTER TABLE `products` ADD COLUMN `lowStockLimit` INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sub_categories` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `categoryName` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `defaultPrice` REAL NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sub_categories` ADD COLUMN `sealedBoxes` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `sub_categories` ADD COLUMN `lowStockLimit` INTEGER NOT NULL DEFAULT 2")
                db.execSQL("UPDATE `categories` SET `trackBoxes` = CASE WHEN LOWER(`name`) LIKE '%ice%' THEN 1 ELSE 0 END")
                db.execSQL(
                    "UPDATE `products` SET `sealedBoxes` = 0 WHERE LOWER(`category`) IN " +
                        "(SELECT LOWER(`name`) FROM `categories` WHERE `trackBoxes` = 0)"
                )
                db.execSQL(
                    """
                    UPDATE `sub_categories`
                    SET `sealedBoxes` = `sealedBoxes` + COALESCE((
                        SELECT SUM(p.`sealedBoxes`)
                        FROM `products` p
                        JOIN `categories` c ON LOWER(c.`name`) = LOWER(p.`category`)
                        WHERE c.`trackBoxes` = 1
                          AND LOWER(c.`name`) = LOWER(`sub_categories`.`categoryName`)
                          AND LOWER(trim(p.`brand`)) = LOWER(trim(`sub_categories`.`name`))
                    ), 0)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `sub_categories` (`id`, `categoryName`, `name`, `defaultPrice`, `sealedBoxes`, `lowStockLimit`)
                    SELECT lower(hex(randomblob(16))), c.`name`, trim(p.`brand`), 0, SUM(p.`sealedBoxes`), 2
                    FROM `products` p
                    JOIN `categories` c ON LOWER(c.`name`) = LOWER(p.`category`)
                    WHERE c.`trackBoxes` = 1 AND trim(p.`brand`) != ''
                      AND NOT EXISTS (
                          SELECT 1 FROM `sub_categories` s
                          WHERE LOWER(s.`categoryName`) = LOWER(c.`name`)
                            AND LOWER(s.`name`) = LOWER(trim(p.`brand`))
                      )
                    GROUP BY c.`name`, LOWER(trim(p.`brand`))
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `sub_categories` (`id`, `categoryName`, `name`, `defaultPrice`, `sealedBoxes`, `lowStockLimit`)
                    SELECT lower(hex(randomblob(16))), c.`name`, trim(b.`name`), 0, 0, 2
                    FROM `brands` b
                    CROSS JOIN `categories` c
                    WHERE c.`trackBoxes` = 1 AND trim(b.`name`) != ''
                      AND NOT EXISTS (
                          SELECT 1 FROM `sub_categories` s
                          WHERE LOWER(s.`categoryName`) = LOWER(c.`name`)
                            AND LOWER(s.`name`) = LOWER(trim(b.`name`))
                      )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ice_cream_variety_stock` (
                        `categoryName` TEXT NOT NULL,
                        `companyName` TEXT NOT NULL,
                        `varietyName` TEXT NOT NULL,
                        `sealedBoxes` INTEGER NOT NULL DEFAULT 0,
                        `lowStockLimit` INTEGER NOT NULL DEFAULT 2,
                        PRIMARY KEY(`categoryName`, `companyName`, `varietyName`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `ice_cream_variety_stock`
                        (`categoryName`, `companyName`, `varietyName`, `sealedBoxes`, `lowStockLimit`)
                    SELECT s.`categoryName`, s.`name`, 'Opening stock (unassigned)', s.`sealedBoxes`, 0
                    FROM `sub_categories` s
                    JOIN `categories` c ON LOWER(c.`name`) = LOWER(s.`categoryName`)
                    WHERE c.`trackBoxes` = 1 AND s.`sealedBoxes` > 0
                    """.trimIndent()
                )
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
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}