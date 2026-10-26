package com.refreshsoda.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.refreshsoda.app.data.firebase.FirebaseSyncService
import com.refreshsoda.app.data.local.ShopDao
import com.refreshsoda.app.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class CartItem(val product: Product, val quantity: Int = 1)

class ShopViewModel(
    private val dao: ShopDao,
    private val firebaseSync: FirebaseSyncService = FirebaseSyncService()
) : ViewModel() {

    // Database flows
    val products = dao.getAllProducts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sales = dao.getAllSales().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val khataList = dao.getAllKhata().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val expenses = dao.getAllExpenses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart state for POS
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart = _cart.asStateFlow()

    // Dashboard calculations
    val totalSalesAmount = sales.map { list -> list.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val cashSalesAmount = sales.map { list -> list.filter { it.paymentMode == "CASH" }.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val upiSalesAmount = sales.map { list -> list.filter { it.paymentMode == "UPI" }.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val udharSalesAmount = sales.map { list -> list.filter { it.paymentMode == "UDHAR" }.sumOf { it.totalAmount } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Initial Default Data seeding
    init {
        viewModelScope.launch {
            products.first().let { current ->
                if (current.isEmpty()) {
                    val initialList = listOf(
                        Product(UUID.randomUUID().toString(), "Nimbu Soda", "Soda", "Hajoori", 20.0, 10),
                        Product(UUID.randomUUID().toString(), "Jeera Masala Soda", "Soda", "Hajoori", 20.0, 14),
                        Product(UUID.randomUUID().toString(), "₹10 Vanilla Cup", "Ice-cream", "Vadilal", 10.0, 5),
                        Product(UUID.randomUUID().toString(), "₹40 Choco Cone", "Ice-cream", "Hocco", 40.0, 4),
                        Product(UUID.randomUUID().toString(), "Mango Milkshake", "Milkshake", "Own Shop", 50.0, 0),
                        Product(UUID.randomUUID().toString(), "Sprite 250ml", "Cold Drink", "Own Shop", 20.0, 3)
                    )
                    initialList.forEach { dao.insertProduct(it) }
                }
            }
        }
    }

    // POS Cart Actions
    fun addToCart(product: Product) {
        val current = _cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            current[index] = current[index].copy(quantity = current[index].quantity + 1)
        } else {
            current.add(CartItem(product, 1))
        }
        _cart.value = current
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun completeOrder(paymentMode: String, customerName: String? = null) {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return

        val total = currentCart.sumOf { it.product.price * it.quantity }
        val summary = currentCart.joinToString(", ") { "${it.product.name} x${it.quantity}" }

        viewModelScope.launch {
            val sale = SaleTransaction(
                id = UUID.randomUUID().toString(),
                totalAmount = total,
                paymentMode = paymentMode,
                customerName = customerName,
                itemsSummary = summary
            )
            dao.insertSale(sale)
            firebaseSync.syncSale(sale)

            if (paymentMode == "UDHAR" && !customerName.isNullOrBlank()) {
                val existing = khataList.value.find { it.name.equals(customerName, ignoreCase = true) }
                if (existing != null) {
                    dao.addDebt(customerName, total)
                } else {
                    val newKhata = CustomerKhata(
                        id = UUID.randomUUID().toString(),
                        name = customerName,
                        outstandingDebt = total
                    )
                    dao.insertOrUpdateKhata(newKhata)
                    firebaseSync.syncKhata(newKhata)
                }
            }
            clearCart()
        }
    }

    // Godown Sealed Box Management
    fun openBox(productId: String) {
        viewModelScope.launch {
            dao.openBox(productId)
        }
    }

    // Menu Setup
    fun addProduct(name: String, category: String, brand: String, price: Double, boxes: Int) {
        viewModelScope.launch {
            val product = Product(
                id = UUID.randomUUID().toString(),
                name = name,
                category = category,
                brand = brand,
                price = price,
                sealedBoxes = boxes
            )
            dao.insertProduct(product)
            firebaseSync.syncProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            dao.deleteProduct(product)
        }
    }

    // Udhaar / Khata Repayment
    fun recordKhataRepayment(customerId: String, amount: Double) {
        viewModelScope.launch {
            dao.recordRepayment(customerId, amount)
        }
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
