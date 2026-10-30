package com.refreshsoda.app.data.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.refreshsoda.app.data.local.ShopDao
import com.refreshsoda.app.data.model.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Cloud mirror of the shop database.
 *
 *  - Everything lives under shops/{shopId}/..., so one shop can never read or overwrite another shop's data.
 *    (shopId is the owner's account id; staff accounts carry their owner's id.)
 *  - Users are stored in the top-level "users" collection and looked up with indexed field queries,
 *    never by downloading the whole collection.
 *  - Large, ever-growing tables (sales, expenses, bills ...) are stored one document per row.
 *    Small master tables (categories, companies, stock, suppliers, offers ...) are stored as one JSON
 *    snapshot document each (shops/{shopId}/snapshots/{table}) so deletes propagate correctly.
 *  - Writes are queued by the Firestore SDK when offline; we only wait a few seconds so the UI never hangs.
 *
 * Security: this app uses its own account table, so Firestore cannot tell users apart. We sign in to
 * Firebase anonymously so the bundled firestore.rules can at least reject unauthenticated access.
 * Enable "Anonymous" sign-in in the Firebase console and publish firestore.rules.
 */
class FirebaseSyncService {
    private val firestore = FirebaseFirestore.getInstance()

    @Volatile
    var shopId: String = ""

    private fun shopCollection(name: String) =
        firestore.collection("shops").document(shopId).collection(name)

