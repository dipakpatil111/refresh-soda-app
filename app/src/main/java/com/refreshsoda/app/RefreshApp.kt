package com.refreshsoda.app

import android.app.Application
import com.refreshsoda.app.data.local.AppDatabase
import com.refreshsoda.app.work.DailyShopWorker

class RefreshApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        try {
            DailyShopWorker.schedule(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
