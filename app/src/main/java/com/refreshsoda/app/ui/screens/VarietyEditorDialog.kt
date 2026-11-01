package com.refreshsoda.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dialog to add or edit an ice-cream variety with name, cost price, and MRP.
 */
@Composable
fun VarietyEditorDialog(
    categoryName: String,
    companyName: String,
    existingName: String = "",
    existingCost: Double = 0.0,
    existingMrp: Double = 0.0,
    existingLowLimit: Int = 2,
    onConfirm: (varietyName: String, costPrice: Double, mrp: Double, lowLimit: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var varietyName by remember { mutableStateOf(existingName) }
    var costPrice by remember { mutableStateOf(if (existingCost > 0) existingCost.toString() else "") }
    var mrp by remember { mutableStateOf(if (existingMrp > 0) existingMrp.toString() else "") }
    var lowLimit by remember { mutableStateOf(existingLowLimit.toString()) }

    val canSave = varietyName.isNotBlank() && 
        costPrice.toDoubleOrNull()?.let { it > 0 } == true &&
        mrp.toDoubleOrNull()?.let { it > 0 } == true &&
        lowLimit.toIntOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingName.isBlank()) "Add Variety" else "Edit Variety",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Category: $categoryName • Company: $companyName",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AppTextField(
                    value = varietyName,
                    onValueChange = { varietyName = it },
                    label = "Variety/Product Name",
                    placeholder = "e.g., Vanilla Cup 10",
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = "Cost Price (₹)",
                        placeholder = "0.0",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = mrp,
                        onValueChange = { mrp = it },
                        label = "MRP (₹)",
                        placeholder = "0.0",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }

                AppTextField(
                    value = lowLimit,
                    onValueChange = { lowLimit = it },
                    label = "Low Stock Limit",
                    placeholder = "2",
                    keyboardType = KeyboardType.Number,
                    singleLine = true
                )

                if (costPrice.toDoubleOrNull() != null && mrp.toDoubleOrNull() != null) {
                    val cost = costPrice.toDoubleOrNull() ?: 0.0
                    val price = mrp.toDoubleOrNull() ?: 0.0
                    val profit = price - cost
                    val margin = if (price > 0) (profit / price) * 100 else 0.0

                    Text(
                        "Profit: ₹${profit.toInt()} • Margin: ${margin.toInt()}%",
                        fontSize = 10.sp,
                        color = if (profit > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPrice.toDoubleOrNull() ?: 0.0
                    val price = mrp.toDoubleOrNull() ?: 0.0
                    val limit = lowLimit.toIntOrNull() ?: 2
                    onConfirm(varietyName, cost, price, limit)
                    onDismiss()
                },
                enabled = canSave
            ) {
                Text(if (existingName.isBlank()) "Add Variety" else "Update Variety")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
