package com.refreshsoda.app.data.local

import androidx.room.*
import com.refreshsoda.app.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

    // User-Defined Categories
    @Query("SELECT * FROM categories ORDER BY orderIndex, name ASC")
    fun getAllCategories(): Flow<List<ProductCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ProductCategory)

    @Delete
    suspend fun deleteCategory(category: ProductCategory)

    // Products & Menu
    @Query("SELECT * FROM products ORDER BY category, name")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE category = :categoryName ORDER BY name ASC")
    fun getProductsByCategory(categoryName: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET sealedBoxes = CASE WHEN sealedBoxes > 0 THEN sealedBoxes - 1 ELSE 0 END WHERE id = :productId")
    suspend fun openBox(productId: String)

    @Query("UPDATE products SET sealedBoxes = sealedBoxes + :count WHERE id = :productId")
    suspend fun addBoxes(productId: String, count: Int)

    // Sales Transactions
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleTransaction)

    @Delete
    suspend fun deleteSale(sale: SaleTransaction)

    // Customers Khata (Udhaar)
    @Query("SELECT * FROM customers_khata ORDER BY outstandingDebt DESC")
    fun getAllKhata(): Flow<List<CustomerKhata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateKhata(customer: CustomerKhata)

    @Query("UPDATE customers_khata SET outstandingDebt = outstandingDebt + :amount WHERE name = :name")
    suspend fun addDebt(name: String, amount: Double)

    @Query("UPDATE customers_khata SET outstandingDebt = CASE WHEN outstandingDebt >= :amount THEN outstandingDebt - :amount ELSE 0 END WHERE id = :id")
    suspend fun recordRepayment(id: String, amount: Double)

    // Khata Repayments History
    @Query("SELECT * FROM khata_repayments ORDER BY timestamp DESC")
    fun getAllRepayments(): Flow<List<KhataRepayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: KhataRepayment)

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // Day Closings
    @Query("SELECT * FROM day_closings ORDER BY date DESC")
    fun getAllClosings(): Flow<List<DayClosing>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateClosing(closing: DayClosing)

    // Paytm Settlements
    @Query("SELECT * FROM paytm_settlements ORDER BY date DESC")
    fun getAllSettlements(): Flow<List<PaytmSettlement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettlement(settlement: PaytmSettlement)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)
}
