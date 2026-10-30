package com.refreshsoda.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.refreshsoda.app.data.model.SaleTransaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareUtils {
    /** Shares a file from the app cache through the FileProvider declared in the manifest. */
    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

/** 80 mm style PDF receipt (also prints fine on A4). */
object ReceiptPdf {
    private const val PAGE_WIDTH = 226 // points, ~80 mm
    private const val MARGIN = 12f

    fun create(context: Context, shopName: String, address: String, phone: String, sale: SaleTransaction): File {
        val normal = Paint().apply { textSize = 9f; isAntiAlias = true }
        val bold = Paint(normal).apply { typeface = Typeface.DEFAULT_BOLD }
        val title = Paint(bold).apply { textSize = 12f; textAlign = Paint.Align.CENTER }
        val center = Paint(normal).apply { textAlign = Paint.Align.CENTER }
        val right = Paint(normal).apply { textAlign = Paint.Align.RIGHT }
        val rightBold = Paint(bold).apply { textAlign = Paint.Align.RIGHT }

        data class Line(val name: String, val qty: Int, val amount: Double)
        val lines = sale.itemsDetails.split(";").mapNotNull { raw ->
            val parts = raw.split("|")
            if (parts.size < 4) {
                null
            } else {
                val qty = parts[2].toIntOrNull() ?: 1
                Line(parts[1].trim(), qty, qty * (parts[3].toDoubleOrNull() ?: 0.0))
            }
        }

        val rowHeight = 13f
        val extraRows = (if (address.isNotBlank()) 1 else 0) + (if (phone.isNotBlank()) 1 else 0) +
            (if (!sale.customerName.isNullOrBlank()) 1 else 0) + (if (sale.discountAmount > 0) 1 else 0)
        val bodyRows = if (lines.isEmpty()) 1 else lines.size
        val height = (MARGIN * 2 + rowHeight * (bodyRows + extraRows + 10)).toInt()

        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, height, 1).create())
        val canvas: Canvas = page.canvas
        var y = MARGIN + 10f
        val centerX = PAGE_WIDTH / 2f
        val rightX = PAGE_WIDTH - MARGIN

        canvas.drawText(shopName.ifBlank { "Refresh Soda & Ice-cream" }, centerX, y, title); y += rowHeight + 2
        if (address.isNotBlank()) { canvas.drawText(address.take(38), centerX, y, center); y += rowHeight }
        if (phone.isNotBlank()) { canvas.drawText("Ph: $phone", centerX, y, center); y += rowHeight }
        canvas.drawText(SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.timestamp)), centerX, y, center)
        y += rowHeight
        canvas.drawLine(MARGIN, y - 4, rightX, y - 4, normal); y += 4

        if (lines.isEmpty()) {
            canvas.drawText(sale.itemsSummary.take(40), MARGIN, y, normal); y += rowHeight
        } else {
            lines.forEach { line ->
                canvas.drawText("${line.name.take(20)} x${line.qty}", MARGIN, y, normal)
                canvas.drawText("₹${"%.0f".format(line.amount)}", rightX, y, right)
                y += rowHeight
            }
        }
        canvas.drawLine(MARGIN, y - 4, rightX, y - 4, normal); y += 4
        if (sale.discountAmount > 0) {
            canvas.drawText("Discount", MARGIN, y, normal)
            canvas.drawText("-₹${"%.0f".format(sale.discountAmount)}", rightX, y, right); y += rowHeight
        }
        canvas.drawText("TOTAL", MARGIN, y, bold)
        canvas.drawText("₹${"%.0f".format(sale.totalAmount)}", rightX, y, rightBold); y += rowHeight
        canvas.drawText("Payment: ${sale.paymentMode}", MARGIN, y, normal); y += rowHeight
        if (!sale.customerName.isNullOrBlank()) { canvas.drawText("Customer: ${sale.customerName}", MARGIN, y, normal); y += rowHeight }
        y += 4
        canvas.drawText("Thank you! Visit again!", centerX, y, center)
        document.finishPage(page)

        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "bill_${sale.id.take(8)}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }
}
