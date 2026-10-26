package com.refreshsoda.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // Soda, Ice-cream, Milkshake, Cold Drink
    val brand: String,    // Vadilal, Hocco, Hajoori, Own Shop
    val price: Double,
    val sealedBoxes: Int = 0
)

@Entity(tableName = "sales")
data class SaleTransaction(
    @PrimaryKey val id: String,
    val totalAmount: Double,
    val paymentMode: String, // CASH, UPI, UDHAR
    val customerName: String? = null,
    val itemsSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers_khata")
data class CustomerKhata(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String = "",
    val outstandingDebt: Double = 0.0,
    val debtLimit: Double = 2000.0
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val title: String, // Barf, Doodh, Gas, Kiraya, Mazdoori
    val amount: Double,
    val paymentMode: String, // CASH, UPI
    val timestamp: Long = System.currentTimeMillis()
)
