package com.refreshsoda.app.data.alerts

import com.refreshsoda.app.data.model.CustomerKhata
import com.refreshsoda.app.data.model.IceCreamVarietyStock
import com.refreshsoda.app.data.model.SaleTransaction
import com.refreshsoda.app.data.model.SupplierPurchase
import com.refreshsoda.app.data.model.SupplierPurchaseItem

data class StockAlert(
    val categoryName: String,
    val companyName: String,
    val varietyName: String,
    val boxes: Int,
    val limit: Int,
    /** Rough days of stock left from the last 14 days of sales; null when it cannot be estimated. */
    val daysLeft: Int?
)

data class ShopAlerts(
    val lowStock: List<StockAlert> = emptyList(),
    val overdueKhata: List<CustomerKhata> = emptyList(),
    val supplierDueTotal: Double = 0.0,
    val supplierDueBills: Int = 0
) {
    val count: Int get() = lowStock.size + overdueKhata.size + (if (supplierDueBills > 0) 1 else 0)
    val isEmpty: Boolean get() = count == 0
}

object AlertsEngine {
    private const val UNASSIGNED = "Opening stock (unassigned)"
    private const val DAY_MS = 24L * 60L * 60L * 1000L
    private const val LOOKBACK_DAYS = 14

    fun compute(
        varieties: List<IceCreamVarietyStock>,
        sales: List<SaleTransaction>,
        purchaseItems: List<SupplierPurchaseItem>,
        khata: List<CustomerKhata>,
        purchases: List<SupplierPurchase>,
        nowMillis: Long = System.currentTimeMillis()
    ): ShopAlerts {
        // pieces sold per variety name over the last 14 days (sale lines look like Category|Name|Qty|Price)
        val since = nowMillis - LOOKBACK_DAYS * DAY_MS
        val soldPieces = HashMap<String, Int>()
        sales.filter { it.timestamp >= since }.forEach { sale ->
            sale.itemsDetails.split(";").forEach { line ->
                val parts = line.split("|")
                if (parts.size >= 3) {
                    val key = parts[1].trim().lowercase()
                    soldPieces[key] = (soldPieces[key] ?: 0) + (parts[2].toIntOrNull() ?: 0)
                }
            }
        }
        val piecesPerBox = HashMap<String, Int>()
        purchaseItems.forEach { item ->
            if (item.piecesPerBox > 0) piecesPerBox[item.varietyName.trim().lowercase()] = item.piecesPerBox
        }

        val lowStock = varieties
            .filter { it.varietyName != UNASSIGNED && it.sealedBoxes <= it.lowStockLimit }
            .map { stock ->
                val key = stock.varietyName.trim().lowercase()
                val perDay = (soldPieces[key] ?: 0).toDouble() / LOOKBACK_DAYS
                val pieces = piecesPerBox[key] ?: 0
                val daysLeft = if (perDay > 0.0 && pieces > 0) {
                    ((stock.sealedBoxes * pieces) / perDay).toInt()
                } else {
                    null
                }
                StockAlert(stock.categoryName, stock.companyName, stock.varietyName, stock.sealedBoxes, stock.lowStockLimit, daysLeft)
            }
            .sortedBy { it.boxes }

        val overdue = khata
            .filter { it.outstandingDebt > 0.0 && it.dueDateMillis != null && it.dueDateMillis < nowMillis }
            .sortedByDescending { it.outstandingDebt }

        val open = purchases.filter { it.totalAmount - it.paidAmount > 0.5 }
        return ShopAlerts(
            lowStock = lowStock,
            overdueKhata = overdue,
            supplierDueTotal = open.sumOf { it.totalAmount - it.paidAmount },
            supplierDueBills = open.size
        )
    }
}
