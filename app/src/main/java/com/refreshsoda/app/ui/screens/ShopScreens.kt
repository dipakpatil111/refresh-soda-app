package com.refreshsoda.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.data.model.Product
import com.refreshsoda.app.ui.viewmodel.ShopViewModel

// Color Palette for Refresh Soda & Ice-cream
val TealPrimary = Color(0xFF0F766E)
val TealDark = Color(0xFF115E59)
val GoldAccent = Color(0xFFD97706)
val SoftBg = Color(0xFFF8FAFC)

// ---------------- SCREEN 1: DASHBOARD ----------------
@Composable
fun DashboardScreen(viewModel: ShopViewModel) {
    val totalSales by viewModel.totalSalesAmount.collectAsState()
    val cashSales by viewModel.cashSalesAmount.collectAsState()
    val upiSales by viewModel.upiSalesAmount.collectAsState()
    val udharSales by viewModel.udharSalesAmount.collectAsState()
    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SoftBg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Total Sales Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("AAJ KA KUL DHANDHA", color = Color(0xFFCCFBF1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("₹${totalSales.toInt()}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0x33FFFFFF))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Cash (Galla)", color = Color(0xFFCCFBF1), fontSize = 11.sp)
                            Text("₹${cashSales.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Paytm / UPI", color = Color(0xFFCCFBF1), fontSize = 11.sp)
                            Text("₹${upiSales.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Udhaar (Khata)", color = Color(0xFFCCFBF1), fontSize = 11.sp)
                            Text("₹${udharSales.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Godown Sealed Box Stock Summary
        item {
            Text("Godown Sealed Box Stock", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        item {
            val stocked = products.filter { it.sealedBoxes > 0 }
            if (stocked.isEmpty()) {
                Text("Koi box stock nahi hai.", color = Color.Gray, fontSize = 13.sp)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stocked.take(3).forEach { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item.brand, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${item.sealedBoxes} Box", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealDark)
                            }
                        }
                    }
                }
            }
        }

        // Recent Orders Log
        item {
            Text("Haal Hi Ki Bikri (Recent Sales)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        items(sales.take(8)) { sale ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sale.itemsSummary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Mode: ${sale.paymentMode}", fontSize = 10.sp, color = Color.Gray)
                    }
                    Text("₹${sale.totalAmount.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                }
            }
        }
    }
}

// ---------------- SCREEN 2: 1-TAP POS QUICK BILLING ----------------
@Composable
fun POSScreen(viewModel: ShopViewModel) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    var showUdharDialog by remember { mutableStateOf(false) }
    var udharCustomerName by remember { mutableStateOf("") }

    val categories = listOf("All", "Soda", "Ice-cream", "Milkshake", "Cold Drink")
    val totalBill = cart.sumOf { it.product.price * it.quantity }

    Column(modifier = Modifier.fillMaxSize().background(SoftBg).padding(12.dp)) {

        // Top Floating Cart Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LIVE COUNTER ORDER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    if (cart.isNotEmpty()) {
                        Text(
                            "Clear",
                            fontSize = 12.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearCart() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val cartSummary = if (cart.isEmpty()) "Neeche se items tap karein..." else cart.joinToString(", ") { "${it.product.name} (x${it.quantity})" }
                Text(cartSummary, fontSize = 13.sp, maxLines = 2, color = if (cart.isEmpty()) Color.Gray else Color.Black)

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total: ₹${totalBill.toInt()}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.completeOrder("CASH") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Cash", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.completeOrder("UPI") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("UPI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showUdharDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Udhaar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Tabs
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Multi-Tap Products Grid
        val filtered = if (selectedCategory == "All") products else products.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered) { product ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(96.dp).clickable { viewModel.addToCart(product) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(product.brand.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(product.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text("₹${product.price.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TealDark)
                    }
                }
            }
        }
    }

    if (showUdharDialog) {
        AlertDialog(
            onDismissRequest = { showUdharDialog = false },
            title = { Text("Udhaar Par Maal Dena", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = udharCustomerName,
                    onValueChange = { udharCustomerName = it },
                    label = { Text("Dukandaar / Grahak Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (udharCustomerName.isNotBlank()) {
                        viewModel.completeOrder("UDHAR", udharCustomerName)
                        showUdharDialog = false
                        udharCustomerName = ""
                    }
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUdharDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ---------------- SCREEN 3: GODOWN BOX INVENTORY ----------------
@Composable
fun InventoryScreen(viewModel: ShopViewModel) {
    val products by viewModel.products.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SoftBg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Sealed Box Godown Inventory", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Counter par peti kholte waqt sirf '-1 Box Kholo' dabayein.", fontSize = 12.sp, color = Color.Gray)
        }

        items(products) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.brand.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("${item.sealedBoxes} Sealed Boxes in Godown", fontSize = 11.sp, color = TealDark, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { viewModel.openBox(item.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("-1 Box Kholo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- SCREEN 4: UDHAAR KHATA ----------------
@Composable
fun KhataScreen(viewModel: ShopViewModel) {
    val khataList by viewModel.khataList.collectAsState()
    val totalOutstanding = khataList.sumOf { it.outstandingDebt }
    var repayCustomerId by remember { mutableStateOf<String?>(null) }
    var repayAmountText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SoftBg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GoldAccent),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("MARKET ME KUL BAAKI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFEF3C7))
                    Text("₹${totalOutstanding.toInt()}", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }

        item {
            Text("Dukandaar & Customer Khata List", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        items(khataList) { customer ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Baaki: ₹${customer.outstandingDebt.toInt()}", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { repayCustomerId = customer.id },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Jama Karein", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (repayCustomerId != null) {
        AlertDialog(
            onDismissRequest = { repayCustomerId = null },
            title = { Text("Paisa Jama Karein") },
            text = {
                OutlinedTextField(
                    value = repayAmountText,
                    onValueChange = { repayAmountText = it },
                    label = { Text("Rakam (₹)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val amt = repayAmountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        viewModel.recordKhataRepayment(repayCustomerId!!, amt)
                        repayCustomerId = null
                        repayAmountText = ""
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { repayCustomerId = null }) { Text("Cancel") }
            }
        )
    }
}

// ---------------- SCREEN 5: OWNER MENU SETUP ----------------
@Composable
fun MenuSetupScreen(viewModel: ShopViewModel) {
    val products by viewModel.products.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Soda") }
    var brand by remember { mutableStateOf("Hajoori") }
    var priceText by remember { mutableStateOf("") }
    var boxesText by remember { mutableStateOf("0") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SoftBg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dukan Menu & Rate Setup", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("+ Naya Item", fontSize = 12.sp)
                }
            }
        }

        items(products) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${item.brand} • ${item.category} • ${item.sealedBoxes} Boxes", fontSize = 11.sp, color = Color.Gray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("₹${item.price.toInt()}", fontWeight = FontWeight.Bold, color = TealDark, fontSize = 15.sp)
                        IconButton(onClick = { viewModel.deleteProduct(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Naya Item Jodein", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand (Vadilal, Hocco, Hajoori)") })
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Soda, Ice-cream, Milkshake)") })
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price (₹)") })
                    OutlinedTextField(value = boxesText, onValueChange = { boxesText = it }, label = { Text("Godown Boxes") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    val b = boxesText.toIntOrNull() ?: 0
                    if (name.isNotBlank() && p > 0) {
                        viewModel.addProduct(name, category, brand, p, b)
                        showAddDialog = false
                        name = ""
                        priceText = ""
                        boxesText = "0"
                    }
                }) {
                    Text("Save Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
