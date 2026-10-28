package com.refreshsoda.app.data.ocr

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.refreshsoda.app.data.model.InwardBillItemDraft
import java.text.SimpleDateFormat
import java.util.Locale

object BillScannerService {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    data class ParsedInvoiceData(
        val detectedBrand: String? = null,
        val detectedInvoiceNo: String? = null,
        val detectedDate: String? = null,
        val items: List<InwardBillItemDraft> = emptyList(),
        val lineAmounts: Map<String, Double> = emptyMap()
    )

    fun scanBillImage(
        context: Context,
        imageUri: Uri,
        onResult: (success: Boolean, rawText: String, detectedBrand: String?, detectedInvoiceNo: String?, items: List<InwardBillItemDraft>) -> Unit
    ) {
        scanInvoiceImage(context, imageUri, emptyList(), false) { success, rawText, parsed ->
            onResult(
                success,
                rawText,
                parsed.detectedBrand,
                parsed.detectedInvoiceNo,
                parsed.items
            )
        }
    }

    fun scanInvoiceImage(
        context: Context,
        imageUri: Uri,
        savedSubCategoryNames: List<String>,
        addGstToCost: Boolean,
        onResult: (success: Boolean, rawText: String, parsed: ParsedInvoiceData) -> Unit
    ) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val raw = visionText.text
                    val parsed = parseInvoiceRows(
                        groupVisionRows(visionText),
                        raw,
                        savedSubCategoryNames,
                        addGstToCost
                    )
                    if (parsed.items.isNotEmpty()) {
                        onResult(true, raw, parsed)
                    } else {
                        scanRotatedImages(
                            context,
                            imageUri,
                            savedSubCategoryNames,
                            addGstToCost,
                            raw,
                            onResult
                        )
                    }
                }
                .addOnFailureListener { error ->
                    onResult(false, error.localizedMessage ?: "OCR Failed", ParsedInvoiceData())
                }
        } catch (error: Exception) {
            onResult(false, error.localizedMessage ?: "File read error", ParsedInvoiceData())
        }
    }

    private fun scanRotatedImages(
        context: Context,
        imageUri: Uri,
        savedSubCategoryNames: List<String>,
        addGstToCost: Boolean,
        originalText: String,
        onResult: (Boolean, String, ParsedInvoiceData) -> Unit
    ) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                onResult(
                    true,
                    originalText,
                    parseInvoiceText(originalText, savedSubCategoryNames, addGstToCost)
                )
                return
            }

            var sampleSize = 1
            while (bounds.outWidth / sampleSize > 2048 || bounds.outHeight / sampleSize > 2048) {
                sampleSize *= 2
            }
            val bitmap = context.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(
                    stream,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize }
                )
            }
            if (bitmap == null) {
                onResult(
                    true,
                    originalText,
                    parseInvoiceText(originalText, savedSubCategoryNames, addGstToCost)
                )
                return
            }

            val rotationAngles = listOf(90, 270)
            fun recognizeAt(index: Int, collectedText: String) {
                if (index >= rotationAngles.size) {
                    bitmap.recycle()
                    onResult(
                        true,
                        collectedText,
                        parseInvoiceText(collectedText, savedSubCategoryNames, addGstToCost)
                    )
                    return
                }

                recognizer.process(InputImage.fromBitmap(bitmap, rotationAngles[index]))
                    .addOnSuccessListener { visionText ->
                        val rotatedText = visionText.text
                        val parsed = parseInvoiceRows(
                            groupVisionRows(visionText),
                            rotatedText,
                            savedSubCategoryNames,
                            addGstToCost
                        )
                        if (parsed.items.isNotEmpty()) {
                            bitmap.recycle()
                            onResult(true, rotatedText, parsed)
                        } else {
                            recognizeAt(
                                index + 1,
                                listOf(collectedText, rotatedText)
                                    .filter { it.isNotBlank() }
                                    .joinToString("\n")
                            )
                        }
                    }
                    .addOnFailureListener {
                        recognizeAt(index + 1, collectedText)
                    }
            }

            recognizeAt(0, originalText)
        } catch (error: Exception) {
            onResult(
                false,
                error.localizedMessage ?: "Could not read the rotated invoice image.",
                ParsedInvoiceData()
            )
        }
    }

    fun parseInvoiceText(
        text: String,
        savedSubCategoryNames: List<String> = emptyList(),
        addGstToCost: Boolean = false
    ): ParsedInvoiceData =
        parseInvoiceRows(
            text.lines().map { it.trim() }.filter { it.isNotBlank() },
            text,
            savedSubCategoryNames,
            addGstToCost
        )

    private fun parseInvoiceRows(
        rows: List<String>,
        fullText: String,
        savedSubCategoryNames: List<String>,
        addGstToCost: Boolean
    ): ParsedInvoiceData {
        val invoiceNo = Regex(
            """invoice\s*no\.?\s*[:#-]?\s*([A-Za-z0-9_-]+)""",
            RegexOption.IGNORE_CASE
        ).find(fullText)?.groupValues?.get(1)
        val detectedDate = Regex(
            """\b(\d{1,2}-[A-Za-z]{3}-\d{4})\b"""
        ).find(fullText)?.groupValues?.get(1)?.toInvoiceDate()
        val detectedBrand = savedSubCategoryNames
            .filter(String::isNotBlank)
            .sortedByDescending(String::length)
            .firstOrNull { brand ->
                Regex(
                    """(?<![\p{L}\p{N}])${Regex.escape(brand)}(?![\p{L}\p{N}])""",
                    RegexOption.IGNORE_CASE
                ).containsMatchIn(fullText)
            }

        val items = mutableListOf<InwardBillItemDraft>()
        val amounts = mutableMapOf<String, Double>()
        rows.forEach { row ->
            val parsed = parseInvoiceItemRow(row, addGstToCost) ?: return@forEach
            val id = java.util.UUID.randomUUID().toString()
            items += InwardBillItemDraft(
                id = id,
                name = parsed.name,
                boxes = parsed.quantity,
                costPrice = parsed.costPrice,
                sellingPrice = parsed.mrp
            )
            amounts[id] = parsed.amount
        }

        return ParsedInvoiceData(
            detectedBrand = detectedBrand,
            detectedInvoiceNo = invoiceNo,
            detectedDate = detectedDate,
            items = items,
            lineAmounts = amounts
        )
    }

    private data class ParsedItem(
        val name: String,
        val quantity: Int,
        val costPrice: Double,
        val mrp: Double,
        val amount: Double
    )

    private fun parseInvoiceItemRow(row: String, addGstToCost: Boolean): ParsedItem? {
        val number = """[\d,]+(?:\.\d{1,5})?"""
        val rowPattern = Regex(
            """^\s*(?:\d+\s+)?\d{8}\s+(?<name>.+?)\s+(?<mrp>$number)\s+(?<qty>\d+)\s+(?:Box|Pcs|Case|Ctn|Nos)\s+(?<rate>$number)\s+(?<disc>$number)\s+(?<sgst>$number)%?\s+(?<cgst>$number)%?\s+(?<amount>$number)\s*$""",
            RegexOption.IGNORE_CASE
        )
        val match = rowPattern.matchEntire(row) ?: return null
        val quantity = match.groups["qty"]?.value?.toLongOrNull()
            ?.coerceIn(1L, 9999L)?.toInt() ?: return null
        val mrp = match.groups["mrp"]?.value?.toMoney() ?: return null
        val rate = match.groups["rate"]?.value?.toMoney() ?: return null
        val amount = match.groups["amount"]?.value?.toMoney() ?: return null
        val itemName = cleanItemName(match.groups["name"]?.value.orEmpty())
        if (itemName.isBlank()) return null
        return ParsedItem(
            name = itemName,
            quantity = quantity,
            costPrice = rate * if (addGstToCost) 1.05 else 1.0,
            mrp = mrp,
            amount = amount
        )
    }

    private fun groupVisionRows(visionText: Text): List<String> {
        data class Line(val text: String, val bounds: Rect) {
            val centerY: Float get() = (bounds.top + bounds.bottom) / 2f
            val height: Int get() = (bounds.bottom - bounds.top).coerceAtLeast(1)
        }

        val lines = visionText.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                val bounds = line.boundingBox
                if (bounds == null || line.text.isBlank()) null else Line(line.text.trim(), bounds)
            }
            .sortedBy { it.centerY }
        if (lines.isEmpty()) return visionText.text.lines().map(String::trim).filter(String::isNotBlank)

        val rows = mutableListOf<MutableList<Line>>()
        lines.forEach { line ->
            val row = rows.lastOrNull()
            if (row == null) {
                rows.add(mutableListOf(line))
            } else {
                val rowCenter = row.map { it.centerY }.average().toFloat()
                val tolerance = maxOf(line.height, row.map { it.height }.maxOrNull() ?: line.height) * 0.5f
                if (kotlin.math.abs(line.centerY - rowCenter) <= tolerance) {
                    row.add(line)
                } else {
                    rows.add(mutableListOf(line))
                }
            }
        }

        return rows.map { row ->
            row.sortedBy { it.bounds.left }.joinToString(" ") { it.text }
        }
    }

    private fun cleanItemName(value: String): String =
        value
            .replace(Regex("""\b\d{8}\b"""), " ")
            .replace(Regex("""\[\s*\d{5,}\s*]"""), " ")
            .replace("#", " ")
            .replace("_", " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase(Locale.getDefault()).replaceFirstChar { first ->
                    if (first.isLowerCase()) first.titlecase(Locale.getDefault()) else first.toString()
                }
            }

    private fun String.toMoney(): Double? =
        replace(",", "").toDoubleOrNull()

    private fun String.toInvoiceDate(): String? {
        val inputFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH).apply {
            isLenient = false
        }
        val parsed = runCatching { inputFormat.parse(this) }.getOrNull() ?: return null
        return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(parsed)
    }
}
