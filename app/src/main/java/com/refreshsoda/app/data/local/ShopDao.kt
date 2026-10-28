package com.refreshsoda.app.data.local

import androidx.room.*
import com.refreshsoda.app.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

    // User-Defined Custom Brands
    @Query("SELECT * FROM brands ORDER BY name ASC")
    fun getAllBrands(): Flow<List<ShopBrand>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrand(brand: ShopBrand)

    @Delete
    suspend fun deleteBrand(brand: ShopBrand)

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 'shop_profile' LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    // User-Defined Categories
    @Query("SELECT * FROM categories ORDER BY orderIndex, name ASC")
    fun getAllCategories(): Flow<List<ProductCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ProductCategory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<ProductCategory>)

    @Delete
    suspend fun deleteCategory(category: ProductCategory)

    @Query("SELECT * FROM sub_categories ORDER BY categoryName, name")
    fun getAllSubCategories(): Flow<List<SubCategory>>

    @Query("SELECT * FROM sub_categories WHERE categoryName = :categoryName ORDER BY name")
    suspend fun getSubCategories(categoryName: String): List<SubCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategory(subCategory: SubCategory)

    @Query("UPDATE sub_categories SET sealedBoxes = :count WHERE id = :id")
    suspend fun updateSubCategoryBoxes(id: String, count: Int)

    @Query("UPDATE sub_categories SET sealedBoxes = sealedBoxes + :count WHERE id = :id")
    suspend fun addSubCategoryBoxes(id: String, count: Int)

    @Query("UPDATE sub_categories SET sealedBoxes = MAX(0, sealedBoxes + :delta) WHERE id = :id")
    suspend fun adjustSubCategoryBoxes(id: String, delta: Int)

    @Query("UPDATE sub_categories SET sealedBoxes = CASE WHEN sealedBoxes > 0 THEN sealedBoxes - 1 ELSE 0 END WHERE id = :id")
    suspend fun openSubCategoryBox(id: String)

    @Query("UPDATE sub_categories SET sealedBoxes = 0 WHERE LOWER(categoryName) = LOWER(:categoryName)")
    suspend fun clearSubCategoryBoxes(categoryName: String)

    @Query("SELECT * FROM ice_cream_variety_stock ORDER BY categoryName, companyName, varietyName")
    fun getAllIceCreamVarietyStock(): Flow<List<IceCreamVarietyStock>>

    @Query("""
        SELECT * FROM ice_cream_variety_stock
        WHERE LOWER(categoryName) = LOWER(:categoryName)
          AND LOWER(companyName) = LOWER(:companyName)
          AND LOWER(varietyName) = LOWER(:varietyName)
        LIMIT 1
    """)
    suspend fun getIceCreamVarietyStock(
        categoryName: String,
        companyName: String,
        varietyName: String
    ): IceCreamVarietyStock?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIceCreamVarietyStock(stock: IceCreamVarietyStock)

    @Delete
    suspend fun deleteSubCategory(subCategory: SubCategory)

    // Products & Menu
    @Query("SELECT * FROM products ORDER BY category, name")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE category = :categoryName ORDER BY name ASC")
    fun getProductsByCategory(categoryName: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET sealedBoxes = CASE WHEN sealedBoxes > 0 THEN sealedBoxes - 1 ELSE 0 END WHERE id = :productId")
    suspend fun openBox(productId: String)

    @Query("UPDATE products SET sealedBoxes = sealedBoxes + :count WHERE id = :productId")
    suspend fun addBoxes(productId: String, count: Int)

    @Query("UPDATE products SET sealedBoxes = 0 WHERE LOWER(category) = LOWER(:categoryName)")
    suspend fun clearCategoryBoxes(categoryName: String)

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

    // User Accounts
    @Query("SELECT * FROM user_accounts ORDER BY createdAt ASC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Query("SELECT * FROM user_accounts WHERE LOWER(emailOrPhone) = LOWER(:identifier) LIMIT 1")
    suspend fun getUserByEmailOrPhone(identifier: String): UserAccount?

    @Query("""
        SELECT * FROM user_accounts
        WHERE substr(
            replace(replace(replace(replace(replace(replace(phone, '+', ''), ' ', ''), '-', ''), '(', ''), ')', ''), '.', ''),
            -10
        ) = :lastTenDigits
        LIMIT 1
    """)
    suspend fun getUserByPhoneLastTenDigits(lastTenDigits: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Delete
    suspend fun deleteUser(user: UserAccount)

    @Query("UPDATE user_accounts SET lastLoginAt = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long)

    // Supplier Purchases
    @Query("SELECT * FROM supplier_purchases ORDER BY timestamp DESC")
    fun getAllSupplierPurchases(): Flow<List<SupplierPurchase>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierPurchase(purchase: SupplierPurchase)

    @Delete
    suspend fun deleteSupplierPurchase(purchase: SupplierPurchase)

    @Query("UPDATE supplier_purchases SET paymentStatus = :status, paidAmount = totalAmount WHERE id = :id")
    suspend fun markPurchaseFullPaid(id: String, status: String = "PAID")
}