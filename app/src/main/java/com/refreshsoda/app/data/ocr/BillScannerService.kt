package com.refreshsoda.app.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.refreshsoda.app.data.model.InwardBillItemDraft

object BillScannerService {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    fun scanBillImage(
        context: Context,
        imageUri: Uri,
        selectedBrand: String,
        onResult: (success: Boolean, rawText: String, detectedBrand: String?, detectedInvoiceNo: String?, items: List<InwardBillItemDraft>) -> Unit
    ) {
        try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val raw = visionText.text
                    val detected = parseInvoiceText(raw, selectedBrand)
                    onResult(true, raw, detected.detectedBrand, detected.detectedInvoiceNo, detected.items)
                }
                .addOnFailureListener { error ->
                    onResult(false, error.localizedMessage ?: "OCR Failed", null, null, emptyList())
                }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "File read error", null, null, emptyList())
        }
    }

    data class ParsedInvoiceData(
        val detectedBrand: String? = null,
        val detectedInvoiceNo: String? = null,
        val items: List<InwardBillItemDraft> = emptyList()
    )

    fun parseInvoiceText(text: String, fallbackBrand: String): ParsedInvoiceData {
        var invoiceNo: String? = null
        val items = mutableListOf<InwardBillItemDraft>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        val invRegex = Regex("""(?:invoice\s*no|inv\s*no|bill\s*no)[.:\s]*([A-Za-z0-9\-_/]+)""", RegexOption.IGNORE_CASE)
        for (line in lines) {
            val match = invRegex.find(line)
            if (match != null && invoiceNo == null) {
                invoiceNo = match.groupValues[1]
                break
            }
        }

        for (line in lines) {
            if (isInvoiceHeader(line)) continue

            val parsedItem = parseDelimitedItem(line)
                ?: parseBoxInvoiceItem(line)
                ?: parseDiscountInvoiceItem(line)
                ?: continue

            val itemName = cleanItemName(parsedItem.name)
            if (itemName.length < 2) continue
            val nameWithBrand = if (fallbackBrand.isNotBlank() && !itemName.contains(fallbackBrand, ignoreCase = true)) {
                "${fallbackBrand.trim()} $itemName"
            } else {
                itemName
            }
            items.add(
                InwardBillItemDraft(
                    name = nameWithBrand,
                    boxes = parsedItem.boxes,
                    costPrice = parsedItem.costPrice,
                    sellingPrice = parsedItem.sellingPrice
                )
            )
        }

        return ParsedInvoiceData(
            detectedBrand = fallbackBrand,
            detectedInvoiceNo = invoiceNo,
            items = items
        )
    }

    private data class ParsedItem(
        val name: String,
        val boxes: Int,
        val costPrice: Double,
        val sellingPrice: Double
    )

    private fun isInvoiceHeader(line: String): Boolean {
        val value = line.lowercase()
        return listOf(
            "road no", "plot no", "udhyog", "fssai", "gstin", "subtotal",
            "original", "tax invoice", "account holder", "description",
            "sgst", "cgst", "total invoice", "receiver", "address"
        ).any { marker -> value.contains(marker) }
    }

    private fun parseDelimitedItem(line: String): ParsedItem? {
        val match = Regex(
            """^\s*(.+?)\s*[,|]\s*(\d{1,5})\s*[,|]\s*([\d.]+)\s*[,|]\s*([\d.]+)\s*$"""
        ).matchEntire(line) ?: return null
        val boxes = match.groupValues[2].toIntOrNull() ?: return null
        val cost = match.groupValues[3].toDoubleOrNull() ?: return null
        val selling = match.groupValues[4].toDoubleOrNull() ?: return null
        return parsedItem(match.groupValues[1], boxes, cost, selling)
    }

    private fun parseBoxInvoiceItem(line: String): ParsedItem? {
        val match = Regex(
            """(?i)(.+?)\s+([\d,]+(?:\.\d{1,2})?)\s+(\d{1,5})\s+box(?:es)?\s+([\d,]+(?:\.\d{1,2})?)\b"""
        ).find(line) ?: return null
        val mrp = match.groupValues[2].toMoney() ?: return null
        val boxes = match.groupValues[3].toIntOrNull() ?: return null
        val cost = match.groupValues[4].toMoney() ?: return null
        return parsedItem(match.groupValues[1], boxes, cost, mrp)
    }

    private fun parseDiscountInvoiceItem(line: String): ParsedItem? {
        val match = Regex(
            """^\s*\d{1,3}\s+(.+?)\s+(\d{1,5})\s+([\d,]+\.\d{2})\s+[\d,]+\.\d{2}\s+[\d,]+\.\d{2}\s+[\d,]+\.\d{2}\s+([\d,]+\.\d{2})\s*$"""
        ).matchEntire(line) ?: return null
        val boxes = match.groupValues[2].toIntOrNull() ?: return null
        val selling = match.groupValues[3].toMoney() ?: return null
        val totalCost = match.groupValues[4].toMoney() ?: return null
        val cost = totalCost / boxes.coerceAtLeast(1)
        return parsedItem(match.groupValues[1], boxes, cost, selling)
    }

    private fun parsedItem(name: String, boxes: Int, costPrice: Double, sellingPrice: Double): ParsedItem? {
        if (boxes !in 1..10000 || costPrice !in 0.01..100000.0 || sellingPrice !in 0.01..100000.0) return null
        return ParsedItem(name, boxes, costPrice, sellingPrice)
    }

    private fun cleanItemName(value: String): String =
        value
            .replace(Regex("""^\s*\d{6,10}\s+"""), " ")
            .replace(Regex("""\b\d{6,10}\b"""), " ")
            .replace(Regex("""\b(?:scheme|hsn|sac)\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""[\[\]_|]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun String.toMoney(): Double? =
        replace(",", "").toDoubleOrNull()
}