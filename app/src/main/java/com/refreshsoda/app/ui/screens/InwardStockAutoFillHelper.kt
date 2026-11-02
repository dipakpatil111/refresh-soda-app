package com.refreshsoda.app.ui.screens

import com.refreshsoda.app.data.model.IceCreamVarietyStock
import com.refreshsoda.app.data.model.InwardBillItemDraft
import com.refreshsoda.app.data.model.ProductCategory

/**
 * Auto-fill helpers for inward stock entry.
 * These functions handle saved pricing auto-population and validation.
 */

/**
 * Filter categories to only ice-cream/box-tracked ones suitable for inward stock.
 */
fun getInwardOnlyCategories(categories: List<ProductCategory>): List<ProductCategory> =
    categories.filter { it.trackBoxes }

/**
 * When an item/variety is selected in inward stock dialog,
 * auto-fill saved cost, MRP, and amount.
 * 
 * @param varietyName The selected variety name
 * @param existingStock The saved stock record if available
 * @param quantity Number of boxes being added
 * @return Updated draft item with auto-filled pricing
 */
fun InwardBillItemDraft.autoFillFromSavedVariety(
    varietyName: String,
    existingStock: IceCreamVarietyStock?,
    quantity: Int = this.boxes
): InwardBillItemDraft {
    val savedCost = existingStock?.lastCostPrice?.takeIf { it > 0.0 } ?: this.costPrice
    val savedMrp = existingStock?.lastMrp?.takeIf { it > 0.0 } ?: this.sellingPrice
    val calculatedAmount = quantity * savedCost

    return this.copy(
        name = varietyName,
        boxes = quantity,
        boxesInput = quantity.toString(),
        costPrice = savedCost,
        costPriceInput = savedCost.toString(),
        sellingPrice = savedMrp,
        sellingPriceInput = savedMrp.toString(),
        lineAmount = calculatedAmount
    )
}

/**
 * When a quantity is changed, recalculate line amount.
 */
fun InwardBillItemDraft.recalculateAmount(newQuantity: Int): InwardBillItemDraft =
    this.copy(
        boxes = newQuantity,
        boxesInput = newQuantity.toString(),
        lineAmount = newQuantity * this.costPrice
    )

/**
 * When cost price changes, recalculate line amount and clear manual override.
 */
fun InwardBillItemDraft.updateCostAndAmount(newCost: Double): InwardBillItemDraft =
    this.copy(
        costPrice = newCost,
        costPriceInput = newCost.toString(),
        lineAmount = this.boxes * newCost
    )

/**
 * When MRP changes, update selling price.
 */
fun InwardBillItemDraft.updateMrp(newMrp: Double): InwardBillItemDraft =
    this.copy(
        sellingPrice = newMrp,
        sellingPriceInput = newMrp.toString()
    )

/**
 * Validate a draft item before saving to database.
 */
data class DraftValidationResult(
    val isValid: Boolean,
    val errorMessage: String = ""
)

fun InwardBillItemDraft.validate(): DraftValidationResult {
    if (this.name.isBlank()) {
        return DraftValidationResult(false, "Item name cannot be empty")
    }
    if (this.boxes <= 0) {
        return DraftValidationResult(false, "Quantity must be greater than 0")
    }
    if (this.costPrice < 0.01) {
        return DraftValidationResult(false, "Cost price must be greater than 0")
    }
    if (this.sellingPrice < 0) {
        return DraftValidationResult(false, "MRP cannot be negative")
    }
    return DraftValidationResult(true)
}

/**
 * Calculate payment status based on amounts.
 */
fun calculatePaymentStatus(billTotal: Double, paidAmount: Double): String = when {
    paidAmount >= billTotal - 0.5 -> "PAID"
    paidAmount <= 0.0 -> "UNPAID"
    else -> "PARTIAL"
}

/**
 * Calculate due amount for supplier.
 */
fun calculateDueAmount(billTotal: Double, cashPaid: Double, onlinePaid: Double): Double {
    val totalPaid = cashPaid + onlinePaid
    return (billTotal - totalPaid).coerceAtLeast(0.0)
}
