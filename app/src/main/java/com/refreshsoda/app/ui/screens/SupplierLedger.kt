package com.refreshsoda.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.data.model.SupplierPurchase
import com.refreshsoda.app.data.model.SupplierPurchaseItem
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import com.refreshsoda.app.ui.viewmodel.addSupplierPayment
import com.refreshsoda.app.ui.viewmodel.balance
import com.refreshsoda.app.ui.viewmodel.deleteInwardPurchase
import com.refreshsoda.app.ui.viewmodel.loadPurchaseItems
import com.refreshsoda.app.ui.viewmodel.recordSupplierReturn
import com.refreshsoda.app.ui.viewmodel.saveSupplier
import com.refreshsoda.app.util.BillImages
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.refreshsoda.app.ui.theme.appColors

private data class SupplierSummary(
    val key: String,
    val name: String,
    val totalPurchased: Double,
    val totalPaid: Double,
    val due: Double,
    val billCount: Int
)

// ---------------- STANDALONE MAAL AAVAK SCREEN ----------------
@Composable
fun MaalAavakScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        SupplierPurchasesView(viewModel = viewModel, context = context)
    }
}

// ---------------- SUBVIEW: SUPPLIER LEDGER, BILLS, PAYMENTS, RETURNS ----------------
@Composable
fun SupplierPurchasesView(viewModel: ShopViewModel, context: Context) {
    val isOwner by viewModel.isOwner.collectAsState()
    if (!isOwner) {
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                "Only the owner can view the supplier ledger.",
                modifier = Modifier.padding(16.dp),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val purchases by viewModel.supplierPurchases.collectAsState()
    val payments by viewModel.supplierPayments.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val totalDebt by viewModel.totalSupplierDebt.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var showInwardDialog by remember { mutableStateOf(false) }
    var payFor by remember { mutableStateOf<SupplierPurchase?>(null) }
    var returnFor by remember { mutableStateOf<SupplierPurchase?>(null) }
    var editFor by remember { mutableStateOf<SupplierPurchase?>(null) }
    var deleteFor by remember { mutableStateOf<SupplierPurchase?>(null) }
    var photoPath by remember { mutableStateOf<String?>(null) }
    var contactFor by remember { mutableStateOf<String?>(null) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var supplierFilter by remember { mutableStateOf<String?>(null) }

    val summaries = purchases
        .groupBy { it.brand.trim().lowercase(Locale.getDefault()) }
        .map { entry ->
            val list = entry.value
            SupplierSummary(
                key = entry.key,
                name = list.first().brand,
                totalPurchased = list.sumOf { it.totalAmount },
                totalPaid = list.sumOf { it.paidAmount },
                due = list.sumOf { it.balance() },
                billCount = list.size
            )
        }
        .sortedByDescending { it.due }

    val visiblePurchases = purchases.filter {
        supplierFilter == null || it.brand.trim().lowercase(Locale.getDefault()) == supplierFilter
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // TOP PRIMARY ACTION BUTTON: + Naya Maal Aavak (Bill Upload / Entry)
        item {
            Button(
                onClick = { showInwardDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(
                    text = "📦 + New Inward Stock (Bill Scan / Entry)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Total Supplier Outstanding", color = Color(0xFFFECDD3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${totalDebt.toInt()} due", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Amount still payable to suppliers.", color = MaterialTheme.appColors.dangerContainer, fontSize = 11.sp)
                }
            }
        }

        if (summaries.isNotEmpty()) {
            item {
                Text("Supplier ledger", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            items(summaries, key = { it.key }) { summary ->
                val master = suppliers.firstOrNull { it.id == summary.key }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (supplierFilter == summary.key) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.name, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                val contact = listOfNotNull(
                                    master?.phone?.takeIf { it.isNotBlank() },
                                    master?.gstNumber?.takeIf { it.isNotBlank() }?.let { "GST $it" }
                                ).joinToString(" • ")
                                if (contact.isNotBlank()) Text(contact, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                if (summary.due > 0.5) "₹${summary.due.toInt()} due" else "Clear ✅",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (summary.due > 0.5) MaterialTheme.appColors.danger else MaterialTheme.appColors.success
                            )
                        }
                        Text(
                            "Purchased ₹${summary.totalPurchased.toInt()} • Paid ₹${summary.totalPaid.toInt()} • ${summary.billCount} bills",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { supplierFilter = if (supplierFilter == summary.key) null else summary.key },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) { Text(if (supplierFilter == summary.key) "All bills" else "Bills dekho", fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = { contactFor = summary.name },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) { Text("Phone/GST", fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = {
                                    val bills = purchases.filter { it.brand.trim().lowercase(Locale.getDefault()) == summary.key && it.balance() > 0.5 }
                                    val message = buildString {
                                        appendLine("🙏 Hello ${summary.name},")
                                        appendLine("${profile.shopName.ifBlank { "Our shop" }} account statement:")
                                        appendLine("Total purchased ₹${summary.totalPurchased.toInt()}, paid ₹${summary.totalPaid.toInt()}, due ₹${summary.due.toInt()}")
                                        bills.forEach { appendLine("• Bill #${it.billNumber} (${it.billDate}): ₹${it.balance().toInt()} due") }
                                        append("Please reconcile. Thank you!")
                                    }
                                    sendWhatsAppMessage(context, master?.phone, message)
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) { Text("WhatsApp statement", fontSize = 10.sp) }
                        }
                    }
                }
            }
        }

        if (purchases.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No supplier invoices added yet.\nTap '+ New Inward Stock' to add a supplier bill.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            item { Text("Bills", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            items(visiblePurchases, key = { it.id }) { purchase ->
                val isPaid = purchase.paymentStatus == "PAID"
                val isUnpaid = purchase.paymentStatus == "UNPAID"
                val balance = purchase.balance()
                val trackBoxes = categories.firstOrNull {
                    it.name.equals(purchase.category, ignoreCase = true)
                }?.trackBoxes ?: false
                val history = payments.filter { it.purchaseId == purchase.id }
                val hasPhoto = !purchase.billImageUri.isNullOrBlank()

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(purchase.brand, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                        Text(
                                            purchase.category,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("Bill #${purchase.billNumber} • ${purchase.billDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPaid) MaterialTheme.appColors.success.copy(alpha = 0.15f) else MaterialTheme.appColors.danger.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    if (isPaid) "✅ Full Paid" else if (isUnpaid) "❌ Unpaid (Credit)" else "⚠️ Partial",
                                    color = if (isPaid) MaterialTheme.appColors.success else MaterialTheme.appColors.danger,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Text(
                            if (trackBoxes) purchase.itemsSummary else purchase.itemsSummary.replace(
                                Regex("\\s*\\(\\d+\\s+Boxes?\\)", RegexOption.IGNORE_CASE),
                                ""
                            ),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Column {
                            Text(
                                if (trackBoxes) "Bill total: ₹${purchase.totalAmount.toInt()} (${purchase.totalBoxes} Boxes)"
                                else "Bill total: ₹${purchase.totalAmount.toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Paid: ₹${purchase.paidAmount.toInt()}", fontSize = 11.sp, color = MaterialTheme.appColors.success)
                            if (!isPaid) {
                                Text("Due: ₹${balance.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.appColors.danger)
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!isPaid) item {
                                Button(
                                    onClick = { payFor = purchase },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.success, contentColor = MaterialTheme.appColors.onSuccess),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("Make payment", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            }
                            item {
                                OutlinedButton(onClick = { returnFor = purchase }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text("Return/Damage", fontSize = 10.sp)
                                }
                            }
                            item {
                                OutlinedButton(onClick = { editFor = purchase }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text("Edit", fontSize = 10.sp)
                                }
                            }
                            if (hasPhoto) item {
                                OutlinedButton(onClick = { photoPath = purchase.billImageUri }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text("📷 Bill photo", fontSize = 10.sp)
                                }
                            }
                            item {
                                OutlinedButton(
                                    onClick = { expandedId = if (expandedId == purchase.id) null else purchase.id },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("Payment history (${history.size})", fontSize = 10.sp) }
                            }
                            item {
                                TextButton(onClick = { deleteFor = purchase }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text("Delete", fontSize = 10.sp, color = MaterialTheme.appColors.danger)
                                }
                            }
                        }

                        if (expandedId == purchase.id) {
                            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            if (history.isEmpty()) {
                                Text("No payments recorded on this bill yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                history.forEach { payment ->
                                    Text(
                                        "• ${dateFormat.format(Date(payment.timestamp))}: ₹${payment.amount.toInt()} (${payment.paymentMode})" +
                                            if (payment.note.isNotBlank()) " – ${payment.note}" else "",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------- dialogs ----------------
    payFor?.let { purchase ->
        SupplierPaymentDialog(
            purchase = purchase,
            onDismiss = { payFor = null },
            onConfirm = { amount, mode, note ->
                viewModel.addSupplierPayment(purchase, amount, mode, note) { error ->
                    if (error == null) {
                        Toast.makeText(context, "Payment saved.", Toast.LENGTH_SHORT).show()
                        payFor = null
                    } else {
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    returnFor?.let { purchase ->
        SupplierReturnDialog(
            viewModel = viewModel,
            purchase = purchase,
            onDismiss = { returnFor = null },
            onConfirm = { variety, qty, credit, reason ->
                viewModel.recordSupplierReturn(purchase, variety, qty, credit, reason) { error ->
                    if (error == null) {
                        Toast.makeText(context, "Return saved. Stock and bill have been adjusted.", Toast.LENGTH_LONG).show()
                        returnFor = null
                    } else {
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    editFor?.let { purchase ->
        InwardBillDialog(viewModel = viewModel, onDismiss = { editFor = null }, editPurchase = purchase)
    }

    deleteFor?.let { purchase ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text("Delete bill?") },
            text = {
                Text(
                    "Bill #${purchase.billNumber} from ${purchase.brand} (₹${purchase.totalAmount.toInt()}) will be deleted. " +
                        "Its boxes will be removed from stock again, and its payments and returns will be removed too."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInwardPurchase(purchase) { error ->
                            if (error == null) {
                                Toast.makeText(context, "Bill deleted and stock adjusted.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            }
                        }
                        deleteFor = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.danger, contentColor = MaterialTheme.appColors.onDanger)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text("Cancel") } }
        )
    }

    photoPath?.let { path ->
        BillPhotoDialog(path = path, onDismiss = { photoPath = null })
    }

    contactFor?.let { name ->
        val existing = suppliers.firstOrNull { it.id == name.trim().lowercase(Locale.getDefault()) }
        SupplierContactDialog(
            name = name,
            initialPhone = existing?.phone.orEmpty(),
            initialGst = existing?.gstNumber.orEmpty(),
            initialAddress = existing?.address.orEmpty(),
            onDismiss = { contactFor = null },
            onSave = { phone, gst, address ->
                viewModel.saveSupplier(name, phone, gst, address)
                contactFor = null
            }
        )
    }

    if (showInwardDialog) {
        InwardBillDialog(viewModel = viewModel, onDismiss = { showInwardDialog = false })
    }
}

@Composable
private fun SupplierPaymentDialog(
    purchase: SupplierPurchase,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String) -> Unit
) {
    val balance = purchase.balance()
    var amountText by remember { mutableStateOf(balance.toInt().toString()) }
    var mode by remember { mutableStateOf("CASH") }
    var note by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull()
    val valid = amount != null && amount > 0.0 && amount <= balance + 0.5

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Payment to ${purchase.brand}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bill #${purchase.billNumber} • due ₹${balance.toInt()}", fontSize = 12.sp)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount paid ₹") },
                    singleLine = true,
                    isError = amountText.isNotBlank() && !valid,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CASH", "UPI", "BANK").forEach { option ->
                        FilterChip(
                            selected = mode == option,
                            onClick = { mode = option },
                            label = { Text(option, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                Text("Cash payments are deducted from the shop till (shown in Day Closing).", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (amount != null && valid) onConfirm(amount, mode, note) }, enabled = valid) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SupplierReturnDialog(
    viewModel: ShopViewModel,
    purchase: SupplierPurchase,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Double, String) -> Unit
) {
    var billItems by remember { mutableStateOf<List<SupplierPurchaseItem>>(emptyList()) }
    var selected by remember { mutableStateOf<SupplierPurchaseItem?>(null) }
    var qtyText by remember { mutableStateOf("1") }
    var creditText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    LaunchedEffect(purchase.id) {
        viewModel.loadPurchaseItems(purchase) { rows ->
            billItems = rows
            selected = rows.firstOrNull()
        }
    }
    val qty = qtyText.toIntOrNull() ?: 0
    val defaultCredit = (selected?.costPerBox ?: 0.0) * qty
    val credit = creditText.toDoubleOrNull() ?: defaultCredit
    val valid = selected != null && qty >= 1 && credit >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Return / Damage – ${purchase.brand}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (billItems.isEmpty()) {
                    Text("No items found on this bill.", fontSize = 12.sp)
                } else {
                    Text("Which item was returned?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(billItems) { item ->
                            FilterChip(
                                selected = selected?.id == item.id,
                                onClick = { selected = item },
                                label = { Text(item.varietyName, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text("Boxes / quantity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = creditText,
                    onValueChange = { creditText = it },
                    label = { Text("Supplier credit ₹ (default ₹${defaultCredit.toInt()})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (melted / damaged / wrong item)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Boxes will be removed from stock and the credit will be adjusted against this bill's balance.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                onClick = { selected?.let { onConfirm(it.varietyName, qty, credit, reason) } },
                enabled = valid
            ) { Text("Save return") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SupplierContactDialog(
    name: String,
    initialPhone: String,
    initialGst: String,
    initialAddress: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var phone by remember { mutableStateOf(initialPhone) }
    var gst by remember { mutableStateOf(initialGst) }
    var address by remember { mutableStateOf(initialAddress) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone (WhatsApp)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = gst, onValueChange = { gst = it }, label = { Text("GST number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(phone, gst, address) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun BillPhotoDialog(path: String, onDismiss: () -> Unit) {
    val bitmap = remember(path) { BillImages.loadBitmap(path) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        text = {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Bill photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                )
            } else {
                Text("Could not open the bill photo (the file may have been removed).")
            }
        }
    )
}
