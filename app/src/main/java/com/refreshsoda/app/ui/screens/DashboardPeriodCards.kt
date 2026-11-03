package com.refreshsoda.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.ui.theme.appColors
import com.refreshsoda.app.ui.viewmodel.DateRangeFilter
import com.refreshsoda.app.ui.viewmodel.PeriodSummary
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import com.refreshsoda.app.ui.viewmodel.endOfDayMillis
import com.refreshsoda.app.ui.viewmodel.startOfDayMillis
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun rs(value: Double): String = "₹${"%,.0f".format(Locale.US, value)}"

/** Compact business card for a period (this month, or the dates picked in the filter). */
@Composable
fun PeriodSummaryCard(
    title: String,
    subtitle: String,
    summary: PeriodSummary,
    hideProfit: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClear: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = accent),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = MaterialTheme.appColors.onHero, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                    Text(subtitle, color = MaterialTheme.appColors.onHeroMuted, fontSize = 11.sp, maxLines = 1)
                }
                if (onClear != null) {
                    IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear filter", tint = MaterialTheme.appColors.onHero, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(
                rs(summary.totalSales),
                color = MaterialTheme.appColors.onHero,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Total sales • ${summary.billCount} bills",
                color = MaterialTheme.appColors.onHeroMuted,
                fontSize = 11.sp
            )
            HorizontalDivider(color = MaterialTheme.appColors.onHero.copy(alpha = 0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Expenses", color = MaterialTheme.appColors.onHeroMuted, fontSize = 10.sp)
                    Text(rs(summary.expenses), color = MaterialTheme.appColors.heroNegative, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Net Profit", color = MaterialTheme.appColors.onHeroMuted, fontSize = 10.sp)
                    if (hideProfit) {
                        Text("₹ ***", color = MaterialTheme.appColors.onHero, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Text(
                            rs(summary.netProfit),
                            color = if (summary.netProfit >= 0) MaterialTheme.appColors.heroPositive else MaterialTheme.appColors.heroNegative,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Cash", color = MaterialTheme.appColors.onHeroMuted, fontSize = 10.sp)
                    Text(rs(summary.cashSales), color = MaterialTheme.appColors.onHero, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("Paytm / UPI", color = MaterialTheme.appColors.onHeroMuted, fontSize = 10.sp)
                    Text(rs(summary.upiSales), color = MaterialTheme.appColors.onHero, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Credit", color = MaterialTheme.appColors.onHeroMuted, fontSize = 10.sp)
                    Text(rs(summary.udharSales), color = MaterialTheme.appColors.heroCaution, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/** "This month" business card. */
@Composable
fun MonthSummaryCard(viewModel: ShopViewModel, hideProfit: Boolean) {
    val month by viewModel.monthSummary.collectAsState()
    val monthTitle = remember(viewModel.todayDateString) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    }
    PeriodSummaryCard(
        title = "This Month",
        subtitle = monthTitle,
        summary = month,
        hideProfit = hideProfit,
        accent = MaterialTheme.appColors.heroSlate
    )
}

/**
 * Date filter: quick presets (Yesterday, Last 7 days) or any custom range.
 * While a filter is active, the card below it and the category / top-seller sections show that period.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardDateFilter(viewModel: ShopViewModel, hideProfit: Boolean) {
    val filter by viewModel.dashboardFilter.collectAsState()
    val filtered by viewModel.filteredSummary.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    val labelFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    fun rangeLabel(start: Long, end: Long): String {
        val a = labelFormat.format(Date(start))
        val b = labelFormat.format(Date(end))
        return if (a == b) a else "$a – $b"
    }

    fun applyDays(daysBack: Int, spanDays: Int) {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        val endDay = startOfDayMillis(now - daysBack * dayMs)
        val startDay = startOfDayMillis(endDay - (spanDays - 1) * dayMs)
        viewModel.setDashboardFilter(DateRangeFilter(startDay, endOfDayMillis(endDay), rangeLabel(startDay, endOfDayMillis(endDay))))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            item {
                FilterChip(
                    selected = false,
                    onClick = { applyDays(1, 1) },
                    label = { Text("Yesterday", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                )
            }
            item {
                FilterChip(
                    selected = false,
                    onClick = { applyDays(0, 7) },
                    label = { Text("Last 7 days", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                )
            }
            item {
                FilterChip(
                    selected = filter != null,
                    onClick = { showPicker = true },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Pick dates", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                )
            }
        }

        val active = filter
        val summary = filtered
        if (active != null && summary != null) {
            PeriodSummaryCard(
                title = "Selected Dates",
                subtitle = active.label,
                summary = summary,
                hideProfit = hideProfit,
                accent = MaterialTheme.appColors.heroAmber,
                onClear = { viewModel.setDashboardFilter(null) }
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    enabled = pickerState.selectedStartDateMillis != null,
                    onClick = {
                        val startUtc = pickerState.selectedStartDateMillis
                        val endUtc = pickerState.selectedEndDateMillis ?: startUtc
                        if (startUtc != null && endUtc != null) {
                            val start = utcDayToLocalStart(startUtc)
                            val end = endOfDayMillis(utcDayToLocalStart(endUtc))
                            viewModel.setDashboardFilter(DateRangeFilter(start, end, rangeLabel(start, end)))
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

/** The picker returns midnight UTC of the chosen day; convert that calendar day to local midnight. */
internal fun utcDayToLocalStart(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    }.timeInMillis
}
