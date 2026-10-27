package com.refreshsoda.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class ProductCategory(
    @PrimaryKey val id: String,
    val name: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val name: String,
    val category: String, // User-defined category: Soda, Ice-cream, Milkshake, Cold Drink, etc.
    val brand: String = "", // Vadilal, Hocco, Hajoori, Own Shop, etc.
    val price: Double,
    val sealedBoxes: Int = 0
)

@Entity(tableName = "sales")
data class SaleTransaction(
    @PrimaryKey val id: String,
    val totalAmount: Double,
    val discountAmount: Double = 0.0,
    val paymentMode: String, // CASH, UPI, UDHAR
    val customerName: String? = null,
    val itemsSummary: String,
    val itemsDetails: String = "", // Stored as "Category|Name|Qty|Price;..."
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

@Entity(tableName = "khata_repayments")
data class KhataRepayment(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val amount: Double,
    val paymentMode: String = "CASH", // CASH or UPI
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val title: String,
    val category: String, // Barf, Soda Mal, Doodh, Bijli, Mazdoori, Kiraya, Anya
    val amount: Double,
    val paymentMode: String, // CASH, UPI
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "day_closings")
data class DayClosing(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val openingCash: Double,
    val cashSales: Double,
    val udharCashRepayment: Double,
    val cashExpenses: Double,
    val expectedCash: Double,
    val actualCash: Double,
    val discrepancy: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "paytm_settlements")
data class PaytmSettlement(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val totalUpiCollection: Double,
    val chargeDeduction: Double = 0.0,
    val netBankAmount: Double,
    val isSettled: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey val id: String,
    val action: String, // SALE_DELETED, STOCK_ADDED, BOX_OPENED, EXPENSE_ADDED, CATEGORY_ADDED
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
