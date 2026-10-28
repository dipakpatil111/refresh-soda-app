package com.refreshsoda.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.refreshsoda.app.data.model.DraftProductItem
import com.refreshsoda.app.data.model.Expense
import com.refreshsoda.app.data.model.Product
import com.refreshsoda.app.data.model.SaleTransaction
import com.refreshsoda.app.data.model.UserAccount
import com.refreshsoda.app.data.model.SupplierPurchase
import com.refreshsoda.app.data.model.InwardBillItemDraft
import com.refreshsoda.app.data.model.isIceCreamCategory
import com.refreshsoda.app.data.ocr.BillScannerService
import com.refreshsoda.app.ui.i18n.AppLanguage
import com.refreshsoda.app.ui.theme.AppThemeMode
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

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
// 1. SETTINGS SCREEN (User Profile, Language: Hindi/Eng/Marathi, Theme: Light/Dark/Sys)
// ==============================================================================
@Composable
fun SettingsScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentLang by viewModel.language.collectAsState()
    val currentTheme by viewModel.themeMode.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<UserAccount?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                viewModel.getString("settings_title"),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 1. ACTIVE USER & MULTI-USER MANAGEMENT CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = if (currentUser?.role == "OWNER") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (currentUser?.role == "OWNER") Icons.Default.AdminPanelSettings else Icons.Default.Badge,
                                        contentDescription = null,
                                        tint = if (currentUser?.role == "OWNER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Column {
                                Text(currentUser?.name ?: "Guest User", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (currentUser?.role == "OWNER") "👑 ${viewModel.getString("role_owner")}" else "👤 ${viewModel.getString("role_staff")}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (currentUser?.role == "OWNER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(viewModel.getString("logout_btn"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    // Registered Accounts List on this device
                    Text(
                        viewModel.getString("manage_users_title"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    allUsers.forEach { user ->
                        val isCurrent = user.id == currentUser?.id
                        val isOwner = user.role == "OWNER"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (isOwner) "👑" else "👤", fontSize = 16.sp)
                                    Column {
                                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${user.role} • ${user.emailOrPhone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!isCurrent) {
                                        TextButton(onClick = { viewModel.quickLogin(user) }) {
                                            Text(viewModel.getString("switch_user_btn"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (currentUser?.role == "OWNER") {
                                            IconButton(onClick = { userToDelete = user }) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    } else {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Active", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add Staff Member Button (Only Owner)
                    if (currentUser?.role == "OWNER") {
                        OutlinedButton(
                            onClick = { showAddStaffDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.getString("add_staff_member"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. USER / SHOP PROFILE CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Column {
                                Text(userProfile.shopName.ifBlank { "Refresh Soda & Ice-cream" }, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(userProfile.ownerName.ifBlank { "Owner Name" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (currentUser?.role == "OWNER") {
                            FilledTonalButton(
                                onClick = { showEditProfileDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(viewModel.getString("edit_profile_btn"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.getString("mobile_number") + ":", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(userProfile.phone.ifBlank { "Not set" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.getString("upi_id") + ":", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(userProfile.upiId.ifBlank { "Not set" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.getString("shop_address") + ":", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(userProfile.address.ifBlank { "Main Bazaar" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 3. LANGUAGE SELECTOR SECTION
        item {
            Text(
                viewModel.getString("language_selector_title"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.values().forEach { lang ->
                        val isSelected = currentLang == lang
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(lang)
                                    Toast.makeText(context, "Language: ${lang.displayName}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        when (lang) {
                                            AppLanguage.ENGLISH -> "🇬🇧"
                                            AppLanguage.HINDI -> "🇮🇳"
                                            AppLanguage.MARATHI -> "🇮🇳"
                                        },
                                        fontSize = 18.sp
                                    )
                                    Column {
                                        Text(lang.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(lang.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. THEME SELECTOR SECTION
        item {
            Text(
                viewModel.getString("theme_selector_title"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        Triple(AppThemeMode.SYSTEM, viewModel.getString("theme_system"), Icons.Default.BrightnessAuto),
                        Triple(AppThemeMode.LIGHT, viewModel.getString("theme_light"), Icons.Default.LightMode),
                        Triple(AppThemeMode.DARK, viewModel.getString("theme_dark"), Icons.Default.DarkMode)
                    )

                    themes.forEach { (mode, label, icon) ->
                        val isSelected = currentTheme == mode
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setThemeMode(mode) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. CLOUD BACKUP & RESTORE SECTION
        item {
            Text(
                "☁️ Cloud Backup & Data Recovery (क्लाउड बैकअप)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("दुकान का डेटा सुरक्षित रखें या नए फोन में तुरंत वापस लाएं:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Backing up to Firebase Cloud...", Toast.LENGTH_SHORT).show()
                                viewModel.backupToCloud { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("क्लाउड बैकअप", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                Toast.makeText(context, "Restoring data from Firebase Cloud...", Toast.LENGTH_SHORT).show()
                                viewModel.restoreFromCloud { success, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("डेटा वापस लाएं", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. APP INFO & DATA BACKUP STATUS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(viewModel.getString("app_info_title"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(viewModel.getString("offline_ready"), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text(viewModel.getString("cloud_sync_status"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Text("Refresh Soda App v1.3 • Master Shop Management", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showEditProfileDialog) {
        var sName by remember { mutableStateOf(userProfile.shopName) }
        var oName by remember { mutableStateOf(userProfile.ownerName) }
        var sPhone by remember { mutableStateOf(userProfile.phone) }
        var sUpi by remember { mutableStateOf(userProfile.upiId) }
        var sAddress by remember { mutableStateOf(userProfile.address) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text(viewModel.getString("edit_profile_btn"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text(viewModel.getString("shop_name")) }, singleLine = true)
                    OutlinedTextField(value = oName, onValueChange = { oName = it }, label = { Text(viewModel.getString("owner_name")) }, singleLine = true)
                    OutlinedTextField(value = sPhone, onValueChange = { sPhone = it }, label = { Text(viewModel.getString("mobile_number")) }, singleLine = true)
                    OutlinedTextField(value = sUpi, onValueChange = { sUpi = it }, label = { Text(viewModel.getString("upi_id")) }, singleLine = true)
                    OutlinedTextField(value = sAddress, onValueChange = { sAddress = it }, label = { Text(viewModel.getString("shop_address")) }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveUserProfile(sName, oName, sPhone, sUpi, sAddress)
                    showEditProfileDialog = false
                    Toast.makeText(context, "Profile Saved!", Toast.LENGTH_SHORT).show()
                }) {
                    Text(viewModel.getString("save_profile_btn"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddStaffDialog) {
        var staffName by remember { mutableStateOf("") }
        var staffIdentifier by remember { mutableStateOf("") }
        var staffPass by remember { mutableStateOf("") }
        var addStaffErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddStaffDialog = false },
            title = { Text(viewModel.getString("add_staff_member"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (addStaffErr != null) {
                        Text(addStaffErr!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = staffName,
                        onValueChange = { staffName = it; addStaffErr = null },
                        label = { Text(viewModel.getString("full_name_label")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = staffIdentifier,
                        onValueChange = { staffIdentifier = it; addStaffErr = null },
                        label = { Text(viewModel.getString("email_or_phone_label")) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = staffPass,
                        onValueChange = { staffPass = it; addStaffErr = null },
                        label = { Text(viewModel.getString("password_label")) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (staffName.isBlank() || staffIdentifier.isBlank() || staffPass.isBlank()) {
                        addStaffErr = viewModel.getString("err_fill_all")
                        return@Button
                    }
                    viewModel.register(
                        name = staffName,
                        identifier = staffIdentifier,
                        pass = staffPass,
                        role = "STAFF",
                        shopName = userProfile.shopName
                    ) { success, err ->
                        if (success) {
                            showAddStaffDialog = false
                            Toast.makeText(context, "Staff account created!", Toast.LENGTH_SHORT).show()
                        } else {
                            addStaffErr = err
                        }
                    }
                }) {
                    Text(viewModel.getString("save_btn"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStaffDialog = false }) { Text(viewModel.getString("cancel")) }
            }
        )
    }

    if (userToDelete != null) {
        val target = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User Account") },
            text = { Text("${viewModel.getString("delete_user_confirm")}\n\n• ${target.name} (${target.role})") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUserAccount(target)
                        userToDelete = null
                        Toast.makeText(context, "User deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(viewModel.getString("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) { Text(viewModel.getString("cancel")) }
            }
        )
    }
}

// ==============================================================================
// 2. HOME DASHBOARD SCREEN (SALES + EXPENSES + NET PROFIT + STOCK ALERTS)
// ==============================================================================
@Composable
fun DashboardScreen(
    viewModel: ShopViewModel,
    onNavigateToPOS: () -> Unit = {}
) {
    val context = LocalContext.current
    val totalSales by viewModel.totalSalesAmount.collectAsState()
    val cashSales by viewModel.cashSalesAmount.collectAsState()
    val upiSales by viewModel.upiSalesAmount.collectAsState()
    val udharSales by viewModel.udharSalesAmount.collectAsState()
    val totalExpenses by viewModel.totalExpensesAmount.collectAsState()
    val netProfit by viewModel.todayNetProfit.collectAsState()
    val categorySalesMap by viewModel.categorySales.collectAsState()
    val topItem by viewModel.topItem.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val topSodaIce by viewModel.topSodaAndIceCream.collectAsState()

    var showAddStockDialog by remember { mutableStateOf(false) }
    var showInwardDialog by remember { mutableStateOf(false) }
    var showOpenBoxDialog by remember { mutableStateOf(false) }
    var selectedSaleForReceipt by remember { mutableStateOf<SaleTransaction?>(null) }

    val boxTrackedProducts = products.filter { product ->
        categories.firstOrNull { it.name.equals(product.category, ignoreCase = true) }?.trackBoxes ?: true
    }
    val lowStockItems = boxTrackedProducts.filter { product ->
        product.sealedBoxes <= (product.lowStockLimit
            ?: categories.firstOrNull { it.name.equals(product.category, ignoreCase = true) }?.lowStockLimit
            ?: 2)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. MASTER FINANCIAL HERO CARD (SALES, EXPENSES & NET PROFIT)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            viewModel.getString("today_total_sales").uppercase(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(viewModel.todayDateString, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${totalSales.toInt()}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Net Profit & Expense Pill Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("दुकान खर्चा (Expenses)", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                Text("₹${totalExpenses.toInt()}", color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(viewModel.getString("net_profit"), color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                if (currentUser?.role == "STAFF") {
                                    Text("₹ ***", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                } else {
                                    Text("₹${netProfit.toInt()}", color = if (netProfit >= 0) Color(0xFF86EFAC) else Color(0xFFFCA5A5), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(viewModel.getString("cash_galla"), color = MaterialTheme.colorScheme.primaryContainer, fontSize = 11.sp)
                            Text("₹${cashSales.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text(viewModel.getString("upi_paytm"), color = MaterialTheme.colorScheme.primaryContainer, fontSize = 11.sp)
                            Text("₹${upiSales.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text(viewModel.getString("udhar_khata"), color = MaterialTheme.colorScheme.primaryContainer, fontSize = 11.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(viewModel.getString("quick_new_sale"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showInwardDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(viewModel.getString("inward_stock_btn"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (boxTrackedProducts.isNotEmpty()) {
                    Button(
                        onClick = { showOpenBoxDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        enabled = boxTrackedProducts.any { it.sealedBoxes > 0 }
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(viewModel.getString("quick_open_box"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // QUICK 1-TAP EXPENSES ROW
        // LOW STOCK RED WARNING BADGE
        if (lowStockItems.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(viewModel.getString("low_stock_warning"), color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        lowStockItems.take(6).forEach { prod ->
                            val limit = prod.lowStockLimit
                                ?: categories.firstOrNull { it.name.equals(prod.category, ignoreCase = true) }?.lowStockLimit
                                ?: 2
                            Text(
                                "• ${prod.name}: only ${prod.sealedBoxes} box left (limit $limit).",
                                color = if (prod.sealedBoxes == 0) Color(0xFF991B1B) else Color(0xFF92400E),
                                fontSize = 12.sp,
                                fontWeight = if (prod.sealedBoxes == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // CATEGORY-WISE SALES BREAKDOWN
        if (categorySalesMap.isNotEmpty()) {
            item {
                Text(viewModel.getString("category_sales"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categorySalesMap.entries.toList()) { entry ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.width(115.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(entry.key, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("₹${entry.value.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // TODAY'S TOP ITEM CARD
        if (topItem.pieces > 0) {
            item {
                Text(viewModel.getString("top_items"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("⭐ Top Performing Item", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(topItem.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${topItem.pieces} Pcs • ₹${topItem.revenue.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // TODAY'S "KYA ZYADA BIKA" (TOP SODA & TOP ICE-CREAM)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📊 आज क्या सबसे ज़्यादा बिका (Top Sellers):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🥤 Top Soda", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(topSodaIce.first, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🍦 Top Ice-cream", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(topSodaIce.second, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // GODOWN BOX STATUS
        if (boxTrackedProducts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(viewModel.getString("godown_box_status"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val totalBoxes = boxTrackedProducts.sumOf { it.sealedBoxes }
                    Text("Total: $totalBoxes Box", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                val brandList = boxTrackedProducts.map { if (it.brand.isNotBlank()) it.brand else it.category }.distinct()
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(brandList) { brandName ->
                        val brandProducts = boxTrackedProducts.filter {
                            it.brand == brandName || (it.brand.isBlank() && it.category == brandName)
                        }
                        val brandBoxes = brandProducts.sumOf { it.sealedBoxes }
                        val isLowStock = brandProducts.any { product ->
                            val limit = product.lowStockLimit
                                ?: categories.firstOrNull { it.name.equals(product.category, ignoreCase = true) }?.lowStockLimit
                                ?: 2
                            product.sealedBoxes <= limit
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(115.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(brandName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "$brandBoxes Box",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isLowStock) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // RECENT SALES LOG WITH 1-CLICK DIGITAL RECEIPT SHARING
        item {
            Text(viewModel.getString("recent_sales"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        if (sales.isEmpty()) {
            item {
                Text("No sales yet today.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            items(sales.take(8)) { sale ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    "CASH" -> Color(0xFF16A34A)
                                    "UPI" -> Color(0xFF0284C7)
                                    else -> Color(0xFFD97706)
                                }
                                Text(sale.paymentMode, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = modeColor)
                                if (sale.customerName != null) {
                                    Text("• ${sale.customerName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("₹${sale.totalAmount.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)

                            // Digital WhatsApp Receipt Button
                            IconButton(
                                onClick = {
                                    val receipt = viewModel.generateBillReceiptText(sale)
                                    sendWhatsAppMessage(context, null, receipt)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = "Share Receipt", tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            }

                            // Delete Sale Button
                            IconButton(
                                onClick = { viewModel.deleteSale(sale) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStockDialog && boxTrackedProducts.isNotEmpty()) {
    var selectedProduct by remember { mutableStateOf(boxTrackedProducts.first()) }
        var boxCountText by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showAddStockDialog = false },
            title = { Text(viewModel.getString("quick_new_stock"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Product:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(boxTrackedProducts) { prod ->
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
                        label = { Text("Total Boxes") },
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
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStockDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showOpenBoxDialog) {
        val availableProducts = boxTrackedProducts.filter { it.sealedBoxes > 0 }
        var selectedProduct by remember { mutableStateOf(availableProducts.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { showOpenBoxDialog = false },
            title = { Text(viewModel.getString("quick_open_box"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Box to open at Counter:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("-1 Box")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenBoxDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showInwardDialog) {
        InwardBillDialog(viewModel = viewModel, onDismiss = { showInwardDialog = false })
    }
}

// ==============================================================================
// 3. POS SALES ENTRY SCREEN
// ==============================================================================
@Composable
fun POSScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val khataList by viewModel.khataList.collectAsState()
    val discount by viewModel.discount.collectAsState()
    val topFavorites by viewModel.topFavoriteProducts.collectAsState(initial = emptyList())
    val lastSaleForUndo by viewModel.lastSaleForUndo.collectAsState()

    var undoSecondsLeft by remember { mutableStateOf(0) }
    LaunchedEffect(lastSaleForUndo) {
        if (lastSaleForUndo != null) {
            undoSecondsLeft = 5
            while (undoSecondsLeft > 0) {
                kotlinx.coroutines.delay(1000)
                undoSecondsLeft--
            }
        }
    }

    var selectedCategoryName by remember { mutableStateOf("All") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showSmartImportDialog by remember { mutableStateOf(false) }
    var showUdharDialog by remember { mutableStateOf(false) }
    var udharCustomerName by remember { mutableStateOf("") }
    var showEditItemDialog by remember { mutableStateOf<Int?>(null) }

    val rawTotal = cart.sumOf { it.total }
    val finalTotal = (rawTotal - discount).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // 5-SECOND SALE UNDO BANNER
        if (lastSaleForUndo != null && undoSecondsLeft > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("✅", fontSize = 16.sp)
                        Column {
                            Text("बिक्री दर्ज: ₹${lastSaleForUndo!!.totalAmount.toInt()} (${lastSaleForUndo!!.paymentMode})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Auto-dismiss in ${undoSecondsLeft}s", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.undoLastSale { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Undo / वापस लें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // TOP CART CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        viewModel.getString("live_counter_order"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    if (cart.isNotEmpty()) {
                        Text(
                            viewModel.getString("clear_cart"),
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearCart() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (cart.isEmpty()) {
                    Text(viewModel.getString("cart_empty_hint"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    Text("₹${item.customPrice.toInt()} x ${item.quantity}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    Text("₹${item.total.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (discount > 0) {
                            Text("Discount: -₹${discount.toInt()}", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                        Text("Total: ₹${finalTotal.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.completeOrder("CASH") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
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

        Spacer(modifier = Modifier.height(8.dp))

        // QUICK ADD-ONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(viewModel.getString("add_ons"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val addOns = listOf("🍋 +Lemon (₹5)" to 5.0, "🧂 +Masala (₹5)" to 5.0, "❄️ +Ice (₹5)" to 5.0)
            addOns.forEach { (label, cost) ->
                SuggestionChip(
                    onClick = {
                        if (cart.isNotEmpty()) {
                            viewModel.addAddOn(cart.lastIndex, label.replace(Regex("[^a-zA-Z0-9]"), ""), cost)
                        }
                    },
                    label = { Text(label, fontSize = 11.sp) }
                )
            }
        }

        // TOP 8 FAVOURITES (QUICK 1-TAP COUNTER)
        if (topFavorites.isNotEmpty() || products.isNotEmpty()) {
            val favsToShow = if (topFavorites.isNotEmpty()) topFavorites.take(8) else products.take(6)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text("⭐ सबसे ज़्यादा बिकने वाले (1-Tap +1):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(3.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(favsToShow) { fav ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.clickable { viewModel.addToCart(fav) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(fav.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text("+1 • ₹${fav.price.toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // CATEGORY BAR & ACTION BUTTONS
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
                FilledTonalButton(
                    onClick = { showSmartImportDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Import", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = { showAddCategoryDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Cat", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showAddProductDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Item", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (categories.isEmpty() && products.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(46.dp))
                    Text(
                        "No Categories or Items Yet!",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "You can add items manually or use Smart Import to upload from Image / PDF / Text.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { showAddCategoryDialog = true }) {
                            Text("+ Add Category")
                        }

                        FilledTonalButton(onClick = { showSmartImportDialog = true }) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Menu")
                        }
                    }
                }
            }
        } else {
            val filtered = if (selectedCategoryName == "All") products else products.filter { it.category.equals(selectedCategoryName, ignoreCase = true) }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No items in this category yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { showAddProductDialog = true }) {
                            Text("+ Add Item Here")
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
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(98.dp)
                                .clickable { viewModel.addToCart(product) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    if (product.brand.isNotBlank()) product.brand.uppercase() else product.category.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(product.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, textAlign = TextAlign.Center)
                                Text("₹${product.price.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSmartImportDialog) {
        SmartBulkImportDialog(viewModel = viewModel, onDismiss = { showSmartImportDialog = false })
    }

    if (showAddCategoryDialog) {
        var catName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(viewModel.getString("add_category_btn"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create a category for your products.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Name") },
                        singleLine = true
                    )
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
                    Text("Save Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddProductDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(if (selectedCategoryName != "All") selectedCategoryName else categories.firstOrNull()?.name.orEmpty()) }
        var brand by remember { mutableStateOf("") }
        var priceText by remember { mutableStateOf("") }
        var boxText by remember { mutableStateOf("0") }
        var productLimitText by remember { mutableStateOf("") }
        val trackBoxes = categories.firstOrNull {
            it.name.equals(category, ignoreCase = true)
        }?.trackBoxes ?: true

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text(viewModel.getString("add_item_btn"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (categories.isEmpty()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category name") },
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
                        label = { Text("Item name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Selling Rate (₹)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand (optional)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = productLimitText,
                        onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) productLimitText = it },
                        label = { Text("Low stock limit (optional)") },
                        singleLine = true
                    )
                    if (trackBoxes) {
                        OutlinedTextField(
                            value = boxText,
                            onValueChange = { boxText = it },
                            label = { Text("Sealed Godown Boxes") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    val b = if (trackBoxes) boxText.toIntOrNull() ?: 0 else 0
                    if (name.isNotBlank() && category.isNotBlank() && p > 0 && b >= 0) {
                        if (categories.none { it.name.equals(category.trim(), ignoreCase = true) }) {
                            viewModel.addCategory(category.trim())
                        }
                        viewModel.addProduct(name, category, brand, p, b, productLimitText.toIntOrNull())
                        showAddProductDialog = false
                    }
                }) {
                    Text("Save Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showUdharDialog) {
        AlertDialog(
            onDismissRequest = { showUdharDialog = false },
            title = { Text("Credit Sale (उधार खाता)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = udharCustomerName,
                        onValueChange = { udharCustomerName = it },
                        label = { Text("ग्राहक का नाम (Customer Name)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingIcon = {
                            if (udharCustomerName.isNotBlank()) {
                                IconButton(onClick = { udharCustomerName = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Autocomplete Customer Suggestions
                    val suggestions = remember(udharCustomerName, khataList) {
                        if (udharCustomerName.isBlank()) khataList.take(6)
                        else khataList.filter {
                            it.name.contains(udharCustomerName, ignoreCase = true) || it.phone.contains(udharCustomerName)
                        }
                    }

                    if (suggestions.isNotEmpty()) {
                        Text(
                            if (udharCustomerName.isBlank()) "हाल के ग्राहक (Tap to Select):" else "मिलते-जुलते ग्राहक (Autocomplete):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                        ) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(suggestions) { cust ->
                                    val isSelected = udharCustomerName.equals(cust.name, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { udharCustomerName = cust.name }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(cust.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                "बकाया: ₹${cust.outstandingDebt.toInt()}",
                                                fontSize = 11.sp,
                                                color = if (cust.outstandingDebt > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (udharCustomerName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("✨ नया ग्राहक '${udharCustomerName}' खाते में जुड़ेगा", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("Confirm Credit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUdharDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEditItemDialog != null) {
        val idx = showEditItemDialog!!
        val item = cart.getOrNull(idx)
        if (item != null) {
            var customRateText by remember { mutableStateOf(item.customPrice.toInt().toString()) }
            var qtyText by remember { mutableStateOf(item.quantity.toString()) }
            var activeKeypadTarget by remember { mutableStateOf("PRICE") } // "PRICE" or "QTY"

            AlertDialog(
                onDismissRequest = { showEditItemDialog = null },
                title = { Text("Edit: ${item.product.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (activeKeypadTarget == "QTY") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeKeypadTarget = "QTY" }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("मात्रा (Quantity)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(qtyText.ifBlank { "0" }, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (activeKeypadTarget == "PRICE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { activeKeypadTarget = "PRICE" }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("रेट (Rate ₹)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("₹${customRateText.ifBlank { "0" }}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // Touch Numeric Keypad for fast 1-hand operation
                        Text("त्वरित कीपैड (1-Hand Keypad):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("C", "0", "⌫")
                            ).forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    row.forEach { digit ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (digit == "C" || digit == "⌫") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clickable {
                                                    val current = if (activeKeypadTarget == "PRICE") customRateText else qtyText
                                                    val updated = when (digit) {
                                                        "C" -> ""
                                                        "⌫" -> if (current.isNotEmpty()) current.dropLast(1) else ""
                                                        else -> current + digit
                                                    }
                                                    if (activeKeypadTarget == "PRICE") customRateText = updated else qtyText = updated
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(digit, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
                        Text("Update (ओके)")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.updateCartItemQuantity(idx, 0)
                        showEditItemDialog = null
                    }) {
                        Text("Remove (हटाएं)", color = Color(0xFFDC2626))
                    }
                }
            )
        }
    }
}

// ==============================================================================
// SMART BULK IMPORT DIALOG
// ==============================================================================
@Composable
fun SmartBulkImportDialog(
    viewModel: ShopViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    var rawText by remember { mutableStateOf("") }
    var imageCategory by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var draftItems by remember { mutableStateOf(listOf<DraftProductItem>()) }
    var isPreviewReady by remember { mutableStateOf(false) }
    var isScanningFile by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileName = uri.lastPathSegment ?: "Selected Document"
            val mime = context.contentResolver.getType(uri) ?: ""
            val uriStr = uri.toString().lowercase()
            val isImage = mime.startsWith("image") || uriStr.endsWith(".jpg") || uriStr.endsWith(".jpeg") || uriStr.endsWith(".png") || uriStr.endsWith(".webp")

            if (isImage) {
                isScanningFile = true
                draftItems = emptyList()
                Toast.makeText(context, "Scanning image with Google ML Kit...", Toast.LENGTH_SHORT).show()
                BillScannerService.scanBillImage(context, uri) { _, text, _, _, items ->
                    isScanningFile = false
                    if (text.isNotBlank()) {
                        rawText = text
                        val parsed = viewModel.parseTextToDraftItems(text)
                        if (parsed.isNotEmpty()) {
                            draftItems = parsed
                        } else if (items.isNotEmpty()) {
                            draftItems = items.map {
                                DraftProductItem(
                                    category = imageCategory.trim(),
                                    name = it.name,
                                    brand = "",
                                    price = it.sellingPrice,
                                    costPrice = it.costPrice,
                                    sealedBoxes = it.boxes
                                )
                            }
                        }
                        if (draftItems.isNotEmpty()) {
                            isPreviewReady = true
                            Toast.makeText(context, "ML Kit OCR parsed ${draftItems.size} items!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No product rows recognized. Enter the menu as text.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "No text found in image.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    if (text.isNotBlank()) {
                        rawText = text
                        draftItems = viewModel.parseTextToDraftItems(text)
                        isPreviewReady = true
                    } else {
                        Toast.makeText(context, "File attached: $selectedFileName", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "File read error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📄 Smart Category & Item Import", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isPreviewReady) {
                    Text(
                        "Upload menu image / PDF / document OR paste text below to auto-import categories and items:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = imageCategory,
                        onValueChange = { imageCategory = it },
                        label = { Text("Category for image items") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (categories.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { category ->
                                FilterChip(
                                    selected = imageCategory.equals(category.name, ignoreCase = true),
                                    onClick = { imageCategory = category.name },
                                    label = { Text(category.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = { filePicker.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(selectedFileName ?: "📁 Upload Menu Image / PDF / File")
                    }

                    Text("OR Paste Rate Card / Menu List:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        placeholder = { Text("[Category]\nItem name, price, brand, boxes, cost price") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        maxLines = 8
                    )

                    Button(
                        onClick = {
                            if (rawText.isNotBlank()) {
                                draftItems = viewModel.parseTextToDraftItems(rawText)
                                isPreviewReady = draftItems.isNotEmpty()
                                if (draftItems.isEmpty()) {
                                    Toast.makeText(context, "No valid products found. Include a category and item price.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Please enter text or pick file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Review Extracted Items (${viewModel.parseTextToDraftItems(rawText).size})")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Editable Preview (${draftItems.size} items)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        TextButton(onClick = { isPreviewReady = false }) { Text("Back / Re-parse") }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(draftItems.size) { idx ->
                            val item = draftItems[idx]
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        OutlinedTextField(
                                            value = item.category,
                                            onValueChange = { category ->
                                                draftItems = draftItems.toMutableList().apply {
                                                    set(idx, item.copy(category = category))
                                                }
                                            },
                                            label = { Text("Category") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            "Brand: ${item.brand.ifBlank { "—" }}" +
                                                if (categories.firstOrNull { it.name.equals(item.category, ignoreCase = true) }?.trackBoxes != false) {
                                                    " • Boxes: ${item.sealedBoxes}"
                                                } else {
                                                    ""
                                                },
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("₹${item.price.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                        IconButton(
                                            onClick = {
                                                draftItems = draftItems.toMutableList().apply { removeAt(idx) }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isPreviewReady) {
                Button(
                    onClick = {
                        if (draftItems.any { it.category.isBlank() || it.name.isBlank() || it.price <= 0 }) {
                            Toast.makeText(context, "Set a category, item name and valid price for every row.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        viewModel.bulkImportCategoriesAndProducts(draftItems)
                        Toast.makeText(context, "${draftItems.size} items saved successfully!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("Save All Items (${draftItems.size})")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ==============================================================================
// 4A. INWARD STOCK AND SUPPLIER BILL DIALOG
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InwardBillDialog(
    viewModel: ShopViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val brands by viewModel.brands.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()

    var selectedBrand by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var billNumber by remember { mutableStateOf("") }
    var billDate by remember { mutableStateOf(viewModel.todayDateString) }
    var paymentStatus by remember { mutableStateOf("PAID") }
    var paymentMode by remember { mutableStateOf("UPI") }
    var paidAmountText by remember { mutableStateOf("") }
    var customBillText by remember { mutableStateOf("") }
    var showPasteSection by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var addGstToCost by remember { mutableStateOf(false) }

    val draftItems = remember { mutableStateListOf<InwardBillItemDraft>() }
    val lineAmountTexts = remember { mutableStateMapOf<String, String>() }

    var isScanning by remember { mutableStateOf(false) }

    fun appendParsedInvoice(parsed: BillScannerService.ParsedInvoiceData) {
        draftItems.addAll(parsed.items)
        parsed.lineAmounts.forEach { (id, amount) -> lineAmountTexts[id] = amount.toString() }
        parsed.detectedBrand?.let { selectedBrand = it }
        parsed.detectedInvoiceNo?.let { billNumber = it }
        parsed.detectedDate?.let { billDate = it }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isScanning = true
            Toast.makeText(context, "Scanning bill with Google ML Kit...", Toast.LENGTH_SHORT).show()
            BillScannerService.scanInvoiceImage(
                context,
                uri,
                subCategories.map { it.name },
                addGstToCost
            ) { success, _, parsed ->
                isScanning = false
                if (success && parsed.items.isNotEmpty()) {
                    appendParsedInvoice(parsed)
                    Toast.makeText(context, "ML Kit: Added ${parsed.items.size} invoice items.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Bill samajh nahi aaya, manually add karein", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val trackBoxes = categories.firstOrNull {
        it.name.equals(selectedCategory, ignoreCase = true)
    }?.trackBoxes ?: true
    val totalBoxes = if (trackBoxes) draftItems.sumOf { it.boxes.toLong() } else 0L
    val totalCost = draftItems.sumOf { draft ->
        lineAmountTexts[draft.id]?.toDoubleOrNull()
            ?: draft.boxes.toDouble() * draft.costPrice
    }
    val totalSelling = draftItems.sumOf { it.boxes.toDouble() * it.sellingPrice }
    val totalProjectedProfit = totalSelling - totalCost
    val overallMargin = if (totalSelling > 0) (totalProjectedProfit / totalSelling) * 100.0 else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(viewModel.getString("inward_stock_title"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. BRAND SELECTOR
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = selectedBrand,
                            onValueChange = { selectedBrand = it },
                            label = { Text(viewModel.getString("select_brand_label")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (brands.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(brands) { brand ->
                                    FilterChip(
                                        selected = selectedBrand.equals(brand.name, ignoreCase = true),
                                        onClick = { selectedBrand = brand.name },
                                        label = { Text(brand.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. CATEGORY SELECTOR
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            viewModel.getString("select_category_label"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(categories) { category ->
                                    FilterChip(
                                        selected = selectedCategory.equals(category.name, ignoreCase = true),
                                        onClick = { selectedCategory = category.name },
                                        label = { Text(category.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                            TextButton(onClick = { showNewCategoryDialog = true }) {
                                Text("+ New", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 3. BILL NUMBER & DATE
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = billNumber,
                            onValueChange = { billNumber = it },
                            label = { Text(viewModel.getString("bill_number_label")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = billDate,
                            onValueChange = { billDate = it },
                            label = { Text(viewModel.getString("bill_date_label")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 4. PAYMENT STATUS
                item {
                    Text(viewModel.getString("payment_status_label"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = paymentStatus == "PAID",
                            onClick = { paymentStatus = "PAID" },
                            label = { Text(viewModel.getString("payment_status_paid"), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = paymentStatus == "UNPAID",
                            onClick = { paymentStatus = "UNPAID" },
                            label = { Text(viewModel.getString("payment_status_unpaid"), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // 5. PAYMENT MODE
                if (paymentStatus != "UNPAID") {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(viewModel.getString("payment_mode_label"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("UPI", "CASH", "BANK")) { mode ->
                                    FilterChip(
                                        selected = paymentMode == mode,
                                        onClick = { paymentMode = mode },
                                        label = { Text(mode, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. UPLOAD IMAGE / PDF OR PASTE BUTTONS
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { launcher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (draftItems.isEmpty()) "Upload Bill Image" else "Upload second page (append)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { showPasteSection = !showPasteSection },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste Bill Text", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isScanning) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Google ML Kit OCR बिल पढ़ रहा है...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (showPasteSection) {
                    item {
                        Column {
                            OutlinedTextField(
                                value = customBillText,
                                onValueChange = { customBillText = it },
                                placeholder = { Text("Paste invoice rows containing HSN, description, MRP, Qty, UOM, Rate, tax and Amount", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().height(90.dp)
                            )
                            Button(
                                onClick = {
                                    val parsedOcr = BillScannerService.parseInvoiceText(
                                        customBillText,
                                        subCategories.map { it.name },
                                        addGstToCost
                                    )
                                    if (parsedOcr.items.isNotEmpty()) {
                                        appendParsedInvoice(parsedOcr)
                                        Toast.makeText(context, "Added ${parsedOcr.items.size} invoice items", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Bill samajh nahi aaya, manually add karein", Toast.LENGTH_LONG).show()
                                    }
                                    showPasteSection = false
                                },
                                modifier = Modifier.padding(top = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Load Parsed Items", fontSize = 11.sp)
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = addGstToCost,
                            onCheckedChange = { addGstToCost = it }
                        )
                        Text("Add 5% GST to ex-GST cost rate", fontSize = 11.sp)
                    }
                }

                // 7. EDITABLE ITEMS TABLE
                item {
                    Text(
                        if (trackBoxes) "📦 इनवॉइस आइटम्स & बॉक्स रिव्यू (${draftItems.size} Items):"
                        else "इनवॉइस आइटम्स (${draftItems.size} Items):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(draftItems, key = { it.id }) { draft ->
                    val idx = draftItems.indexOfFirst { it.id == draft.id }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = draft.name,
                                    onValueChange = { draftItems[idx] = draft.copy(name = it) },
                                    label = { Text("Product name", fontSize = 9.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { draftItems.removeAt(idx) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (trackBoxes) {
                                    OutlinedTextField(
                                        value = draft.boxesInput,
                                        onValueChange = {
                                            draftItems[idx] = draft.copy(
                                                boxesInput = it,
                                                boxes = it.toIntOrNull() ?: 0
                                            )
                                        },
                                        label = { Text("Boxes", fontSize = 9.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = draft.costPriceInput,
                                    onValueChange = {
                                        draftItems[idx] = draft.copy(
                                            costPriceInput = it,
                                            costPrice = it.toDoubleOrNull() ?: 0.0
                                        )
                                    },
                                    label = { Text("Cost ₹", fontSize = 9.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = draft.sellingPriceInput,
                                    onValueChange = {
                                        draftItems[idx] = draft.copy(
                                            sellingPriceInput = it,
                                            sellingPrice = it.toDoubleOrNull() ?: 0.0
                                        )
                                    },
                                    label = { Text("MRP ₹", fontSize = 9.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = lineAmountTexts[draft.id]
                                        ?.takeIf { it.isNotBlank() }
                                        ?: (draft.boxes * draft.costPrice).toString(),
                                    onValueChange = { lineAmountTexts[draft.id] = it },
                                    label = { Text("Amount ₹", fontSize = 9.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "Rate ₹${draft.costPrice} • MRP ₹${draft.sellingPrice}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = {
                            val draft = InwardBillItemDraft(name = "", boxes = 1)
                            draftItems.add(draft)
                            lineAmountTexts[draft.id] = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Add Another Product Line", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 8. BILL SUMMARY CARD
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (trackBoxes) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("कुल पेटियां (Total Boxes):", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text("$totalBoxes Boxes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("कुल बिल खरीद रकम:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(
                                    "₹${String.format(Locale.getDefault(), "%.2f", totalCost)}",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("अनुमानित कुल बिक्री (MRP):", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(
                                    "₹${String.format(Locale.getDefault(), "%.2f", totalSelling)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("इस माल पर अनुमानित शुद्ध मुनाफ़ा:", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    "+₹${String.format(Locale.getDefault(), "%.2f", totalProjectedProfit)} (${overallMargin.toInt()}%)",
                                    color = Color(0xFF4ADE80),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedBrand.isBlank() || selectedCategory.isBlank()) {
                        Toast.makeText(context, "Enter a supplier and choose or add a category.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (draftItems.isEmpty() || draftItems.any {
                            it.name.isBlank() || (trackBoxes && it.boxes !in 1..9999) ||
                                it.costPrice !in 0.01..100000.0 || it.sellingPrice !in 0.01..100000.0
                        }) {
                        Toast.makeText(context, "Complete each item with a name, quantity, cost and selling price.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val paid = if (paymentStatus == "PAID") totalCost else if (paymentStatus == "PARTIAL") {
                        paidAmountText.toDoubleOrNull()?.takeIf { it in 0.0..totalCost }
                    } else {
                        0.0
                    }
                    if (paid == null) {
                        Toast.makeText(context, "Enter a valid amount paid between ₹0 and the bill total.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.saveInwardStockPurchase(
                        brand = selectedBrand,
                        category = selectedCategory,
                        billNumber = billNumber,
                        billDate = billDate,
                        totalAmount = totalCost,
                        paidAmount = paid,
                        paymentStatus = paymentStatus,
                        paymentMode = paymentMode,
                        draftItems = draftItems.map { if (trackBoxes) it else it.copy(boxes = 1) }
                    ) { error ->
                        if (error == null) {
                            Toast.makeText(context, viewModel.getString("inward_success_msg"), Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Text(if (trackBoxes) "Save Inward Stock & Update Boxes" else "Save Inward Purchase")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("Add category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val categoryName = newCategoryName.trim()
                        if (categoryName.isNotBlank()) {
                            viewModel.addCategory(categoryName)
                            selectedCategory = categoryName
                            newCategoryName = ""
                            showNewCategoryDialog = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ==============================================================================
// 4. GODOWN BOX-ONLY STOCK TRACKER SCREEN
// ==============================================================================
@Composable
fun InventoryScreen(viewModel: ShopViewModel) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val iceCreamProfit by viewModel.iceCreamProfitStats.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var showAddStockDialog by remember { mutableStateOf(false) }
    var showInwardDialog by remember { mutableStateOf(false) }

    val boxTrackedProducts = products.filter { product ->
        categories.firstOrNull { it.name.equals(product.category, ignoreCase = true) }?.trackBoxes ?: true
    }
    val filterOptions = listOf("All") + categories.filter { it.trackBoxes }.map { it.name }
    val filtered = if (selectedFilter == "All") {
        boxTrackedProducts
    } else {
        boxTrackedProducts.filter { it.category.equals(selectedFilter, ignoreCase = true) }
    }
    val totalBoxes = boxTrackedProducts.sumOf { it.sealedBoxes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ICE-CREAM PROFIT ANALYTICS CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🍨", fontSize = 20.sp)
                            Column {
                                Text(viewModel.getString("icecream_profit_title"), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("बिक्री व खरीद लागत के आधार पर शुद्ध मुनाफ़ा", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF16A34A).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "${iceCreamProfit.marginPercent.toInt()}% Margin",
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("कुल आइसक्रीम बिक्री:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${iceCreamProfit.totalSalesRevenue.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column {
                            Text("खरीद लागत (Cost):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${iceCreamProfit.totalCostOfGoods.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("शुद्ध मुनाफ़ा (Profit):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("+₹${iceCreamProfit.grossProfit.toInt()} 🟢", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                        }
                    }

                    // Prominent button to upload / inward stock bill!
                    Button(
                        onClick = { showInwardDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(viewModel.getString("inward_stock_btn"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        if (boxTrackedProducts.isNotEmpty()) item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📦 " + viewModel.getString("godown_box_status"), color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalBoxes Sealed Boxes", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap '-1 Open' when opening a box at counter.", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                }
            }
        }

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
                if (boxTrackedProducts.isNotEmpty()) {
                    Button(
                        onClick = { showAddStockDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(viewModel.getString("quick_new_stock"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Text("No products in stock yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            items(filtered) { item ->
                val categoryLimit = categories.firstOrNull {
                    it.name.equals(item.category, ignoreCase = true)
                }?.lowStockLimit ?: 2
                val limit = item.lowStockLimit ?: categoryLimit
                val isLowStock = item.sealedBoxes <= limit
                var limitText by remember(item.id, item.lowStockLimit) {
                    mutableStateOf(item.lowStockLimit?.toString().orEmpty())
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.category.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isLowStock) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (item.sealedBoxes == 0) Color(0xFFDC2626) else Color(0xFFB45309),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    "${item.sealedBoxes} Boxes in Godown",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        item.sealedBoxes == 0 -> Color(0xFFDC2626)
                                        isLowStock -> Color(0xFFB45309)
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                            }
                            if (isLowStock) {
                                Text(
                                    "${item.name}: only ${item.sealedBoxes} box left (limit $limit).",
                                    color = if (item.sealedBoxes == 0) Color(0xFFDC2626) else Color(0xFFB45309),
                                    fontSize = 11.sp,
                                    fontWeight = if (item.sealedBoxes == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            OutlinedTextField(
                                value = limitText,
                                onValueChange = { value ->
                                    if (value.isEmpty() || value.all(Char::isDigit)) {
                                        limitText = value
                                    }
                                },
                                label = { Text("Low stock limit (optional)") },
                                supportingText = { Text("Category default: $categoryLimit") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(
                                onClick = {
                                    viewModel.updateProduct(item.copy(lowStockLimit = limitText.toIntOrNull()))
                                }
                            ) {
                                Text("Save limit")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { viewModel.addBoxes(item.id, item.name, 1) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+1 Box", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }

                            Button(
                                onClick = { viewModel.openBox(item.id, item.name) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                enabled = item.sealedBoxes > 0
                            ) {
                                Text("-1 Open", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStockDialog && boxTrackedProducts.isNotEmpty()) {
        var selectedProduct by remember { mutableStateOf(boxTrackedProducts.first()) }
        var boxCountText by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showAddStockDialog = false },
            title = { Text(viewModel.getString("quick_new_stock"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Product:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(boxTrackedProducts) { prod ->
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
                        label = { Text("Total Boxes") },
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
                    Text("Add Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStockDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showInwardDialog) {
        InwardBillDialog(viewModel = viewModel, onDismiss = { showInwardDialog = false })
    }
}

// ==============================================================================
// 5. CUSTOMER KHATA (UDHAAR) & RECOVERY SCREEN
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
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(viewModel.getString("total_market_outstanding"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFEF3C7), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${totalOutstanding.toInt()}", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)

                    val debtorCount = khataList.count { it.outstandingDebt > 0 }
                    if (debtorCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val debtorsWithPhone = khataList.filter { it.outstandingDebt > 0 && it.phone.isNotBlank() }
                                val first = debtorsWithPhone.firstOrNull()
                                if (first != null) {
                                    val msg = "नमस्ते ${first.name} जी,\nरिफ्रेश सोडा दुकान पर आपका पिछला बकाया ₹${first.outstandingDebt.toInt()} है। कृपया जल्द भुगतान करें। धन्यवाद!"
                                    sendWhatsAppMessage(context, first.phone, msg)
                                } else {
                                    Toast.makeText(context, "$debtorCount बकायेदार हैं। कृपया ग्राहक कार्ड पर Send दबाएं।", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📢 बकाया तकादा रिमाइंडर ($debtorCount ग्राहक)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(viewModel.getString("add_customer"))
                }
            }
        }

        items(filteredCustomers) { customer ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (customer.outstandingDebt >= 400) {
                                Surface(
                                    color = Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("⚠️ 7+ दिन पुराना", color = Color(0xFFDC2626), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                        if (customer.phone.isNotBlank()) {
                            Text("📱 ${customer.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Due: ₹${customer.outstandingDebt.toInt()}", color = Color(0xFFDC2626), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val msg = "Hello ${customer.name} ji,\nYour outstanding balance at Refresh Soda is ₹${customer.outstandingDebt.toInt()}. Please clear it at your earliest convenience.\nThank you!"
                                sendWhatsAppMessage(context, customer.phone, msg)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "WhatsApp", tint = Color(0xFF16A34A))
                        }

                        Button(
                            onClick = {
                                repayCustomer = customer
                                repayAmountText = customer.outstandingDebt.toInt().toString()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
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
            title = { Text("Payment: ${cust.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total Due: ₹${cust.outstandingDebt.toInt()}", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = repayAmountText,
                        onValueChange = { repayAmountText = it },
                        label = { Text("Received Amount (₹)") },
                        singleLine = true
                    )
                    Text("Payment Mode:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    Text("Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { repayCustomer = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var limitText by remember { mutableStateOf("2000") }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text(viewModel.getString("add_customer"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number (WhatsApp)") }, singleLine = true)
                    OutlinedTextField(value = limitText, onValueChange = { limitText = it }, label = { Text("Credit Limit (₹)") }, singleLine = true)
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
                    Text("Save Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ==============================================================================
// 6. HISAAB, CLOSING, COMPREHENSIVE EXPENSES & MENU SETUP SCREEN
// ==============================================================================
@Composable
fun HisaabScreen(
    viewModel: ShopViewModel,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableStateOf(0) }

    val subTabs = listOf(
        viewModel.getString("day_closing_title"),
        viewModel.getString("daily_expenses"),
        viewModel.getString("paytm_settlement_title"),
        viewModel.getString("supplier_bills_title"),
        "Category & Menu Setup"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            3 -> SupplierPurchasesView(viewModel, context)
            4 -> MenuAndCategoryManagementView(viewModel)
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(viewModel.getString("day_closing_title"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.getString("opening_cash"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${openingCash.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• (+) Cash Sales:", fontSize = 12.sp, color = Color(0xFF16A34A))
                        Text("+ ₹${cashSales.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF16A34A))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• (+) Udhar Cash Recovery:", fontSize = 12.sp, color = Color(0xFF16A34A))
                        Text("+ ₹${udharCashRepay.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF16A34A))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• (-) Cash Expenses:", fontSize = 12.sp, color = Color(0xFFDC2626))
                        Text("- ₹${cashExpenses.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFDC2626))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.getString("expected_cash"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("₹${expectedCash.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(viewModel.getString("actual_cash_input"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = actualCashText,
                        onValueChange = { actualCashText = it },
                        label = { Text("Counted Cash in Till (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    val diffColor = if (difference == 0.0) Color(0xFF16A34A) else if (difference < 0) Color(0xFFDC2626) else Color(0xFF0284C7)
                    val diffText = if (difference == 0.0) {
                        "✅ Perfect Match! (₹0 Difference)"
                    } else if (difference < 0) {
                        "❌ ₹${(-difference).toInt()} Short!"
                    } else {
                        "➕ ₹${difference.toInt()} Surplus"
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
                                Toast.makeText(context, "Day closing saved!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
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

// ---------------- SUBVIEW 2: COMPREHENSIVE DAILY & MONTHLY EXPENSES ----------------
@Composable
fun DailyExpensesView(viewModel: ShopViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val totalExpenses by viewModel.totalExpensesAmount.collectAsState()
    val categoryExpMap by viewModel.categoryExpenses.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "❄️ बर्फ (Ice)",
        "🍋 नींबू (Lemons)",
        "🥤 सोडे का माल व गैस (Syrup & Gas)",
        "🥛 दूध व शेक सामग्री (Milk & Shake)",
        "🧻 ग्लास व स्ट्रॉ (Glasses & Straws)",
        "💡 बिजली बिल (Electricity Bill)",
        "🏪 दुकान का किराया (Shop Rent)",
        "👷 कारीगर / मज़दूरी (Labor / Salary)",
        "🔧 मशीन मेंटेनेंस (Machine Repair)",
        "📦 अन्य खर्च (Other Miscellaneous)"
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // TOP EXPENSE SUMMARY CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("आज का कुल खर्चा (Total Expenses)", fontSize = 12.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                        Text("₹${totalExpenses.toInt()}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(viewModel.getString("add_expense"))
                    }
                }
            }
        }

        // CATEGORY BREAKDOWN CHIPS
        if (categoryExpMap.isNotEmpty()) {
            item {
                Text("खर्चों का विवरण (Breakdown):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categoryExpMap.entries.toList()) { entry ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(entry.key, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${entry.value.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("हाल के खर्चे (Recent Expenses Log)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (expenses.isEmpty()) {
            item {
                Text("आज कोई खर्चा दर्ज नहीं हुआ है।", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else {
            items(expenses) { exp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            Text("${exp.category} • Mode: ${exp.paymentMode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹${exp.amount.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFFDC2626))
                            IconButton(onClick = { viewModel.deleteExpense(exp) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626))
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
                    Text("कैटेगरी चुनें:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = {
                                    category = cat
                                    if (title.isBlank()) title = cat.substringAfter(" ").trim()
                                },
                                label = { Text(cat, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("खर्चे का विवरण (Title/Note)") }, singleLine = true)
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("रकम (₹ Amount)") }, singleLine = true)

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
                            label = { Text("📱 बैंक / UPI") }
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
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(viewModel.getString("paytm_settlement_title"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Today's Total Online/UPI Collection:", fontSize = 13.sp)
                        Text("₹${upiTotal.toInt()}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0284C7))
                    }

                    OutlinedTextField(
                        value = chargeText,
                        onValueChange = { chargeText = it },
                        label = { Text("Manual MDR / Charge Deductions (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(viewModel.getString("net_bank_amount"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("₹${netExpected.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF16A34A))
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
                        Text("Update Settlement")
                    }
                }
            }
        }
    }
}

// ---------------- SUBVIEW 4: SUPPLIER PURCHASES & DISTRIBUTOR BILLS ----------------
@Composable
fun SupplierPurchasesView(viewModel: ShopViewModel, context: Context) {
    val purchases by viewModel.supplierPurchases.collectAsState()
    val totalDebt by viewModel.totalSupplierDebt.collectAsState()
    val categories by viewModel.categories.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Outstanding Debt Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("सप्लायर उधारी (Total Supplier Outstanding)", color = Color(0xFFFECDD3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${totalDebt.toInt()} बाकी", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("सप्लायर को चुकाई जाने वाली बकाया रकम।", color = Color(0xFFFEE2E2), fontSize = 11.sp)
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
                        "अभी तक कोई सप्लायर इनवॉइस नहीं जोड़ा गया है।\nगोदाम स्टॉक स्क्रीन में '+ नया माल (बिल अपलोड)' दबाकर सप्लायर का बिल जोड़ें।",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(purchases) { purchase ->
                val isPaid = purchase.paymentStatus == "PAID"
                val isUnpaid = purchase.paymentStatus == "UNPAID"
                val balance = purchase.totalAmount - purchase.paidAmount
                val trackBoxes = categories.firstOrNull {
                    it.name.equals(purchase.category, ignoreCase = true)
                }?.trackBoxes ?: true

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
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(purchase.brand, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(purchase.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text("Bill #${purchase.billNumber} • ${purchase.billDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPaid) Color(0xFF16A34A).copy(alpha = 0.15f) else Color(0xFFDC2626).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    if (isPaid) "✅ Full Paid" else if (isUnpaid) "❌ Unpaid (Udhar)" else "⚠️ Partial",
                                    color = if (isPaid) Color(0xFF16A34A) else Color(0xFFDC2626),
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    if (trackBoxes) "कुल बिल: ₹${purchase.totalAmount.toInt()} (${purchase.totalBoxes} Boxes)"
                                    else "कुल बिल: ₹${purchase.totalAmount.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!isPaid) {
                                    Text("बकाया: ₹${balance.toInt()} बाकी", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                            }

                            if (!isPaid) {
                                Button(
                                    onClick = {
                                        viewModel.markSupplierBillPaid(purchase)
                                        Toast.makeText(context, "Bill marked as Full Paid!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Mark as Paid", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- SUBVIEW 5: USER CATEGORY & MENU MANAGEMENT ----------------
@Composable
fun MenuAndCategoryManagementView(viewModel: ShopViewModel) {
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showSmartImportDialog by remember { mutableStateOf(false) }
    var selectedCatForProduct by remember { mutableStateOf<String?>(null) }
    var productBeingEdited by remember { mutableStateOf<Product?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { showSmartImportDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("📄 Smart Bulk Import", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showAddCategoryDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ New Category", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text("User-Defined Categories (${categories.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (categories.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        "No categories created yet. Add a category first, then add its products.",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(categories) { cat ->
                val count = products.count { it.category.equals(cat.name, ignoreCase = true) }
                var limitText by remember(cat.id, cat.lowStockLimit) { mutableStateOf(cat.lowStockLimit.toString()) }
                var subCategoryName by remember(cat.id) { mutableStateOf("") }
                var subCategoryPrice by remember(cat.id) { mutableStateOf("") }
                val categorySubCategories = subCategories.filter {
                    it.categoryName.equals(cat.name, ignoreCase = true)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("$count items attached", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        selectedCatForProduct = cat.name
                                        showAddProductDialog = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("+ Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626))
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Track boxes", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = cat.trackBoxes,
                                onCheckedChange = { viewModel.updateCategory(cat.copy(trackBoxes = it)) }
                            )
                        }
                        OutlinedTextField(
                            value = limitText,
                            onValueChange = { value ->
                                if (value.all(Char::isDigit)) {
                                    limitText = value
                                    value.toIntOrNull()?.let { viewModel.updateCategory(cat.copy(lowStockLimit = it)) }
                                }
                            },
                            label = { Text("Low stock limit") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Sub-categories", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        categorySubCategories.forEach { subCategory ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${subCategory.name}${if (subCategory.defaultPrice > 0) " • ₹${subCategory.defaultPrice.toInt()}" else ""}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 12.sp
                                )
                                IconButton(onClick = { viewModel.deleteSubCategory(subCategory) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete sub-category", tint = Color(0xFFDC2626))
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = subCategoryName,
                                onValueChange = { subCategoryName = it },
                                label = { Text("Sub-category name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = subCategoryPrice,
                                onValueChange = { subCategoryPrice = it },
                                label = { Text("Default price (optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        TextButton(
                            onClick = {
                                viewModel.addSubCategory(
                                    cat.name,
                                    subCategoryName,
                                    subCategoryPrice.toDoubleOrNull() ?: 0.0
                                )
                                subCategoryName = ""
                                subCategoryPrice = ""
                            },
                            enabled = subCategoryName.isNotBlank()
                        ) {
                            Text("+ Add sub-category")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All Items (${products.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Button(
                    onClick = {
                        selectedCatForProduct = categories.firstOrNull()?.name.orEmpty()
                        showAddProductDialog = true
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Add Item")
                }
            }
        }

        if (products.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                    Text("No items added yet. Click '+ Add Item' or 'Smart Bulk Import'.", modifier = Modifier.padding(14.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(products) { prod ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            Text(
                                "Category: ${prod.category} ${if (prod.brand.isNotBlank()) "• ${prod.brand}" else ""}" +
                                    if (viewModel.isBoxTrackingEnabled(prod.category)) " • ${prod.sealedBoxes} Boxes" else "",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("₹${prod.price.toInt()}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { productBeingEdited = prod }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit item")
                            }
                            IconButton(onClick = { viewModel.deleteProduct(prod) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text("Audit & Change Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        items(auditLogs.take(10)) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(log.action, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Text(log.details, fontSize = 12.sp)
                }
            }
        }
    }

    if (showSmartImportDialog) {
        SmartBulkImportDialog(viewModel = viewModel, onDismiss = { showSmartImportDialog = false })
    }

    if (showAddCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("+ Add Category", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = catName,
                    onValueChange = { catName = it },
                    label = { Text("Category name") },
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
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddProductDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(selectedCatForProduct ?: categories.firstOrNull()?.name.orEmpty()) }
        var brand by remember { mutableStateOf("") }
        var priceText by remember { mutableStateOf("") }
        var boxText by remember { mutableStateOf("0") }
        var productLimitText by remember { mutableStateOf("") }
        val trackBoxes = categories.firstOrNull {
            it.name.equals(category, ignoreCase = true)
        }?.trackBoxes ?: true

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("+ Add Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (categories.isNotEmpty()) {
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
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category name") },
                            singleLine = true
                        )
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item name") }, singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price (₹)") }, singleLine = true)
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand (optional)") }, singleLine = true)
                    OutlinedTextField(
                        value = productLimitText,
                        onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) productLimitText = it },
                        label = { Text("Low stock limit (optional)") },
                        singleLine = true
                    )
                    if (trackBoxes) {
                        OutlinedTextField(value = boxText, onValueChange = { boxText = it }, label = { Text("Godown Boxes") }, singleLine = true)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    val b = if (trackBoxes) boxText.toIntOrNull() ?: 0 else 0
                    if (name.isNotBlank() && category.isNotBlank() && p > 0 && b >= 0) {
                        if (categories.none { it.name.equals(category.trim(), ignoreCase = true) }) {
                            viewModel.addCategory(category.trim())
                        }
                        viewModel.addProduct(name, category, brand, p, b, productLimitText.toIntOrNull())
                        showAddProductDialog = false
                    }
                }) {
                    Text("Save Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("Cancel") }
            }
        )
    }

    productBeingEdited?.let { product ->
        var name by remember(product.id) { mutableStateOf(product.name) }
        var brand by remember(product.id) { mutableStateOf(product.brand) }
        var priceText by remember(product.id) { mutableStateOf(product.price.toString()) }
        var productLimitText by remember(product.id) {
            mutableStateOf(product.lowStockLimit?.toString().orEmpty())
        }
        AlertDialog(
            onDismissRequest = { productBeingEdited = null },
            title = { Text("Edit Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item name") }, singleLine = true)
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand (optional)") }, singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price (₹)") }, singleLine = true)
                    OutlinedTextField(
                        value = productLimitText,
                        onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) productLimitText = it },
                        label = { Text("Low stock limit (optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val price = priceText.toDoubleOrNull()
                    if (name.isNotBlank() && price != null && price > 0) {
                        viewModel.updateProduct(
                            product.copy(
                                name = name.trim(),
                                brand = brand.trim(),
                                price = price,
                                lowStockLimit = productLimitText.toIntOrNull()
                            )
                        )
                        productBeingEdited = null
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { productBeingEdited = null }) { Text("Cancel") }
            }
        )
    }
}
