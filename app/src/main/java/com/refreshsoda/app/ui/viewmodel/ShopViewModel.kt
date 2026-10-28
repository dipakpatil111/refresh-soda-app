package com.refreshsoda.app.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.refreshsoda.app.data.firebase.FirebaseSyncService
import com.refreshsoda.app.data.local.ShopDao
import com.refreshsoda.app.data.model.*
import com.refreshsoda.app.data.security.SecurityUtils
import com.refreshsoda.app.ui.i18n.AppLanguage
import com.refreshsoda.app.ui.i18n.AppStrings
import com.refreshsoda.app.ui.theme.AppThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val addOns: List<String> = emptyList(),
    val addOnCost: Double = 0.0,
    val customPrice: Double = product.price
) {
    val total: Double get() = (customPrice + addOnCost) * quantity
}

data class TopItemStats(
    val name: String = "N/A",
    val pieces: Int = 0,
    val revenue: Double = 0.0
)

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
    private val dao: ShopDao,
    context: Context,
    private val firebaseSync: FirebaseSyncService = FirebaseSyncService()
) : ViewModel() {

    private val sessionPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences("shop_session", Context.MODE_PRIVATE)
    private val sessionUserIdKey = "current_user_id"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDateString: String = dateFormat.format(Date())

    // Language State
    private val _language = MutableStateFlow(AppLanguage.HINDI)
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
    val expenses = dao.getAllExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val closings = dao.getAllClosings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settlements = dao.getAllSettlements().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = dao.getAllAuditLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Multi-User Authentication Flows
    val allUsers = dao.getAllUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser = _currentUser.asStateFlow()
    private val _sessionChecked = MutableStateFlow(false)
    val sessionChecked = _sessionChecked.asStateFlow()
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
    val topFavoriteProducts = combine(sales, products) { salesList, productList ->
        val countMap = mutableMapOf<String, Int>()
        salesList.forEach { sale ->
            if (sale.itemsDetails.isNotBlank()) {
                sale.itemsDetails.split(";").forEach { itemStr ->
                    val parts = itemStr.split("|")
                    if (parts.size >= 3) {
                        val name = parts[1].trim()
                        val qty = parts[2].toIntOrNull() ?: 1
                        countMap[name] = (countMap[name] ?: 0) + qty
                    }
                }
            }
        }
        val favList = mutableListOf<Product>()
        if (countMap.isNotEmpty()) {
            val sortedNames = countMap.entries.sortedByDescending { it.value }.map { it.key }
            sortedNames.take(8).forEach { name ->
                val prod = productList.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (prod != null) favList.add(prod)
            }
        }
        if (favList.size < 8) {
            productList.filter { !favList.contains(it) }.take(8 - favList.size).forEach { favList.add(it) }
        }
        favList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------------- DASHBOARD CALCULATIONS ----------------
    // Total Sales (Revenue from all modes)
    val totalSalesAmount = sales.map { list -> list.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val cashSalesAmount = sales.map { list -> list.filter { it.paymentMode == "CASH" }.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val upiSalesAmount = sales.map { list -> list.filter { it.paymentMode == "UPI" }.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val udharSalesAmount = sales.map { list -> list.filter { it.paymentMode == "UDHAR" }.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category-wise Sales Breakdown (Dynamically populated from user categories)
    val categorySales = combine(sales, categories) { salesList, catList ->
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

    val totalExpensesAmount = expenses.map { list -> list.sumOf { it.amount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayNetProfit = combine(totalSalesAmount, totalExpensesAmount) { sales, exp ->
        sales - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categoryExpenses = expenses.map { list ->
        val map = mutableMapOf<String, Double>()
        list.forEach { exp ->
            map[exp.category] = (map[exp.category] ?: 0.0) + exp.amount
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Today's Top Selling Item overall
    val topItem = sales.map { list ->
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
    val cashExpensesTotal = expenses.map { list ->
        list.filter { it.paymentMode == "CASH" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val udharCashRepaymentTotal = repayments.map { list ->
        list.filter { it.paymentMode == "CASH" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Expected Cash = Opening Cash + Cash Sales + Cash Udhar Recovery - Cash Expenses
    val expectedCash = combine(openingCash, cashSalesAmount, udharCashRepaymentTotal, cashExpensesTotal) { opening, cashSale, recovery, expense ->
        opening + cashSale + recovery - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // UPI Total Collection for Paytm Settlement (UPI Sales + UPI Udhar Repayments)
    val totalUpiCollection = combine(upiSalesAmount, repayments) { upiSale, repayList ->
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
    val topSodaAndIceCream = sales.map { list ->
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
    val overallProfitSummary = combine(sales, products, expenses) { salesList, productList, expenseList ->
        val costMap = productList.costLookup()
        val revenue = salesList.sumOf { it.totalAmount }
        val cost = salesList.sumOf { sale ->
            sale.soldLines().sumOf { line -> (costMap[line.name.lowercase()] ?: 0.0) * line.qty }
        }
        val gross = revenue - cost
        val expenseTotal = expenseList.sumOf { it.amount }
        OverallProfitSummary(
            totalRevenue = revenue,
            totalSalesCount = salesList.size,
            totalCost = cost,
            grossProfit = gross,
            grossMarginPercent = if (revenue > 0) (gross / revenue) * 100.0 else 0.0,
            totalExpenses = expenseTotal,
            netProfit = gross - expenseTotal
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
            dao.deleteCategory(category)
        }
    }

    fun updateCategory(category: ProductCategory) {
        viewModelScope.launch {
            dao.insertCategory(category.copy(lowStockLimit = category.lowStockLimit.coerceAtLeast(0)))
            if (!category.trackBoxes) {
                dao.clearCategoryBoxes(category.name)
                dao.clearSubCategoryBoxes(category.name)
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
            dao.insertSubCategory(
                subCategory.copy(
                    defaultPrice = subCategory.defaultPrice.coerceAtLeast(0.0),
                    sealedBoxes = subCategory.sealedBoxes.coerceAtLeast(0),
                    lowStockLimit = subCategory.lowStockLimit.coerceAtLeast(0)
                )
            )
        }
    }

    fun deleteSubCategory(subCategory: SubCategory) {
        viewModelScope.launch {
            dao.deleteSubCategory(subCategory)
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

    fun completeOrder(paymentMode: String, customerName: String? = null) {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return

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
                itemsDetails = details
            )
            dao.insertSale(sale)
            _lastSaleForUndo.value = sale
            saleUndoExpiryJob?.cancel()
            saleUndoExpiryJob = viewModelScope.launch {
                delay(5_000)
                if (_lastSaleForUndo.value?.id == sale.id) {
                    _lastSaleForUndo.value = null
                }
            }

            // If Udhaar sale, update or create Khata
            if (paymentMode == "UDHAR" && !customerName.isNullOrBlank()) {
                val existing = khataList.value.find { it.name.equals(customerName.trim(), ignoreCase = true) }
                if (existing != null) {
                    dao.addDebt(existing.name, finalTotal)
                } else {
                    val newKhata = CustomerKhata(
                        id = UUID.randomUUID().toString(),
                        name = customerName.trim(),
                        outstandingDebt = finalTotal
                    )
                    dao.insertOrUpdateKhata(newKhata)
                    firebaseSync.syncKhata(newKhata)
                }
            }

            clearCart()
            firebaseSync.syncSale(sale)
        }
    }

    fun deleteSale(sale: SaleTransaction) {
        viewModelScope.launch {
            dao.deleteSale(sale)
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
            dao.deleteSale(sale)
            if (sale.paymentMode == "UDHAR" && !sale.customerName.isNullOrBlank()) {
                val existing = khataList.value.find { it.name.equals(sale.customerName.trim(), ignoreCase = true) }
                if (existing != null) {
                    dao.recordRepayment(existing.id, sale.totalAmount)
                }
            }
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
                val prods = products.value
                prods.forEach { firebaseSync.syncProduct(it) }
                val sls = sales.value
                sls.take(50).forEach { firebaseSync.syncSale(it) }
                val kht = khataList.value
                kht.forEach { firebaseSync.syncKhata(it) }
                val exp = expenses.value
                exp.take(50).forEach { firebaseSync.syncExpense(it) }
                val pur = supplierPurchases.value
                pur.forEach { firebaseSync.syncPurchase(it) }

                dao.insertAuditLog(
                    AuditLog(
                        id = UUID.randomUUID().toString(),
                        action = "CLOUD_BACKUP",
                        details = "Backed up shop data to Firebase Cloud"
                    )
                )
                onResult(true, "All shop data backed up to cloud successfully!")
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

    fun openSubCategoryBox(subCategory: SubCategory) {
        viewModelScope.launch {
            val category = categories.value.firstOrNull {
                it.name.equals(subCategory.categoryName, ignoreCase = true)
            }
            if (category?.trackBoxes != true) return@launch
            dao.openSubCategoryBox(subCategory.id)
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
            dao.addSubCategoryBoxes(subCategory.id, count)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "STOCK_ADDED",
                    details = "$count Boxes added for: ${subCategory.name}"
                )
            )
        }
    }

    private suspend fun addVarietyBoxes(
        categoryName: String,
        companyName: String,
        varietyName: String,
        count: Int
    ) {
        val existing = dao.getIceCreamVarietyStock(categoryName, companyName, varietyName)
        val defaultLimit = categories.value.firstOrNull {
            it.name.equals(categoryName, ignoreCase = true)
        }?.lowStockLimit ?: 2
        dao.insertIceCreamVarietyStock(
            existing?.copy(sealedBoxes = (existing.sealedBoxes + count).coerceAtLeast(0))
                ?: IceCreamVarietyStock(
                    categoryName = categoryName,
                    companyName = companyName,
                    varietyName = varietyName.trim(),
                    sealedBoxes = count.coerceAtLeast(0),
                    lowStockLimit = defaultLimit
                )
        )
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
            addVarietyBoxes(category.name, company.name, cleanVarietyName, count)
            dao.addSubCategoryBoxes(company.id, count)
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
            val current = dao.getIceCreamVarietyStock(
                stock.categoryName,
                stock.companyName,
                stock.varietyName
            )
            val oldBoxes = current?.sealedBoxes ?: 0
            val updated = stock.copy(
                sealedBoxes = sealedBoxes.coerceAtLeast(0),
                lowStockLimit = lowStockLimit.coerceAtLeast(0)
            )
            dao.insertIceCreamVarietyStock(updated)
            dao.getSubCategories(stock.categoryName).firstOrNull {
                it.name.equals(stock.companyName, ignoreCase = true)
            }?.let { company ->
                dao.adjustSubCategoryBoxes(company.id, updated.sealedBoxes - oldBoxes)
            }
        }
    }

    fun openIceCreamVarietyBox(stock: IceCreamVarietyStock) {
        viewModelScope.launch {
            val company = dao.getSubCategories(stock.categoryName).firstOrNull {
                it.name.equals(stock.companyName, ignoreCase = true)
            } ?: return@launch
            val current = dao.getIceCreamVarietyStock(
                stock.categoryName,
                stock.companyName,
                stock.varietyName
            ) ?: return@launch
            if (current.sealedBoxes <= 0) return@launch
            dao.insertIceCreamVarietyStock(current.copy(sealedBoxes = current.sealedBoxes - 1))
            dao.adjustSubCategoryBoxes(company.id, -1)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "BOX_OPENED",
                    details = "Opened box: ${stock.companyName} • ${stock.varietyName}"
                )
            )
        }
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
    fun addNewCustomer(name: String, phone: String, limit: Double = 2000.0) {
        viewModelScope.launch {
            val customer = CustomerKhata(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone.trim(),
                outstandingDebt = 0.0,
                debtLimit = limit
            )
            dao.insertOrUpdateKhata(customer)
            firebaseSync.syncKhata(customer)
        }
    }

    fun recordKhataRepayment(customerId: String, customerName: String, amount: Double, paymentMode: String = "CASH") {
        viewModelScope.launch {
            dao.recordRepayment(customerId, amount)
            val repayment = KhataRepayment(
                id = UUID.randomUUID().toString(),
                customerId = customerId,
                customerName = customerName,
                amount = amount,
                paymentMode = paymentMode
            )
            dao.insertRepayment(repayment)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "KHATA_REPAYMENT",
                    details = "Received ₹${amount.toInt()} ($paymentMode) from $customerName"
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
            "• (कोई बिक्री नहीं)"
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
            "⚠️ *कम स्टॉक चेतावनी:*\n" + lowStock.joinToString("\n") {
                "• ${it.companyName} ${it.varietyName}: only ${it.sealedBoxes} box left (limit ${it.lowStockLimit})."
            }
        } else {
            "✅ *स्टॉक स्थिति:* सभी बॉक्स पर्याप्त हैं।"
        }

        return """
🍧 *REFRESH SODA & ICE-CREAM* 🍧
📅 *तारीख:* $todayDateString
━━━━━━━━━━━━━━━━━━━━
💰 *आज का कुल धंधा:* ₹$total
$catSummary

💳 *पेमेंट विभाजन:*
• Cash बिक्री: ₹$cash
• Paytm / UPI: ₹$upi
• आज का उधार: ₹$udhar

💵 *गल्ला डे-एंड क्लोज़िंग:*
• सुबह का कैश: ₹${_openingCash.value.toInt()}
• (+) Cash बिक्री: ₹$cash
• (+) उधार वसूली (Cash): ₹$rec
• (-) गल्ले का खर्चा: ₹$exp
━━━━━━━━━━━━━━━━━━━━
• सिस्टम हिसाब: ₹$expected
• असली गल्ला: ₹$act
• *फर्क:* ${if (diff == 0) "✅ बिल्कुल सही (₹0)" else if (diff < 0) "❌ ₹${-diff} Short" else "➕ ₹$diff Surplus"}

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
            // Auto upgrade legacy plain password to secure SHA-256 hash
            if (user.password == pass) {
                val upgradedUser = user.copy(password = SecurityUtils.hashPassword(pass))
                dao.insertUser(upgradedUser)
                firebaseSync.syncUserToCloud(upgradedUser)
            }
            dao.updateLastLogin(user.id, System.currentTimeMillis())
            _currentUser.value = user
            sessionPreferences.edit().putString(sessionUserIdKey, user.id).apply()
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "USER_LOGIN",
                    details = "User logged in: ${user.name} (${user.role})"
                )
            )
            onResult(true, null)
        }
    }

    fun quickLogin(user: UserAccount) {
        viewModelScope.launch {
            dao.updateLastLogin(user.id, System.currentTimeMillis())
            _currentUser.value = user
            sessionPreferences.edit().putString(sessionUserIdKey, user.id).apply()
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "USER_LOGIN",
                    details = "Quick login: ${user.name} (${user.role})"
                )
            )
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
            val newUser = UserAccount(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                emailOrPhone = identifier.trim(),
                password = SecurityUtils.hashPassword(pass),
                role = role,
                shopName = shopName.trim(),
                phone = phone.ifBlank { identifier.trim() }
            )
            dao.insertUser(newUser)
            _currentUser.value = newUser
            sessionPreferences.edit().putString(sessionUserIdKey, newUser.id).apply()

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

    // ---------------- INWARD STOCK & DISTRIBUTOR BILLS ----------------
    fun saveInwardStockPurchase(
        brand: String,
        category: String,
        billNumber: String,
        billDate: String,
        totalAmount: Double,
        paidAmount: Double,
        paymentStatus: String,
        paymentMode: String,
        draftItems: List<InwardBillItemDraft>,
        onComplete: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val categoryName = category.trim()
            val brandName = brand.trim()
            if (categoryName.isBlank() || brandName.isBlank()) {
                onComplete("Enter a supplier and choose or add a category.")
                return@launch
            }
            val currentCategories = dao.getAllCategories().first()
            val trackBoxes = currentCategories.firstOrNull {
                it.name.equals(categoryName, ignoreCase = true)
            }?.trackBoxes ?: false
            if (!trackBoxes) {
                onComplete("Inward stock is only available for ice-cream categories.")
                return@launch
            }
            if (draftItems.isEmpty() || draftItems.any {
                    it.name.isBlank() || (trackBoxes && it.boxes !in 1..10000) ||
                        it.costPrice !in 0.01..100000.0 || it.sellingPrice !in 0.01..100000.0
                }) {
                onComplete("Complete every item with a name, quantity, cost and selling price.")
                return@launch
            }
            if (paidAmount !in 0.0..totalAmount) {
                onComplete("Paid amount must be between zero and the bill total.")
                return@launch
            }
            val company = dao.getSubCategories(categoryName).firstOrNull {
                it.name.equals(brandName, ignoreCase = true)
            } ?: SubCategory(categoryName = categoryName, name = brandName).also {
                dao.insertSubCategory(it)
            }
            val validItems = draftItems

            val totalBoxes = if (trackBoxes) validItems.sumOf { it.boxes } else 0
            val itemsSummary = validItems.joinToString(", ") { item ->
                "${item.name} (${item.boxes} Box • Cost ₹${item.costPrice} • MRP ₹${item.sellingPrice})"
            }
            val purchase = SupplierPurchase(
                id = UUID.randomUUID().toString(),
                brand = brandName,
                category = categoryName,
                billNumber = billNumber.trim().ifBlank { "INV-${(System.currentTimeMillis() % 10000)}" },
                billDate = billDate.trim().ifBlank { todayDateString },
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                paymentStatus = paymentStatus,
                paymentMode = paymentMode,
                totalBoxes = totalBoxes,
                itemsSummary = itemsSummary
            )
            dao.insertSupplierPurchase(purchase)
            dao.addSubCategoryBoxes(company.id, totalBoxes)
            validItems
                .filter { it.name.isNotBlank() && it.boxes > 0 }
                .groupBy { it.name.trim().lowercase(Locale.getDefault()) }
                .values
                .forEach { items ->
                    val varietyName = items.first().name.trim()
                    val boxes = items.sumOf { it.boxes }
                    addVarietyBoxes(categoryName, company.name, varietyName, boxes)
                }

            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "STOCK_INWARD_BILL",
                    details = "Inward $brandName ($categoryName): $totalBoxes Boxes, Bill ₹${totalAmount.toInt()} [$paymentStatus, Paid: ₹${paidAmount.toInt()}]"
                )
            )

            firebaseSync.syncPurchase(purchase)
            onComplete(null)
        }
    }

    fun markSupplierBillPaid(purchase: SupplierPurchase) {
        viewModelScope.launch {
            dao.markPurchaseFullPaid(purchase.id)
            dao.insertAuditLog(
                AuditLog(
                    id = UUID.randomUUID().toString(),
                    action = "SUPPLIER_BILL_PAID",
                    details = "Paid balance for ${purchase.brand} Bill #${purchase.billNumber}"
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