package com.refreshsoda.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.refreshsoda.app.data.model.*

@Database(
    entities = [
        ProductCategory::class,
        Product::class,
        SaleTransaction::class,
        CustomerKhata::class,
        KhataRepayment::class,
        Expense::class,
        DayClosing::class,
        PaytmSettlement::class,
        AuditLog::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "refresh_soda_shop_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
