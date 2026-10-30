package com.refreshsoda.app.data.firebase

import com.refreshsoda.app.data.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Explicit entity <-> Map conversion for Firestore.
 *
 * Why not `document.toObject(Entity::class.java)`? Firestore needs a public no-argument constructor,
 * and most of our entities (Product, SaleTransaction, Expense, ...) have required constructor
 * parameters, so `toObject()` throws and the old restore silently stopped after the first collection.
 * Boolean properties named `isXxx` are also serialised under a different key by Firestore's
 * JavaBean mapper. Plain maps avoid both problems.
 */

private fun Map<String, Any?>.str(key: String, default: String = ""): String = (this[key] as? String) ?: default
private fun Map<String, Any?>.dbl(key: String, default: Double = 0.0): Double = (this[key] as? Number)?.toDouble() ?: default
private fun Map<String, Any?>.int(key: String, default: Int = 0): Int = (this[key] as? Number)?.toInt() ?: default
private fun Map<String, Any?>.lng(key: String, default: Long = 0L): Long = (this[key] as? Number)?.toLong() ?: default
private fun Map<String, Any?>.lngOrNull(key: String): Long? = (this[key] as? Number)?.toLong()
private fun Map<String, Any?>.intOrNull(key: String): Int? = (this[key] as? Number)?.toInt()
private fun Map<String, Any?>.strOrNull(key: String): String? = this[key] as? String
private fun Map<String, Any?>.bool(key: String, default: Boolean = false): Boolean = (this[key] as? Boolean) ?: default

// ---------------- Products / sales / khata / expenses ----------------
fun Product.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "category" to category, "brand" to brand, "price" to price,
    "sealedBoxes" to sealedBoxes, "costPrice" to costPrice, "lowStockLimit" to lowStockLimit
)

fun productFromMap(m: Map<String, Any?>): Product? {
    val id = m.strOrNull("id") ?: return null
    return Product(
        id = id,
        name = m.strOrNull("name") ?: return null,
        category = m.str("category"),
        brand = m.str("brand"),
        price = m.dbl("price"),
        sealedBoxes = m.int("sealedBoxes"),
        costPrice = m.dbl("costPrice"),
        lowStockLimit = m.intOrNull("lowStockLimit")
    )
}

fun SaleTransaction.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "totalAmount" to totalAmount, "discountAmount" to discountAmount, "paymentMode" to paymentMode,
    "customerName" to customerName, "itemsSummary" to itemsSummary, "itemsDetails" to itemsDetails,
    "timestamp" to timestamp, "soldBy" to soldBy
)

fun saleFromMap(m: Map<String, Any?>): SaleTransaction? {
    val id = m.strOrNull("id") ?: return null
    return SaleTransaction(
        id = id,
        totalAmount = m.dbl("totalAmount"),
        discountAmount = m.dbl("discountAmount"),
        paymentMode = m.str("paymentMode", "CASH"),
        customerName = m.strOrNull("customerName"),
        itemsSummary = m.str("itemsSummary"),
        itemsDetails = m.str("itemsDetails"),
        timestamp = m.lng("timestamp"),
        soldBy = m.str("soldBy")
    )
}

fun CustomerKhata.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "phone" to phone, "outstandingDebt" to outstandingDebt,
    "debtLimit" to debtLimit, "dueDateMillis" to dueDateMillis
)

fun khataFromMap(m: Map<String, Any?>): CustomerKhata? {
    val id = m.strOrNull("id") ?: return null
    return CustomerKhata(
        id = id,
        name = m.strOrNull("name") ?: return null,
        phone = m.str("phone"),
        outstandingDebt = m.dbl("outstandingDebt"),
        debtLimit = m.dbl("debtLimit", 2000.0),
        dueDateMillis = m.lngOrNull("dueDateMillis")
    )
}

fun KhataRepayment.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "customerId" to customerId, "customerName" to customerName, "amount" to amount,
    "paymentMode" to paymentMode, "timestamp" to timestamp
)

fun repaymentFromMap(m: Map<String, Any?>): KhataRepayment? {
    val id = m.strOrNull("id") ?: return null
    return KhataRepayment(
        id = id,
        customerId = m.str("customerId"),
        customerName = m.str("customerName"),
        amount = m.dbl("amount"),
        paymentMode = m.str("paymentMode", "CASH"),
        timestamp = m.lng("timestamp")
    )
}

