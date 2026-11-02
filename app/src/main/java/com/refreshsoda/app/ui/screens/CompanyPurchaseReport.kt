package com.refreshsoda.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.data.model.SupplierPurchase
import com.refreshsoda.app.data.model.SupplierPurchaseItem
import com.refreshsoda.app.ui.theme.appColors
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import com.refreshsoda.app.ui.viewmodel.balance
import com.refreshsoda.app.ui.viewmodel.parseLegacyItems
import java.util.Locale

private data class PurchaseStats(
    val purchases: List<SupplierPurchase>,
    val totalAmount: Double,
    val paid: Double,
    val due: Double,
    val boxes: Int,
    val salesValue: Double,
    val profit: Double
) {
    val marginPercent: Double get() = if (salesValue > 0) profit / salesValue * 100.0 else 0.0
}

private fun monthLabel(ym: String): String {
    val parts = ym.split("-")
    val year = parts.getOrNull(0) ?: return ym
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return ym
    val names = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "${names.getOrElse(month - 1) { "?" }} $year"
}

private fun shiftMonth(ym: String, delta: Int): String {
    val parts = ym.split("-")
    var year = parts.getOrNull(0)?.toIntOrNull() ?: return ym
    var month = parts.getOrNull(1)?.toIntOrNull() ?: return ym
    month += delta
    while (month < 1) { month += 12; year -= 1 }
    while (month > 12) { month -= 12; year += 1 }
    return "%04d-%02d".format(year, month)
}

private fun money(value: Double): String = "₹${"%,.0f".format(Locale.US, value)}"

/**
 * Company-wise monthly purchase report:
 * which company's stock arrived on which date, how much was bought this month,
 * and the estimated profit on it (MRP value of boxes minus bill cost).
 */
