package com.refreshsoda.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.refreshsoda.app.MainActivity
import com.refreshsoda.app.data.alerts.AlertsEngine
import com.refreshsoda.app.data.backup.LocalBackup
import com.refreshsoda.app.data.local.AppDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Runs once a day: takes a local backup copy and notifies about low stock, overdue udhaar and supplier dues. */
class DailyShopWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            LocalBackup.autoBackup(applicationContext)

            val dao = AppDatabase.getDatabase(applicationContext).shopDao()
            val alerts = AlertsEngine.compute(
                varieties = dao.snapshotVarietyStock(),
                sales = dao.snapshotSales(),
                purchaseItems = dao.snapshotPurchaseItems(),
                khata = dao.snapshotKhata(),
                purchases = dao.snapshotPurchases()
            )
            if (!alerts.isEmpty) postNotification(alerts.lowStock.size, alerts.overdueKhata.size, alerts.supplierDueTotal, alerts.supplierDueBills,
                alerts.lowStock.firstOrNull()?.let { "${it.companyName} ${it.varietyName}: ${it.boxes} box" })
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun postNotification(lowStock: Int, overdue: Int, supplierDue: Double, supplierBills: Int, firstLow: String?) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Dukaan alerts", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val lines = mutableListOf<String>()
        if (lowStock > 0) lines.add("⚠️ $lowStock item ka stock kam hai" + (firstLow?.let { " ($it)" } ?: ""))
        if (overdue > 0) lines.add("📒 $overdue customer ka udhaar due date se upar")
        if (supplierBills > 0) lines.add("🏭 Supplier ka baaki ₹${supplierDue.toInt()} ($supplierBills bill)")

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Aaj ke alerts")
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "shop_alerts"
        private const val NOTIFICATION_ID = 4101
        private const val UNIQUE_NAME = "daily_shop_worker"

        /** Schedules the daily run at about 9 AM. Safe to call on every app start. */
        fun schedule(context: Context) {
            val now = Calendar.getInstance()
            val next = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
            }
            val request = PeriodicWorkRequestBuilder<DailyShopWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(next.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
