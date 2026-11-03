package com.refreshsoda.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.data.model.Expense
import com.refreshsoda.app.ui.theme.appColors
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import com.refreshsoda.app.ui.viewmodel.endOfDayMillis
import com.refreshsoda.app.ui.viewmodel.startOfDayMillis
import com.refreshsoda.app.ui.viewmodel.startOfMonthMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val EXPENSE_CATEGORIES = listOf(
    "❄️ Ice", "🍋 Lemons", "🥤 Syrup & Gas", "🥛 Milk & Shake", "🧻 Glasses & Straws",
    "💡 Electricity Bill", "🏪 Shop Rent", "👷 Labor / Salary", "🔧 Machine Repair", "📦 Other Miscellaneous"
)

private enum class ExpensePeriod { TODAY, MONTH, CUSTOM }

/** Shop Expenses: add expenses (cash / online), see today, this month or any date range. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: ShopViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    var period by remember { mutableStateOf(ExpensePeriod.TODAY) }
    var customStart by remember { mutableStateOf(0L) }
    var customEnd by remember { mutableStateOf(0L) }
    var showAdd by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }

    val now = System.currentTimeMillis()
    val dayFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    val (start, end) = when (period) {
        ExpensePeriod.TODAY -> startOfDayMillis(now) to Long.MAX_VALUE
        ExpensePeriod.MONTH -> startOfMonthMillis(now) to Long.MAX_VALUE
        ExpensePeriod.CUSTOM -> customStart to customEnd
    }
    val periodLabel = when (period) {
        ExpensePeriod.TODAY -> "Today"
        ExpensePeriod.MONTH -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(now))
        ExpensePeriod.CUSTOM -> {
            val a = dayFormat.format(Date(customStart)); val b = dayFormat.format(Date(customEnd))
            if (a == b) a else "$a – $b"
        }
    }
    val shown = expenses.filter { it.timestamp in start..end }.sortedByDescending { it.timestamp }
    val total = shown.sumOf { it.amount }
    val cashTotal = shown.filter { it.paymentMode == "CASH" }.sumOf { it.amount }
    val onlineTotal = total - cashTotal
    val byCategory = shown.groupBy { expenseCategoryLabel(it.category) }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Shop Expenses", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showAdd = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.danger, contentColor = MaterialTheme.appColors.onDanger),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("+ Add Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(selected = period == ExpensePeriod.TODAY, onClick = { period = ExpensePeriod.TODAY },
                        label = { Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) })
                }
                item {
                    FilterChip(selected = period == ExpensePeriod.MONTH, onClick = { period = ExpensePeriod.MONTH },
                        label = { Text("This month", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) })
                }
                item {
                    FilterChip(
                        selected = period == ExpensePeriod.CUSTOM,
                        onClick = { showPicker = true },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Pick dates", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dangerContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Total expenses • $periodLabel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.appColors.onDangerContainer)
                    Text("₹${"%,.0f".format(Locale.US, total)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.appColors.danger)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("💵 Cash ₹${cashTotal.toInt()}", fontSize = 12.sp, color = MaterialTheme.appColors.onDangerContainer)
                        Text("📱 Online ₹${onlineTotal.toInt()}", fontSize = 12.sp, color = MaterialTheme.appColors.onDangerContainer)
                        Text("${shown.size} entries", fontSize = 12.sp, color = MaterialTheme.appColors.onDangerContainer)
                    }
                }
            }
        }

        if (byCategory.isNotEmpty()) {
            item { Text("Category-wise", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(byCategory) { entry ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(10.dp)) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(entry.key, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Text("₹${entry.value.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.appColors.danger)
                            }
                        }
                    }
                }
            }
        }

        item { Text("Expenses ($periodLabel)", fontWeight = FontWeight.Bold, fontSize = 14.sp) }

        if (shown.isEmpty()) {
            item { Text("No expenses in this period.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
        }
        items(shown, key = { it.id }) { exp ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exp.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text(
                            "${expenseCategoryLabel(exp.category)} • ${if (exp.paymentMode == "CASH") "Cash" else "Online"}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                        )
                        Text(timeFormat.format(Date(exp.timestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("₹${exp.amount.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.appColors.danger)
                    IconButton(onClick = { pendingDelete = exp }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.appColors.danger)
                    }
                }
            }
        }
    }

    // ADD EXPENSE
    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(EXPENSE_CATEGORIES.first()) }
        var amountText by remember { mutableStateOf("") }
        var paymentMode by remember { mutableStateOf("CASH") }
        val amount = amountText.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("+ Add Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(EXPENSE_CATEGORIES) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = {
                                    category = cat
                                    if (title.isBlank()) title = cat.substringAfter(" ").trim()
                                },
                                label = { Text(cat, fontSize = 10.sp, maxLines = 1) }
                            )
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title / Note") }, singleLine = true)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Text("Paid from", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = paymentMode == "CASH", onClick = { paymentMode = "CASH" }, label = { Text("💵 Cash", maxLines = 1) })
                        FilterChip(selected = paymentMode == "UPI", onClick = { paymentMode = "UPI" }, label = { Text("📱 Online", maxLines = 1) })
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = title.isNotBlank() && amount > 0,
                    onClick = {
                        viewModel.addExpense(title, category, amount, paymentMode)
                        showAdd = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }

    // DELETE CONFIRM
    pendingDelete?.let { exp ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete expense?", fontWeight = FontWeight.Bold) },
            text = { Text("${exp.title} • ₹${exp.amount.toInt()}") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteExpense(exp); pendingDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.danger, contentColor = MaterialTheme.appColors.onDanger)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }

    // DATE RANGE
    if (showPicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedStartDateMillis != null,
                    onClick = {
                        val s = pickerState.selectedStartDateMillis
                        val e = pickerState.selectedEndDateMillis ?: s
                        if (s != null && e != null) {
                            customStart = utcDayToLocalStart(s)
                            customEnd = endOfDayMillis(utcDayToLocalStart(e))
                            period = ExpensePeriod.CUSTOM
                        }
                        showPicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.heightIn(max = 520.dp))
        }
    }
}