@Composable
fun CompanyPurchaseReportScreen(viewModel: ShopViewModel) {
    val isOwner by viewModel.isOwner.collectAsState()
    if (!isOwner) {
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                "Only the owner can view this report.",
                modifier = Modifier.padding(16.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val purchases by viewModel.supplierPurchases.collectAsState()
    val allItems by viewModel.dao.getAllPurchaseItems().collectAsState(initial = emptyList())
    val itemsByPurchase = remember(allItems) { allItems.groupBy { it.purchaseId } }

    var month by remember { mutableStateOf(viewModel.todayDateString.take(7)) }
    var selectedCompany by remember { mutableStateOf<String?>(null) }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val monthPurchases = remember(purchases, month) {
        purchases.filter { it.billDate.take(7) == month }.sortedByDescending { it.billDate }
    }

    fun itemsOf(purchase: SupplierPurchase): List<SupplierPurchaseItem> =
        itemsByPurchase[purchase.id]?.takeIf { it.isNotEmpty() } ?: parseLegacyItems(purchase)

    fun statsFor(list: List<SupplierPurchase>): PurchaseStats {
        var sales = 0.0
        var cost = 0.0
        list.forEach { purchase ->
            itemsOf(purchase).forEach { item ->
                sales += item.quantity * item.mrp
                cost += item.lineAmount
            }
        }
        return PurchaseStats(
            purchases = list,
            totalAmount = list.sumOf { it.totalAmount },
            paid = list.sumOf { it.paidAmount },
            due = list.sumOf { it.balance() },
            boxes = list.sumOf { it.totalBoxes },
            salesValue = sales,
            profit = sales - cost
        )
    }

    val companyNames = remember(monthPurchases) {
        monthPurchases.map { it.brand.trim() }.distinctBy { it.lowercase(Locale.getDefault()) }
    }
    val companyStats = remember(monthPurchases, itemsByPurchase) {
        companyNames.map { name ->
            name to statsFor(monthPurchases.filter { it.brand.trim().equals(name, ignoreCase = true) })
        }.sortedByDescending { it.second.totalAmount }
    }
    val visiblePurchases = monthPurchases.filter {
        selectedCompany == null || it.brand.trim().equals(selectedCompany, ignoreCase = true)
    }
    val shownStats = statsFor(visiblePurchases)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        item {
            Text("Company Purchase Report", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // MONTH SWITCHER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { month = shiftMonth(month, -1); selectedCompany = null }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                }
                Text(monthLabel(month), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                IconButton(onClick = { month = shiftMonth(month, 1); selectedCompany = null }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                }
            }
        }

        // COMPANY FILTER
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(
                        selected = selectedCompany == null,
                        onClick = { selectedCompany = null },
                        label = { Text("All companies", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                    )
                }
                items(companyNames) { name ->
                    FilterChip(
                        selected = selectedCompany.equals(name, ignoreCase = true),
                        onClick = { selectedCompany = name },
                        label = { Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                    )
                }
            }
        }

        // MONTH TOTAL CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.heroTeal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        (selectedCompany ?: "All companies") + " • " + monthLabel(month),
                        color = MaterialTheme.appColors.onHeroMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        money(shownStats.totalAmount),
                        color = MaterialTheme.appColors.onHero,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Total purchase • ${shownStats.purchases.size} bills • ${shownStats.boxes} boxes",
                        color = MaterialTheme.appColors.onHeroMuted,
                        fontSize = 12.sp
                    )
                    HorizontalDivider(color = MaterialTheme.appColors.onHero.copy(alpha = 0.2f))
                    ReportRow("Paid", money(shownStats.paid), MaterialTheme.appColors.onHero)
                    ReportRow("Due", money(shownStats.due), MaterialTheme.appColors.heroCaution)
                    ReportRow("Sales value (at MRP)", money(shownStats.salesValue), MaterialTheme.appColors.onHero)
                    ReportRow(
                        "Estimated profit",
                        "${money(shownStats.profit)} (${"%.1f".format(Locale.US, shownStats.marginPercent)}%)",
                        if (shownStats.profit >= 0) MaterialTheme.appColors.heroPositive else MaterialTheme.appColors.heroNegative
                    )
                }
            }
        }

        if (monthPurchases.isEmpty()) {
            item {
                Text(
                    "No inward bills recorded in ${monthLabel(month)}.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // PER-COMPANY SUMMARY (only when viewing all)
        if (selectedCompany == null && companyStats.size > 1) {
            item { Text("Company-wise summary", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            items(companyStats, key = { it.first.lowercase(Locale.getDefault()) }) { (name, stats) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().clickable { selectedCompany = name }
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(money(stats.totalAmount), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            "${stats.purchases.size} bills • ${stats.boxes} boxes",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Est. profit ${money(stats.profit)} (${"%.1f".format(Locale.US, stats.marginPercent)}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (stats.profit >= 0) MaterialTheme.appColors.success else MaterialTheme.appColors.danger
                        )
                    }
                }
            }
        }

        // BILL-WISE LIST WITH DATES
        if (visiblePurchases.isNotEmpty()) {
            item { Text("Bills (date-wise)", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
        items(visiblePurchases, key = { it.id }) { purchase ->
            val billItems = itemsOf(purchase)
            val billStats = statsFor(listOf(purchase))
            val expanded = expandedId == purchase.id
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().clickable { expandedId = if (expanded) null else purchase.id }
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(purchase.billDate, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                purchase.brand + if (purchase.billNumber.isNotBlank()) " • #${purchase.billNumber}" else "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        Text(money(purchase.totalAmount), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        "${purchase.totalBoxes} boxes • ${purchase.paymentStatus}" +
                            if (purchase.balance() > 0) " • Due ${money(purchase.balance())}" else "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Est. profit ${money(billStats.profit)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (billStats.profit >= 0) MaterialTheme.appColors.success else MaterialTheme.appColors.danger
                    )
                    if (expanded) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        billItems.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "${item.varietyName} × ${item.quantity}",
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(
                                    "Cost ${money(item.lineAmount)} → MRP ${money(item.quantity * item.mrp)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Estimated profit = (boxes × MRP) − bill cost. Actual profit depends on your real selling price.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.appColors.onHeroMuted, fontSize = 12.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
