package com.refreshsoda.app.util

import android.graphics.Bitmap
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Locale

/** Builds a UPI payment QR that already contains the bill amount. */
object UpiQr {
    fun paymentUri(upiId: String, payeeName: String, amount: Double, note: String): String =
        Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId.trim())
            .appendQueryParameter("pn", payeeName.ifBlank { "Shop" })
            .appendQueryParameter("am", String.format(Locale.US, "%.2f", amount))
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", note)
            .build()
            .toString()

    fun bitmap(content: String, size: Int = 640): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        return bitmap
    }
}