fun Expense.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "title" to title, "category" to category, "amount" to amount,
    "paymentMode" to paymentMode, "timestamp" to timestamp
)

fun expenseFromMap(m: Map<String, Any?>): Expense? {
    val id = m.strOrNull("id") ?: return null
    return Expense(
        id = id,
        title = m.str("title"),
        category = m.str("category"),
        amount = m.dbl("amount"),
        paymentMode = m.str("paymentMode", "CASH"),
        timestamp = m.lng("timestamp")
    )
}

fun DayClosing.toMap(): Map<String, Any?> = mapOf(
    "date" to date, "openingCash" to openingCash, "cashSales" to cashSales,
    "udharCashRepayment" to udharCashRepayment, "cashExpenses" to cashExpenses,
    "expectedCash" to expectedCash, "actualCash" to actualCash, "discrepancy" to discrepancy,
    "timestamp" to timestamp
)

fun closingFromMap(m: Map<String, Any?>): DayClosing? {
    val date = m.strOrNull("date") ?: return null
    return DayClosing(
        date = date,
        openingCash = m.dbl("openingCash"),
        cashSales = m.dbl("cashSales"),
        udharCashRepayment = m.dbl("udharCashRepayment"),
        cashExpenses = m.dbl("cashExpenses"),
        expectedCash = m.dbl("expectedCash"),
        actualCash = m.dbl("actualCash"),
        discrepancy = m.dbl("discrepancy"),
        timestamp = m.lng("timestamp")
    )
}

fun PaytmSettlement.toMap(): Map<String, Any?> = mapOf(
    "date" to date, "totalUpiCollection" to totalUpiCollection, "chargeDeduction" to chargeDeduction,
    "netBankAmount" to netBankAmount, "settled" to isSettled, "timestamp" to timestamp
)

fun settlementFromMap(m: Map<String, Any?>): PaytmSettlement? {
    val date = m.strOrNull("date") ?: return null
    return PaytmSettlement(
        date = date,
        totalUpiCollection = m.dbl("totalUpiCollection"),
        chargeDeduction = m.dbl("chargeDeduction"),
        netBankAmount = m.dbl("netBankAmount"),
        // "isSettled" is what Firestore's JavaBean mapper wrote in older backups
        isSettled = m.bool("settled", m.bool("isSettled")),
        timestamp = m.lng("timestamp")
    )
}

// ---------------- Supplier ledger ----------------
/** The local bill photo path is device specific, so it is intentionally not uploaded. */
fun SupplierPurchase.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "brand" to brand, "category" to category, "billNumber" to billNumber, "billDate" to billDate,
    "totalAmount" to totalAmount, "paidAmount" to paidAmount, "paymentStatus" to paymentStatus,
    "paymentMode" to paymentMode, "totalBoxes" to totalBoxes, "itemsSummary" to itemsSummary,
    "timestamp" to timestamp
)

fun purchaseFromMap(m: Map<String, Any?>): SupplierPurchase? {
    val id = m.strOrNull("id") ?: return null
    return SupplierPurchase(
        id = id,
        brand = m.str("brand"),
        category = m.str("category"),
        billNumber = m.str("billNumber"),
        billDate = m.str("billDate"),
        totalAmount = m.dbl("totalAmount"),
        paidAmount = m.dbl("paidAmount"),
        paymentStatus = m.str("paymentStatus", "PAID"),
        paymentMode = m.str("paymentMode", "UPI"),
        totalBoxes = m.int("totalBoxes"),
        itemsSummary = m.str("itemsSummary"),
        billImageUri = null,
        timestamp = m.lng("timestamp")
    )
}

fun SupplierPurchaseItem.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "purchaseId" to purchaseId, "varietyName" to varietyName, "quantity" to quantity,
    "costPerBox" to costPerBox, "mrp" to mrp, "lineAmount" to lineAmount, "piecesPerBox" to piecesPerBox
)

