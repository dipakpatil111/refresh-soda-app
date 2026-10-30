package com.refreshsoda.app.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.refreshsoda.app.data.local.AppDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Local (offline) backup, restore and CSV export of the shop database. */
object LocalBackup {
    private const val SQLITE_HEADER = "SQLite format 3"
    private const val KEEP_AUTO_BACKUPS = 7

    fun databaseFile(context: Context): File = context.getDatabasePath(AppDatabase.DB_NAME)

    /** Flushes the write-ahead log so a plain file copy contains every record. */
    private fun checkpoint(context: Context) {
        try {
            AppDatabase.getDatabase(context).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .use { cursor -> cursor.moveToFirst() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Copies the live database into the file the user picked. Call from a background thread. */
    fun exportDatabase(context: Context, target: Uri): Boolean {
        checkpoint(context)
        val source = databaseFile(context)
        if (!source.exists()) return false
        val stream = context.contentResolver.openOutputStream(target) ?: return false
        stream.use { out -> source.inputStream().use { input -> input.copyTo(out) } }
        return true
    }

    /**
     * Replaces the live database with a backup file. Returns an error message, or null on success.
     * The caller must restart the app afterwards (see [restartApp]).
     */
    fun importDatabase(context: Context, source: Uri): String? {
        val candidate = File(context.cacheDir, "restore_candidate.db")
        try {
            val input = context.contentResolver.openInputStream(source) ?: return "File open nahi ho payi."
            input.use { stream -> candidate.outputStream().use { out -> stream.copyTo(out) } }

            val header = ByteArray(SQLITE_HEADER.length)
            val read = candidate.inputStream().use { it.read(header) }
            if (read < header.size || String(header, Charsets.US_ASCII) != SQLITE_HEADER) {
                candidate.delete()
                return "Ye valid backup file nahi hai."
            }

            AppDatabase.closeInstance()
            val target = databaseFile(context)
            File(target.path + "-wal").delete()
            File(target.path + "-shm").delete()
            File(target.path + "-journal").delete()
            candidate.copyTo(target, overwrite = true)
            candidate.delete()
            return null
        } catch (e: Exception) {
            e.printStackTrace()
            return e.localizedMessage ?: "Restore fail ho gaya."
        }
    }

    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        Runtime.getRuntime().exit(0)
    }

    /** Daily automatic copy into the app's external files folder; keeps the newest [KEEP_AUTO_BACKUPS]. */
    fun autoBackup(context: Context): File? {
        return try {
            checkpoint(context)
            val source = databaseFile(context)
            if (!source.exists()) return null
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            val target = File(dir, "auto_$stamp.db")
            source.copyTo(target, overwrite = true)
            dir.listFiles { f -> f.name.startsWith("auto_") && f.name.endsWith(".db") }
                ?.sortedByDescending { it.name }
                ?.drop(KEEP_AUTO_BACKUPS)
                ?.forEach { it.delete() }
            target
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ---------------- CSV ----------------
    private fun csvCell(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    /** Writes a UTF-8 CSV (with BOM so Excel shows Hindi correctly) into the app cache and returns the file. */
    fun writeCsv(context: Context, fileName: String, header: List<String>, rows: List<List<String>>): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("\uFEFF")
            writer.write(header.joinToString(",") { csvCell(it) })
            writer.write("\n")
            rows.forEach { row ->
                writer.write(row.joinToString(",") { csvCell(it) })
                writer.write("\n")
            }
        }
        return file
    }
}
