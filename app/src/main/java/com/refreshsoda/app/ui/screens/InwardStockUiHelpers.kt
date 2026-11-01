package com.refreshsoda.app.ui.screens

import com.refreshsoda.app.data.model.IceCreamVarietyStock
import com.refreshsoda.app.data.model.InwardBillItemDraft
import com.refreshsoda.app.data.model.ProductCategory

/** Shared rules for the inward-stock UI. */
fun iceCreamOnlyInwardCategories(categories: List<ProductCategory>): List<ProductCategory> =
    categories.filter { it.trackBoxes }

/**
 * Applies saved variety pricing when an inward-stock item is selected.
 * Manual values remain untouched when no saved price exists.
 */
fun InwardBillItemDraft.applySavedVarietyPricing(
    stock: IceCreamVarietyStock?,
    quantityOverride: Int? = null
): InwardBillItemDraft {
    val quantity = quantityOverride ?: boxes
    val cost = stock?.lastCostPrice?.takeIf { it > 0.0 } ?: costPrice
    val mrp = stock?.lastMrp?.takeIf { it > 0.0 } ?: sellingPrice
    val amount = quantity.toDouble() * cost
    return copy(
        boxes = quantity,
        boxesInput = quantity.toString(),
        costPrice = cost,
        costPriceInput = if (cost > 0.0) cost.toString() else costPriceInput,
        sellingPrice = mrp,
        sellingPriceInput = if (mrp > 0.0) mrp.toString() else sellingPriceInput,
        lineAmount = amount,
        id = id
    )
}
