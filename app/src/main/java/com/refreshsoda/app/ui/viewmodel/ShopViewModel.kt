package com.refreshsoda.app.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.refreshsoda.app.RefreshApp
import com.refreshsoda.app.data.firebase.FirebaseSyncService
import com.refreshsoda.app.data.firebase.toMap
import com.refreshsoda.app.data.local.AppDatabase
import com.refreshsoda.app.data.local.ShopDao
import com.refreshsoda.app.data.model.*
import com.refreshsoda.app.data.security.SecurityUtils
import com.refreshsoda.app.ui.i18n.AppLanguage
import com.refreshsoda.app.ui.i18n.AppStrings
import com.refreshsoda.app.ui.theme.AppThemeMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/** Variety row that holds boxes which were counted before per-variety tracking existed. */
const val UNASSIGNED_VARIETY = "Opening stock (unassigned)"

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val addOns: List<String> = emptyList(),
    val addOnCost: Double = 0.0,
    val customPrice: Double = product.price
) {
    val total: Double get() = (customPrice + addOnCost) * quantity
}

/** A customer's order that was parked ("hold") so the next customer can be served first. */
data class HeldBill(
    val id: String,
    val number: Int,
    val items: List<CartItem>,
    val discount: Double,
    val heldAt: Long
) {
    val total: Double get() = (items.sumOf { it.total } - discount).coerceAtLeast(0.0)
    val itemCount: Int get() = items.sumOf { it.quantity }
}

data class TopItemStats(
    val name: String = "N/A",
    val pieces: Int = 0,
    val revenue: Double = 0.0
)

/** Starting point the owner entered for cash-in-shop and online money; everything after setAt is added/subtracted. */
data class BalanceBase(val cash: Double = 0.0, val online: Double = 0.0, val setAt: Long = 0L)

/** Live shop balances. hasBase=false until the owner enters starting amounts. */
data class ShopBalance(
    val hasBase: Boolean = false,
    val cash: Double = 0.0,
    val online: Double = 0.0,
    val setAt: Long = 0L
) {
    val total: Double get() = cash + online
}

/** A custom date range picked on the Dashboard (both ends inclusive, in local time). */
data class DateRangeFilter(val startMillis: Long, val endMillis: Long, val label: String)

/** Business numbers for one period (today / this month / a picked date range). */
data class PeriodSummary(
    val totalSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val upiSales: Double = 0.0,
    val udharSales: Double = 0.0,
    val expenses: Double = 0.0,
    val billCount: Int = 0,
    val categorySales: Map<String, Double> = emptyMap(),
    val topItem: TopItemStats = TopItemStats(),
    val topSoda: String = "—",
    val topIceCream: String = "—"
) {
    val netProfit: Double get() = totalSales - expenses
}

fun buildPeriodSummary(
    sales: List<SaleTransaction>,
    expenses: List<Expense>,
    categories: List<ProductCategory>,
    start: Long,
    end: Long
): PeriodSummary {
    val periodSales = sales.filter { it.timestamp in start..end }
    val periodExpenses = expenses.filter { it.timestamp in start..end }

    val categoryMap = mutableMapOf<String, Double>()
    categories.forEach { categoryMap[it.name] = 0.0 }
    val itemMap = mutableMapOf<String, Pair<Int, Double>>()
    val sodaMap = mutableMapOf<String, Int>()
    val iceMap = mutableMapOf<String, Int>()

    periodSales.forEach { sale ->
        if (sale.itemsDetails.isBlank()) return@forEach
        sale.itemsDetails.split(";").forEach { itemStr ->
            val parts = itemStr.split("|")
            if (parts.size >= 4) {
                val cat = parts[0]
                val name = parts[1]
                val qty = parts[2].toIntOrNull() ?: 1
                val price = parts[3].toDoubleOrNull() ?: 0.0
                categoryMap[cat] = (categoryMap[cat] ?: 0.0) + qty * price
                val current = itemMap.getOrDefault(name, Pair(0, 0.0))
                itemMap[name] = Pair(current.first + qty, current.second + qty * price)
                val lowerCat = cat.trim().lowercase()
                if (lowerCat.isIceCreamCategory()) {
                    iceMap[name.trim()] = (iceMap[name.trim()] ?: 0) + qty
                } else if (lowerCat.contains("soda", ignoreCase = true)) {
                    sodaMap[name.trim()] = (sodaMap[name.trim()] ?: 0) + qty
                }
            }
        }
    }
    val top = itemMap.maxByOrNull { it.value.first }
    return PeriodSummary(
        totalSales = periodSales.sumOf { it.totalAmount },
        cashSales = periodSales.filter { it.paymentMode == "CASH" }.sumOf { it.totalAmount },
        upiSales = periodSales.filter { it.paymentMode == "UPI" }.sumOf { it.totalAmount },
        udharSales = periodSales.filter { it.paymentMode == "UDHAR" }.sumOf { it.totalAmount },
        expenses = periodExpenses.sumOf { it.amount },
        billCount = periodSales.size,
        categorySales = categoryMap,
        topItem = if (top != null) TopItemStats(top.key, top.value.first, top.value.second) else TopItemStats(),
        topSoda = sodaMap.maxByOrNull { it.value }?.let { "${it.key} (${it.value} glass)" } ?: "—",
        topIceCream = iceMap.maxByOrNull { it.value }?.let { "${it.key} (${it.value} pcs)" } ?: "—"
    )
}