fun purchaseItemFromMap(m: Map<String, Any?>): SupplierPurchaseItem? {
    val id = m.strOrNull("id") ?: return null
    return SupplierPurchaseItem(
        id = id,
        purchaseId = m.str("purchaseId"),
        varietyName = m.str("varietyName"),
        quantity = m.int("quantity"),
        costPerBox = m.dbl("costPerBox"),
        mrp = m.dbl("mrp"),
        lineAmount = m.dbl("lineAmount"),
        piecesPerBox = m.int("piecesPerBox")
    )
}

fun SupplierPayment.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "purchaseId" to purchaseId, "brand" to brand, "amount" to amount,
    "paymentMode" to paymentMode, "note" to note, "timestamp" to timestamp
)

fun supplierPaymentFromMap(m: Map<String, Any?>): SupplierPayment? {
    val id = m.strOrNull("id") ?: return null
    return SupplierPayment(
        id = id,
        purchaseId = m.str("purchaseId"),
        brand = m.str("brand"),
        amount = m.dbl("amount"),
        paymentMode = m.str("paymentMode", "CASH"),
        note = m.str("note"),
        timestamp = m.lng("timestamp")
    )
}

fun SupplierReturn.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "brand" to brand, "category" to category, "purchaseId" to purchaseId,
    "varietyName" to varietyName, "quantity" to quantity, "creditAmount" to creditAmount,
    "reason" to reason, "timestamp" to timestamp
)

fun supplierReturnFromMap(m: Map<String, Any?>): SupplierReturn? {
    val id = m.strOrNull("id") ?: return null
    return SupplierReturn(
        id = id,
        brand = m.str("brand"),
        category = m.str("category"),
        purchaseId = m.str("purchaseId"),
        varietyName = m.str("varietyName"),
        quantity = m.int("quantity"),
        creditAmount = m.dbl("creditAmount"),
        reason = m.str("reason"),
        timestamp = m.lng("timestamp")
    )
}

fun WastageRecord.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "category" to category, "companyName" to companyName, "varietyName" to varietyName,
    "quantity" to quantity, "estimatedCost" to estimatedCost, "reason" to reason, "timestamp" to timestamp
)

fun wastageFromMap(m: Map<String, Any?>): WastageRecord? {
    val id = m.strOrNull("id") ?: return null
    return WastageRecord(
        id = id,
        category = m.str("category"),
        companyName = m.str("companyName"),
        varietyName = m.str("varietyName"),
        quantity = m.int("quantity"),
        estimatedCost = m.dbl("estimatedCost"),
        reason = m.str("reason"),
        timestamp = m.lng("timestamp")
    )
}

// ---------------- Small master tables (stored as one JSON snapshot document each) ----------------
fun ProductCategory.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "orderIndex" to orderIndex, "trackBoxes" to trackBoxes, "lowStockLimit" to lowStockLimit
)

fun categoryFromMap(m: Map<String, Any?>): ProductCategory? {
    val id = m.strOrNull("id") ?: return null
    return ProductCategory(
        id = id,
        name = m.strOrNull("name") ?: return null,
        orderIndex = m.int("orderIndex"),
        trackBoxes = m.bool("trackBoxes"),
        lowStockLimit = m.int("lowStockLimit", 2)
    )
}

fun SubCategory.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "categoryName" to categoryName, "name" to name, "defaultPrice" to defaultPrice,
    "sealedBoxes" to sealedBoxes, "lowStockLimit" to lowStockLimit
)

fun subCategoryFromMap(m: Map<String, Any?>): SubCategory? {
    val id = m.strOrNull("id") ?: return null
    return SubCategory(
        id = id,
        categoryName = m.str("categoryName"),
        name = m.strOrNull("name") ?: return null,
        defaultPrice = m.dbl("defaultPrice"),
        sealedBoxes = m.int("sealedBoxes"),
        lowStockLimit = m.int("lowStockLimit", 2)
    )
}

fun IceCreamVarietyStock.toMap(): Map<String, Any?> = mapOf(
    "categoryName" to categoryName, "companyName" to companyName, "varietyName" to varietyName,
    "sealedBoxes" to sealedBoxes, "lowStockLimit" to lowStockLimit,
    "lastCostPrice" to lastCostPrice, "lastMrp" to lastMrp
)

