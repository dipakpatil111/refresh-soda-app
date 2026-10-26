package com.refreshsoda.app.data.local

import androidx.room.*
import com.refreshsoda.app.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

    // Products & Menu
    @Query("SELECT * FROM products ORDER BY category, name")
    fun getAllProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET sealedBoxes = CASE WHEN sealedBoxes > 0 THEN sealedBoxes - 1 ELSE 0 END WHERE id = :productId")
    suspend fun openBox(productId: String)

    // Sales Transactions
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleTransaction)

    // Customers Khata (Udhaar)
    @Query("SELECT * FROM customers_khata ORDER BY outstandingDebt DESC")
    fun getAllKhata(): Flow<List<CustomerKhata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateKhata(customer: CustomerKhata)

    @Query("UPDATE customers_khata SET outstandingDebt = outstandingDebt + :amount WHERE name = :name")
    suspend fun addDebt(name: String, amount: Double)

    @Query("UPDATE customers_khata SET outstandingDebt = CASE WHEN outstandingDebt >= :amount THEN outstandingDebt - :amount ELSE 0 END WHERE id = :id")
    suspend fun recordRepayment(id: String, amount: Double)

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)
}
