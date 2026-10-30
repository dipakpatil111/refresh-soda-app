package com.refreshsoda.app.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.data.alerts.AlertsEngine
import com.refreshsoda.app.data.backup.LocalBackup
import com.refreshsoda.app.data.model.CustomerKhata
import com.refreshsoda.app.data.model.IceCreamVarietyStock
import com.refreshsoda.app.data.model.Offer
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import com.refreshsoda.app.ui.viewmodel.UNASSIGNED_VARIETY
import com.refreshsoda.app.ui.viewmodel.bulkAdjustPrices
import com.refreshsoda.app.ui.viewmodel.deleteOffer
import com.refreshsoda.app.ui.viewmodel.recordWastage
import com.refreshsoda.app.ui.viewmodel.saveOffer
import com.refreshsoda.app.util.ShareUtils
import com.refreshsoda.app.util.UpiQr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ==============================================================================
// DYNAMIC UPI QR (amount is already inside the QR)
// ==============================================================================
@Composable
fun UpiQrDialog(
    upiId: String,
    shopName: String,
    amount: Double,
    onPaid: () -> Unit,
    onDismiss: () -> Unit
) {
    val bitmap = remember(upiId, amount) {
        try {
            UpiQr.bitmap(UpiQr.paymentUri(upiId, shopName, amount, "Bill payment"))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("₹${amount.toInt()} ka UPI QR", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (bitmap != null) {
                    Image(bitmap = bitmap.asImageBitmap(), contentDescription = "UPI QR", modifier = Modifier.size(260.dp))
                } else {
                    Text("QR nahi ban paya. UPI ID check karein.", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("UPI ID: $upiId", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Customer apne UPI app se scan kare. Paisa aane ke baad niche wala button dabayein.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onPaid, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))) {
                Text("Payment mil gaya ✅")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ==============================================================================
// OFFERS ON THE POS COUNTER
// ==============================================================================
@Composable
fun OfferChips(viewModel: ShopViewModel, rawTotal: Double, discount: Double) {
    val offers by viewModel.offers.collectAsState()
    val activeOffers = offers.filter { it.active }
    if (activeOffers.isEmpty() && discount <= 0.0) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        if (discount > 0.0) {
            item {
                AssistChip(
                    onClick = { viewModel.setDiscount(0.0) },
                    label = { Text("❌ Discount hatao", fontSize = 11.sp) }
                )
            }
        }
        items(activeOffers) { offer ->
            val amount = offer.discountFor(rawTotal)
            FilterChip(
                selected = discount > 0.0 && kotlin.math.abs(discount - amount) < 0.01,
                onClick = { if (amount > 0.0) viewModel.setDiscount(amount) },
                enabled = amount > 0.0,
                label = {
                    Text(
                        "🏷 ${offer.name}" + if (amount > 0.0) " (-₹${amount.toInt()})" else " (min ₹${offer.minBill.toInt()})",
                        fontSize = 11.sp
                    )
                }
            )
        }
    }
}

@Composable
fun ManageOffersDialog(viewModel: ShopViewModel, onDismiss: () -> Unit) {
    val offers by viewModel.offers.collectAsState()
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("PERCENT") }
    var valueText by remember { mutableStateOf("") }
    var minBillText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offers / Combo discount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (offers.isEmpty()) {
                    Text("Abhi koi offer nahi hai.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                offers.forEach { offer ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(offer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                (if (offer.discountType == "PERCENT") "${offer.value}% off" else "₹${offer.value.toInt()} off") +
                                    if (offer.minBill > 0) " • min bill ₹${offer.minBill.toInt()}" else "",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = offer.active, onCheckedChange = { viewModel.saveOffer(offer.copy(active = it)) })
                        TextButton(onClick = { viewModel.deleteOffer(offer) }) { Text("Delete", color = Color(0xFFDC2626), fontSize = 11.sp) }
                    }
                }
                HorizontalDivider()
                Text("Naya offer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Naam (jaise Diwali 10% off)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = type == "PERCENT", onClick = { type = "PERCENT" }, label = { Text("% off", fontSize = 11.sp) })
                    FilterChip(selected = type == "FLAT", onClick = { type = "FLAT" }, label = { Text("₹ flat", fontSize = 11.sp) })
                }
                OutlinedTextField(value = valueText, onValueChange = { valueText = it }, label = { Text(if (type == "PERCENT") "Kitna %" else "Kitne ₹") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = minBillText, onValueChange = { minBillText = it }, label = { Text("Minimum bill ₹ (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                val value = valueText.toDoubleOrNull()
                val valid = name.isNotBlank() && value != null && value > 0.0 && (type != "PERCENT" || value <= 100.0)
                Button(
                    onClick = {
                        if (value != null) {
                            viewModel.saveOffer(
                                Offer(name = name.trim(), discountType = type, value = value, minBill = minBillText.toDoubleOrNull() ?: 0.0)
                            )
                            name = ""; valueText = ""; minBillText = ""
                        }
                    },
                    enabled = valid
                ) { Text("Offer add karein") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

// ==============================================================================
// WASTAGE (melted / damaged boxes)
// ==============================================================================
@Composable
fun WastageDialog(viewModel: ShopViewModel, stock: IceCreamVarietyStock, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var qtyText by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }
    val qty = qtyText.toIntOrNull() ?: 0
    val valid = qty in 1..stock.sealedBoxes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wastage – ${stock.varietyName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${stock.companyName} • stock mein ${stock.sealedBoxes} box", fontSize = 12.sp)
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Kitne box kharab/melt hue") },
                    singleLine = true,
                    isError = qtyText.isNotBlank() && !valid,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Wajah (melted / expired / toota)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (stock.lastCostPrice > 0.0) {
                    Text(
                        "Nuksan ≈ ₹${(qty.coerceAtLeast(0) * stock.lastCostPrice).toInt()} (net profit mein ginta jayega)",
                        fontSize = 11.sp,
                        color = Color(0xFFDC2626)
                    )
                } else {
                    Text("Is variety ka cost bill se nahi mila, isliye nuksan ₹0 ginta jayega.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.recordWastage(stock, qty, reason) { error ->
                        if (error == null) {
                            Toast.makeText(context, "Wastage save ho gaya.", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) { Text("Save wastage") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Button + dialog in one, so the Inventory row only needs a single line. */
@Composable
fun WastageButton(viewModel: ShopViewModel, stock: IceCreamVarietyStock) {
    var show by remember { mutableStateOf(false) }
    Button(
        onClick = { show = true },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        enabled = stock.sealedBoxes > 0 && stock.varietyName != UNASSIGNED_VARIETY
    ) {
        Text("Wastage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
    }
    if (show) WastageDialog(viewModel = viewModel, stock = stock, onDismiss = { show = false })
}

// ==============================================================================
// DASHBOARD ALERTS (low stock, overdue udhaar, supplier dues)
// ==============================================================================
@Composable
fun AlertsCard(viewModel: ShopViewModel, includeLowStock: Boolean = true) {
    val varieties by viewModel.iceCreamVarietyStock.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val items by viewModel.purchaseItems.collectAsState()
    val khata by viewModel.khataList.collectAsState()
    val purchases by viewModel.supplierPurchases.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()

    val alerts = remember(varieties, sales, items, khata, purchases) {
        AlertsEngine.compute(varieties, sales, items, khata, purchases)
    }
    val showSupplier = isOwner && alerts.supplierDueBills > 0
    val lowStock = if (includeLowStock) alerts.lowStock else emptyList()
    if (lowStock.isEmpty() && alerts.overdueKhata.isEmpty() && !showSupplier) return

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("⚠️ Aaj ke alerts", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color(0xFF92400E))
            lowStock.take(5).forEach { alert ->
                val days = alert.daysLeft?.let { " • ~$it din ka stock" } ?: ""
                Text(
                    "📦 ${alert.companyName} ${alert.varietyName}: ${alert.boxes} box bache$days → mangwayein",
                    fontSize = 12.sp,
                    color = Color(0xFF78350F)
                )
            }
            if (lowStock.size > 5) {
                Text("… aur ${lowStock.size - 5} item ka stock kam hai", fontSize = 11.sp, color = Color(0xFF78350F))
            }
            alerts.overdueKhata.take(5).forEach { customer ->
                Text(
                    "📒 ${customer.name}: ₹${customer.outstandingDebt.toInt()} udhaar due date se upar",
                    fontSize = 12.sp,
                    color = Color(0xFF78350F)
                )
            }
            if (showSupplier) {
                Text(
                    "🏭 Supplier ka baaki ₹${alerts.supplierDueTotal.toInt()} (${alerts.supplierDueBills} bill)",
                    fontSize = 12.sp,
                    color = Color(0xFF78350F)
                )
            }
        }
    }
}

// ==============================================================================
// OWNER TOOLS: bulk price update, offers, local backup / restore, CSV export
// ==============================================================================
@Composable
fun OwnerToolsSection(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val categories by viewModel.categories.collectAsState()

    var showBulkPrice by remember { mutableStateOf(false) }
    var showOffers by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            busy = true
            scope.launch {
                val ok = withContext(Dispatchers.IO) { LocalBackup.exportDatabase(context, uri) }
                busy = false
                Toast.makeText(
                    context,
                    if (ok) "Backup file save ho gayi." else "Backup nahi ban paya.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> if (uri != null) pendingRestore = uri }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Owner tools", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)

            Text("Backup & data", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                        exportLauncher.launch("refresh_soda_backup_$stamp.db")
                    },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text("Backup file", fontSize = 11.sp) }
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text("Backup restore", fontSize = 11.sp) }
            }
            Text(
                "Roz subah automatic backup bhi bantaa hai (last 7 din, app ke backups folder mein).",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Excel / CSV export", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { exportCsv(context, viewModel, "sales") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) { Text("Sales", fontSize = 11.sp) }
                OutlinedButton(
                    onClick = { exportCsv(context, viewModel, "purchases") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) { Text("Maal aavak", fontSize = 11.sp) }
                OutlinedButton(
                    onClick = { exportCsv(context, viewModel, "khata") },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) { Text("Khata", fontSize = 11.sp) }
            }

            Text("Prices & offers", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showBulkPrice = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text("Sab prices badlo", fontSize = 11.sp) }
                OutlinedButton(
                    onClick = { showOffers = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) { Text("Offers / Combo", fontSize = 11.sp) }
            }
        }
    }

    if (showOffers) ManageOffersDialog(viewModel = viewModel, onDismiss = { showOffers = false })

    if (showBulkPrice) {
        var selectedCategory by remember { mutableStateOf<String?>(null) }
        var percentText by remember { mutableStateOf("") }
        val percent = percentText.toDoubleOrNull()
        val valid = percent != null && percent != 0.0 && percent >= -90.0 && percent <= 500.0
        AlertDialog(
            onDismissRequest = { showBulkPrice = false },
            title = { Text("Sab prices ek saath badlo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kis category ke?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(selected = selectedCategory == null, onClick = { selectedCategory = null }, label = { Text("Sab", fontSize = 11.sp) })
                        }
                        items(categories) { category ->
                            FilterChip(
                                selected = selectedCategory == category.name,
                                onClick = { selectedCategory = category.name },
                                label = { Text(category.name, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = percentText,
                        onValueChange = { percentText = it },
                        label = { Text("Kitna % (badhane ke liye 10, ghatane ke liye -10)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Naye prices nearest rupaye mein round honge.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (percent != null) {
                            viewModel.bulkAdjustPrices(selectedCategory, percent) { count ->
                                Toast.makeText(context, "$count items ka price badal gaya.", Toast.LENGTH_LONG).show()
                            }
                            showBulkPrice = false
                        }
                    },
                    enabled = valid
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showBulkPrice = false }) { Text("Cancel") } }
        )
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Backup restore karein?") },
            text = {
                Text(
                    "Abhi ka saara data is backup file se REPLACE ho jayega aur app restart hogi. " +
                        "Pehle 'Backup file' se abhi ka backup le lein."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestore = null
                        busy = true
                        scope.launch {
                            val error = withContext(Dispatchers.IO) { LocalBackup.importDatabase(context, uri) }
                            busy = false
                            if (error == null) {
                                Toast.makeText(context, "Restore ho gaya, app restart ho rahi hai...", Toast.LENGTH_LONG).show()
                                LocalBackup.restartApp(context)
                            } else {
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text("Haan, restore karo") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } }
        )
    }
}

private fun exportCsv(context: Context, viewModel: ShopViewModel, kind: String) {
    try {
        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = when (kind) {
            "sales" -> LocalBackup.writeCsv(
                context, "sales_$stamp.csv",
                listOf("Date & time", "Bill id", "Total", "Discount", "Payment", "Customer", "Sold by", "Items"),
                viewModel.sales.value.map { sale ->
                    listOf(
                        dateTime.format(Date(sale.timestamp)), sale.id.take(8), sale.totalAmount.toString(),
                        sale.discountAmount.toString(), sale.paymentMode, sale.customerName.orEmpty(), sale.soldBy, sale.itemsSummary
                    )
                }
            )
            "purchases" -> LocalBackup.writeCsv(
                context, "maal_aavak_$stamp.csv",
                listOf("Bill date", "Supplier", "Category", "Bill no", "Total", "Paid", "Baaki", "Status", "Items"),
                viewModel.supplierPurchases.value.map { p ->
                    listOf(
                        p.billDate, p.brand, p.category, p.billNumber, p.totalAmount.toString(), p.paidAmount.toString(),
                        (p.totalAmount - p.paidAmount).coerceAtLeast(0.0).toString(), p.paymentStatus, p.itemsSummary
                    )
                }
            )
            else -> LocalBackup.writeCsv(
                context, "khata_$stamp.csv",
                listOf("Name", "Phone", "Outstanding", "Limit", "Due date"),
                viewModel.khataList.value.map { c ->
                    listOf(
                        c.name, c.phone, c.outstandingDebt.toString(), c.debtLimit.toString(),
                        c.dueDateMillis?.let { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it)) }.orEmpty()
                    )
                }
            )
        }
        ShareUtils.shareFile(context, file, "text/csv", "CSV share karein")
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export nahi ho paya: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

// ==============================================================================
// KHATA DUE DATE
// ==============================================================================
fun isKhataOverdue(customer: CustomerKhata, now: Long = System.currentTimeMillis()): Boolean {
    val due = customer.dueDateMillis
    return due != null && customer.outstandingDebt > 0.0 && due < now
}

@Composable
fun KhataDueDateRow(viewModel: ShopViewModel, customer: CustomerKhata) {
    val context = LocalContext.current
    val due = customer.dueDateMillis
    val overdue = isKhataOverdue(customer)
    val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = {
                val cal = Calendar.getInstance()
                if (due != null) cal.timeInMillis = due
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val picked = Calendar.getInstance().apply {
                            set(year, month, day, 23, 59, 59)
                        }
                        viewModel.setKhataDueDate(customer.id, picked.timeInMillis)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
        ) {
            Text(
                if (due != null) "📅 Due: ${format.format(Date(due))}" + if (overdue) " (nikal gayi)" else ""
                else "📅 Due date set karein",
                fontSize = 11.sp,
                color = if (overdue) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                fontWeight = if (overdue) FontWeight.Bold else FontWeight.Normal
            )
        }
        if (due != null) {
            TextButton(
                onClick = { viewModel.setKhataDueDate(customer.id, null) },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) { Text("✕", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
