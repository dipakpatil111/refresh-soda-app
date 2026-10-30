package com.refreshsoda.app.ui.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.refreshsoda.app.data.firebase.toMap
import com.refreshsoda.app.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

/** Maximum boxes / quantity on one bill line. Used by both the dialog and the ViewModel. */
const val MAX_INWARD_QTY = 9999

// =================================================================================================
// INWARD STOCK (maal aavak) v2
// =================================================================================================

data class InwardRequest(
    /** Non-null when an existing bill is being edited. */
    val purchaseId: String? = null,
    val brand: String,
    val category: String,
    val billNumber: String,
    val billDate: String,
    /** Total printed on the supplier bill (GST, round-off ...). Null = use the sum of the lines. */
    val printedTotal: Double? = null,
    /** Amount paid together with the bill. Ignored when editing (payments are managed separately). */
    val paidAmount: Double,
    val paymentMode: String,
    val items: List<InwardBillItemDraft>,
    /** Path of the bill photo already copied into app storage. */
    val billImagePath: String? = null,
    /** Also overwrite the menu selling price of matching products with the bill MRP. */
    val updateMenuPrice: Boolean = false
)

fun paymentStatusFor(total: Double, paid: Double): String = when {
    paid >= total - 0.5 -> "PAID"
    paid <= 0.0 -> "UNPAID"
    else -> "PARTIAL"
}

fun SupplierPurchase.balance(): Double = (totalAmount - paidAmount).coerceAtLeast(0.0)

private val legacyItemRegex = Regex("""(.+?) \((\d+) Box • Cost ₹([0-9.]+) • MRP ₹([0-9.]+)\)(?:, |$)""")

/** Bills saved by older app versions only have a text summary; this rebuilds item rows from it (best effort). */
fun parseLegacyItems(purchase: SupplierPurchase): List<SupplierPurchaseItem> =
    legacyItemRegex.findAll(purchase.itemsSummary).map { match ->
        val qty = match.groupValues[2].toIntOrNull() ?: 0
        val cost = match.groupValues[3].toDoubleOrNull() ?: 0.0
        SupplierPurchaseItem(
            purchaseId = purchase.id,
            varietyName = match.groupValues[1].trim(),
            quantity = qty,
            costPerBox = cost,
            mrp = match.groupValues[4].toDoubleOrNull() ?: 0.0,
            lineAmount = qty * cost
        )
    }.toList()

/** Items of a bill: structured rows when present, otherwise parsed from the legacy text summary. */
suspend fun ShopViewModel.itemsOfPurchase(purchase: SupplierPurchase): List<SupplierPurchaseItem> {
    val rows = dao.getItemsForPurchase(purchase.id)
    return if (rows.isNotEmpty()) rows else parseLegacyItems(purchase)
}

fun ShopViewModel.loadPurchaseItems(purchase: SupplierPurchase, onLoaded: (List<SupplierPurchaseItem>) -> Unit) {
    viewModelScope.launch { onLoaded(itemsOfPurchase(purchase)) }
}

/** Takes stock back out for a bill's items (used by delete and edit). Runs inside the caller's transaction. */
private suspend fun ShopViewModel.reverseStock(
    category: String,
    company: String,
    items: List<SupplierPurchaseItem>,
    returns: List<SupplierReturn>
) {
    // One-shot query (not a Flow): collecting a Room Flow inside a transaction can deadlock.
    val trackBoxes = dao.snapshotCategories().firstOrNull {
        it.name.equals(category, ignoreCase = true)
    }?.trackBoxes == true
    if (!trackBoxes) return
    items.forEach { item ->
        val alreadyReturned = returns
            .filter { it.varietyName.equals(item.varietyName, ignoreCase = true) }
            .sumOf { it.quantity }
        val toRemove = (item.quantity - alreadyReturned).coerceAtLeast(0)
        if (toRemove > 0) {
            val current = dao.getIceCreamVarietyStock(category, company, item.varietyName.trim())
            if (current != null) {
                dao.insertIceCreamVarietyStock(
                    current.copy(sealedBoxes = (current.sealedBoxes - toRemove).coerceAtLeast(0))
                )
            }
        }
    }
}

