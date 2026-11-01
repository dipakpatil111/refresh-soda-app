package com.refreshsoda.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Payment section for inward stock dialog.
 * Allows split payment: separate cash and online amounts.
 */
@Composable
fun PaymentSection(
    paymentStatus: String,
    onPaymentStatusChange: (String) -> Unit,
    cashAmount: String,
    onCashAmountChange: (String) -> Unit,
    onlineAmount: String,
    onOnlineAmountChange: (String) -> Unit,
    billTotal: Double,
    isEditingPurchase: Boolean
) {
    if (isEditingPurchase) return

    // 1. PAYMENT STATUS
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Payment Status",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("PAID", "UNPAID", "PARTIAL")) { status ->
                FilterChip(
                    selected = paymentStatus == status,
                    onClick = { onPaymentStatusChange(status) },
                    label = {
                        Text(
                            status.replaceFirstChar { it.uppercase() },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    }

    // 2. SPLIT PAYMENT (Cash + Online)
    if (paymentStatus != "UNPAID") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Payment Amount",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AppTextField(
                    value = cashAmount,
                    onValueChange = onCashAmountChange,
                    label = "Cash ₹",
                    placeholder = "0",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                AppTextField(
                    value = onlineAmount,
                    onValueChange = onOnlineAmountChange,
                    label = "Online (UPI/Bank) ₹",
                    placeholder = "0",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            // Total and validation
            val cashVal = cashAmount.toDoubleOrNull() ?: 0.0
            val onlineVal = onlineAmount.toDoubleOrNull() ?: 0.0
            val totalPaid = cashVal + onlineVal
            val remaining = (billTotal - totalPaid).coerceAtLeast(0.0)

            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bill Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${billTotal.toInt()}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "₹${totalPaid.toInt()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (totalPaid <= billTotal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    if (remaining > 0.5) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Due Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "₹${remaining.toInt()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Validation info
            if (totalPaid > billTotal + 0.5) {
                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.width(16.dp)
                        )
                        Text(
                            "Paid amount exceeds bill total",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