fun varietyStockFromMap(m: Map<String, Any?>): IceCreamVarietyStock? {
    return IceCreamVarietyStock(
        categoryName = m.strOrNull("categoryName") ?: return null,
        companyName = m.strOrNull("companyName") ?: return null,
        varietyName = m.strOrNull("varietyName") ?: return null,
        sealedBoxes = m.int("sealedBoxes"),
        lowStockLimit = m.int("lowStockLimit", 2),
        lastCostPrice = m.dbl("lastCostPrice"),
        lastMrp = m.dbl("lastMrp")
    )
}

fun Supplier.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "phone" to phone, "gstNumber" to gstNumber, "address" to address
)

fun supplierFromMap(m: Map<String, Any?>): Supplier? {
    val id = m.strOrNull("id") ?: return null
    return Supplier(
        id = id,
        name = m.strOrNull("name") ?: return null,
        phone = m.str("phone"),
        gstNumber = m.str("gstNumber"),
        address = m.str("address")
    )
}

fun Offer.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "discountType" to discountType, "value" to value,
    "minBill" to minBill, "active" to active
)

fun offerFromMap(m: Map<String, Any?>): Offer? {
    val id = m.strOrNull("id") ?: return null
    return Offer(
        id = id,
        name = m.strOrNull("name") ?: return null,
        discountType = m.str("discountType", "PERCENT"),
        value = m.dbl("value"),
        minBill = m.dbl("minBill"),
        active = m.bool("active", true)
    )
}

fun ShopBrand.toMap(): Map<String, Any?> = mapOf("id" to id, "name" to name)

fun brandFromMap(m: Map<String, Any?>): ShopBrand? {
    val id = m.strOrNull("id") ?: return null
    return ShopBrand(id = id, name = m.strOrNull("name") ?: return null)
}

fun UserProfile.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "shopName" to shopName, "ownerName" to ownerName, "phone" to phone,
    "upiId" to upiId, "address" to address
)

fun profileFromMap(m: Map<String, Any?>): UserProfile = UserProfile(
    id = m.str("id", "shop_profile"),
    shopName = m.str("shopName"),
    ownerName = m.str("ownerName"),
    phone = m.str("phone"),
    upiId = m.str("upiId"),
    address = m.str("address")
)

// ---------------- Users ----------------
/** Extra lower-cased / digits-only fields let login look an account up with an indexed query instead of downloading every user. */
fun UserAccount.toMap(): Map<String, Any?> = mapOf(
    "id" to id, "name" to name, "emailOrPhone" to emailOrPhone,
    "emailOrPhoneLower" to emailOrPhone.trim().lowercase(),
    "phoneLast10" to phone.filter(Char::isDigit).takeLast(10),
    "password" to password, "role" to role, "shopName" to shopName, "phone" to phone,
    "createdAt" to createdAt, "lastLoginAt" to lastLoginAt, "shopId" to effectiveShopId
)

fun userFromMap(m: Map<String, Any?>, fallbackId: String = ""): UserAccount? {
    val emailOrPhone = m.strOrNull("emailOrPhone") ?: return null
    val password = m.strOrNull("password") ?: return null
    return UserAccount(
        id = m.strOrNull("id") ?: fallbackId.ifBlank { return null },
        name = m.strOrNull("name") ?: return null,
        emailOrPhone = emailOrPhone,
        password = password,
        role = m.str("role", "OWNER"),
        shopName = m.str("shopName"),
        phone = m.str("phone"),
        createdAt = m.lng("createdAt"),
        lastLoginAt = m.lng("lastLoginAt"),
        shopId = m.str("shopId")
    )
}

// ---------------- JSON helpers for snapshot documents ----------------
fun rowsToJson(rows: List<Map<String, Any?>>): String {
    val array = JSONArray()
    rows.forEach { row ->
        val obj = JSONObject()
        row.forEach { (key, value) -> obj.put(key, value ?: JSONObject.NULL) }
        array.put(obj)
    }
    return array.toString()
}

fun jsonToRows(json: String): List<Map<String, Any?>> {
    val array = JSONArray(json)
    val rows = ArrayList<Map<String, Any?>>(array.length())
    for (i in 0 until array.length()) {
        val obj = array.optJSONObject(i) ?: continue
        val row = HashMap<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.opt(key)
            row[key] = if (value == null || value == JSONObject.NULL) null else value
        }
        rows.add(row)
    }
    return rows
}
