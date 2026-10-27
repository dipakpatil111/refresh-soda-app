package com.refreshsoda.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.data.model.CustomerKhata
import com.refreshsoda.app.data.model.Product
import com.refreshsoda.app.data.model.ProductCategory
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import java.net.URLEncoder

// Color Palette for Refresh Soda & Ice-cream
val TealPrimary = Color(0xFF0F766E)
val TealDark = Color(0xFF115E59)
val TealLight = Color(0xFFCCFBF1)
val GoldAccent = Color(0xFFD97706)
val SoftBg = Color(0xFFF8FAFC)
val CardBg = Color.White
val AlertRed = Color(0xFFDC2626)
val SuccessGreen = Color(0xFF16A34A)
val SlateGray = Color(0xFF64748B)

fun sendWhatsAppMessage(context: Context, phone: String?, message: String) {
    try {
        val cleanPhone = phone?.filter { it.isDigit() } ?: ""
        val url = if (cleanPhone.isNotBlank()) {
            val formatted = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
            "https://api.whatsapp.com/send?phone=$formatted&text=${URLEncoder.encode(message, "UTF-8")}"
        } else {
            "https://api.whatsapp.com/send?text=${URLEncoder.encode(message, "UTF-8")}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

// ==============================================================================
// 1. HOME DASHBOARD SCREEN
// ==============================================================================
@Composable
fun DashboardScreen(
    viewModel: ShopViewModel,
    onNavigateToPOS: () -> Unit = {}
) {
    val totalSales by viewModel.totalSalesAmount.collectAsState()
    val cashSales by viewModel.cashSalesAmount.collectAsState()
    val upiSales by viewModel.upiSalesAmount.collectAsState()
    val udharSales by viewModel.udharSalesAmount.collectAsState()
    val categorySalesMap by viewModel.categorySales.collectAsState()
    val topItem by viewModel.topItem.collectAsState()
    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()

    var showAddStockDialog by remember { mutableStateOf(false) }
    var showOpenBoxDialog by remember { mutableStateOf(false) }

    val lowStockItems = products.filter { it.sealedBoxes in 1..2 }
    val outOfStockItems = products.filter { it.sealedBoxes == 0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // TOP HERO CARD: TODAY'S TOTAL SALES & PAYMENT BREAKDOWN
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            viewModel.getString("today_total_sales").uppercase(),
                            color = TealLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(viewModel.todayDateString, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${totalSales.toInt()}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(viewModel.getString("cash_galla"), color = TealLight, fontSize = 11.sp)
                            Text("₹${cashSales.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text(viewModel.getString("upi_paytm"), color = TealLight, fontSize = 11.sp)
                            Text("₹${upiSales.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text(viewModel.getString("udhar_khata"), color = TealLight, fontSize = 11.sp)
                            Text("₹${udharSales.toInt()}", color = Color(0xFFFDE68A), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }

        // QUICK ACTION BUTTONS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToPOS,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(viewModel.getString("quick_new_sale"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showAddStockDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    enabled = products.isNotEmpty()
                ) {
                    Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(viewModel.getString("quick_new_stock"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showOpenBoxDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    enabled = products.any { it.sealedBoxes > 0 }
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(viewModel.getString("quick_open_box"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // LOW STOCK RED WARNING BADGE
        if (lowStockItems.isNotEmpty() || outOfStockItems.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.getString("low_stock_warning"), color = AlertRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        lowStockItems.take(4).forEach { prod ->
                            Text("• ${prod.name} (${prod.category}): सिर्फ ${prod.sealedBoxes} Box बाकी!", color = Color(0xFF991B1B), fontSize = 12.sp)
                        }
                        outOfStockItems.take(2).forEach { prod ->
                            Text("• ${prod.name} (${prod.category}): ❌ खत्म (0 Box)!", color = Color(0xFF991B1B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // CATEGORY-WISE SALES BREAKDOWN (Dynamic from user categories)
        if (categorySalesMap.isNotEmpty()) {
            item {
                Text(viewModel.getString("category_sales"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categorySalesMap.entries.toList()) { entry ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.width(110.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(entry.key, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SlateGray, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("₹${entry.value.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TealDark)
                            }
                        }
                    }
                }
            }
        }

        // TODAY'S TOP ITEM CARD
        if (topItem.pieces > 0) {
            item {
                Text(viewModel.getString("top_items"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E293B))
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("⭐ सबसे ज़्यादा बिकने वाला:", fontSize = 11.sp, color = SlateGray)
                            Text(topItem.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${topItem.pieces} Pieces • ₹${topItem.revenue.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = SuccessGreen)
                    }
                }
            }
        }

        // GODOWN BOX STATUS
        if (products.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(viewModel.getString("godown_box_status"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val totalBoxes = products.sumOf { it.sealedBoxes }
                    Text("Total: $totalBoxes Box", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealDark)
                }
            }
            item {
                val brandList = products.map { if (it.brand.isNotBlank()) it.brand else it.category }.distinct()
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(brandList) { brandName ->
                        val brandBoxes = products.filter { (it.brand == brandName || (it.brand.isBlank() && it.category == brandName)) }.sumOf { it.sealedBoxes }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(110.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(brandName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateGray, maxLines = 1)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$brandBoxes Box", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = if (brandBoxes <= 2) AlertRed else TealDark)
                            }
                        }
                    }
                }
            }
        }

        // RECENT SALES LOG (Haal Hi Ki Bikri) WITH DELETE ACTION
        item {
            Text(viewModel.getString("recent_sales"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        if (sales.isEmpty()) {
            item {
                Text("अभी तक कोई बिक्री दर्ज नहीं हुई है।", color = SlateGray, fontSize = 13.sp)
            }
        } else {
            items(sales.take(6)) { sale ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sale.itemsSummary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                val modeColor = when (sale.paymentMode) {
                                    "CASH" -> SuccessGreen
                                    "UPI" -> Color(0xFF0284C7)
                                    else -> GoldAccent
                                }
                                Text(sale.paymentMode, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = modeColor)
                                if (sale.customerName != null) {
                                    Text("• ${sale.customerName}", fontSize = 10.sp, color = SlateGray)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("₹${sale.totalAmount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TealPrimary)
                            IconButton(
                                onClick = { viewModel.deleteSale(sale) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG: ADD NEW STOCK
    if (showAddStockDialog && products.isNotEmpty()) {
        var selectedProduct by remember { mutableStateOf(products.first()) }
        var boxCountText by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showAddStockDialog = false },
            title = { Text(viewModel.getString("quick_new_stock"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("प्रोडक्ट चुनें:", fontSize = 12.sp, color = SlateGray)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(products) { prod ->
                            FilterChip(
                                selected = selectedProduct.id == prod.id,
                                onClick = { selectedProduct = prod },
                                label = { Text("${prod.name} (${prod.category})", fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = boxCountText,
                        onValueChange = { boxCountText = it },
                        label = { Text("कुल पेटी / बॉक्स संख्या") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val count = boxCountText.toIntOrNull() ?: 0
                    if (count > 0) {
                        viewModel.addBoxes(selectedProduct.id, selectedProduct.name, count)
                        showAddStockDialog = false
                    }
                }) {
                    Text("जोड़ें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStockDialog = false }) { Text("रद्द") }
            }
        )
    }

    // DIALOG: OPEN BOX QUICK ACTION
    if (showOpenBoxDialog) {
        val availableProducts = products.filter { it.sealedBoxes > 0 }
        var selectedProduct by remember { mutableStateOf(availableProducts.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { showOpenBoxDialog = false },
            title = { Text(viewModel.getString("quick_open_box"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("काउंटर पर कौन सा बॉक्स खोला जा रहा है?", fontSize = 12.sp, color = SlateGray)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableProducts) { prod ->
                            FilterChip(
                                selected = selectedProduct?.id == prod.id,
                                onClick = { selectedProduct = prod },
                                label = { Text("${prod.name} (${prod.sealedBoxes} Left)", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (selectedProduct != null) {
                        viewModel.openBox(selectedProduct!!.id, selectedProduct!!.name)
                        showOpenBoxDialog = false
                    }
                }) {
                    Text("-1 खोलें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenBoxDialog = false }) { Text("रद्द") }
            }
        )
    }
}

// ==============================================================================
// 2. POS SALES ENTRY SCREEN (DYNAMIC USER CATEGORIES & SUB-ITEMS)
// ==============================================================================
@Composable
fun POSScreen(viewModel: ShopViewModel) {
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val khataList by viewModel.khataList.collectAsState()
    val discount by viewModel.discount.collectAsState()

    var selectedCategoryName by remember { mutableStateOf("All") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showUdharDialog by remember { mutableStateOf(false) }
    var udharCustomerName by remember { mutableStateOf("") }
    var showEditItemDialog by remember { mutableStateOf<Int?>(null) }

    val rawTotal = cart.sumOf { it.total }
    val finalTotal = (rawTotal - discount).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBg)
            .padding(12.dp)
    ) {
        // TOP LIVE COUNTER ORDER / CART CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(viewModel.getString("live_counter_order"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateGray, letterSpacing = 1.sp)
                    if (cart.isNotEmpty()) {
                        Text(
                            viewModel.getString("clear_cart"),
                            fontSize = 12.sp,
                            color = AlertRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearCart() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (cart.isEmpty()) {
                    Text(viewModel.getString("cart_empty_hint"), fontSize = 13.sp, color = SlateGray)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        cart.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showEditItemDialog = index },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val addOnStr = if (item.addOns.isNotEmpty()) " (+${item.addOns.joinToString(",")})" else ""
                                    Text("${item.product.name}$addOnStr", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("₹${item.customPrice.toInt()} x ${item.quantity}", fontSize = 11.sp, color = SlateGray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { viewModel.updateCartItemQuantity(index, item.quantity - 1) }, modifier = Modifier.size(24.dp)) {
                                        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    IconButton(onClick = { viewModel.updateCartItemQuantity(index, item.quantity + 1) }, modifier = Modifier.size(24.dp)) {
                                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("₹${item.total.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TealDark)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(8.dp))

                // TOTAL & QUICK PAYMENT BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (discount > 0) {
                            Text("Discount: -₹${discount.toInt()}", fontSize = 10.sp, color = AlertRed, fontWeight = FontWeight.Bold)
                        }
                        Text("Total: ₹${finalTotal.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.completeOrder("CASH") },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            enabled = cart.isNotEmpty()
                        ) {
                            Text(viewModel.getString("btn_cash"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.completeOrder("UPI") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            enabled = cart.isNotEmpty()
                        ) {
                            Text(viewModel.getString("btn_upi"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showUdharDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            enabled = cart.isNotEmpty()
                        ) {
                            Text(viewModel.getString("btn_udhar"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // QUICK ADD-ONS BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(viewModel.getString("add_ons"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateGray)
            val addOns = listOf("🍋 +नींबू (₹5)" to 5.0, "🧂 +मसाला (₹5)" to 5.0, "❄️ +बर्फ (₹5)" to 5.0)
            addOns.forEach { (label, cost) ->
                SuggestionChip(
                    onClick = {
                        if (cart.isNotEmpty()) {
                            viewModel.addAddOn(cart.lastIndex, label.replace(Regex("[^a-zA-Z0-9\u0900-\u097F]"), ""), cost)
                        }
                    },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // DYNAMIC USER CATEGORIES BAR (+ ADD CATEGORY & + ADD ITEM BUTTONS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryName == "All",
                        onClick = { selectedCategoryName = "All" },
                        label = { Text("All", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryName == cat.name,
                        onClick = { selectedCategoryName = cat.name },
                        label = { Text(cat.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { showAddCategoryDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealLight),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(viewModel.getString("add_category_btn"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TealDark)
                }

                Button(
                    onClick = { showAddProductDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(viewModel.getString("add_item_btn"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // EMPTY STATE: IF NO CATEGORIES OR NO PRODUCTS
        if (categories.isEmpty() && products.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(44.dp))
                    Text(
                        viewModel.getString("no_categories_hint"),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(
                        onClick = { showAddCategoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ नई कैटेगरी बनाएं")
                    }
                }
            }
        } else {
            // MULTI-TAP PRODUCTS GRID
            val filtered = if (selectedCategoryName == "All") products else products.filter { it.category.equals(selectedCategoryName, ignoreCase = true) }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(viewModel.getString("no_items_in_category"), fontSize = 13.sp, color = SlateGray)
                        Button(
                            onClick = { showAddProductDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Text("+ इस कैटेगरी में आइटम जोड़ें")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered) { product ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(98.dp)
                                .clickable { viewModel.addToCart(product) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(if (product.brand.isNotBlank()) product.brand.uppercase() else product.category.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SlateGray, maxLines = 1)
                                Text(product.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, textAlign = TextAlign.Center)
                                Text("₹${product.price.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TealDark)
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG: ADD NEW USER CATEGORY
    if (showAddCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        val sampleCats = listOf("Soda", "Ice-cream", "Milkshake", "Cold Drink", "Snacks")

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("+ नई कैटेगरी जोड़ें", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("उदा. Soda, Ice-cream, Milkshake आदि", fontSize = 12.sp, color = SlateGray)
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("कैटेगरी का नाम (Category Name)") },
                        singleLine = true
                    )
                    Text("जल्दी चुनने के लिए सुझाव:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(sampleCats) { sample ->
                            SuggestionChip(
                                onClick = { catName = sample },
                                label = { Text(sample, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (catName.isNotBlank()) {
                        viewModel.addCategory(catName)
                        selectedCategoryName = catName.trim()
                        showAddCategoryDialog = false
                    }
                }) {
                    Text("कैटेगरी बनाएं")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("रद्द") }
            }
        )
    }

    // DIALOG: ADD NEW PRODUCT UNDER CATEGORY
    if (showAddProductDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(if (selectedCategoryName != "All") selectedCategoryName else (categories.firstOrNull()?.name ?: "Soda")) }
        var brand by remember { mutableStateOf("") }
        var priceText by remember { mutableStateOf("") }
        var boxText by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("+ नया आइटम जोड़ें", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("कैटेगरी चुनें:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (categories.isEmpty()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("कैटेगरी का नाम (उदा. Soda)") },
                            singleLine = true
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = category == cat.name,
                                    onClick = { category = cat.name },
                                    label = { Text(cat.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("आइटम का नाम (उदा. Jeera Soda, Limbu Soda)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("बिक्री रेट (₹ Price)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("ब्रांड (उदा. Hajoori, Vadilal, Own Shop)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = boxText,
                        onValueChange = { boxText = it },
                        label = { Text("गोदाम में सीलबंद पेटी / बॉक्स संख्या") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    val b = boxText.toIntOrNull() ?: 0
                    if (name.isNotBlank() && p > 0) {
                        viewModel.addProduct(name, category, brand, p, b)
                        showAddProductDialog = false
                    }
                }) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("रद्द") }
            }
        )
    }

    // DIALOG: UDHAR CUSTOMER SEARCH OR NEW SELECTION
    if (showUdharDialog) {
        AlertDialog(
            onDismissRequest = { showUdharDialog = false },
            title = { Text("उधार पर माल देना (Khata)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ग्राहक चुनें या नया नाम डालें:", fontSize = 12.sp, color = SlateGray)
                    OutlinedTextField(
                        value = udharCustomerName,
                        onValueChange = { udharCustomerName = it },
                        label = { Text("दुकानदार / ग्राहक का नाम") },
                        singleLine = true
                    )
                    if (khataList.isNotEmpty()) {
                        Text("हाल के खातेदार:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(khataList) { cust ->
                                FilterChip(
                                    selected = udharCustomerName.equals(cust.name, ignoreCase = true),
                                    onClick = { udharCustomerName = cust.name },
                                    label = { Text("${cust.name} (₹${cust.outstandingDebt.toInt()})", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (udharCustomerName.isNotBlank()) {
                            viewModel.completeOrder("UDHAR", udharCustomerName)
                            showUdharDialog = false
                            udharCustomerName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) {
                    Text("उधार सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUdharDialog = false }) { Text("रद्द") }
            }
        )
    }

    // DIALOG: EDIT CART ITEM
    if (showEditItemDialog != null) {
        val idx = showEditItemDialog!!
        val item = cart.getOrNull(idx)
        if (item != null) {
            var customRateText by remember { mutableStateOf(item.customPrice.toString()) }
            var qtyText by remember { mutableStateOf(item.quantity.toString()) }

            AlertDialog(
                onDismissRequest = { showEditItemDialog = null },
                title = { Text(item.product.name, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = qtyText, onValueChange = { qtyText = it }, label = { Text("मात्रा (Quantity)") }, singleLine = true)
                        OutlinedTextField(value = customRateText, onValueChange = { customRateText = it }, label = { Text("रेट (₹ Rate)") }, singleLine = true)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val q = qtyText.toIntOrNull() ?: item.quantity
                        val p = customRateText.toDoubleOrNull() ?: item.customPrice
                        viewModel.updateCartItemQuantity(idx, q)
                        viewModel.updateCartItemPrice(idx, p)
                        showEditItemDialog = null
                    }) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.updateCartItemQuantity(idx, 0)
                        showEditItemDialog = null
                    }) {
                        Text("हटाएं", color = AlertRed)
                    }
                }
            )
        }
    }
}

// ==============================================================================
// 3. GODOWN BOX-ONLY STOCK TRACKER SCREEN
// ==============================================================================
@Composable
fun InventoryScreen(viewModel: ShopViewModel) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var showAddStockDialog by remember { mutableStateOf(false) }

    val filterOptions = listOf("All") + categories.map { it.name }
    val filtered = if (selectedFilter == "All") products else products.filter { it.category.equals(selectedFilter, ignoreCase = true) }
    val totalBoxes = products.sumOf { it.sealedBoxes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP SUMMARY CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📦 गोदाम सीलबंद पेटी (Box-Only Stock)", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalBoxes Sealed Boxes", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("काउंटर पर पेटी खोलते वक्त सिर्फ '-1 खोलें' दबाएं।", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                }
            }
        }

        // FILTER TABS & ADD STOCK BUTTON
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    items(filterOptions) { opt ->
                        FilterChip(
                            selected = selectedFilter == opt,
                            onClick = { selectedFilter = opt },
                            label = { Text(opt, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
                Button(
                    onClick = { showAddStockDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    enabled = products.isNotEmpty()
                ) {
                    Text("+ नया माल", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text("कोई प्रोडक्ट स्टॉक में नहीं है। पहले POS या मेन्यू से आइटम जोड़ें।", color = SlateGray, fontSize = 13.sp)
            }
        } else {
            items(filtered) { item ->
                val isLowStock = item.sealedBoxes <= 2
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.category.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SlateGray)
                            Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isLowStock) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    "${item.sealedBoxes} Sealed Boxes in Godown",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLowStock) AlertRed else TealDark
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { viewModel.addBoxes(item.id, item.name, 1) },
                                colors = ButtonDefaults.buttonColors(containerColor = TealLight),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+1 Box", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TealDark)
                            }

                            Button(
                                onClick = { viewModel.openBox(item.id, item.name) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                enabled = item.sealedBoxes > 0
                            ) {
                                Text("-1 खोलें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStockDialog && products.isNotEmpty()) {
        var selectedProduct by remember { mutableStateOf(products.first()) }
        var countText by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showAddStockDialog = false },
            title = { Text("+ नया बॉक्स स्टॉक जोड़ें", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("प्रोडक्ट चुनें:", fontSize = 12.sp, color = SlateGray)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(products) { prod ->
                            FilterChip(
                                selected = selectedProduct.id == prod.id,
                                onClick = { selectedProduct = prod },
                                label = { Text("${prod.name} (${prod.category})", fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it },
                        label = { Text("कुल पेटी संख्या") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val c = countText.toIntOrNull() ?: 0
                    if (c > 0) {
                        viewModel.addBoxes(selectedProduct.id, selectedProduct.name, c)
                        showAddStockDialog = false
                    }
                }) {
                    Text("जोड़ें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStockDialog = false }) { Text("रद्द") }
            }
        )
    }
}

// ==============================================================================
// 4. CUSTOMER KHATA (UDHAAR) & RECOVERY SCREEN
// ==============================================================================
@Composable
fun KhataScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val khataList by viewModel.khataList.collectAsState()
    val totalOutstanding by viewModel.totalKhataOutstanding.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var repayCustomer by remember { mutableStateOf<CustomerKhata?>(null) }
    var repayAmountText by remember { mutableStateOf("") }
    var repayMode by remember { mutableStateOf("CASH") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    val filteredCustomers = if (searchQuery.isBlank()) khataList else khataList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP TOTAL OUTSTANDING CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(viewModel.getString("total_market_outstanding"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFEF3C7), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${totalOutstanding.toInt()}", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("दुकानदारों और ग्राहकों का कुल बाकी उधार।", color = Color(0xFFFFFBEB), fontSize = 12.sp)
                }
            }
        }

        // SEARCH BAR & ADD NEW CUSTOMER BUTTON
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(viewModel.getString("search_customer")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = { showAddCustomerDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(viewModel.getString("add_customer"))
                }
            }
        }

        // CUSTOMERS LIST
        items(filteredCustomers) { customer ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (customer.phone.isNotBlank()) {
                            Text("📱 ${customer.phone}", fontSize = 11.sp, color = SlateGray)
                        }
                        Text("बाकी: ₹${customer.outstandingDebt.toInt()}", color = AlertRed, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val msg = "नमस्ते ${customer.name} जी,\nRefresh Soda & Ice-cream par aapka kul ₹${customer.outstandingDebt.toInt()} baaki hai. Kripya samay par jama karwayein.\nDhanyawad!"
                                sendWhatsAppMessage(context, customer.phone, msg)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "WhatsApp Takada", tint = SuccessGreen)
                        }

                        Button(
                            onClick = {
                                repayCustomer = customer
                                repayAmountText = customer.outstandingDebt.toInt().toString()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(viewModel.getString("record_repayment"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (repayCustomer != null) {
        val cust = repayCustomer!!
        AlertDialog(
            onDismissRequest = { repayCustomer = null },
            title = { Text("उधार वसूली: ${cust.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("कुल बाकी: ₹${cust.outstandingDebt.toInt()}", color = AlertRed, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = repayAmountText,
                        onValueChange = { repayAmountText = it },
                        label = { Text("जमा रकम (₹)") },
                        singleLine = true
                    )
                    Text("पेमेंट कहाँ आया?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = repayMode == "CASH",
                            onClick = { repayMode = "CASH" },
                            label = { Text(viewModel.getString("cash_galla")) }
                        )
                        FilterChip(
                            selected = repayMode == "UPI",
                            onClick = { repayMode = "UPI" },
                            label = { Text(viewModel.getString("upi_paytm")) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = repayAmountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        viewModel.recordKhataRepayment(cust.id, cust.name, amt, repayMode)
                        repayCustomer = null
                        repayAmountText = ""
                    }
                }) {
                    Text("जमा दर्ज करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { repayCustomer = null }) { Text("रद्द") }
            }
        )
    }

    if (showAddCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var limitText by remember { mutableStateOf("2000") }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("+ नया खाता ग्राहक जोड़ें", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ग्राहक का नाम") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("मोबाइल नंबर (WhatsApp)") }, singleLine = true)
                    OutlinedTextField(value = limitText, onValueChange = { limitText = it }, label = { Text("उधार लिमिट (₹)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 2000.0
                    if (name.isNotBlank()) {
                        viewModel.addNewCustomer(name, phone, limit)
                        showAddCustomerDialog = false
                    }
                }) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) { Text("रद्द") }
            }
        )
    }
}

// ==============================================================================
// 5. HISAAB, KHARCHA, CLOSING & PAYTM SETTLEMENT SCREEN
// ==============================================================================
@Composable
fun HisaabScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf(0) }

    val subTabs = listOf(
        viewModel.getString("day_closing_title"),
        viewModel.getString("daily_expenses"),
        viewModel.getString("paytm_settlement_title"),
        "कैटेगरी & मेन्यू सेटअप"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBg)
            .padding(14.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(subTabs.size) { index ->
                FilterChip(
                    selected = activeSubTab == index,
                    onClick = { activeSubTab = index },
                    label = { Text(subTabs[index], fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (activeSubTab) {
            0 -> DayEndClosingView(viewModel, context)
            1 -> DailyExpensesView(viewModel)
            2 -> PaytmSettlementView(viewModel)
            3 -> MenuAndCategoryManagementView(viewModel)
        }
    }
}

// ---------------- SUBVIEW 1: DAY-END CLOSING ----------------
@Composable
fun DayEndClosingView(viewModel: ShopViewModel, context: Context) {
    val openingCash by viewModel.openingCash.collectAsState()
    val cashSales by viewModel.cashSalesAmount.collectAsState()
    val udharCashRepay by viewModel.udharCashRepaymentTotal.collectAsState()
    val cashExpenses by viewModel.cashExpensesTotal.collectAsState()
    val expectedCash by viewModel.expectedCash.collectAsState()

    var actualCashText by remember { mutableStateOf("") }
    val actualCashVal = actualCashText.toDoubleOrNull() ?: expectedCash
    val difference = actualCashVal - expectedCash

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(viewModel.getString("day_closing_title"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.getString("opening_cash"), fontSize = 12.sp, color = SlateGray)
                        Text("₹${openingCash.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• (+) Cash बिक्री:", fontSize = 12.sp, color = SuccessGreen)
                        Text("+ ₹${cashSales.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• (+) उधार की Cash वसूली:", fontSize = 12.sp, color = SuccessGreen)
                        Text("+ ₹${udharCashRepay.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessGreen)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• (-) गल्ले से दिया गया खर्चा:", fontSize = 12.sp, color = AlertRed)
                        Text("- ₹${cashExpenses.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AlertRed)
                    }

                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.getString("expected_cash"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("₹${expectedCash.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TealDark)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(viewModel.getString("actual_cash_input"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = actualCashText,
                        onValueChange = { actualCashText = it },
                        label = { Text("गिनकर असली कैश रकम (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    val diffColor = if (difference == 0.0) SuccessGreen else if (difference < 0) AlertRed else Color(0xFF0284C7)
                    val diffText = if (difference == 0.0) {
                        "✅ गल्ला बिल्कुल सही है! (Exact Match)"
                    } else if (difference < 0) {
                        "❌ ₹${(-difference).toInt()} कम है (Short!)"
                    } else {
                        "➕ ₹${difference.toInt()} ज़्यादा है (Surplus)"
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = diffColor.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(diffText, color = diffColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.saveDayClosing(actualCashVal)
                                Toast.makeText(context, "डे-एंड क्लोज़िंग सेव हो गई!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(viewModel.getString("save_closing"))
                        }

                        Button(
                            onClick = {
                                val reportText = viewModel.generateDailyReportWhatsAppText(actualCashVal)
                                sendWhatsAppMessage(context, null, reportText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(viewModel.getString("whatsapp_summary"))
                        }
                    }
                }
            }
        }
    }
}

// ---------------- SUBVIEW 2: DAILY EXPENSES ----------------
@Composable
fun DailyExpensesView(viewModel: ShopViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("बर्फ (Ice)", "सोडे का माल", "दूध और शेक", "बिजली", "मज़दूरी", "किराया", "अन्य")

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(viewModel.getString("daily_expenses"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(viewModel.getString("add_expense"))
                }
            }
        }

        if (expenses.isEmpty()) {
            item {
                Text("आज कोई खर्चा दर्ज नहीं किया गया है।", color = SlateGray, fontSize = 13.sp)
            }
        } else {
            items(expenses) { exp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(exp.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${exp.category} • Mode: ${exp.paymentMode}", fontSize = 11.sp, color = SlateGray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹${exp.amount.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = AlertRed)
                            IconButton(onClick = { viewModel.deleteExpense(exp) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(categories.first()) }
        var amountText by remember { mutableStateOf("") }
        var paymentMode by remember { mutableStateOf("CASH") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("+ नया खर्चा दर्ज करें", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("खर्चे का विवरण (Title)") }, singleLine = true)
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("रकम (₹)") }, singleLine = true)

                    Text("कैटेगरी चुनें:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("पेमेंट कहाँ से दिया?", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paymentMode == "CASH",
                            onClick = { paymentMode = "CASH" },
                            label = { Text("💵 गल्ले से (Cash)") }
                        )
                        FilterChip(
                            selected = paymentMode == "UPI",
                            onClick = { paymentMode = "UPI" },
                            label = { Text("📱 UPI") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        viewModel.addExpense(title, category, amt, paymentMode)
                        showAddDialog = false
                    }
                }) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("रद्द") }
            }
        )
    }
}

// ---------------- SUBVIEW 3: PAYTM MERCHANT SETTLEMENT ----------------
@Composable
fun PaytmSettlementView(viewModel: ShopViewModel) {
    val upiTotal by viewModel.totalUpiCollection.collectAsState()
    var chargeText by remember { mutableStateOf("0") }
    var isSettled by remember { mutableStateOf(false) }

    val charges = chargeText.toDoubleOrNull() ?: 0.0
    val netExpected = (upiTotal - charges).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(viewModel.getString("paytm_settlement_title"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("आज का कुल UPI कलेक्शन:", fontSize = 13.sp)
                        Text("₹${upiTotal.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0284C7))
                    }

                    OutlinedTextField(
                        value = chargeText,
                        onValueChange = { chargeText = it },
                        label = { Text("चार्ज कटौती (₹ हाथ से डालें)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.getString("net_bank_amount"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("₹${netExpected.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SuccessGreen)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isSettled, onCheckedChange = { isSettled = it })
                        Text(viewModel.getString("settled_in_bank"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { viewModel.savePaytmSettlement(charges, isSettled) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("सेटलमेंट स्टेटस अपडेट करें")
                    }
                }
            }
        }
    }
}

// ---------------- SUBVIEW 4: USER CATEGORY & MENU MANAGEMENT ----------------
@Composable
fun MenuAndCategoryManagementView(viewModel: ShopViewModel) {
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var selectedCatForProduct by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // CATEGORY SECTION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("आपकी बनाई गई कैटेगरी (Categories)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Button(
                    onClick = { showAddCategoryDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ नई कैटेगरी")
                }
            }
        }

        if (categories.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "अभी कोई कैटेगरी नहीं बनी है। '+ नई कैटेगरी' दबाकर Soda, Ice-cream, Milkshake आदि जोड़ें।",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                }
            }
        } else {
            items(categories) { cat ->
                val count = products.count { it.category.equals(cat.name, ignoreCase = true) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("$count आइटम्स जुड़े हैं", fontSize = 11.sp, color = SlateGray)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = {
                                    selectedCatForProduct = cat.name
                                    showAddProductDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TealLight),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+ आइटम", fontSize = 11.sp, color = TealDark, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }

        // PRODUCTS SECTION
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("सभी आइटम्स (Items List)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Button(
                    onClick = {
                        selectedCatForProduct = categories.firstOrNull()?.name ?: "Soda"
                        showAddProductDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ नया आइटम")
                }
            }
        }

        if (products.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp)) {
                    Text("कोई आइटम नहीं है। ऊपर से '+ नया आइटम' जोड़ें।", modifier = Modifier.padding(14.dp), fontSize = 12.sp, color = SlateGray)
                }
            }
        } else {
            items(products) { prod ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("कैटेगरी: ${prod.category} ${if (prod.brand.isNotBlank()) "• ${prod.brand}" else ""} • ${prod.sealedBoxes} Box", fontSize = 10.sp, color = SlateGray)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("₹${prod.price.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TealDark)
                            IconButton(onClick = { viewModel.deleteProduct(prod) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }

        // AUDIT LOGS
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text("ऑडिट व बदलाव लॉग (Audit Logs)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        items(auditLogs.take(10)) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(log.action, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TealDark)
                    Text(log.details, fontSize = 12.sp)
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("+ नई कैटेगरी बनाएं", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = catName,
                    onValueChange = { catName = it },
                    label = { Text("कैटेगरी नाम (उदा. Soda, Ice-cream)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (catName.isNotBlank()) {
                        viewModel.addCategory(catName)
                        showAddCategoryDialog = false
                    }
                }) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("रद्द") }
            }
        )
    }

    if (showAddProductDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(selectedCatForProduct ?: (categories.firstOrNull()?.name ?: "Soda")) }
        var brand by remember { mutableStateOf("") }
        var priceText by remember { mutableStateOf("") }
        var boxText by remember { mutableStateOf("0") }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("+ नया आइटम जोड़ें", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("कैटेगरी चुनें:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (categories.isEmpty()) {
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("कैटेगरी का नाम") }, singleLine = true)
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { c ->
                                FilterChip(
                                    selected = category == c.name,
                                    onClick = { category = c.name },
                                    label = { Text(c.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("आइटम का नाम (उदा. Jeera Soda, Limbu Soda)") }, singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("रेट (₹)") }, singleLine = true)
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("ब्रांड (उदा. Hajoori, Vadilal, Own Shop)") }, singleLine = true)
                    OutlinedTextField(value = boxText, onValueChange = { boxText = it }, label = { Text("सीलबंद पेटी संख्या") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    val b = boxText.toIntOrNull() ?: 0
                    if (name.isNotBlank() && p > 0) {
                        viewModel.addProduct(name, category, brand, p, b)
                        showAddProductDialog = false
                    }
                }) {
                    Text("सेव करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("रद्द") }
            }
        )
    }
}
