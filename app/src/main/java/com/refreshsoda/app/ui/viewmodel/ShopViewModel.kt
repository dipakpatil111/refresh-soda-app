package com.refreshsoda.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.refreshsoda.app.data.firebase.FirebaseSyncService
import com.refreshsoda.app.data.local.ShopDao
import com.refreshsoda.app.data.model.*
import com.refreshsoda.app.ui.i18n.AppLanguage
import com.refreshsoda.app.ui.i18n.AppStrings
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

class ShopViewModel(
    private val dao: ShopDao,
    private val firebaseSync: FirebaseSyncService = FirebaseSyncService()
) : ViewModel() {

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

    // Database flows
    val categories = dao.getAllCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val products = dao.getAllProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sales = dao.getAllSales().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val khataList = dao.getAllKhata().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val repayments = dao.getAllRepayments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = dao.getAllExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val closings = dao.getAllClosings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settlements = dao.getAllSettlements().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val auditLogs = dao.getAllAuditLogs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // POS Cart State
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart = _cart.asStateFlow()

    private val _discount = MutableStateFlow(0.0)
    val discount = _discount.asStateFlow()

    private val _openingCash = MutableStateFlow(500.0) // Default morning cash
    val openingCash = _openingCash.asStateFlow()

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 500.0)

    // UPI Total Collection for Paytm Settlement (UPI Sales + UPI Udhar Repayments)
    val totalUpiCollection = combine(upiSalesAmount, repayments) { upiSale, repayList ->
        val upiRepay = repayList.filter { it.paymentMode == "UPI" }.sumOf { it.amount }
        upiSale + upiRepay
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // NOTE: Default data seeding is intentionally removed! All categories and items are added by the user!

    // ---------------- CATEGORY MANAGEMENT ----------------
    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val category = ProductCategory(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                orderIndex = categories.value.size
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
            firebaseSync.syncSale(sale)

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

    // ---------------- GODOWN BOX STOCK ----------------
    fun openBox(productId: String, productName: String) {
        viewModelScope.launch {
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

    fun addBoxes(productId: String, productName: String, count: Int) {
        viewModelScope.launch {
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
    fun addProduct(name: String, category: String, brand: String, price: Double, boxes: Int) {
        viewModelScope.launch {
            val product = Product(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                category = category.trim(),
                brand = brand.trim(),
                price = price,
                sealedBoxes = boxes
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

        val lowStock = products.value.filter { it.sealedBoxes <= 2 }
        val stockWarningText = if (lowStock.isNotEmpty()) {
            "⚠️ *कम स्टॉक चेतावनी (<=2 Box):*\n" + lowStock.joinToString("\n") { "• ${it.name} (${it.brand}): ${it.sealedBoxes} Box" }
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
}

class ShopViewModelFactory(private val dao: ShopDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
