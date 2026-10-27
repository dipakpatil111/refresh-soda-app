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
}
