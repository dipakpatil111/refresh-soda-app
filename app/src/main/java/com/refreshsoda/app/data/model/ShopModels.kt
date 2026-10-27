package com.refreshsoda.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "shop_profile",
    val shopName: String = "Refresh Soda & Ice-cream",
    val ownerName: String = "Dipak Patil",
    val phone: String = "",
    val upiId: String = "",
    val address: String = ""
)

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
    val sealedBoxes: Int = 0,
    val costPrice: Double = 0.0 // Purchase / Cost price to calculate profit
)

data class DraftProductItem(
    var category: String,
    var name: String,
    var brand: String = "",
    var price: Double = 20.0,
    var costPrice: Double = 14.0,
    var sealedBoxes: Int = 0
)

data class InwardBillItemDraft(
    var name: String,
    var boxes: Int = 1,
    var costPrice: Double = 350.0, // Purchase rate per box
    var sellingPrice: Double = 500.0 // Selling revenue per box
) {
    val lineTotal: Double get() = boxes * costPrice
    val profitPerBox: Double get() = sellingPrice - costPrice
    val profitMarginPercent: Double get() = if (costPrice > 0) ((sellingPrice - costPrice) / costPrice) * 100.0 else 0.0
}

@Entity(tableName = "supplier_purchases")
data class SupplierPurchase(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val brand: String, // "Hocco", "Vadilal", "Hajoori", etc.
    val category: String = "Ice-cream", // "Ice-cream", "Soda", etc.
    val billNumber: String = "",
    val billDate: String, // YYYY-MM-DD
    val totalAmount: Double,
    val paidAmount: Double,
    val paymentStatus: String = "PAID", // "PAID", "PARTIAL", "UNPAID"
    val paymentMode: String = "UPI", // "CASH", "UPI", "BANK_TRANSFER", "UDHAR"
    val totalBoxes: Int,
    val itemsSummary: String,
    val billImageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
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
    val category: String, // Barf, Nimbu, Soda Mal, Doodh, Glasses, Bijli, Kiraya, Mazdoori, Repair, Anya
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
    val action: String, // SALE_DELETED, STOCK_ADDED, BOX_OPENED, EXPENSE_ADDED, CATEGORY_ADDED, BULK_IMPORTED, USER_LOGIN, USER_REGISTER
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val emailOrPhone: String,
    val password: String,
    val role: String = "OWNER", // "OWNER" or "STAFF"
    val shopName: String = "Refresh Soda & Ice-cream",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