/** Creates (or edits) a supplier bill, its item rows, stock, supplier master row and product cost links in ONE transaction. */
fun ShopViewModel.saveInwardPurchase(request: InwardRequest, onComplete: (String?) -> Unit) {
    viewModelScope.launch {
        try {
            val categoryName = request.category.trim()
            val brandName = request.brand.trim()
            if (categoryName.isBlank() || brandName.isBlank()) {
                onComplete("Supplier aur category chuniye.")
                return@launch
            }
            val category = dao.getAllCategories().first().firstOrNull {
                it.name.equals(categoryName, ignoreCase = true)
            }
            if (category == null) {
                onComplete("Ye category ab maujood nahi hai.")
                return@launch
            }
            val trackBoxes = category.trackBoxes
            val items = request.items
            if (items.isEmpty() || items.any {
                    it.name.isBlank() || it.boxes !in 1..MAX_INWARD_QTY ||
                        it.costPrice !in 0.01..100000.0 || it.sellingPrice !in 0.0..100000.0
                }
            ) {
                onComplete("Har item ka naam, quantity (1-$MAX_INWARD_QTY) aur cost bhariye.")
                return@launch
            }
            val linesTotal = items.sumOf { it.lineAmount ?: it.lineTotal }
            val total = request.printedTotal ?: linesTotal
            if (total <= 0.0) {
                onComplete("Bill ka total zero nahi ho sakta.")
                return@launch
            }
            val editingId = request.purchaseId
            val typedBillNumber = request.billNumber.trim()
            if (typedBillNumber.isNotBlank() &&
                dao.countDuplicateBills(brandName, typedBillNumber, editingId ?: "") > 0
            ) {
                onComplete("Is supplier ka bill number \"$typedBillNumber\" pehle se save hai.")
                return@launch
            }
            if (editingId == null && request.paidAmount !in 0.0..total) {
                onComplete("Paid amount 0 se bill total ke beech hona chahiye.")
                return@launch
            }

            val oldPurchase = editingId?.let { dao.getPurchaseById(it) }
            if (editingId != null && oldPurchase == null) {
                onComplete("Ye bill ab maujood nahi hai.")
                return@launch
            }

            val purchaseId = editingId ?: UUID.randomUUID().toString()
            val billNumber = typedBillNumber.ifBlank { oldPurchase?.billNumber ?: "INV-${System.currentTimeMillis() % 10000}" }
            val itemRows = items.map { draft ->
                SupplierPurchaseItem(
                    purchaseId = purchaseId,
                    varietyName = draft.name.trim(),
                    quantity = draft.boxes,
                    costPerBox = draft.costPrice,
                    mrp = draft.sellingPrice,
                    lineAmount = draft.lineAmount ?: draft.lineTotal,
                    piecesPerBox = draft.piecesPerBox
                )
            }
            val summary = itemRows.joinToString(", ") { item ->
                "${item.varietyName} (${item.quantity} Box • Cost ₹${item.costPerBox} • MRP ₹${item.mrp})"
            }
            val initialPayment = if (oldPurchase == null && request.paidAmount > 0.0) {
                SupplierPayment(
                    purchaseId = purchaseId,
                    brand = brandName,
                    amount = request.paidAmount,
                    paymentMode = request.paymentMode,
                    note = "Bill ke saath"
                )
            } else {
                null
            }
            val paid = oldPurchase?.paidAmount ?: request.paidAmount

            var removedItemIds: List<String> = emptyList()
            val updatedProducts = mutableListOf<Product>()
            lateinit var savedPurchase: SupplierPurchase

            db.withTransaction {
                // Supplier = company. Only box-tracked categories keep a per-category company row.
                val company = if (trackBoxes) {
                    dao.getSubCategories(categoryName).firstOrNull { it.name.equals(brandName, ignoreCase = true) }
                        ?: SubCategory(categoryName = categoryName, name = brandName).also { dao.insertSubCategory(it) }
                } else {
                    null
                }
                val companyName = company?.name ?: brandName

                if (oldPurchase != null) {
                    val oldItems = itemsOfPurchase(oldPurchase)
                    val oldReturns = dao.getReturnsForPurchase(oldPurchase.id)
                    reverseStock(oldPurchase.category.ifBlank { categoryName }, oldPurchase.brand, oldItems, oldReturns)
                    removedItemIds = oldItems.map { it.id }
                    dao.deleteItemsForPurchase(oldPurchase.id)
                }

                dao.insertPurchaseItems(itemRows)

                if (trackBoxes) {
                    itemRows
                        .groupBy { it.varietyName.lowercase(Locale.getDefault()) }
                        .values
                        .forEach { group ->
                            val last = group.last()
                            addVarietyBoxes(
                                categoryName = categoryName,
                                companyName = companyName,
                                varietyName = last.varietyName,
                                count = group.sumOf { it.quantity },
                                costPrice = last.costPerBox,
                                mrp = last.mrp
                            )
                        }
                    dao.reconcileCompanyBoxes()
                }

                // Link the bill cost (and optionally MRP) to the matching menu product so profit reports use real cost.
                val menuProducts = dao.snapshotProducts()
                itemRows.forEach { item ->
                    val product = menuProducts.firstOrNull {
                        it.category.equals(categoryName, ignoreCase = true) &&
                            it.name.trim().equals(item.varietyName, ignoreCase = true)
                    } ?: return@forEach
                    val pieces = item.piecesPerBox
                    val unitCost = if (pieces > 0) item.costPerBox / pieces else if (trackBoxes) null else item.costPerBox
                    val unitMrp = if (pieces > 0) item.mrp / pieces else if (trackBoxes) null else item.mrp
                    var updated = product
                    if (unitCost != null && unitCost > 0.0) updated = updated.copy(costPrice = unitCost)
                    if (request.updateMenuPrice && unitMrp != null && unitMrp > 0.0) updated = updated.copy(price = unitMrp)
                    if (updated != product) {
                        dao.insertProduct(updated)
                        updatedProducts.add(updated)
                    }
                }

                val supplierKey = brandName.lowercase(Locale.getDefault())
                if (dao.snapshotSuppliers().none { it.id == supplierKey }) {
                    dao.insertSupplier(Supplier(id = supplierKey, name = brandName))
                }

                savedPurchase = SupplierPurchase(
                    id = purchaseId,
                    brand = brandName,
                    category = categoryName,
                    billNumber = billNumber,
                    billDate = request.billDate.trim().ifBlank { todayDateString },
                    totalAmount = total,
                    paidAmount = paid,
                    paymentStatus = paymentStatusFor(total, paid),
                    paymentMode = oldPurchase?.paymentMode ?: request.paymentMode,
                    totalBoxes = if (trackBoxes) itemRows.sumOf { it.quantity } else 0,
                    itemsSummary = summary,
                    billImageUri = request.billImagePath ?: oldPurchase?.billImageUri,
                    timestamp = oldPurchase?.timestamp ?: System.currentTimeMillis()
                )
                dao.insertSupplierPurchase(savedPurchase)
                if (initialPayment != null) dao.insertSupplierPayment(initialPayment)

                dao.insertAuditLog(
                    AuditLog(
                        id = UUID.randomUUID().toString(),
                        action = if (oldPurchase == null) "STOCK_INWARD_BILL" else "STOCK_INWARD_EDITED",
                        details = "Inward $brandName ($categoryName): ${savedPurchase.totalBoxes} Boxes, " +
                            "Bill ₹${total.toInt()} [${savedPurchase.paymentStatus}, Paid: ₹${paid.toInt()}]"
                    )
                )
            }

            onComplete(null)

            // Cloud sync happens after the local save; the UI never waits for it.
            removedItemIds.forEach { firebaseSync.delete("supplier_purchase_items", it) }
            firebaseSync.syncPurchase(savedPurchase)
            firebaseSync.syncPurchaseItems(itemRows)
            if (initialPayment != null) firebaseSync.syncSupplierPayment(initialPayment)
            updatedProducts.forEach { firebaseSync.syncProduct(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(e.localizedMessage ?: "Bill save nahi ho paya.")
        }
    }
}

/** Deletes a bill and takes its boxes back out of stock (items, payments and returns are removed too). */
fun ShopViewModel.deleteInwardPurchase(purchase: SupplierPurchase, onComplete: (String?) -> Unit) {
    viewModelScope.launch {
        try {
            var itemIds: List<String> = emptyList()
            var paymentIds: List<String> = emptyList()
            var returnIds: List<String> = emptyList()
            db.withTransaction {
                val items = itemsOfPurchase(purchase)
                val payments = dao.getPaymentsForPurchase(purchase.id)
                val returns = dao.getReturnsForPurchase(purchase.id)
                reverseStock(purchase.category, purchase.brand, items, returns)
                dao.deleteItemsForPurchase(purchase.id)
                dao.deletePaymentsForPurchase(purchase.id)
                dao.deleteReturnsForPurchase(purchase.id)
                dao.deleteSupplierPurchase(purchase)
                dao.reconcileCompanyBoxes()
                itemIds = items.map { it.id }
                paymentIds = payments.map { it.id }
                returnIds = returns.map { it.id }
                dao.insertAuditLog(
                    AuditLog(
                        id = UUID.randomUUID().toString(),
                        action = "STOCK_INWARD_DELETED",
                        details = "Deleted bill #${purchase.billNumber} of ${purchase.brand} (₹${purchase.totalAmount.toInt()}), stock reversed"
                    )
                )
            }
            onComplete(null)
            firebaseSync.deletePurchaseCascade(purchase.id, itemIds, paymentIds, returnIds)
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(e.localizedMessage ?: "Bill delete nahi ho paya.")
        }
    }
}

// =================================================================================================
// SUPPLIER PAYMENTS (part payments) AND RETURNS
// =================================================================================================

fun ShopViewModel.addSupplierPayment(
    purchase: SupplierPurchase,
    amount: Double,
    paymentMode: String,
    note: String,
    onComplete: (String?) -> Unit
) {
    viewModelScope.launch {
        try {
            var savedPayment: SupplierPayment? = null
            var savedPurchase: SupplierPurchase? = null
            var error: String? = null
            db.withTransaction {
                val current = dao.getPurchaseById(purchase.id)
                if (current == null) {
                    error = "Ye bill ab maujood nahi hai."
                    return@withTransaction
                }
                val balance = current.balance()
                if (amount <= 0.0 || amount > balance + 0.5) {
                    error = "Amount 0 se zyada aur baaki rakam (₹${balance.toInt()}) se kam hona chahiye."
                    return@withTransaction
                }
                val applied = min(amount, balance)
                val newPaid = current.paidAmount + applied
                val payment = SupplierPayment(
                    purchaseId = current.id,
                    brand = current.brand,
                    amount = applied,
                    paymentMode = paymentMode,
                    note = note.trim()
                )
                dao.insertSupplierPayment(payment)
                dao.updatePurchasePayment(current.id, newPaid, paymentStatusFor(current.totalAmount, newPaid))
                dao.insertAuditLog(
                    AuditLog(
                        id = UUID.randomUUID().toString(),
                        action = "SUPPLIER_PAYMENT",
                        details = "Paid ₹${applied.toInt()} ($paymentMode) to ${current.brand} for bill #${current.billNumber}"
                    )
                )
                savedPayment = payment
                savedPurchase = dao.getPurchaseById(current.id)
            }
            onComplete(error)
            savedPayment?.let { firebaseSync.syncSupplierPayment(it) }
            savedPurchase?.let { firebaseSync.syncPurchase(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(e.localizedMessage ?: "Payment save nahi ho paya.")
        }
    }
}

/** Returns / damaged boxes sent back to the supplier. Reduces stock and credits the bill. */
fun ShopViewModel.recordSupplierReturn(
    purchase: SupplierPurchase,
    varietyName: String,
    quantity: Int,
    creditAmount: Double,
    reason: String,
    onComplete: (String?) -> Unit
) {
    viewModelScope.launch {
        try {
            var error: String? = null
            var savedReturn: SupplierReturn? = null
            var creditPayment: SupplierPayment? = null
            var savedPurchase: SupplierPurchase? = null
            db.withTransaction {
                val current = dao.getPurchaseById(purchase.id)
                if (current == null) {
                    error = "Ye bill ab maujood nahi hai."
                    return@withTransaction
                }
                if (quantity < 1 || creditAmount < 0.0 || varietyName.isBlank()) {
                    error = "Variety, quantity (kam se kam 1) aur credit bhariye."
                    return@withTransaction
                }
                val category = dao.snapshotCategories().firstOrNull {
                    it.name.equals(current.category, ignoreCase = true)
                }
                if (category?.trackBoxes == true) {
                    val stock = dao.getIceCreamVarietyStock(current.category, current.brand, varietyName.trim())
                    if (stock == null || stock.sealedBoxes < quantity) {
                        error = "Stock mein sirf ${stock?.sealedBoxes ?: 0} box hain, ${quantity} wapas nahi kar sakte."
                        return@withTransaction
                    }
                    dao.insertIceCreamVarietyStock(stock.copy(sealedBoxes = stock.sealedBoxes - quantity))
                    dao.reconcileCompanyBoxes()
                }
                val supplierReturn = SupplierReturn(
                    brand = current.brand,
                    category = current.category,
                    purchaseId = current.id,
                    varietyName = varietyName.trim(),
                    quantity = quantity,
                    creditAmount = creditAmount,
                    reason = reason.trim()
                )
                dao.insertSupplierReturn(supplierReturn)
                val applied = min(creditAmount, current.balance())
                if (applied > 0.0) {
                    val payment = SupplierPayment(
                        purchaseId = current.id,
                        brand = current.brand,
                        amount = applied,
                        paymentMode = "CREDIT_NOTE",
                        note = "Return: ${varietyName.trim()} x$quantity"
                    )
                    dao.insertSupplierPayment(payment)
                    val newPaid = current.paidAmount + applied
                    dao.updatePurchasePayment(current.id, newPaid, paymentStatusFor(current.totalAmount, newPaid))
                    creditPayment = payment
                }
                dao.insertAuditLog(
                    AuditLog(
                        id = UUID.randomUUID().toString(),
                        action = "SUPPLIER_RETURN",
                        details = "Returned $quantity x ${varietyName.trim()} to ${current.brand}, credit ₹${creditAmount.toInt()}"
                    )
                )
                savedReturn = supplierReturn
                savedPurchase = dao.getPurchaseById(current.id)
            }
            onComplete(error)
            savedReturn?.let { firebaseSync.syncSupplierReturn(it) }
            creditPayment?.let { firebaseSync.syncSupplierPayment(it) }
            savedPurchase?.let { firebaseSync.syncPurchase(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(e.localizedMessage ?: "Return save nahi ho paya.")
        }
    }
}

fun ShopViewModel.saveSupplier(name: String, phone: String, gstNumber: String, address: String) {
    val clean = name.trim()
    if (clean.isBlank()) return
    viewModelScope.launch {
        dao.insertSupplier(
            Supplier(
                id = clean.lowercase(Locale.getDefault()),
                name = clean,
                phone = phone.trim(),
                gstNumber = gstNumber.trim().uppercase(Locale.getDefault()),
                address = address.trim()
            )
        )
    }
}

// =================================================================================================
// WASTAGE (melted / damaged boxes)
// =================================================================================================

fun ShopViewModel.recordWastage(
    stock: IceCreamVarietyStock,
    quantity: Int,
    reason: String,
    onComplete: (String?) -> Unit
) {
    viewModelScope.launch {
        try {
            var error: String? = null
            var saved: WastageRecord? = null
            db.withTransaction {
                val current = dao.getIceCreamVarietyStock(stock.categoryName, stock.companyName, stock.varietyName)
                if (current == null || quantity < 1 || quantity > current.sealedBoxes) {
                    error = "Quantity 1 se ${current?.sealedBoxes ?: 0} ke beech honi chahiye."
                    return@withTransaction
                }
                dao.insertIceCreamVarietyStock(current.copy(sealedBoxes = current.sealedBoxes - quantity))
                dao.reconcileCompanyBoxes()
                val record = WastageRecord(
                    category = current.categoryName,
                    companyName = current.companyName,
                    varietyName = current.varietyName,
                    quantity = quantity,
                    estimatedCost = quantity * current.lastCostPrice,
                    reason = reason.trim().ifBlank { "Melted / kharab" }
                )
                dao.insertWastage(record)
                dao.insertAuditLog(
                    AuditLog(
                        id = UUID.randomUUID().toString(),
                        action = "WASTAGE",
                        details = "Wastage: $quantity box ${current.companyName} • ${current.varietyName} (₹${record.estimatedCost.toInt()})"
                    )
                )
                saved = record
            }
            onComplete(error)
            saved?.let { firebaseSync.syncWastage(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(e.localizedMessage ?: "Wastage save nahi ho paya.")
        }
    }
}

// =================================================================================================
// OFFERS AND BULK PRICE UPDATE
// =================================================================================================

fun ShopViewModel.saveOffer(offer: Offer) {
    if (offer.name.isBlank() || offer.value <= 0.0) return
    viewModelScope.launch { dao.insertOffer(offer) }
}

fun ShopViewModel.deleteOffer(offer: Offer) {
    viewModelScope.launch { dao.deleteOffer(offer) }
}

/** Raises (or lowers, with a negative percent) menu prices. categoryName = null means every category. */
fun ShopViewModel.bulkAdjustPrices(categoryName: String?, percent: Double, onComplete: (Int) -> Unit) {
    viewModelScope.launch {
        if (percent == 0.0 || percent < -90.0 || percent > 500.0) {
            onComplete(0)
            return@launch
        }
        val changed = mutableListOf<Product>()
        db.withTransaction {
            dao.snapshotProducts()
                .filter { categoryName == null || it.category.equals(categoryName, ignoreCase = true) }
                .forEach { product ->
                    val newPrice = (product.price * (1.0 + percent / 100.0)).roundToInt().toDouble().coerceAtLeast(0.0)
                    if (newPrice != product.price) {
                        val updated = product.copy(price = newPrice)
                        dao.insertProduct(updated)
                        changed.add(updated)
                    }
                }
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "BULK_PRICE_UPDATE",
                    details = "Prices ${if (percent > 0) "+" else ""}$percent% for ${categoryName ?: "all categories"} (${changed.size} items)"
                )
            )
        }
        onComplete(changed.size)
        changed.forEach { firebaseSync.syncProduct(it) }
    }
}
