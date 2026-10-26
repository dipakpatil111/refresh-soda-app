package com.refreshsoda.app

import android.app.Application
import com.refreshsoda.app.data.local.AppDatabase

class RefreshApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
