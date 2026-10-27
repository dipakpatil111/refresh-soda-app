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
        var brand: String? = null
        var invoiceNo: String? = null
        val items = mutableListOf<InwardBillItemDraft>()

        val lower = text.lowercase()
        if (lower.contains("hocco")) brand = "Hocco"
        else if (lower.contains("vadilal")) brand = "Vadilal"
        else if (lower.contains("hajoori")) brand = "Hajoori"
        else if (lower.contains("amul")) brand = "Amul"

        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Find invoice number pattern
        val invRegex = Regex("""(?:inv(?:oice)?|bill|no|memo)\s*[:#.-]?\s*([A-Za-z0-9\-_/]+)""", RegexOption.IGNORE_CASE)
        for (line in lines) {
            val match = invRegex.find(line)
            if (match != null && invoiceNo == null) {
                invoiceNo = match.groupValues.getOrNull(1)
            }
        }

        // Parse line items
        // Common invoice line formats:
        // "1. Mango Dolly 5 Box 320 480"
        // "Hocco Chocobar Qty: 4 Rate: 450"
        // "Jeera Soda 10 220 360"
        val numberRegex = Regex("""\b\d+(?:\.\d+)?\b""")

        for (line in lines) {
            val lowerLine = line.lowercase()
            // Ignore headers and totals
            if (lowerLine.contains("total") || lowerLine.contains("subtotal") || lowerLine.contains("gst") ||
                lowerLine.contains("invoice") || lowerLine.contains("date") || lowerLine.contains("signature")) {
                continue
            }

            val numbers = numberRegex.findAll(line).map { it.value }.toList()
            if (numbers.size >= 2) {
                // Remove digits to get item title
                var name = line.replace(Regex("""\b\d+(?:\.\d+)?\b"""), "")
                    .replace(Regex("""[₹,:\-_/|]"""), " ")
                    .replace(Regex("""\b(?:box|boxes|qty|rate|mrp|rs|pcs)\b""", RegexOption.IGNORE_CASE), "")
                    .trim()

                if (name.length > 2) {
                    val qty = numbers.firstOrNull()?.toIntOrNull() ?: 1
                    val cost = numbers.getOrNull(1)?.toDoubleOrNull() ?: 350.0
                    val sell = numbers.getOrNull(2)?.toDoubleOrNull() ?: (cost * 1.4)

                    // Normalize name with brand if missing
                    val activeBrand = brand ?: fallbackBrand
                    if (!name.contains(activeBrand, ignoreCase = true)) {
                        name = "$activeBrand $name"
                    }

                    items.add(InwardBillItemDraft(name, qty, cost, sell))
                }
            }
        }

        return ParsedInvoiceData(brand, invoiceNo, items)
    }
}