    private suspend fun ensureSignedIn() {
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                withTimeoutOrNull(WRITE_TIMEOUT_MS) { auth.signInAnonymously().await() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Waits at most [timeoutMs]; a timeout is not an error because the SDK keeps the write queued. */
    private suspend fun <T> Task<T>.awaitBriefly(timeoutMs: Long = WRITE_TIMEOUT_MS): T? =
        withTimeoutOrNull(timeoutMs) { await() }

    // ---------------------------------------------------------------------------------------------
    // Generic writes
    // ---------------------------------------------------------------------------------------------
    suspend fun upsert(collection: String, id: String, data: Map<String, Any?>) {
        if (shopId.isBlank() || id.isBlank()) return
        try {
            ensureSignedIn()
            shopCollection(collection).document(id).set(data).awaitBriefly()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun delete(collection: String, id: String) {
        if (shopId.isBlank() || id.isBlank()) return
        try {
            ensureSignedIn()
            shopCollection(collection).document(id).delete().awaitBriefly()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun pushTable(name: String, rows: List<Map<String, Any?>>) {
        if (shopId.isBlank()) return
        try {
            ensureSignedIn()
            val payload = mapOf("json" to rowsToJson(rows), "updatedAt" to System.currentTimeMillis())
            shopCollection("snapshots").document(name).set(payload).awaitBriefly()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun pullTable(name: String): List<Map<String, Any?>> {
        val doc = shopCollection("snapshots").document(name).get().awaitBriefly(READ_TIMEOUT_MS)
            ?: throw IllegalStateException("Cloud request timed out. Check your internet connection.")
        val json = doc.getString("json") ?: return emptyList()
        return jsonToRows(json)
    }

    // ---------------------------------------------------------------------------------------------
    // Typed per-row sync (names kept so existing call sites keep working)
    // ---------------------------------------------------------------------------------------------
    suspend fun syncSale(sale: SaleTransaction) = upsert("sales", sale.id, sale.toMap())
    suspend fun syncProduct(product: Product) = upsert("products", product.id, product.toMap())
    suspend fun syncKhata(khata: CustomerKhata) = upsert("khata", khata.id, khata.toMap())
    suspend fun syncRepayment(repayment: KhataRepayment) = upsert("khata_repayments", repayment.id, repayment.toMap())
    suspend fun syncExpense(expense: Expense) = upsert("expenses", expense.id, expense.toMap())
    suspend fun syncClosing(closing: DayClosing) = upsert("closings", closing.date, closing.toMap())
    suspend fun syncSettlement(settlement: PaytmSettlement) = upsert("settlements", settlement.date, settlement.toMap())
    suspend fun syncPurchase(purchase: SupplierPurchase) = upsert("supplier_purchases", purchase.id, purchase.toMap())
    suspend fun syncSupplierPayment(payment: SupplierPayment) = upsert("supplier_payments", payment.id, payment.toMap())
    suspend fun syncSupplierReturn(item: SupplierReturn) = upsert("supplier_returns", item.id, item.toMap())
    suspend fun syncWastage(record: WastageRecord) = upsert("wastage", record.id, record.toMap())

    suspend fun syncPurchaseItems(items: List<SupplierPurchaseItem>) {
        items.forEach { upsert("supplier_purchase_items", it.id, it.toMap()) }
    }

    suspend fun deletePurchaseCascade(
        purchaseId: String,
        itemIds: List<String>,
        paymentIds: List<String>,
        returnIds: List<String>
    ) {
        itemIds.forEach { delete("supplier_purchase_items", it) }
        paymentIds.forEach { delete("supplier_payments", it) }
        returnIds.forEach { delete("supplier_returns", it) }
        delete("supplier_purchases", purchaseId)
    }

    // ---------------------------------------------------------------------------------------------
    // Users
    // ---------------------------------------------------------------------------------------------
    suspend fun syncUserToCloud(user: UserAccount) {
        try {
            ensureSignedIn()
            firestore.collection("users").document(user.id).set(user.toMap()).awaitBriefly()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteUserFromCloud(userId: String) {
        try {
            ensureSignedIn()
            firestore.collection("users").document(userId).delete().awaitBriefly()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun findUserById(userId: String): UserAccount? {
        ensureSignedIn()
        val document = firestore.collection("users").document(userId).get().awaitBriefly(READ_TIMEOUT_MS)
            ?: throw IllegalStateException("Cloud request timed out.")
        return document.takeIf { it.exists() }?.let(::userFromDocument)
    }

    /** Indexed lookup (limit 1). Accounts created by older app versions lack the lookup fields until they log in once on the old device. */
    suspend fun findUserByEmailOrPhone(identifier: String): UserAccount? {
        ensureSignedIn()
        val normalized = identifier.trim()
        val digits = normalized.filter(Char::isDigit)
        val users = firestore.collection("users")

        val byEmailOrPhone = users.whereEqualTo("emailOrPhoneLower", normalized.lowercase()).limit(1)
            .get().awaitBriefly(READ_TIMEOUT_MS)
            ?: throw IllegalStateException("Cloud request timed out.")
        val emailMatch = byEmailOrPhone.documents.firstOrNull()
        if (emailMatch != null) return userFromDocument(emailMatch)

        val looksLikePhone = normalized.all { it.isDigit() || it in "+-(). " }
        if (looksLikePhone && digits.length >= 10) {
            val byPhone = users.whereEqualTo("phoneLast10", digits.takeLast(10)).limit(1)
                .get().awaitBriefly(READ_TIMEOUT_MS)
                ?: throw IllegalStateException("Cloud request timed out.")
            val phoneMatch = byPhone.documents.firstOrNull()
            if (phoneMatch != null) return userFromDocument(phoneMatch)
        }
        return null
    }

    private fun userFromDocument(document: DocumentSnapshot): UserAccount? {
        val data = document.data ?: return null
        return userFromMap(data, document.id)
    }

    // ---------------------------------------------------------------------------------------------
    // Full backup / restore
    // ---------------------------------------------------------------------------------------------
    /** Pushes every table of the local database to the cloud. Returns the number of records written. */
    suspend fun backupEverything(dao: ShopDao): Int {
        if (shopId.isBlank()) throw IllegalStateException("Login first.")
        ensureSignedIn()
        var count = 0

        suspend fun <T> rows(list: List<T>, id: (T) -> String, map: (T) -> Map<String, Any?>, collection: String) {
            list.forEach { upsert(collection, id(it), map(it)) }
            count += list.size
        }

        rows(dao.snapshotProducts(), { it.id }, { it.toMap() }, "products")
        rows(dao.snapshotSales(), { it.id }, { it.toMap() }, "sales")
        rows(dao.snapshotKhata(), { it.id }, { it.toMap() }, "khata")
        rows(dao.snapshotRepayments(), { it.id }, { it.toMap() }, "khata_repayments")
        rows(dao.snapshotExpenses(), { it.id }, { it.toMap() }, "expenses")
        rows(dao.snapshotClosings(), { it.date }, { it.toMap() }, "closings")
        rows(dao.snapshotSettlements(), { it.date }, { it.toMap() }, "settlements")
        rows(dao.snapshotPurchases(), { it.id }, { it.toMap() }, "supplier_purchases")
        rows(dao.snapshotPurchaseItems(), { it.id }, { it.toMap() }, "supplier_purchase_items")
        rows(dao.snapshotSupplierPayments(), { it.id }, { it.toMap() }, "supplier_payments")
        rows(dao.snapshotSupplierReturns(), { it.id }, { it.toMap() }, "supplier_returns")
        rows(dao.snapshotWastage(), { it.id }, { it.toMap() }, "wastage")
        dao.snapshotUsers()
            .filter { it.effectiveShopId == shopId }
            .forEach { syncUserToCloud(it); count++ }

        pushTable("categories", dao.snapshotCategories().map { it.toMap() })
        pushTable("sub_categories", dao.snapshotSubCategories().map { it.toMap() })
        pushTable("variety_stock", dao.snapshotVarietyStock().map { it.toMap() })
        pushTable("suppliers", dao.snapshotSuppliers().map { it.toMap() })
        pushTable("offers", dao.snapshotOffers().map { it.toMap() })
        pushTable("brands", dao.snapshotBrands().map { it.toMap() })
        pushTable("profile", listOfNotNull(dao.snapshotProfile()).map { it.toMap() })
        return count
    }

    /** Restores this shop's data from the cloud. Throws on network problems instead of reporting a fake success. */
    suspend fun restoreAllFromCloud(dao: ShopDao): Int {
        if (shopId.isBlank()) throw IllegalStateException("Login first.")
        ensureSignedIn()
        var restored = 0

        suspend fun documents(collection: String): List<Map<String, Any?>> {
            val snap = shopCollection(collection).get().awaitBriefly(READ_TIMEOUT_MS)
                ?: throw IllegalStateException("Cloud request timed out. Check your internet connection.")
            return snap.documents.mapNotNull { it.data }
        }

        // Accounts of this shop
        val userSnap = firestore.collection("users").whereEqualTo("shopId", shopId).limit(100).get().awaitBriefly(READ_TIMEOUT_MS)
            ?: throw IllegalStateException("Cloud request timed out. Check your internet connection.")
        userSnap.documents.forEach { doc ->
            userFromDocument(doc)?.let { dao.insertUser(it); restored++ }
        }

        // Master snapshots
        pullTable("brands").mapNotNull(::brandFromMap).also { dao.insertBrands(it); restored += it.size }
        pullTable("profile").firstOrNull()?.let { dao.insertOrUpdateProfile(profileFromMap(it)); restored++ }
        pullTable("categories").mapNotNull(::categoryFromMap).also { dao.insertCategories(it); restored += it.size }
        pullTable("sub_categories").mapNotNull(::subCategoryFromMap).also { dao.insertSubCategories(it); restored += it.size }
        pullTable("variety_stock").mapNotNull(::varietyStockFromMap).also { dao.insertVarietyStocks(it); restored += it.size }
        pullTable("suppliers").mapNotNull(::supplierFromMap).also { dao.insertSuppliers(it); restored += it.size }
        pullTable("offers").mapNotNull(::offerFromMap).also { dao.insertOffers(it); restored += it.size }

        // Row documents
        documents("products").mapNotNull(::productFromMap).also { dao.insertProducts(it); restored += it.size }
        documents("sales").mapNotNull(::saleFromMap).forEach { dao.insertSale(it); restored++ }
        documents("khata").mapNotNull(::khataFromMap).forEach { dao.insertOrUpdateKhata(it); restored++ }
        documents("khata_repayments").mapNotNull(::repaymentFromMap).forEach { dao.insertRepayment(it); restored++ }
        documents("expenses").mapNotNull(::expenseFromMap).forEach { dao.insertExpense(it); restored++ }
        documents("closings").mapNotNull(::closingFromMap).forEach { dao.insertOrUpdateClosing(it); restored++ }
        documents("settlements").mapNotNull(::settlementFromMap).forEach { dao.insertSettlementRestore(it); restored++ }
        documents("supplier_purchases").mapNotNull(::purchaseFromMap).forEach { dao.insertSupplierPurchase(it); restored++ }
        documents("supplier_purchase_items").mapNotNull(::purchaseItemFromMap).also { dao.insertPurchaseItems(it); restored += it.size }
        documents("supplier_payments").mapNotNull(::supplierPaymentFromMap).forEach { dao.insertSupplierPayment(it); restored++ }
        documents("supplier_returns").mapNotNull(::supplierReturnFromMap).forEach { dao.insertSupplierReturn(it); restored++ }
        documents("wastage").mapNotNull(::wastageFromMap).forEach { dao.insertWastage(it); restored++ }

        dao.reconcileCompanyBoxes()
        return restored
    }

    companion object {
        private const val WRITE_TIMEOUT_MS = 6_000L
        private const val READ_TIMEOUT_MS = 20_000L
    }
}
