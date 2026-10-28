package com.refreshsoda.app.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.refreshsoda.app.data.model.*
import kotlinx.coroutines.tasks.await

class FirebaseSyncService {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun syncSale(sale: SaleTransaction) {
        try {
            firestore.collection("sales").document(sale.id).set(sale).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncProduct(product: Product) {
        try {
            firestore.collection("products").document(product.id).set(product).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncKhata(khata: CustomerKhata) {
        try {
            firestore.collection("khata").document(khata.id).set(khata).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncExpense(expense: Expense) {
        try {
            firestore.collection("expenses").document(expense.id).set(expense).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncClosing(closing: DayClosing) {
        try {
            firestore.collection("closings").document(closing.date).set(closing).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncSettlement(settlement: PaytmSettlement) {
        try {
            firestore.collection("settlements").document(settlement.date).set(settlement).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncUserToCloud(user: UserAccount) {
        try {
            firestore.collection("users").document(user.id).set(user).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncPurchase(purchase: SupplierPurchase) {
        try {
            firestore.collection("supplier_purchases").document(purchase.id).set(purchase).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------------- TWO-WAY CLOUD RESTORE & BACKUP ----------------
    suspend fun restoreAllFromCloud(dao: com.refreshsoda.app.data.local.ShopDao): Int {
        var restoredCount = 0
        try {
            // Restore Products
            val prodSnap = firestore.collection("products").get().await()
            for (doc in prodSnap.documents) {
                val prod = doc.toObject(Product::class.java)
                if (prod != null) {
                    dao.insertProduct(prod)
                    restoredCount++
                }
            }

            // Restore Sales
            val salesSnap = firestore.collection("sales").get().await()
            for (doc in salesSnap.documents) {
                val sale = doc.toObject(SaleTransaction::class.java)
                if (sale != null) {
                    dao.insertSale(sale)
                    restoredCount++
                }
            }

            // Restore Khata
            val khataSnap = firestore.collection("khata").get().await()
            for (doc in khataSnap.documents) {
                val khata = doc.toObject(CustomerKhata::class.java)
                if (khata != null) {
                    dao.insertOrUpdateKhata(khata)
                    restoredCount++
                }
            }

            // Restore Expenses
            val expSnap = firestore.collection("expenses").get().await()
            for (doc in expSnap.documents) {
                val exp = doc.toObject(Expense::class.java)
                if (exp != null) {
                    dao.insertExpense(exp)
                    restoredCount++
                }
            }

            // Restore Supplier Purchases
            val purSnap = firestore.collection("supplier_purchases").get().await()
            for (doc in purSnap.documents) {
                val pur = doc.toObject(SupplierPurchase::class.java)
                if (pur != null) {
                    dao.insertSupplierPurchase(pur)
                    restoredCount++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return restoredCount
    }
}
