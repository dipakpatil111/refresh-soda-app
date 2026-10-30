package com.refreshsoda.app.data.model

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "brands")
data class ShopBrand(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "shop_profile",
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val upiId: String = "",
    val address: String = ""
)

@Entity(tableName = "categories")
data class ProductCategory(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(defaultValue = "0")
    val orderIndex: Int = 0,
    @ColumnInfo(defaultValue = "1")
    val trackBoxes: Boolean = false,
    @ColumnInfo(defaultValue = "2")
    val lowStockLimit: Int = 2
)

@Entity(tableName = "sub_categories")
data class SubCategory(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val categoryName: String,
    val name: String,
    @ColumnInfo(defaultValue = "0")
    val defaultPrice: Double = 0.0,
    @ColumnInfo(defaultValue = "0")
    val sealedBoxes: Int = 0,
    @ColumnInfo(defaultValue = "2")
    val lowStockLimit: Int = 2
)

@Entity(
    tableName = "ice_cream_variety_stock",
    primaryKeys = ["categoryName", "companyName", "varietyName"]
)
data class IceCreamVarietyStock(
    val categoryName: String,
    val companyName: String,
    val varietyName: String,
    @ColumnInfo(defaultValue = "0")
    val sealedBoxes: Int = 0,
    @ColumnInfo(defaultValue = "2")
    val lowStockLimit: Int = 2,
    val lastCostPrice: Double = 0.0,
    val lastMrp: Double = 0.0
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val brand: String = "",
    val price: Double,
    val sealedBoxes: Int = 0,
    @ColumnInfo(defaultValue = "0.0")
    val costPrice: Double = 0.0,
    val lowStockLimit: Int? = null
)

data class DraftProductItem(
    val category: String,
    val name: String,
    val brand: String = "",
    val price: Double,
    val costPrice: Double = 0.0,
    val sealedBoxes: Int = 0
)

data class InwardBillItemDraft(
    val name: String,
    val boxes: Int = 0,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val id: String = java.util.UUID.randomUUID().toString(),
    val boxesInput: String = boxes.toString(),
    val costPriceInput: String = costPrice.toString(),
    val sellingPriceInput: String = sellingPrice.toString(),
    val piecesPerBox: Int = 0,
    val piecesPerBoxInput: String = if (piecesPerBox > 0) piecesPerBox.toString() else "",
    val lineAmount: Double? = null
) {
    val lineTotal: Double get() = boxes * costPrice
    val profitPerBox: Double get() = sellingPrice - costPrice
    val profitMarginPercent: Double get() = if (costPrice > 0) ((sellingPrice - costPrice) / costPrice) * 100.0 else 0.0
}

@Entity(tableName = "supplier_purchases")
data class SupplierPurchase(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val brand: String,
    val category: String = "",
    val billNumber: String = "",
    val billDate: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val paymentStatus: String = "PAID",
    val paymentMode: String = "UPI",
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
    val paymentMode: String,
    val customerName: String? = null,
    val itemsSummary: String,
    val itemsDetails: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val soldBy: String = ""
)

@Entity(tableName = "customers_khata")
data class CustomerKhata(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String = "",
    val outstandingDebt: Double = 0.0,
    val debtLimit: Double = 2000.0,
    val dueDateMillis: Long? = null
)

@Entity(tableName = "khata_repayments")
data class KhataRepayment(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val amount: Double,
    val paymentMode: String = "CASH",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val paymentMode: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "day_closings")
data class DayClosing(
    @PrimaryKey val date: String,
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
    @PrimaryKey val date: String,
    val totalUpiCollection: Double,
    val chargeDeduction: Double = 0.0,
    val netBankAmount: Double,
    val isSettled: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey val id: String,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val emailOrPhone: String,
    val password: String,
    val role: String = "OWNER",
    val shopName: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val shopId: String = ""
) {
    /** Owner's own id acts as the shop id; staff carry their owner's shop id. */
    val effectiveShopId: String get() = shopId.ifBlank { id }
}

fun String.isIceCreamCategory(): Boolean =
    lowercase().filter(Char::isLetterOrDigit).contains("icecream")

// ---------------- INWARD / SUPPLIER LEDGER ----------------
@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey val id: String,          // lower-cased company name
    val name: String,
    val phone: String = "",
    val gstNumber: String = "",
    val address: String = ""
)

@Entity(tableName = "supplier_purchase_items")
data class SupplierPurchaseItem(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val purchaseId: String,
    val varietyName: String,
    val quantity: Int,
    val costPerBox: Double,
    val mrp: Double,
    val lineAmount: Double,
    val piecesPerBox: Int = 0
)

@Entity(tableName = "supplier_payments")
data class SupplierPayment(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val purchaseId: String,
    val brand: String,
    val amount: Double,
    val paymentMode: String = "CASH",   // CASH / UPI / BANK / CREDIT_NOTE
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "supplier_returns")
data class SupplierReturn(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val brand: String,
    val category: String,
    val purchaseId: String = "",
    val varietyName: String,
    val quantity: Int,
    val creditAmount: Double,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "wastage")
data class WastageRecord(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val category: String,
    val companyName: String,
    val varietyName: String,
    val quantity: Int,
    val estimatedCost: Double = 0.0,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "offers")
data class Offer(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val discountType: String = "PERCENT",  // PERCENT or FLAT
    val value: Double,
    val minBill: Double = 0.0,
    val active: Boolean = true
) {
    fun discountFor(billTotal: Double): Double {
        if (!active || billTotal <= 0.0 || billTotal < minBill) return 0.0
        val raw = if (discountType == "PERCENT") billTotal * value / 100.0 else value
        return raw.coerceIn(0.0, billTotal)
    }
}
