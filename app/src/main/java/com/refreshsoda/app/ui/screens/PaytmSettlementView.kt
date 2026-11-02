package com.refreshsoda.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.ui.theme.appColors
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class UpiDay(val date: String, val total: Double, val settled: Boolean, val bankAmount: Double)

private fun rupees(value: Double): String = "₹${"%,.0f".format(Locale.US, value)}"

/**
 * Paytm merchant settlement tracker.
 * Paytm credits UPI money to the bank 1-2 days later (often several days in one transfer),
 * so this shows which days' UPI collection is still pending and lets the owner mark them as received.
 */
@Composable
fun PaytmSettlementView(viewModel: ShopViewModel) {
    val sales by viewModel.sales.collectAsState()
    val repayments by viewModel.repayments.collectAsState()
    val settlements by viewModel.settlements.collectAsState()

    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val days = remember(sales, repayments, settlements) {
        val totals = sortedMapOf<String, Double>(compareByDescending { it })
        sales.filter { it.paymentMode == "UPI" }.forEach {
            val d = dayFormat.format(Date(it.timestamp))
            totals[d] = (totals[d] ?: 0.0) + it.totalAmount
        }
        repayments.filter { it.paymentMode == "UPI" }.forEach {
            val d = dayFormat.format(Date(it.timestamp))
            totals[d] = (totals[d] ?: 0.0) + it.amount
        }
        val byDate = settlements.associateBy { it.date }
        totals.filter { it.value > 0 }.map { (date, total) ->
            val saved = byDate[date]
            UpiDay(
                date = date,
                total = total,
                settled = saved?.isSettled == true,
                bankAmount = saved?.netBankAmount ?: total
            )
        }
    }

    val pending = days.filter { !it.settled }
    val pendingTotal = pending.sumOf { it.total }
    val settledDays = days.filter { it.settled }
    val monthKey = viewModel.todayDateString.take(7)
    val receivedThisMonth = settledDays.filter { it.date.startsWith(monthKey) }.sumOf { it.bankAmount }
    val chargesThisMonth = settledDays.filter { it.date.startsWith(monthKey) }.sumOf { (it.total - it.bankAmount).coerceAtLeast(0.0) }

    var editDay by remember { mutableStateOf<UpiDay?>(null) }
    var confirmAll by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
        // SUMMARY
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.heroSlate),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pending from Paytm", color = MaterialTheme.appColors.onHeroMuted, fontSize = 12.sp)
                    Text(
                        rupees(pendingTotal),
                        color = MaterialTheme.appColors.heroCaution,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "${pending.size} day(s) not yet received in bank",
                        color = MaterialTheme.appColors.onHeroMuted,
                        fontSize = 12.sp
                    )
                    HorizontalDivider(color = MaterialTheme.appColors.onHero.copy(alpha = 0.2f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Received this month", color = MaterialTheme.appColors.onHeroMuted, fontSize = 12.sp)
                        Text(rupees(receivedThisMonth), color = MaterialTheme.appColors.heroPositive, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Charges cut this month", color = MaterialTheme.appColors.onHeroMuted, fontSize = 12.sp)
                        Text(rupees(chargesThisMonth), color = MaterialTheme.appColors.heroNegative, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        if (pending.size > 1) {
            item {
                Button(
                    onClick = { confirmAll = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark all ${pending.size} pending days as received", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        if (days.isEmpty()) {
            item {
                Text(
                    "No UPI collection yet.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // DAY LIST
        items(days, key = { it.date }) { day ->
            val isToday = day.date == viewModel.todayDateString
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            day.date + if (isToday) " (today)" else "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            if (day.settled) {
                                val charge = (day.total - day.bankAmount).coerceAtLeast(0.0)
                                "Received ${rupees(day.bankAmount)}" + if (charge > 0) " • charge ${rupees(charge)}" else ""
                            } else "Pending",
                            fontSize = 11.sp,
                            color = if (day.settled) MaterialTheme.appColors.success else MaterialTheme.appColors.warning,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(rupees(day.total), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        TextButton(
                            onClick = {
                                if (day.settled) viewModel.saveSettlementForDate(day.date, day.total, day.bankAmount, false)
                                else editDay = day
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(if (day.settled) "Undo" else "Mark received", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    // SINGLE DAY: how much did Paytm actually credit?
    editDay?.let { day ->
        var received by remember(day.date) { mutableStateOf(day.total.toLong().toString()) }
        val receivedValue = received.toDoubleOrNull()
        AlertDialog(
            onDismissRequest = { editDay = null },
            title = { Text("${day.date} • ${rupees(day.total)}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = received,
                        onValueChange = { received = it },
                        label = { Text("Amount credited in bank ₹") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (receivedValue != null && receivedValue < day.total) {
                        Text(
                            "Charge: ${rupees(day.total - receivedValue)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.appColors.danger
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = receivedValue != null && receivedValue >= 0 && receivedValue <= day.total,
                    onClick = {
                        viewModel.saveSettlementForDate(day.date, day.total, receivedValue ?: day.total, true)
                        editDay = null
                    }
                ) { Text("Mark received") }
            },
            dismissButton = { TextButton(onClick = { editDay = null }) { Text("Cancel") } }
        )
    }

    // ALL PENDING DAYS IN ONE GO
    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("Mark all received?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "${pending.size} days, total ${rupees(pendingTotal)} will be marked as received in bank with no charge.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.settleDates(pending.associate { it.date to it.total })
                    confirmAll = false
                }) { Text("Yes, mark all") }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Cancel") } }
        )
    }
}
