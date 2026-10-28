package com.refreshsoda.app.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.refreshsoda.app.data.model.*
import com.google.firebase.firestore.DocumentSnapshot
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

    suspend fun findUserById(userId: String): UserAccount? {
        val document = firestore.collection("users").document(userId).get().await()
        return document.takeIf { it.exists() }?.let(::userFromDocument)
    }

    suspend fun findUserByEmailOrPhone(identifier: String): UserAccount? {
        val users = firestore.collection("users").get().await()
        val normalizedIdentifier = identifier.trim()
        val identifierDigits = normalizedIdentifier.filter(Char::isDigit)
        val isPhoneIdentifier = normalizedIdentifier.all {
            it.isDigit() || it in "+-(). "
        }
        return users.documents
            .asSequence()
            .mapNotNull(::userFromDocument)
            .firstOrNull { user ->
                user.emailOrPhone.equals(normalizedIdentifier, ignoreCase = true) ||
                    (isPhoneIdentifier && identifierDigits.length >= 10 &&
                        user.phone.filter(Char::isDigit).takeLast(10) == identifierDigits.takeLast(10))
            }
    }

    private fun userFromDocument(document: DocumentSnapshot): UserAccount? {
        val data = document.data ?: return null
        val emailOrPhone = data["emailOrPhone"] as? String ?: return null
        val password = data["password"] as? String ?: return null
        return UserAccount(
            id = data["id"] as? String ?: document.id,
            name = data["name"] as? String ?: return null,
            emailOrPhone = emailOrPhone,
            password = password,
            role = data["role"] as? String ?: "OWNER",
            shopName = data["shopName"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            lastLoginAt = (data["lastLoginAt"] as? Number)?.toLong() ?: 0L
        )
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
            val userSnap = firestore.collection("users").get().await()
            for (doc in userSnap.documents) {
                val user = userFromDocument(doc)
                if (user != null) {
                    dao.insertUser(user)
                    restoredCount++
                }
            }

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