fun startOfDayMillis(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun endOfDayMillis(millis: Long): Long = startOfDayMillis(millis) + 24L * 60 * 60 * 1000 - 1

fun startOfMonthMillis(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = startOfDayMillis(millis)
    set(Calendar.DAY_OF_MONTH, 1)
}.timeInMillis

data class IceCreamProfitStats(
    val totalSalesRevenue: Double = 0.0,
    val totalCostOfGoods: Double = 0.0,
    val grossProfit: Double = 0.0,
    val marginPercent: Double = 0.0
)

// ---------------- ANALYTICS SCREEN MODELS ----------------
data class OverallProfitSummary(
    val totalRevenue: Double = 0.0,
    val totalSalesCount: Int = 0,
    val totalCost: Double = 0.0,
    val grossProfit: Double = 0.0,
    val grossMarginPercent: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalWastage: Double = 0.0,
    val netProfit: Double = 0.0
)

data class CategoryProfitStat(
    val category: String,
    val itemsSold: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalCost: Double = 0.0,
    val grossProfit: Double = 0.0,
    val marginPercent: Double = 0.0,
    val percentageOfTotalSales: Double = 0.0,
    val colorHex: Long = 0xFF2A9D8F
)

data class DayTrendStat(
    val dayLabel: String,
    val revenue: Double = 0.0,
    val profit: Double = 0.0
)

data class TopProductRank(
    val rank: Int,
    val productName: String,
    val category: String,
    val qtySold: Int = 0,
    val revenue: Double = 0.0,
    val profit: Double = 0.0
)

private data class SoldLine(
    val category: String,
    val name: String,
    val qty: Int,
    val price: Double
)

// itemsDetails format: Category|Name|Qty|Price;Category|Name|Qty|Price
private fun SaleTransaction.soldLines(): List<SoldLine> {
    if (itemsDetails.isBlank()) return emptyList()
    return itemsDetails.split(";").mapNotNull { itemStr ->
        val parts = itemStr.split("|")
        if (parts.size < 4) {
            null
        } else {
            SoldLine(
                category = parts[0].trim(),
                name = parts[1].trim(),
                qty = parts[2].toIntOrNull() ?: 1,
                price = parts[3].toDoubleOrNull() ?: 0.0
            )
        }
    }
}

private fun List<Product>.costLookup(): Map<String, Double> =
    associate { it.name.trim().lowercase() to it.costPrice }

private val analyticsPalette = listOf(
    0xFFE76F51, 0xFF2A9D8F, 0xFFE9C46A, 0xFF264653,
    0xFF8AB17D, 0xFFF4A261, 0xFF6D597A, 0xFFB56576
)

class ShopViewModel(
    val dao: ShopDao,
    context: Context,
    val firebaseSync: FirebaseSyncService = FirebaseSyncService()
) : ViewModel() {

    val appContext: Context = context.applicationContext
    val db: AppDatabase get() = (appContext as RefreshApp).database

    private val sessionPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences("shop_session", Context.MODE_PRIVATE)
    private val sessionUserIdKey = "current_user_id"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateString: String get() = dateFormat.format(Date())

    // Language State
    private val _language = MutableStateFlow(AppLanguage.ENGLISH)
    val language = _language.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun getString(key: String): String {
        return AppStrings.get(key, _language.value)
    }

    // Theme Mode
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val themeMode = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    // User / Shop Profile
    val userProfile = dao.getUserProfile().map { it ?: UserProfile() }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserProfile()
    )

    fun saveUserProfile(shopName: String, ownerName: String, phone: String, upiId: String, address: String) {
        viewModelScope.launch {
            val profile = UserProfile(
                id = "shop_profile",
                shopName = shopName.trim(),
                ownerName = ownerName.trim(),
                phone = phone.trim(),
                upiId = upiId.trim(),
                address = address.trim()
            )
            dao.insertOrUpdateProfile(profile)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            val trackBoxes = dao.getAllCategories().first().firstOrNull {
                it.name.equals(product.category, ignoreCase = true)
            }?.trackBoxes == true
            val updatedProduct = product.copy(
                brand = if (trackBoxes) product.brand.trim() else "",
                sealedBoxes = if (trackBoxes) product.sealedBoxes.coerceAtLeast(0) else 0,
                lowStockLimit = if (trackBoxes) product.lowStockLimit?.coerceAtLeast(0) else null
            )
            dao.insertProduct(updatedProduct)
            firebaseSync.syncProduct(updatedProduct)
        }
    }

    // Database flows
    val categories = dao.getAllCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val brands = dao.getAllBrands().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val products = dao.getAllProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sales = dao.getAllSales().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val khataList = dao.getAllKhata().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val repayments = dao.getAllRepayments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val khataCredits = dao.getAllKhataCredits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = dao.getAllExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val closings = dao.getAllClosings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settlements = dao.getAllSettlements().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Start of the current day; re-checked every 30s so "today" rolls over at midnight without restarting the app.
    private val todayStart: StateFlow<Long> = flow {
        while (true) {
            emit(startOfDayMillis(System.currentTimeMillis()))
            delay(30_000L)
        }
    }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, startOfDayMillis(System.currentTimeMillis()))

    private val salesToday = combine(sales, todayStart) { list, start -> list.filter { it.timestamp >= start } }
    private val expensesToday = combine(expenses, todayStart) { list, start -> list.filter { it.timestamp >= start } }
    private val repaymentsToday = combine(repayments, todayStart) { list, start -> list.filter { it.timestamp >= start } }
    val auditLogs = dao.getAllAuditLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Multi-User Authentication Flows
    val allUsers = dao.getAllUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser = _currentUser.asStateFlow()
    private val _sessionChecked = MutableStateFlow(false)
    val sessionChecked = _sessionChecked.asStateFlow()

    /** Staff accounts must not see cost, profit, supplier ledger, backups ... */
    val isOwner: StateFlow<Boolean> = _currentUser
        .map { user -> user == null || user.role != "STAFF" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val subCategories = dao.getAllSubCategories().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val iceCreamVarietyStock = dao.getAllIceCreamVarietyStock().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val suppliers = dao.getAllSuppliers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val purchaseItems = dao.getAllPurchaseItems().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val supplierPayments = dao.getAllSupplierPayments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val supplierPaymentsToday = combine(supplierPayments, todayStart) { list, start -> list.filter { it.timestamp >= start } }
    val supplierReturns = dao.getAllSupplierReturns().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val wastageRecords = dao.getAllWastage().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val offers = dao.getAllOffers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                val savedUserId = sessionPreferences.getString(sessionUserIdKey, null)
                if (savedUserId != null) {
                    val user = dao.getUserById(savedUserId)
                        ?: firebaseSync.findUserById(savedUserId)?.also { dao.insertUser(it) }
                    if (user != null) {
                        if (_currentUser.value == null &&
                            sessionPreferences.getString(sessionUserIdKey, null) == savedUserId
                        ) {
                            _currentUser.value = user
                            onUserSessionStarted(user)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _sessionChecked.value = true
            }
        }
    }

    // ---------------- CLOUD MIRROR ----------------
    private var mirrorStarted = false

    /** Called whenever a user session begins (login, register, app restart with saved session). */
    private fun onUserSessionStarted(user: UserAccount) {
        firebaseSync.shopId = user.effectiveShopId
        startCloudMirror()
        viewModelScope.launch {
            try {
                dao.reconcileCompanyBoxes()
                // Also (re)writes the lookup fields that make indexed cloud login possible.
                firebaseSync.syncUserToCloud(user)
                // One-time full upload of data that already exists on this phone, so a later uninstall
                // never loses it. Skipped when the local database is empty (it would wipe the cloud copy).
                val backupFlag = "cloud_backup_done_${user.effectiveShopId}"
                if (user.role != "STAFF" && !sessionPreferences.getBoolean(backupFlag, false)) {
                    val hasLocalData = dao.snapshotSales().isNotEmpty() ||
                        dao.snapshotProducts().isNotEmpty() ||
                        dao.snapshotVarietyStock().isNotEmpty()
                    if (hasLocalData) {
                        firebaseSync.backupEverything(dao)
                        sessionPreferences.edit().putBoolean(backupFlag, true).apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Small master tables are mirrored to the cloud as soon as they change (debounced).
     * The very first emission is skipped on purpose: on a fresh install the local tables are empty and
     * pushing them would wipe the cloud copy before it could be restored.
     */
    @OptIn(FlowPreview::class)
    private fun startCloudMirror() {
        if (mirrorStarted) return
        mirrorStarted = true

        fun <T> mirror(flow: Flow<List<T>>, name: String, toRow: (T) -> Map<String, Any?>) {
            viewModelScope.launch {
                flow.drop(1).debounce(2000).distinctUntilChanged().collect { rows ->
                    if (_currentUser.value != null && firebaseSync.shopId.isNotBlank()) {
                        firebaseSync.pushTable(name, rows.map(toRow))
                    }
                }
            }
        }

        mirror(dao.getAllCategories(), "categories") { it.toMap() }
        mirror(dao.getAllSubCategories(), "sub_categories") { it.toMap() }
        mirror(dao.getAllIceCreamVarietyStock(), "variety_stock") { it.toMap() }
        mirror(dao.getAllSuppliers(), "suppliers") { it.toMap() }
        mirror(dao.getAllOffers(), "offers") { it.toMap() }
        mirror(dao.getAllBrands(), "brands") { it.toMap() }
        mirror(dao.getUserProfile().map { listOfNotNull(it) }, "profile") { it.toMap() }
    }

    fun isBoxTrackingEnabled(categoryName: String): Boolean =
        categories.value.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }?.trackBoxes ?: false

    fun lowStockLimitFor(product: Product): Int =
        product.lowStockLimit
            ?: categories.value.firstOrNull { it.name.equals(product.category, ignoreCase = true) }?.lowStockLimit
            ?: 2

    // POS Cart State
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart = _cart.asStateFlow()

    private val _discount = MutableStateFlow(0.0)
    val discount = _discount.asStateFlow()

    private val _openingCash = MutableStateFlow(0.0)
    val openingCash = _openingCash.asStateFlow()

    // Sale Undo State (Holds last sale for 5-second undo snackbar)
    private val _lastSaleForUndo = MutableStateFlow<SaleTransaction?>(null)
    val lastSaleForUndo = _lastSaleForUndo.asStateFlow()
    private var saleUndoExpiryJob: Job? = null

    // Quick Favourites / Top 8 Products for Fast POS
    /**
     * How well each product sells: key = "category|name" (lowercase), value = (units sold in last 30 days, units sold ever).
     * The POS screen sorts items by this so top sellers come first; recent sales win so the order follows the season.
     */
    val productSalesRank: StateFlow<Map<String, Pair<Int, Int>>> = sales.map { salesList ->
        val recentSince = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val recent = mutableMapOf<String, Int>()
        val ever = mutableMapOf<String, Int>()
        salesList.forEach { sale ->
            if (sale.itemsDetails.isBlank()) return@forEach
            sale.itemsDetails.split(";").forEach { itemStr ->
                val parts = itemStr.split("|")
                if (parts.size >= 3) {
                    val key = "${parts[0].trim()}|${parts[1].trim()}".lowercase()
                    val qty = parts[2].toIntOrNull() ?: 1
                    ever[key] = (ever[key] ?: 0) + qty
                    if (sale.timestamp >= recentSince) recent[key] = (recent[key] ?: 0) + qty
                }
            }
        }
        ever.mapValues { (key, total) -> Pair(recent[key] ?: 0, total) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /** Top 8 sellers for the 1-tap "Best Sellers" strip (recent sales first, then all-time). */
    val topFavoriteProducts = combine(productSalesRank, products) { rank, productList ->
        fun rankOf(p: Product) = rank["${p.category.trim()}|${p.name.trim()}".lowercase()]
        val sold = productList
            .filter { (rankOf(it)?.second ?: 0) > 0 }
            .sortedWith(
                compareByDescending<Product> { rankOf(it)?.first ?: 0 }
                    .thenByDescending { rankOf(it)?.second ?: 0 }
            )
            .take(8)
        if (sold.size >= 8) sold else sold + productList.filter { it !in sold }.take(8 - sold.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------------- DASHBOARD CALCULATIONS ----------------
    // Total Sales (Revenue from all modes)
    val totalSalesAmount = salesToday.map { list -> list.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val cashSalesAmount = salesToday.map { list -> list.filter { it.paymentMode == "CASH" }.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val upiSalesAmount = salesToday.map { list -> list.filter { it.paymentMode == "UPI" }.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val udharSalesAmount = salesToday.map { list -> list.filter { it.paymentMode == "UDHAR" }.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category-wise Sales Breakdown (Dynamically populated from user categories)
    val categorySales = combine(salesToday, categories) { salesList, catList ->
        val map = mutableMapOf<String, Double>()
        catList.forEach { cat -> map[cat.name] = 0.0 }
        salesList.forEach { sale ->
            if (sale.itemsDetails.isNotBlank()) {
                val items = sale.itemsDetails.split(";")
                items.forEach { itemStr ->
                    val parts = itemStr.split("|")
                    if (parts.size >= 4) {
                        val cat = parts[0]
                        val qty = parts[2].toIntOrNull() ?: 1
                        val price = parts[3].toDoubleOrNull() ?: 0.0
                        map[cat] = (map[cat] ?: 0.0) + (qty * price)
                    }
                }
            }
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val totalExpensesAmount = expensesToday.map { list -> list.sumOf { it.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayNetProfit = combine(totalSalesAmount, totalExpensesAmount) { sales, exp ->
        sales - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ---------------- DASHBOARD PERIODS: TODAY / THIS MONTH / PICKED DATES ----------------
    val todaySummary = combine(sales, expenses, categories, todayStart) { s, e, c, start ->
        buildPeriodSummary(s, e, c, start, Long.MAX_VALUE)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary())

    val monthSummary = combine(sales, expenses, categories, todayStart) { s, e, c, start ->
        buildPeriodSummary(s, e, c, startOfMonthMillis(start), Long.MAX_VALUE)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PeriodSummary())

    private val _dashboardFilter = MutableStateFlow<DateRangeFilter?>(null)
    val dashboardFilter: StateFlow<DateRangeFilter?> = _dashboardFilter.asStateFlow()
    fun setDashboardFilter(filter: DateRangeFilter?) { _dashboardFilter.value = filter }

    val filteredSummary: StateFlow<PeriodSummary?> = combine(sales, expenses, categories, _dashboardFilter) { s, e, c, f ->
        f?.let { buildPeriodSummary(s, e, c, it.startMillis, it.endMillis) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val categoryExpenses = expensesToday.map { list ->
        val map = mutableMapOf<String, Double>()
        list.forEach { exp ->
            map[exp.category] = (map[exp.category] ?: 0.0) + exp.amount
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Today's Top Selling Item overall
    val topItem = salesToday.map { list ->
        val countMap = mutableMapOf<String, Pair<Int, Double>>()
        list.forEach { sale ->
            if (sale.itemsDetails.isNotBlank()) {
                val items = sale.itemsDetails.split(";")
                items.forEach { itemStr ->
                    val parts = itemStr.split("|")
                    if (parts.size >= 4) {
                        val name = parts[1]
                        val qty = parts[2].toIntOrNull() ?: 1
                        val price = parts[3].toDoubleOrNull() ?: 0.0
                        val current = countMap.getOrDefault(name, Pair(0, 0.0))
                        countMap[name] = Pair(current.first + qty, current.second + (qty * price))
                    }
                }
            }
        }
        val top = countMap.maxByOrNull { it.value.first }
        if (top != null) TopItemStats(name = top.key, pieces = top.value.first, revenue = top.value.second) else TopItemStats()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TopItemStats())

    // Total Market Khata Outstanding
    val totalKhataOutstanding = khataList.map { list -> list.sumOf { it.outstandingDebt } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Cash Galla Closing Calculations
    // Cash kharcha + cash paid to suppliers (credit notes are not cash)
    val shopExpensesCashToday = expensesToday.map { list ->
        list.filter { it.paymentMode == "CASH" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val supplierCashPaidToday = supplierPaymentsToday.map { list ->
        list.filter { it.paymentMode == "CASH" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cashExpensesTotal = combine(expensesToday, supplierPaymentsToday) { expenseList, paymentList ->
        expenseList.filter { it.paymentMode == "CASH" }.sumOf { it.amount } +
            paymentList.filter { it.paymentMode == "CASH" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val udharCashRepaymentTotal = repaymentsToday.map { list ->
        list.filter { it.paymentMode == "CASH" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ---------------- SHOP BALANCE: CASH IN SHOP + ONLINE MONEY ----------------
    private val balancePrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("shop_balances", Context.MODE_PRIVATE)

    private val _balanceBase = MutableStateFlow(
        BalanceBase(
            cash = balancePrefs.getString("cash", null)?.toDoubleOrNull() ?: 0.0,
            online = balancePrefs.getString("online", null)?.toDoubleOrNull() ?: 0.0,
            setAt = balancePrefs.getLong("setAt", 0L)
        )
    )

    /**
     * Cash in shop  = entered cash   + cash sales + cash udhar recovery − cash expenses − cash supplier payments
     * Online money  = entered online + UPI sales  + UPI udhar recovery  − online expenses − UPI/bank supplier payments
     * Only entries made after the moment the balance was entered are counted.
     */
    val shopBalance: StateFlow<ShopBalance> = combine(
        _balanceBase, sales, expenses, repayments, supplierPayments
    ) { base, saleList, expenseList, repayList, paymentList ->
        if (base.setAt == 0L) {
            ShopBalance()
        } else {
            val since = base.setAt
            val s = saleList.filter { it.timestamp >= since }
            val e = expenseList.filter { it.timestamp >= since }
            val r = repayList.filter { it.timestamp >= since }
            val p = paymentList.filter { it.timestamp >= since }
            val cash = base.cash +
                s.filter { it.paymentMode == "CASH" }.sumOf { it.totalAmount } +
                r.filter { it.paymentMode == "CASH" }.sumOf { it.amount } -
                e.filter { it.paymentMode == "CASH" }.sumOf { it.amount } -
                p.filter { it.paymentMode == "CASH" }.sumOf { it.amount }
            val online = base.online +
                s.filter { it.paymentMode == "UPI" }.sumOf { it.totalAmount } +
                r.filter { it.paymentMode != "CASH" }.sumOf { it.amount } -
                e.filter { it.paymentMode != "CASH" }.sumOf { it.amount } -
                p.filter { it.paymentMode == "UPI" || it.paymentMode == "BANK" }.sumOf { it.amount }
            ShopBalance(hasBase = true, cash = cash, online = online, setAt = since)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopBalance())

    /** Owner says: "right now I have this much cash and this much online". Calculation continues from here. */
    fun setShopBalance(cash: Double, online: Double) {
        val base = BalanceBase(cash = cash, online = online, setAt = System.currentTimeMillis())
        balancePrefs.edit()
            .putString("cash", cash.toString())
            .putString("online", online.toString())
            .putLong("setAt", base.setAt)
            .apply()
        _balanceBase.value = base
    }

    // Expected Cash = Opening Cash + Cash Sales + Cash Udhar Recovery - Cash Expenses
    val expectedCash = combine(openingCash, cashSalesAmount, udharCashRepaymentTotal, cashExpensesTotal) { opening, cashSale, recovery, expense ->
        opening + cashSale + recovery - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // UPI Total Collection for Paytm Settlement (UPI Sales + UPI Udhar Repayments)
    val totalUpiCollection = combine(upiSalesAmount, repaymentsToday) { upiSale, repayList ->
        val upiRepay = repayList.filter { it.paymentMode == "UPI" }.sumOf { it.amount }
        upiSale + upiRepay
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Supplier Purchases & Distributor Bills
    val supplierPurchases = dao.getAllSupplierPurchases().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val totalSupplierDebt = supplierPurchases.map { list ->
        list.filter { it.paymentStatus != "PAID" }.sumOf { it.totalAmount - it.paidAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Ice-Cream Profit Analytics (Calculating gross profit from sold ice-cream)
    val iceCreamProfitStats = combine(sales, products) { salesList, productList ->
        var rev = 0.0
        var cogs = 0.0
        salesList.forEach { sale ->
            if (sale.itemsDetails.isNotBlank()) {
                val items = sale.itemsDetails.split(";")
                items.forEach itemLoop@ { itemStr ->
                    val parts = itemStr.split("|")
                    if (parts.size >= 4) {
                        val cat = parts[0].trim()
                        val name = parts[1].trim()
                        val qty = parts[2].toIntOrNull() ?: 1
                        val price = parts[3].toDoubleOrNull() ?: 0.0

                        if (cat.isIceCreamCategory()) {
                            val lineRevenue = price * qty
                            val matchingProd = productList.firstOrNull { it.name.equals(name, ignoreCase = true) }
                            val unitCost = matchingProd?.costPrice?.takeIf { it > 0 } ?: return@itemLoop
                            val lineCost = unitCost * qty

                            rev += lineRevenue
                            cogs += lineCost
                        }
                    }
                }
            }
        }
        val gross = rev - cogs
        val margin = if (rev > 0) (gross / rev) * 100.0 else 0.0
        IceCreamProfitStats(
            totalSalesRevenue = rev,
            totalCostOfGoods = cogs,
            grossProfit = gross,
            marginPercent = margin
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), IceCreamProfitStats())

    // Daily Top Soda & Ice-cream Tracker
    val topSodaAndIceCream = salesToday.map { list ->
        val sodaMap = mutableMapOf<String, Int>()
        val iceMap = mutableMapOf<String, Int>()
        list.forEach { sale ->
            if (sale.itemsDetails.isNotBlank()) {
                val items = sale.itemsDetails.split(";")
                items.forEach { itemStr ->
                    val parts = itemStr.split("|")
                    if (parts.size >= 3) {
                        val cat = parts[0].trim().lowercase()
                        val name = parts[1].trim()
                        val qty = parts[2].toIntOrNull() ?: 1
                        if (cat.isIceCreamCategory()) {
                            iceMap[name] = (iceMap[name] ?: 0) + qty
                        } else if (cat.contains("soda", ignoreCase = true)) {
                            sodaMap[name] = (sodaMap[name] ?: 0) + qty
                        }
                    }
                }
            }
        }
        val topSoda = sodaMap.maxByOrNull { it.value }?.let { "${it.key} (${it.value} glass)" } ?: "—"
        val topIce = iceMap.maxByOrNull { it.value }?.let { "${it.key} (${it.value} pcs)" } ?: "—"
        Pair(topSoda, topIce)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair("—", "—"))

    // ---------------- ANALYTICS SCREEN FLOWS ----------------
    val overallProfitSummary = combine(sales, products, expenses, wastageRecords) { salesList, productList, expenseList, wastageList ->
        val costMap = productList.costLookup()
        val revenue = salesList.sumOf { it.totalAmount }
        val cost = salesList.sumOf { sale ->
            sale.soldLines().sumOf { line -> (costMap[line.name.lowercase()] ?: 0.0) * line.qty }
        }
        val gross = revenue - cost
        val expenseTotal = expenseList.sumOf { it.amount }
        val wastageTotal = wastageList.sumOf { it.estimatedCost }
        OverallProfitSummary(
            totalRevenue = revenue,
            totalSalesCount = salesList.size,
            totalCost = cost,
            grossProfit = gross,
            grossMarginPercent = if (revenue > 0) (gross / revenue) * 100.0 else 0.0,
            totalExpenses = expenseTotal,
            totalWastage = wastageTotal,
            netProfit = gross - expenseTotal - wastageTotal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallProfitSummary())

    val categoryPerformanceStats = combine(sales, products) { salesList, productList ->
        val costMap = productList.costLookup()
        val revenueMap = mutableMapOf<String, Double>()
        val costAgg = mutableMapOf<String, Double>()
        val qtyMap = mutableMapOf<String, Int>()
        salesList.forEach { sale ->
            sale.soldLines().forEach { line ->
                val cat = line.category
                revenueMap[cat] = (revenueMap[cat] ?: 0.0) + (line.qty * line.price)
                costAgg[cat] = (costAgg[cat] ?: 0.0) + (costMap[line.name.lowercase()] ?: 0.0) * line.qty
                qtyMap[cat] = (qtyMap[cat] ?: 0) + line.qty
            }
        }
        val totalRevenue = revenueMap.values.sum()
        revenueMap.entries
            .sortedByDescending { it.value }
            .mapIndexed { index, entry ->
                val revenue = entry.value
                val cost = costAgg[entry.key] ?: 0.0
                val gross = revenue - cost
                CategoryProfitStat(
                    category = entry.key,
                    itemsSold = qtyMap[entry.key] ?: 0,
                    totalRevenue = revenue,
                    totalCost = cost,
                    grossProfit = gross,
                    marginPercent = if (revenue > 0) (gross / revenue) * 100.0 else 0.0,
                    percentageOfTotalSales = if (totalRevenue > 0) (revenue / totalRevenue) * 100.0 else 0.0,
                    colorHex = analyticsPalette[index % analyticsPalette.size]
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklySalesTrend = combine(sales, products) { salesList, productList ->
        val costMap = productList.costLookup()
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val revenueByDay = mutableMapOf<String, Double>()
        val profitByDay = mutableMapOf<String, Double>()
        salesList.forEach { sale ->
            val key = keyFormat.format(Date(sale.timestamp))
            val cost = sale.soldLines().sumOf { line -> (costMap[line.name.lowercase()] ?: 0.0) * line.qty }
            revenueByDay[key] = (revenueByDay[key] ?: 0.0) + sale.totalAmount
            profitByDay[key] = (profitByDay[key] ?: 0.0) + (sale.totalAmount - cost)
        }
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val days = mutableListOf<DayTrendStat>()
        repeat(7) {
            val date = calendar.time
            val key = keyFormat.format(date)
            days.add(
                DayTrendStat(
                    dayLabel = labelFormat.format(date),
                    revenue = revenueByDay[key] ?: 0.0,
                    profit = profitByDay[key] ?: 0.0
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        days.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topRankedProducts = combine(sales, products) { salesList, productList ->
        val costMap = productList.costLookup()
        salesList
            .flatMap { it.soldLines() }
            .groupBy { it.name.lowercase() }
            .map { entry ->
                val group = entry.value
                val first = group.first()
                val qty = group.sumOf { it.qty }
                val revenue = group.sumOf { it.qty * it.price }
                val cost = (costMap[first.name.lowercase()] ?: 0.0) * qty
                TopProductRank(
                    rank = 0,
                    productName = first.name,
                    category = first.category,
                    qtySold = qty,
                    revenue = revenue,
                    profit = revenue - cost
                )
            }
            .sortedByDescending { it.qtySold }
            .take(10)
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // NOTE: Default data seeding is intentionally removed! All categories and items are added by the user!

    // ---------------- CATEGORY MANAGEMENT ----------------
    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            if (dao.getAllCategories().first().any { it.name.equals(trimmed, ignoreCase = true) }) return@launch
            val category = ProductCategory(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                orderIndex = categories.value.size,
                trackBoxes = false
            )
            dao.insertCategory(category)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "CATEGORY_ADDED",
                    details = "New Category created: $trimmed"
                )
            )
        }
    }

    fun deleteCategory(category: ProductCategory) {
        viewModelScope.launch {
            db.withTransaction {
                dao.deleteVarietyStockForCategory(category.name)
                dao.deleteSubCategoriesForCategory(category.name)
                dao.deleteCategory(category)
            }
        }
    }

    fun updateCategory(category: ProductCategory) {
        viewModelScope.launch {
            db.withTransaction {
                dao.insertCategory(category.copy(lowStockLimit = category.lowStockLimit.coerceAtLeast(0)))
                if (!category.trackBoxes) {
                    dao.clearCategoryBoxes(category.name)
                    dao.clearVarietyStockForCategory(category.name)
                    dao.clearSubCategoryBoxes(category.name)
                }
            }
        }
    }

    fun addSubCategory(
        categoryName: String,
        name: String,
        defaultPrice: Double,
        sealedBoxes: Int = 0,
        lowStockLimit: Int = 2,
        onAdded: ((SubCategory) -> Unit)? = null
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            val existing = dao.getSubCategories(categoryName).firstOrNull {
                it.name.equals(trimmedName, ignoreCase = true)
            }
            if (existing != null) {
                onAdded?.invoke(existing)
                return@launch
            }
            val subCategory = SubCategory(
                categoryName = categoryName.trim(),
                name = trimmedName,
                defaultPrice = defaultPrice.coerceAtLeast(0.0),
                sealedBoxes = sealedBoxes.coerceAtLeast(0),
                lowStockLimit = lowStockLimit.coerceAtLeast(0)
            )
            dao.insertSubCategory(subCategory)
            onAdded?.invoke(subCategory)
        }
    }

    fun updateSubCategory(subCategory: SubCategory) {
        viewModelScope.launch {
            // Box counts are owned by the per-variety stock table; never overwrite them from here.
            val currentBoxes = dao.getSubCategoryById(subCategory.id)?.sealedBoxes ?: subCategory.sealedBoxes
            dao.insertSubCategory(
                subCategory.copy(
                    defaultPrice = subCategory.defaultPrice.coerceAtLeast(0.0),
                    sealedBoxes = currentBoxes.coerceAtLeast(0),
                    lowStockLimit = subCategory.lowStockLimit.coerceAtLeast(0)
                )
            )
        }
    }

    fun deleteSubCategory(subCategory: SubCategory) {
        viewModelScope.launch {
            db.withTransaction {
                dao.deleteVarietyStockForCompany(subCategory.categoryName, subCategory.name)
                dao.deleteSubCategory(subCategory)
            }
        }
    }

    fun parseTextToDraftItems(text: String): List<DraftProductItem> {
        val result = mutableListOf<DraftProductItem>()
        var currentCategory = ""

        text.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isBlank() || line.startsWith("#") || line.startsWith("//")) return@forEach

            if (line.startsWith("[") && line.endsWith("]")) {
                currentCategory = line.removeSurrounding("[", "]").trim()
                return@forEach
            }

            val parts = if (line.contains(",")) line.split(",") else if (line.contains("|")) line.split("|") else line.split("\t")
            if (parts.isNotEmpty()) {
                val name = parts[0].trim()
                val price = parts.getOrNull(1)?.trim()?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull()
                if (name.length > 1 && currentCategory.isNotBlank() && price != null && price > 0) {
                    val brand = parts.getOrNull(2)?.trim() ?: ""
                    val boxes = if (isBoxTrackingEnabled(currentCategory)) {
                        parts.getOrNull(3)?.trim()?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                    } else {
                        0
                    }
                    val cost = parts.getOrNull(4)?.trim()?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull() ?: 0.0

                    result.add(
                        DraftProductItem(
                            category = currentCategory,
                            name = name,
                            brand = brand,
                            price = price,
                            costPrice = cost,
                            sealedBoxes = boxes
                        )
                    )
                }
            }
        }
        return result
    }

    fun bulkImportCategoriesAndProducts(items: List<DraftProductItem>) {
        viewModelScope.launch {
            val distinctCategories = items.map { it.category.trim() }.distinct().filter { it.isNotBlank() }
            val knownCategories = dao.getAllCategories().first().toMutableList()
            val existingCategoryNames = knownCategories.map { it.name.lowercase() }.toSet()
            distinctCategories.filterNot { it.lowercase() in existingCategoryNames }.forEachIndexed { index, catName ->
                val category = ProductCategory(
                    id = UUID.randomUUID().toString(),
                    name = catName,
                    orderIndex = categories.value.size + index
                )
                dao.insertCategory(category)
                knownCategories.add(category)
            }
            items.forEach { draft ->
                val trackBoxes = knownCategories.firstOrNull {
                    it.name.equals(draft.category.trim(), ignoreCase = true)
                }?.trackBoxes ?: false
                if (trackBoxes && draft.brand.isNotBlank()) {
                    val company = dao.getSubCategories(draft.category.trim()).firstOrNull {
                        it.name.equals(draft.brand.trim(), ignoreCase = true)
                    }
                    if (company == null) {
                        dao.insertSubCategory(
                            SubCategory(
                                categoryName = draft.category.trim(),
                                name = draft.brand.trim(),
                                sealedBoxes = draft.sealedBoxes.coerceAtLeast(0)
                            )
                        )
                    } else {
                        dao.insertSubCategory(
                            company.copy(sealedBoxes = company.sealedBoxes + draft.sealedBoxes.coerceAtLeast(0))
                        )
                    }
                }
                val prod = Product(
                    id = UUID.randomUUID().toString(),
                    name = draft.name.trim(),
                    category = draft.category.trim(),
                    brand = if (trackBoxes) draft.brand.trim() else "",
                    price = draft.price,
                    costPrice = draft.costPrice,
                    sealedBoxes = 0
                )
                dao.insertProduct(prod)
                firebaseSync.syncProduct(prod)
            }
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "BULK_IMPORT",
                    details = "Imported ${items.size} items across ${distinctCategories.size} categories"
                )
            )
        }
    }

    // ---------------- POS CART ACTIONS ----------------
    fun addToCart(product: Product) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id && it.addOns.isEmpty() }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1)
        } else {
            current.add(CartItem(product, quantity = 1))
        }
        _cart.value = current
    }

    fun addAddOn(cartIndex: Int, addOnName: String, cost: Double) {
        val current = _cart.value.toMutableList()
        if (cartIndex in current.indices) {
            val item = current[cartIndex]
            val updatedAddOns = item.addOns.toMutableList().apply { add(addOnName) }
            val updatedCost = item.addOnCost + cost
            current[cartIndex] = item.copy(addOns = updatedAddOns, addOnCost = updatedCost)
            _cart.value = current
        }
    }

    fun updateCartItemQuantity(index: Int, newQty: Int) {
        val current = _cart.value.toMutableList()
        if (index in current.indices) {
            if (newQty <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQty)
            }
            _cart.value = current
        }
    }

    fun updateCartItemPrice(index: Int, newPrice: Double) {
        val current = _cart.value.toMutableList()
        if (index in current.indices && newPrice >= 0) {
            current[index] = current[index].copy(customPrice = newPrice)
            _cart.value = current
        }
    }

    fun setDiscount(amount: Double) {
        _discount.value = amount.coerceAtLeast(0.0)
    }

    fun clearCart() {
        _cart.value = emptyList()
        _discount.value = 0.0
    }

    // ---------------- HOLD / PARK BILL ----------------
    private val holdPrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("held_bills", Context.MODE_PRIVATE)

    private val _heldBills = MutableStateFlow(loadHeldBills())
    val heldBills: StateFlow<List<HeldBill>> = _heldBills.asStateFlow()

    private fun loadHeldBills(): List<HeldBill> = try {
        val arr = org.json.JSONArray(holdPrefs.getString("bills", "[]") ?: "[]")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val itemsJson = o.getJSONArray("items")
            HeldBill(
                id = o.getString("id"),
                number = o.getInt("number"),
                discount = o.getDouble("discount"),
                heldAt = o.getLong("heldAt"),
                items = (0 until itemsJson.length()).map { j ->
                    val it = itemsJson.getJSONObject(j)
                    val addOns = it.getJSONArray("addOns")
                    CartItem(
                        product = Product(
                            id = it.getString("pid"),
                            name = it.getString("name"),
                            category = it.getString("category"),
                            brand = it.optString("brand", ""),
                            price = it.getDouble("price"),
                            costPrice = it.optDouble("costPrice", 0.0)
                        ),
                        quantity = it.getInt("qty"),
                        addOns = (0 until addOns.length()).map { k -> addOns.getString(k) },
                        addOnCost = it.getDouble("addOnCost"),
                        customPrice = it.getDouble("customPrice")
                    )
                }
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun persistHeldBills(bills: List<HeldBill>) {
        val arr = org.json.JSONArray()
        bills.forEach { b ->
            val items = org.json.JSONArray()
            b.items.forEach { c ->
                items.put(
                    org.json.JSONObject()
                        .put("pid", c.product.id).put("name", c.product.name)
                        .put("category", c.product.category).put("brand", c.product.brand)
                        .put("price", c.product.price).put("costPrice", c.product.costPrice)
                        .put("qty", c.quantity).put("addOnCost", c.addOnCost)
                        .put("customPrice", c.customPrice)
                        .put("addOns", org.json.JSONArray(c.addOns))
                )
            }
            arr.put(
                org.json.JSONObject()
                    .put("id", b.id).put("number", b.number).put("discount", b.discount)
                    .put("heldAt", b.heldAt).put("items", items)
            )
        }
        holdPrefs.edit().putString("bills", arr.toString()).apply()
    }

    private fun setHeldBills(bills: List<HeldBill>) {
        _heldBills.value = bills
        persistHeldBills(bills)
    }

    /** Parks the current cart as a held bill and empties the counter for the next customer. */
    fun holdCurrentBill() {
        val items = _cart.value
        if (items.isEmpty()) return
        val current = _heldBills.value
        val number = (1..99).firstOrNull { n -> current.none { it.number == n } } ?: (current.size + 1)
        val bill = HeldBill(
            id = java.util.UUID.randomUUID().toString(),
            number = number,
            items = items,
            discount = _discount.value,
            heldAt = System.currentTimeMillis()
        )
        setHeldBills(current + bill)
        clearCart()
    }

    /** Brings a held bill back to the counter. If the counter already has items they are parked, never lost. */
    fun resumeHeldBill(id: String) {
        val bill = _heldBills.value.firstOrNull { it.id == id } ?: return
        var remaining = _heldBills.value.filter { it.id != id }
        val currentItems = _cart.value
        if (currentItems.isNotEmpty()) {
            val number = (1..99).firstOrNull { n -> remaining.none { it.number == n } && n != bill.number } ?: bill.number
            remaining = remaining + HeldBill(
                id = java.util.UUID.randomUUID().toString(),
                number = number,
                items = currentItems,
                discount = _discount.value,
                heldAt = System.currentTimeMillis()
            )
        }
        setHeldBills(remaining)
        _cart.value = bill.items
        _discount.value = bill.discount
    }

    fun discardHeldBill(id: String) {
        setHeldBills(_heldBills.value.filter { it.id != id })
    }

    fun completeOrder(paymentMode: String, customerName: String? = null) {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return
        // Udhaar without a customer would be lost (no khata entry), so it is never accepted.
        if (paymentMode == "UDHAR" && customerName.isNullOrBlank()) return

        val rawTotal = currentCart.sumOf { it.total }
        val finalTotal = (rawTotal - _discount.value).coerceAtLeast(0.0)

        // Summary strings
        val summary = currentCart.joinToString(", ") { item ->
            val addonText = if (item.addOns.isNotEmpty()) " +${item.addOns.joinToString("+")}" else ""
            "${item.product.name}$addonText x${item.quantity}"
        }

        // Serialized details for Category & Top item analysis: Category|Name|Qty|Price
        val details = currentCart.joinToString(";") { item ->
            "${item.product.category}|${item.product.name}|${item.quantity}|${item.customPrice + item.addOnCost}"
        }

        viewModelScope.launch {
            val sale = SaleTransaction(
                id = UUID.randomUUID().toString(),
                totalAmount = finalTotal,
                discountAmount = _discount.value,
                paymentMode = paymentMode,
                customerName = customerName,
                itemsSummary = summary,
                itemsDetails = details,
                soldBy = _currentUser.value?.name.orEmpty()
            )
            // Sale + khata entry are written together or not at all.
            val khataToSync = db.withTransaction {
                dao.insertSale(sale)
                if (paymentMode == "UDHAR" && !customerName.isNullOrBlank()) {
                    val existing = dao.getKhataByName(customerName.trim())
                    if (existing != null) {
                        dao.addDebt(existing.name, finalTotal)
                    } else {
                        dao.insertOrUpdateKhata(
                            CustomerKhata(
                                id = UUID.randomUUID().toString(),
                                name = customerName.trim(),
                                outstandingDebt = finalTotal
                            )
                        )
                    }
                    dao.getKhataByName(customerName.trim())
                } else {
                    null
                }
            }
            _lastSaleForUndo.value = sale
            saleUndoExpiryJob?.cancel()
            saleUndoExpiryJob = viewModelScope.launch {
                delay(5_000)
                if (_lastSaleForUndo.value?.id == sale.id) {
                    _lastSaleForUndo.value = null
                }
            }

            clearCart()
            firebaseSync.syncSale(sale)
            if (khataToSync != null) firebaseSync.syncKhata(khataToSync)
        }
    }

    /** Gives the udhaar of a removed UDHAR sale back to the customer's khata. Must run inside a transaction. */
    private suspend fun reverseUdharForSale(sale: SaleTransaction): CustomerKhata? {
        val customer = sale.customerName
        if (sale.paymentMode != "UDHAR" || customer.isNullOrBlank()) return null
        val existing = dao.getKhataByName(customer.trim()) ?: return null
        dao.recordRepayment(existing.id, sale.totalAmount)
        return dao.getKhataById(existing.id)
    }

    fun deleteSale(sale: SaleTransaction) {
        viewModelScope.launch {
            val khata = db.withTransaction {
                dao.deleteSale(sale)
                reverseUdharForSale(sale)
            }
            firebaseSync.delete("sales", sale.id)
            if (khata != null) firebaseSync.syncKhata(khata)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "SALE_DELETED",
                    details = "Deleted ₹${sale.totalAmount.toInt()} (${sale.paymentMode}) - ${sale.itemsSummary}"
                )
            )
        }
    }

    fun undoLastSale(onUndone: (String) -> Unit) {
        val sale = _lastSaleForUndo.value ?: return
        if (System.currentTimeMillis() - sale.timestamp > 5_000) {
            _lastSaleForUndo.value = null
            return
        }
        saleUndoExpiryJob?.cancel()
        viewModelScope.launch {
            val khata = db.withTransaction {
                dao.deleteSale(sale)
                reverseUdharForSale(sale)
            }
            firebaseSync.delete("sales", sale.id)
            if (khata != null) firebaseSync.syncKhata(khata)
            _lastSaleForUndo.value = null
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "SALE_UNDONE",
                    details = "Undid Sale ₹${sale.totalAmount.toInt()} (${sale.itemsSummary})"
                )
            )
            onUndone("Sale ₹${sale.totalAmount.toInt()} undone successfully")
        }
    }

    fun restoreFromCloud(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val count = firebaseSync.restoreAllFromCloud(dao)
                dao.insertAuditLog(
                    AuditLog(
                        id = UUID.randomUUID().toString(),
                        action = "CLOUD_RESTORE",
                        details = "Restored $count records from Firebase Cloud"
                    )
                )
                onResult(true, "Cloud restore successful! $count items restored.")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Cloud restore failed")
            }
        }
    }

    fun backupToCloud(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val count = firebaseSync.backupEverything(dao)
                dao.insertAuditLog(
                    AuditLog(
                        id = UUID.randomUUID().toString(),
                        action = "CLOUD_BACKUP",
                        details = "Backed up $count records to Firebase Cloud"
                    )
                )
                onResult(true, "All shop data backed up to cloud ($count records).")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Cloud backup failed")
            }
        }
    }

    // ---------------- GODOWN BOX STOCK ----------------
    fun openBox(productId: String, productName: String) {
        viewModelScope.launch {
            val product = products.value.firstOrNull { it.id == productId } ?: return@launch
            val category = dao.getAllCategories().first().firstOrNull {
                it.name.equals(product.category, ignoreCase = true)
            }
            if (category?.trackBoxes != true) return@launch
            dao.openBox(productId)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "BOX_OPENED",
                    details = "-1 Box opened for: $productName"
                )
            )
        }
    }

    /** Company-level helpers kept for compatibility. They operate on the variety table so totals never drift. */
    fun openSubCategoryBox(subCategory: SubCategory) {
        viewModelScope.launch {
            val category = categories.value.firstOrNull {
                it.name.equals(subCategory.categoryName, ignoreCase = true)
            }
            if (category?.trackBoxes != true) return@launch
            db.withTransaction {
                val variety = dao.snapshotVarietyStock().firstOrNull {
                    it.categoryName.equals(subCategory.categoryName, ignoreCase = true) &&
                        it.companyName.equals(subCategory.name, ignoreCase = true) &&
                        it.sealedBoxes > 0
                }
                if (variety != null) {
                    dao.insertIceCreamVarietyStock(variety.copy(sealedBoxes = variety.sealedBoxes - 1))
                    dao.reconcileCompanyBoxes()
                }
            }
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "BOX_OPENED",
                    details = "-1 Box opened for: ${subCategory.name}"
                )
            )
        }
    }

    fun addSubCategoryBoxes(subCategory: SubCategory, count: Int) {
        if (count <= 0) return
        viewModelScope.launch {
            val category = categories.value.firstOrNull {
                it.name.equals(subCategory.categoryName, ignoreCase = true)
            }
            if (category?.trackBoxes != true) return@launch
            db.withTransaction {
                addVarietyBoxes(subCategory.categoryName, subCategory.name, UNASSIGNED_VARIETY, count)
                dao.reconcileCompanyBoxes()
            }
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "STOCK_ADDED",
                    details = "$count Boxes added for: ${subCategory.name}"
                )
            )
        }
    }

    /** Adds [count] boxes to one variety row (creating it if needed). Caller supplies the transaction. */
    suspend fun addVarietyBoxes(
        categoryName: String,
        companyName: String,
        varietyName: String,
        count: Int,
        costPrice: Double = 0.0,
        mrp: Double = 0.0
    ) {
        val existing = dao.getIceCreamVarietyStock(categoryName, companyName, varietyName)
        val defaultLimit = categories.value.firstOrNull {
            it.name.equals(categoryName, ignoreCase = true)
        }?.lowStockLimit ?: 2
        val updated = if (existing != null) {
            existing.copy(
                sealedBoxes = (existing.sealedBoxes + count).coerceAtLeast(0),
                lastCostPrice = if (costPrice > 0) costPrice else existing.lastCostPrice,
                lastMrp = if (mrp > 0) mrp else existing.lastMrp
            )
        } else {
            IceCreamVarietyStock(
                categoryName = categoryName,
                companyName = companyName,
                varietyName = varietyName.trim(),
                sealedBoxes = count.coerceAtLeast(0),
                lowStockLimit = defaultLimit,
                lastCostPrice = costPrice,
                lastMrp = mrp
            )
        }
        dao.insertIceCreamVarietyStock(updated)
    }

    fun addIceCreamVarietyBoxes(categoryName: String, companyName: String, varietyName: String, count: Int) {
        val cleanVarietyName = varietyName.trim()
        if (count <= 0 || cleanVarietyName.isBlank()) return
        viewModelScope.launch {
            val category = categories.value.firstOrNull {
                it.trackBoxes && it.name.equals(categoryName, ignoreCase = true)
            } ?: return@launch
            val company = dao.getSubCategories(category.name).firstOrNull {
                it.name.equals(companyName, ignoreCase = true)
            } ?: return@launch
            db.withTransaction {
                addVarietyBoxes(category.name, company.name, cleanVarietyName, count)
                dao.reconcileCompanyBoxes()
            }
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "STOCK_ADDED",
                    details = "$count Boxes added: ${company.name} • $cleanVarietyName"
                )
            )
        }
    }

    fun updateIceCreamVarietyStock(stock: IceCreamVarietyStock, sealedBoxes: Int, lowStockLimit: Int) {
        viewModelScope.launch {
            db.withTransaction {
                val current = dao.getIceCreamVarietyStock(
                    stock.categoryName,
                    stock.companyName,
                    stock.varietyName
                ) ?: stock
                dao.insertIceCreamVarietyStock(
                    current.copy(
                        sealedBoxes = sealedBoxes.coerceAtLeast(0),
                        lowStockLimit = lowStockLimit.coerceAtLeast(0)
                    )
                )
                dao.reconcileCompanyBoxes()
            }
        }
    }

    fun openIceCreamVarietyBox(stock: IceCreamVarietyStock) {
        viewModelScope.launch {
            val opened = db.withTransaction {
                val current = dao.getIceCreamVarietyStock(
                    stock.categoryName,
                    stock.companyName,
                    stock.varietyName
                )
                if (current == null || current.sealedBoxes <= 0) {
                    false
                } else {
                    dao.insertIceCreamVarietyStock(current.copy(sealedBoxes = current.sealedBoxes - 1))
                    dao.reconcileCompanyBoxes()
                    true
                }
            }
            if (!opened) return@launch
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "BOX_OPENED",
                    details = "-1 Box opened: ${stock.companyName} • ${stock.varietyName}"
                )
            )
        }
    }

    fun addVarietiesBulk(
        categoryName: String,
        companyName: String,
        varietiesText: String,
        defaultLimit: Int = 2,
        initialBoxes: Int = 0,
        costPrice: Double = 0.0,
        mrp: Double = 0.0
    ) {
        val names = varietiesText
            .split(",", "\n", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.getDefault()) }

        if (names.isEmpty()) return
        viewModelScope.launch {
            db.withTransaction {
                val company = dao.getSubCategories(categoryName).firstOrNull {
                    it.name.equals(companyName, ignoreCase = true)
                } ?: SubCategory(categoryName = categoryName, name = companyName).also { dao.insertSubCategory(it) }

                var addedCount = 0
                names.forEach { name ->
                    val existing = dao.getIceCreamVarietyStock(categoryName, company.name, name)
                    if (existing == null) {
                        dao.insertIceCreamVarietyStock(
                            IceCreamVarietyStock(
                                categoryName = categoryName,
                                companyName = company.name,
                                varietyName = name,
                                sealedBoxes = initialBoxes.coerceAtLeast(0),
                                lowStockLimit = defaultLimit.coerceAtLeast(0),
                                lastCostPrice = costPrice,
                                lastMrp = mrp
                            )
                        )
                        addedCount++
                    }
                }
                dao.reconcileCompanyBoxes()
                dao.insertAuditLog(
                    AuditLog(
                        id = UUID.randomUUID().toString(),
                        action = "VARIETIES_ADDED",
                        details = "$companyName: Added $addedCount new varieties"
                    )
                )
            }
        }
    }

    fun mergeVarieties(
        categoryName: String,
        companyName: String,
        sourceVariety: String,
        targetVariety: String,
        onComplete: (String?) -> Unit = {}
    ) {
        val source = sourceVariety.trim()
        val target = targetVariety.trim()
        if (source.isBlank() || target.isBlank()) {
            onComplete("Please select both varieties.")
            return
        }
        if (source.equals(target, ignoreCase = true)) {
            onComplete("Source and target variety must be different.")
            return
        }
        viewModelScope.launch {
            try {
                db.withTransaction {
                    val srcStock = dao.getIceCreamVarietyStock(categoryName, companyName, source)
                    val tgtStock = dao.getIceCreamVarietyStock(categoryName, companyName, target)
                    if (srcStock == null) {
                        onComplete("Source variety not found.")
                        return@withTransaction
                    }
                    val transferredBoxes = srcStock.sealedBoxes
                    val updatedTgt = (tgtStock ?: IceCreamVarietyStock(
                        categoryName = categoryName,
                        companyName = companyName,
                        varietyName = target,
                        sealedBoxes = 0,
                        lowStockLimit = srcStock.lowStockLimit
                    )).let {
                        it.copy(
                            sealedBoxes = it.sealedBoxes + transferredBoxes,
                            lastCostPrice = if (it.lastCostPrice > 0) it.lastCostPrice else srcStock.lastCostPrice,
                            lastMrp = if (it.lastMrp > 0) it.lastMrp else srcStock.lastMrp
                        )
                    }
                    dao.insertIceCreamVarietyStock(updatedTgt)
                    dao.deleteIceCreamVarietyStock(categoryName, companyName, source)
                    dao.updateVarietyInPurchaseItems(source, target)
                    dao.reconcileCompanyBoxes()

                    dao.insertAuditLog(
                        AuditLog(
                            id = UUID.randomUUID().toString(),
                            action = "VARIETIES_MERGED",
                            details = "$companyName: merged '$source' into '$target' ($transferredBoxes boxes moved)."
                        )
                    )
                }
                onComplete(null)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(e.localizedMessage ?: "Could not merge.")
            }
        }
    }

    fun recordStockCorrection(
        stock: IceCreamVarietyStock,
        newBoxes: Int,
        reason: String,
        onComplete: (String?) -> Unit = {}
    ) {
        val cleanReason = reason.trim()
        if (cleanReason.isBlank()) {
            onComplete("A reason for the correction is required.")
            return
        }
        viewModelScope.launch {
            try {
                val oldBoxes = stock.sealedBoxes
                val diff = newBoxes - oldBoxes
                db.withTransaction {
                    dao.insertIceCreamVarietyStock(stock.copy(sealedBoxes = newBoxes.coerceAtLeast(0)))
                    dao.reconcileCompanyBoxes()
                    dao.insertAuditLog(
                        AuditLog(
                            id = UUID.randomUUID().toString(),
                            action = "STOCK_CORRECTION",
                            details = "${stock.companyName} • ${stock.varietyName}: Stock changed to $newBoxes boxes (was $oldBoxes, difference: $diff). Reason: $cleanReason"
                        )
                    )
                }
                onComplete(null)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(e.localizedMessage ?: "Could not save correction.")
            }
        }
    }

    fun deleteVariety(stock: IceCreamVarietyStock, onComplete: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                db.withTransaction {
                    dao.deleteIceCreamVarietyStock(stock.categoryName, stock.companyName, stock.varietyName)
                    dao.reconcileCompanyBoxes()
                    dao.insertAuditLog(
                        AuditLog(
                            id = UUID.randomUUID().toString(),
                            action = "VARIETY_DELETED",
                            details = "Variety hatayi: ${stock.companyName} • ${stock.varietyName} (${stock.sealedBoxes} box)"
                        )
                    )
                }
                onComplete(null)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(e.localizedMessage ?: "Could not delete variety.")
            }
        }
    }

    fun generateWhatsAppOrderText(companyName: String, items: List<Pair<String, Int>>): String {
        val shop = userProfile.value.shopName.ifBlank { "Refresh Soda & Ice-cream" }
        val phone = userProfile.value.phone
        val address = userProfile.value.address
        return buildString {
            appendLine("🙏 *Order - $shop*")
            appendLine("📅 Date: $todayDateString")
            appendLine("🏢 Company: $companyName")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("Please send the following items:")
            items.forEach { (name, qty) ->
                appendLine("• $name: $qty Box")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            if (address.isNotBlank()) appendLine("📍 Address: $address")
            if (phone.isNotBlank()) appendLine("📞 Phone: $phone")
            append("Thank you!")
        }
    }

    fun getIceCreamInsights(daysLookback: Int): Pair<List<CompanyInsightsItem>, List<VarietyInsightsItem>> {
        val now = System.currentTimeMillis()
        val since = if (daysLookback > 0) now - (daysLookback.toLong() * 24L * 60L * 60L * 1000L) else 0L

        val openLogs = auditLogs.value.filter {
            it.action == "BOX_OPENED" && (since == 0L || it.timestamp >= since)
        }
        val consumedMap = mutableMapOf<String, Int>()
        openLogs.forEach { log ->
            val parts = log.details.removePrefix("-1 Box opened:").split("•")
            if (parts.size >= 2) {
                val company = parts[0].trim().lowercase(Locale.getDefault())
                val variety = parts[1].trim().lowercase(Locale.getDefault())
                val key = "$company|$variety"
                consumedMap[key] = (consumedMap[key] ?: 0) + 1
            }
        }

        val filteredSales = sales.value.filter { since == 0L || it.timestamp >= since }
        filteredSales.forEach { sale ->
            if (sale.itemsDetails.isNotBlank()) {
                sale.itemsDetails.split(";").forEach { itemStr ->
                    val seg = itemStr.split("|")
                    if (seg.size >= 3) {
                        val vName = seg[1].trim().lowercase(Locale.getDefault())
                        val qty = seg[2].toIntOrNull() ?: 1
                        val matchingStock = iceCreamVarietyStock.value.firstOrNull {
                            it.varietyName.trim().lowercase(Locale.getDefault()) == vName
                        }
                        if (matchingStock != null) {
                            val key = "${matchingStock.companyName.trim().lowercase(Locale.getDefault())}|$vName"
                            consumedMap[key] = (consumedMap[key] ?: 0) + qty
                        }
                    }
                }
            }
        }

        val activeDays = if (daysLookback > 0) daysLookback else 30
        val varietyItems = iceCreamVarietyStock.value
            .filter { it.varietyName != "Opening stock (unassigned)" }
            .map { stock ->
                val key = "${stock.companyName.trim().lowercase(Locale.getDefault())}|${stock.varietyName.trim().lowercase(Locale.getDefault())}"
                val consumed = consumedMap[key] ?: 0
                val dailyRate = consumed.toDouble() / activeDays.toDouble()
                val daysLeft = if (dailyRate > 0.0) (stock.sealedBoxes.toDouble() / dailyRate).toInt() else null
                VarietyInsightsItem(
                    categoryName = stock.categoryName,
                    companyName = stock.companyName,
                    varietyName = stock.varietyName,
                    totalConsumed = consumed,
                    currentBoxes = stock.sealedBoxes,
                    lowStockLimit = stock.lowStockLimit,
                    estimatedDaysLeft = daysLeft
                )
            }
            .sortedByDescending { it.totalConsumed }

        val companyMap = mutableMapOf<String, Int>()
        varietyItems.forEach { item ->
            companyMap[item.companyName] = (companyMap[item.companyName] ?: 0) + item.totalConsumed
        }
        val totalAll = companyMap.values.sum().coerceAtLeast(1)
        val companyItems = companyMap.map { (comp, sum) ->
            CompanyInsightsItem(
                companyName = comp,
                totalConsumed = sum,
                percentage = (sum.toFloat() / totalAll.toFloat()) * 100f
            )
        }.sortedByDescending { it.totalConsumed }

        return Pair(companyItems, varietyItems)
    }

    fun addBoxes(productId: String, productName: String, count: Int) {
        viewModelScope.launch {
            val product = products.value.firstOrNull { it.id == productId } ?: return@launch
            val category = dao.getAllCategories().first().firstOrNull {
                it.name.equals(product.category, ignoreCase = true)
            }
            if (category?.trackBoxes != true) return@launch
            dao.addBoxes(productId, count)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "STOCK_ADDED",
                    details = "+$count Box added for: $productName"
                )
            )
        }
    }

    // ---------------- KHATA (UDHAAR) & REPAYMENTS ----------------
    /**
     * Adds a customer once. [openingDue] (optional) is an amount the customer already owes; it is saved
     * as a credit entry so it shows up in the customer's ledger.
     */
    fun addNewCustomer(
        name: String,
        phone: String,
        limit: Double = 2000.0,
        openingDue: Double = 0.0,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()
            if (cleanName.isBlank()) {
                onResult(false, "Customer name is required.")
                return@launch
            }
            val exists = khataList.value.any { it.name.trim().equals(cleanName, ignoreCase = true) }
            if (exists) {
                onResult(false, "A customer named '$cleanName' already exists.")
                return@launch
            }
            val due = openingDue.coerceAtLeast(0.0)
            val customer = CustomerKhata(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                phone = phone.trim(),
                outstandingDebt = due,
                debtLimit = limit
            )
            val openingCredit = if (due > 0) {
                KhataCredit(
                    id = UUID.randomUUID().toString(),
                    customerId = customer.id,
                    customerName = cleanName,
                    amount = due,
                    note = "Opening balance"
                )
            } else null
            db.withTransaction {
                dao.insertOrUpdateKhata(customer)
                openingCredit?.let { dao.insertKhataCredit(it) }
            }
            firebaseSync.syncKhata(customer)
            openingCredit?.let { firebaseSync.syncKhataCredit(it) }
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "KHATA_CUSTOMER_ADDED",
                    details = "Customer added: $cleanName" + if (due > 0) " (opening due ₹${due.toInt()})" else ""
                )
            )
            onResult(true, null)
        }
    }

    fun setKhataDueDate(customerId: String, dueDateMillis: Long?) {
        viewModelScope.launch {
            dao.updateKhataDueDate(customerId, dueDateMillis)
            dao.getKhataById(customerId)?.let { firebaseSync.syncKhata(it) }
        }
    }

    /** Credit (udhaar) given to a customer outside of a POS sale. Increases the customer's due. */
    fun recordKhataCredit(customerId: String, customerName: String, amount: Double, note: String = "") {
        viewModelScope.launch {
            if (amount <= 0.0) return@launch
            val credit = KhataCredit(
                id = UUID.randomUUID().toString(),
                customerId = customerId,
                customerName = customerName,
                amount = amount,
                note = note.trim()
            )
            val updatedKhata = db.withTransaction {
                dao.insertKhataCredit(credit)
                dao.addDebtById(customerId, amount)
                dao.getKhataById(customerId)
            }
            firebaseSync.syncKhataCredit(credit)
            if (updatedKhata != null) firebaseSync.syncKhata(updatedKhata)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "KHATA_CREDIT",
                    details = "Credit ₹${amount.toInt()} given to $customerName" + if (credit.note.isNotBlank()) " (${credit.note})" else ""
                )
            )
        }
    }

    /**
     * Payment received from a customer. [paymentMode] is "CASH" (goes into the till / Day Closing) or
     * "UPI" (online, not part of the till cash). The amount is limited to what the customer owes.
     */
    fun recordKhataRepayment(customerId: String, customerName: String, amount: Double, paymentMode: String = "CASH") {
        viewModelScope.launch {
            val due = dao.getKhataById(customerId)?.outstandingDebt ?: 0.0
            val applied = amount.coerceAtMost(due)
            if (applied <= 0.0) return@launch
            val mode = if (paymentMode == "UPI") "UPI" else "CASH"
            val repayment = KhataRepayment(
                id = UUID.randomUUID().toString(),
                customerId = customerId,
                customerName = customerName,
                amount = applied,
                paymentMode = mode
            )
            val updatedKhata = db.withTransaction {
                dao.recordRepayment(customerId, applied)
                dao.insertRepayment(repayment)
                dao.getKhataById(customerId)
            }
            firebaseSync.syncRepayment(repayment)
            if (updatedKhata != null) firebaseSync.syncKhata(updatedKhata)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "KHATA_REPAYMENT",
                    details = "Received ₹${applied.toInt()} ($mode) from $customerName"
                )
            )
        }
    }

    /** Removes a wrongly entered manual credit and takes it back off the customer's due. */
    fun deleteKhataCreditEntry(credit: KhataCredit) {
        viewModelScope.launch {
            val updatedKhata = db.withTransaction {
                dao.deleteKhataCredit(credit.id)
                dao.recordRepayment(credit.customerId, credit.amount)
                dao.getKhataById(credit.customerId)
            }
            firebaseSync.delete("khata_credits", credit.id)
            if (updatedKhata != null) firebaseSync.syncKhata(updatedKhata)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "KHATA_CREDIT_DELETED",
                    details = "Credit entry ₹${credit.amount.toInt()} removed for ${credit.customerName}"
                )
            )
        }
    }

    /** Removes a wrongly entered payment and adds the amount back to the customer's due. */
    fun deleteKhataRepaymentEntry(repayment: KhataRepayment) {
        viewModelScope.launch {
            val updatedKhata = db.withTransaction {
                dao.deleteRepaymentById(repayment.id)
                dao.addDebtById(repayment.customerId, repayment.amount)
                dao.getKhataById(repayment.customerId)
            }
            firebaseSync.delete("khata_repayments", repayment.id)
            if (updatedKhata != null) firebaseSync.syncKhata(updatedKhata)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "KHATA_REPAYMENT_DELETED",
                    details = "Payment ₹${repayment.amount.toInt()} (${repayment.paymentMode}) removed for ${repayment.customerName}"
                )
            )
        }
    }

    /** Remove an item from the menu (asks for confirmation in the UI) and keeps an audit trail. */
    fun deleteProductWithLog(product: Product) {
        viewModelScope.launch {
            dao.deleteProduct(product)
            firebaseSync.delete("products", product.id)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "PRODUCT_DELETED",
                    details = "Item deleted: ${product.name} (Category: ${product.category})"
                )
            )
        }
    }

    // ---------------- DAILY EXPENSES ----------------
    fun addExpense(title: String, category: String, amount: Double, paymentMode: String) {
        viewModelScope.launch {
            val expense = Expense(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                category = category,
                amount = amount,
                paymentMode = paymentMode
            )
            dao.insertExpense(expense)
            firebaseSync.syncExpense(expense)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "EXPENSE_ADDED",
                    details = "Expense ₹${amount.toInt()} ($category, $paymentMode): $title"
                )
            )
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            dao.deleteExpense(expense)
            firebaseSync.delete("expenses", expense.id)
        }
    }

    // ---------------- DAY-END CLOSING & SETTLEMENT ----------------
    fun setOpeningCash(amount: Double) {
        _openingCash.value = amount
    }

    fun saveDayClosing(actualCash: Double) {
        viewModelScope.launch {
            val expected = expectedCash.value
            val discrepancy = actualCash - expected
            val closing = DayClosing(
                date = todayDateString,
                openingCash = _openingCash.value,
                cashSales = cashSalesAmount.value,
                udharCashRepayment = udharCashRepaymentTotal.value,
                cashExpenses = cashExpensesTotal.value,
                expectedCash = expected,
                actualCash = actualCash,
                discrepancy = discrepancy
            )
            dao.insertOrUpdateClosing(closing)
            firebaseSync.syncClosing(closing)
        }
    }

    fun savePaytmSettlement(chargeDeduction: Double, isSettled: Boolean) {
        viewModelScope.launch {
            val totalUpi = totalUpiCollection.value
            val netBank = (totalUpi - chargeDeduction).coerceAtLeast(0.0)
            val settlement = PaytmSettlement(
                date = todayDateString,
                totalUpiCollection = totalUpi,
                chargeDeduction = chargeDeduction,
                netBankAmount = netBank,
                isSettled = isSettled
            )
            dao.insertOrUpdateSettlement(settlement)
            firebaseSync.syncSettlement(settlement)
        }
    }

    /** Marks one day's UPI collection as settled (or pending again). bankAmount = what Paytm actually credited. */
    fun saveSettlementForDate(date: String, totalUpi: Double, bankAmount: Double, isSettled: Boolean) {
        viewModelScope.launch {
            val settlement = PaytmSettlement(
                date = date,
                totalUpiCollection = totalUpi,
                chargeDeduction = (totalUpi - bankAmount).coerceAtLeast(0.0),
                netBankAmount = bankAmount.coerceAtLeast(0.0),
                isSettled = isSettled
            )
            dao.insertOrUpdateSettlement(settlement)
            firebaseSync.syncSettlement(settlement)
        }
    }

    /** Paytm usually credits several days in one transfer: mark all given days settled (no charge). */
    fun settleDates(daysWithUpi: Map<String, Double>) {
        daysWithUpi.forEach { (date, total) -> saveSettlementForDate(date, total, total, true) }
    }

    // ---------------- PRODUCT / MENU SETUP ----------------
    fun addProduct(
        name: String,
        category: String,
        brand: String,
        price: Double,
        boxes: Int,
        lowStockLimit: Int? = null
    ) {
        viewModelScope.launch {
            val trackBoxes = dao.getAllCategories().first().firstOrNull {
                it.name.equals(category.trim(), ignoreCase = true)
            }?.trackBoxes ?: false
            val product = Product(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                category = category.trim(),
                brand = if (trackBoxes) brand.trim() else "",
                price = price,
                sealedBoxes = if (trackBoxes) boxes else 0,
                lowStockLimit = if (trackBoxes) lowStockLimit else null
            )
            dao.insertProduct(product)
            firebaseSync.syncProduct(product)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "PRODUCT_ADDED",
                    details = "Item added: ${name.trim()} (Category: ${category.trim()}, Rate: ₹$price)"
                )
            )
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            dao.deleteProduct(product)
            firebaseSync.delete("products", product.id)
        }
    }

    // ---------------- WHATSAPP REPORT GENERATION ----------------
    fun generateDailyReportWhatsAppText(actualCash: Double?): String {
        val total = totalSalesAmount.value.toInt()
        val cash = cashSalesAmount.value.toInt()
        val upi = upiSalesAmount.value.toInt()
        val udhar = udharSalesAmount.value.toInt()

        val catMap = categorySales.value
        val catSummary = if (catMap.isNotEmpty()) {
            catMap.entries.joinToString("\n") { "• ${it.key}: ₹${it.value.toInt()}" }
        } else {
            "• (No sales)"
        }

        val exp = cashExpensesTotal.value.toInt()
        val rec = udharCashRepaymentTotal.value.toInt()
        val expected = expectedCash.value.toInt()
        val act = actualCash?.toInt() ?: expected
        val diff = act - expected

        val iceCreamCategories = categories.value.filter { it.trackBoxes }.map { it.name }
        val lowStock = iceCreamVarietyStock.value.filter { stock ->
            stock.varietyName != "Opening stock (unassigned)" &&
                iceCreamCategories.any { it.equals(stock.categoryName, ignoreCase = true) } &&
                stock.sealedBoxes <= stock.lowStockLimit
        }
        val stockWarningText = if (lowStock.isNotEmpty()) {
            "⚠️ *Low stock alert:*\n" + lowStock.joinToString("\n") {
                "• ${it.companyName} ${it.varietyName}: only ${it.sealedBoxes} box left (limit ${it.lowStockLimit})."
            }
        } else {
            "✅ *Stock status:* All boxes are sufficient."
        }

        return """
🍧 *REFRESH SODA & ICE-CREAM* 🍧
📅 *Date:* $todayDateString
━━━━━━━━━━━━━━━━━━━━
💰 *Today's Total Sales:* ₹$total
$catSummary

💳 *Payment Split:*
• Cash Sales: ₹$cash
• Paytm / UPI: ₹$upi
• Credit Today: ₹$udhar

💵 *Till Day-End Closing:*
• Opening Cash: ₹${_openingCash.value.toInt()}
• (+) Cash Sales: ₹$cash
• (+) Credit Recovered (Cash): ₹$rec
• (-) Till Expenses: ₹$exp
━━━━━━━━━━━━━━━━━━━━
• System Total: ₹$expected
• Actual Cash: ₹$act
• *Difference:* ${if (diff == 0) "✅ Exact match (₹0)" else if (diff < 0) "❌ ₹${-diff} Short" else "➕ ₹$diff Surplus"}

$stockWarningText
━━━━━━━━━━━━━━━━━━━━
_Report generated by Refresh Soda App_
        """.trimIndent()
    }

    fun generateBillReceiptText(sale: SaleTransaction): String {
        val shop = userProfile.value.shopName.ifBlank { "Refresh Soda & Ice-cream" }
        val saleDate = dateFormat.format(Date(sale.timestamp))
        val sb = StringBuilder()
        sb.appendLine("🧾 *${shop.uppercase()}*")
        sb.appendLine("📅 Date: $saleDate")
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        if (sale.itemsDetails.isNotBlank()) {
            sale.itemsDetails.split(";").forEach { itemStr ->
                val parts = itemStr.split("|")
                if (parts.size >= 4) {
                    val name = parts[1]
                    val qty = parts[2]
                    val price = parts[3]
                    sb.appendLine("• $name x$qty = ₹${(qty.toIntOrNull() ?: 1) * (price.toDoubleOrNull() ?: 0.0)}")
                }
            }
        } else {
            sb.appendLine("• Items: ${sale.itemsSummary}")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        sb.appendLine("💰 *Total: ₹${sale.totalAmount.toInt()}*")
        sb.appendLine("💳 Payment: ${sale.paymentMode}")
        if (!sale.customerName.isNullOrBlank()) {
            sb.appendLine("👤 Customer: ${sale.customerName}")
        }
        sb.appendLine("🙏 Thank you! Visit Again!")
        return sb.toString()
    }

    // ---------------- AUTHENTICATION & MULTI-USER ACTIONS ----------------
    fun login(identifier: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (identifier.isBlank() || pass.isBlank()) {
                onResult(false, getString("err_fill_all"))
                return@launch
            }
            val isPhoneIdentifier = identifier.trim().all {
                it.isDigit() || it in "+-(). "
            }
            val normalizedDigits = identifier.filter(Char::isDigit).takeLast(10)
            val localUser = dao.getUserByEmailOrPhone(identifier.trim())
                ?: if (isPhoneIdentifier && normalizedDigits.length == 10) {
                    dao.getUserByPhoneLastTenDigits(normalizedDigits)
                } else {
                    null
                }
            val user = localUser ?: try {
                firebaseSync.findUserByEmailOrPhone(identifier.trim())
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Login could not connect to account backup. Check your internet and try again.")
                return@launch
            }
            if (user == null) {
                onResult(false, getString("err_user_not_found"))
                return@launch
            }
            if (!SecurityUtils.verifyPassword(pass, user.password)) {
                onResult(false, getString("err_wrong_password"))
                return@launch
            }
            if (localUser == null) {
                dao.insertUser(user)
            }
            // Upgrade legacy credentials (plain text / fixed-salt SHA-256) to per-user-salt PBKDF2.
            val activeUser = if (SecurityUtils.needsUpgrade(user.password)) {
                user.copy(password = SecurityUtils.hashPassword(pass)).also { dao.insertUser(it) }
            } else {
                user
            }
            dao.updateLastLogin(activeUser.id, System.currentTimeMillis())
            // Fresh install / new phone: this account was not on the device, so pull the shop's data
            // back from the cloud BEFORE the session (and the cloud mirror) starts.
            if (localUser == null) {
                try {
                    firebaseSync.shopId = activeUser.effectiveShopId
                    firebaseSync.restoreAllFromCloud(dao)
                    sessionPreferences.edit().putBoolean("cloud_backup_done_${activeUser.effectiveShopId}", true).apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Login still succeeds; the owner can retry from Settings > Restore Data.
                }
            }
            _currentUser.value = activeUser
            sessionPreferences.edit().putString(sessionUserIdKey, activeUser.id).apply()
            onUserSessionStarted(activeUser)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "USER_LOGIN",
                    details = "User logged in: ${activeUser.name} (${activeUser.role})"
                )
            )
            onResult(true, null)
        }
    }

    fun register(
        name: String,
        identifier: String,
        pass: String,
        role: String,
        shopName: String,
        phone: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || identifier.isBlank() || pass.isBlank()) {
                onResult(false, getString("err_fill_all"))
                return@launch
            }
            val existing = dao.getUserByEmailOrPhone(identifier.trim())
            if (existing != null) {
                onResult(false, getString("err_user_exists"))
                return@launch
            }
            // Staff accounts can only be created by a logged-in owner (from Settings), and they join that owner's shop.
            val actor = _currentUser.value
            if (role == "STAFF" && actor?.role != "OWNER") {
                onResult(false, "Only the owner can create a staff account from Settings.")
                return@launch
            }
            val newUser = UserAccount(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                emailOrPhone = identifier.trim(),
                password = SecurityUtils.hashPassword(pass),
                role = role,
                shopName = shopName.trim(),
                phone = phone.ifBlank { identifier.trim() },
                shopId = if (role == "STAFF") actor?.effectiveShopId.orEmpty() else ""
            )
            dao.insertUser(newUser)
            if (role != "STAFF") {
                // Creating a staff account must not switch the owner's session to the staff user.
                _currentUser.value = newUser
                sessionPreferences.edit().putString(sessionUserIdKey, newUser.id).apply()
                onUserSessionStarted(newUser)
            }

            if (role == "OWNER") {
                dao.insertOrUpdateProfile(
                    UserProfile(
                        shopName = newUser.shopName,
                        ownerName = newUser.name,
                        phone = newUser.phone
                    )
                )
            }

            firebaseSync.syncUserToCloud(newUser)

            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "USER_REGISTER",
                    details = "Registered new user: ${newUser.name} (${newUser.role})"
                )
            )
            onResult(true, null)
        }
    }

    fun logout() {
        viewModelScope.launch {
            val user = _currentUser.value
            _currentUser.value = null
            firebaseSync.shopId = ""
            sessionPreferences.edit().remove(sessionUserIdKey).apply()
            if (user != null) {
                dao.insertAuditLog(
                    AuditLog(
                        id = UUID.randomUUID().toString(),
                        action = "USER_LOGOUT",
                        details = "User logged out: ${user.name}"
                    )
                )
            }
        }
    }

    fun deleteUserAccount(user: UserAccount) {
        viewModelScope.launch {
            dao.deleteUser(user)
            firebaseSync.deleteUserFromCloud(user.id)
            if (_currentUser.value?.id == user.id ||
                sessionPreferences.getString(sessionUserIdKey, null) == user.id
            ) {
                _currentUser.value = null
                sessionPreferences.edit().remove(sessionUserIdKey).apply()
            }
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "USER_DELETED",
                    details = "Deleted staff user: ${user.name}"
                )
            )
        }
    }
}

class ShopViewModelFactory(
    private val dao: ShopDao,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(dao, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
