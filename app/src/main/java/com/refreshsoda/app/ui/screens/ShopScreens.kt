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
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import com.refreshsoda.app.data.model.CustomerKhata
import com.refreshsoda.app.data.model.DraftProductItem
import com.refreshsoda.app.data.model.Expense
import com.refreshsoda.app.data.model.Product
import com.refreshsoda.app.data.model.SubCategory
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

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        label = label?.let { { Text(it, fontSize = 12.sp) } },
        placeholder = placeholder?.let { { Text(it, fontSize = 13.sp, color = Color(0xFF94A3B8)) } },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF366A55),
            unfocusedBorderColor = Color(0xFFE2E8F0),
            focusedContainerColor = Color(0xFFF8FAFC),
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedLabelColor = Color(0xFF366A55),
            unfocusedLabelColor = Color(0xFF64748B),
            cursorColor = Color(0xFF366A55)
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
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
                "☁️ Cloud Backup & Data Recovery",
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
                    Text("Secure your shop data or restore on a new device:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                            Text("Backup Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            Text("Restore Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    onNavigateToPOS: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToKhata: () -> Unit = {},
    onNavigateToHisaab: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onLogout: () -> Unit = {}
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
    val subCategories by viewModel.subCategories.collectAsState()
    val varietyStocks by viewModel.iceCreamVarietyStock.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val topSodaIce by viewModel.topSodaAndIceCream.collectAsState()

    var showAddStockDialog by remember { mutableStateOf(false) }
    var showInwardDialog by remember { mutableStateOf(false) }
    var showOpenBoxDialog by remember { mutableStateOf(false) }
    var selectedCompanyForStock by remember { mutableStateOf<SubCategory?>(null) }
    var stockVarietyName by remember { mutableStateOf("") }
    var stockCountText by remember { mutableStateOf("1") }

    val boxTrackedSubCategories = subCategories.filter { subCategory ->
        categories.any { it.trackBoxes && it.name.equals(subCategory.categoryName, ignoreCase = true) }
    }
    val trackedVarietyStocks = varietyStocks.filter { stock ->
        categories.any { it.trackBoxes && it.name.equals(stock.categoryName, ignoreCase = true) }
    }
    val boxTrackedVarietyStocks = trackedVarietyStocks.filter {
        it.varietyName != "Opening stock (unassigned)"
    }
    val openableVarietyStocks = trackedVarietyStocks.filter { it.sealedBoxes > 0 }
    val lowStockItems = boxTrackedVarietyStocks.filter { stock ->
        stock.sealedBoxes <= stock.lowStockLimit
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. HEADER (Namaste, Owner / Shop Name / Avatar DP)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val ownerName = currentUser?.name?.takeIf { it.isNotBlank() } ?: "Dipak"
                    Text(
                        text = "Namaste, $ownerName",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    val shopTitle = userProfile.shopName.ifBlank {
                        currentUser?.shopName?.takeIf { it.isNotBlank() } ?: "Refresh Soda"
                    }
                    Text(
                        text = shopTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }

                // Avatar DP Circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5C7A9))
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    val initials = currentUser?.name?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("")?.uppercase() ?: "DP"
                    Text(
                        text = initials.ifBlank { "DP" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }
        }

        // 2. HERO CARD (Forest Green #366A55 - Aaj ki bikri, Cash/UPI/Udhaar pills, Kharcha & Munafa)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF366A55)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Aaj ki bikri",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹" + totalSales.toInt(),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Pills: Cash, UPI, Udhaar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF2A5443),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Cash ₹" + cashSales.toInt(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF2A5443),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "UPI ₹" + upiSales.toInt(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF2A5443),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Udhaar ₹" + udharSales.toInt(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bottom Row: Kharcha & Munafa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kharcha ₹" + totalExpenses.toInt(),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Munafa ₹" + netProfit.toInt(),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. TOP SELLERS (2 Side-by-Side Cards: AAJ KA TOP SODA & AAJ KA TOP ICE-CREAM)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Soda Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "AAJ KA TOP SODA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val sodaParts = topSodaIce.first.split("(")
                        val sodaName = sodaParts.firstOrNull()?.trim() ?: "Jeera Soda"
                        val sodaCount = sodaParts.getOrNull(1)?.replace(")", "")?.trim() ?: "0 glass"
                        Text(
                            text = if (sodaName.isBlank() || sodaName == "—") "Jeera Soda" else sodaName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (sodaCount.isBlank() || sodaCount == "0 glass") "42 bike" else "$sodaCount bike",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Top Ice-cream Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "AAJ KA TOP ICE-CREAM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val iceParts = topSodaIce.second.split("(")
                        val iceName = iceParts.firstOrNull()?.trim() ?: "Hocco"
                        val iceCount = iceParts.getOrNull(1)?.replace(")", "")?.trim() ?: "0 pcs"
                        Text(
                            text = if (iceName.isBlank() || iceName == "—") "Hocco" else iceName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (iceCount.isBlank() || iceCount == "0 pcs") "18 bike" else "$iceCount bike",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // 4. STOCK ALERT CARD (Soft Peach/Pink #FEECE8 - Stock kam hai (ice-cream))
        item {
            val lowStockDesc = if (lowStockItems.isNotEmpty()) {
                lowStockItems.joinToString(" · ") { stock -> stock.companyName + " " + stock.sealedBoxes + " box" }
            } else {
                "Hocco 3 box · Vadilal 2 box"
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEECE8)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFCDAD4)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToInventory() }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Stock kam hai (ice-cream)",
                        color = Color(0xFFC2410C),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = lowStockDesc,
                        color = Color(0xFF7C2D12),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 5. QUICK ACTIONS (3 Buttons: Maal aaya, Kharcha, Din band)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Maal aaya
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showInwardDialog = true }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📦", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Maal aaya", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                }

                // 2. Kharcha
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToHisaab() }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👛", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Kharcha", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                }

                // 3. Din band
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToHisaab() }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📋", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Din band", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    }
                }
            }
        }

        // 6. BUSINESS ANALYTICS & CHARTS CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAnalytics() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF366A55).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertChart,
                                contentDescription = null,
                                tint = Color(0xFF366A55),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Analytics & Profit Charts",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Category Sales, Trends & Margin",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = "Open",
                        tint = Color(0xFF366A55),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 7. RECENT SALES LOG
        item {
            Text(
                text = "Recent Sales",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }
        if (sales.isEmpty()) {
            item {
                Text(
                    text = "No sales recorded yet today.",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }
        } else {
            items(sales.take(6)) { sale ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sale.itemsSummary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B),
                                maxLines = 1
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val modeColor = when (sale.paymentMode) {
                                    "CASH" -> Color(0xFF366A55)
                                    "UPI" -> Color(0xFF0284C7)
                                    else -> Color(0xFFD97706)
                                }
                                Text(
                                    text = sale.paymentMode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = modeColor
                                )
                                if (!sale.customerName.isNullOrBlank()) {
                                    Text(
                                        text = "• " + sale.customerName,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "₹" + sale.totalAmount.toInt(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF366A55)
                            )

                            // Digital WhatsApp Receipt Button
                            IconButton(
                                onClick = {
                                    val receipt = viewModel.generateBillReceiptText(sale)
                                    sendWhatsAppMessage(context, null, receipt)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = "Share",
                                    tint = Color(0xFF366A55),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Delete Sale Button
                            IconButton(
                                onClick = { viewModel.deleteSale(sale) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStockDialog && boxTrackedSubCategories.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showAddStockDialog = false },
            title = { Text("Add Box Stock", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Company:", fontSize = 12.sp, color = Color(0xFF64748B))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(boxTrackedSubCategories) { prod ->
                            FilterChip(
                                selected = selectedCompanyForStock?.id == prod.id,
                                onClick = { selectedCompanyForStock = prod },
                                label = { Text(prod.name + " (" + prod.categoryName + ")", fontSize = 11.sp) }
                            )
                        }
                    }
                    AppTextField(
                        value = stockVarietyName,
                        onValueChange = { stockVarietyName = it },
                        label = "Product / variety name"
                    )
                    AppTextField(
                        value = stockCountText,
                        onValueChange = { stockCountText = it },
                        label = "Boxes"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = stockCountText.toIntOrNull() ?: 0
                        val company = selectedCompanyForStock
                        if (count > 0 && company != null && stockVarietyName.isNotBlank()) {
                            viewModel.addIceCreamVarietyBoxes(
                                company.categoryName,
                                company.name,
                                stockVarietyName,
                                count
                            )
                            showAddStockDialog = false
                            selectedCompanyForStock = null
                            stockVarietyName = ""
                            stockCountText = "1"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF366A55))
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStockDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showOpenBoxDialog) {
        val availableProducts = openableVarietyStocks
        var selectedProduct by remember { mutableStateOf(availableProducts.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { showOpenBoxDialog = false },
            title = { Text("Open Box at Counter", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Box to open at Counter:", fontSize = 12.sp, color = Color(0xFF64748B))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableProducts) { prod ->
                            FilterChip(
                                selected = selectedProduct == prod,
                                onClick = { selectedProduct = prod },
                                label = { Text(prod.companyName + " • " + prod.varietyName + " (" + prod.sealedBoxes + " Left)", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedProduct != null) {
                            viewModel.openIceCreamVarietyBox(selectedProduct!!)
                            showOpenBoxDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF366A55))
                ) {
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
fun getCategoryFoodIcon(category: String, name: String): String {
    val lower = "$category $name".lowercase(java.util.Locale.getDefault())
    return when {
        lower.contains("ice") || lower.contains("cream") || lower.contains("kulfi") || lower.contains("cone") -> "🍨"
        lower.contains("soda") || lower.contains("cola") || lower.contains("jeera") || lower.contains("nimbu") -> "🥤"
        lower.contains("shake") || lower.contains("smoothie") || lower.contains("milk") -> "🧋"
        lower.contains("snack") || lower.contains("chips") || lower.contains("wafer") || lower.contains("tasty") -> "🍿"
        lower.contains("burger") || lower.contains("sandwich") -> "🍔"
        lower.contains("fruit") || lower.contains("juice") -> "🍹"
        lower.contains("fish") || lower.contains("salmon") || lower.contains("nonveg") -> "🍲"
        lower.contains("tea") || lower.contains("chai") || lower.contains("coffee") -> "☕"
        else -> "🍽️"
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun POSScreen(
    viewModel: ShopViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val khataList by viewModel.khataList.collectAsState()
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

    // Default category & selection
    var selectedCategoryName by remember { mutableStateOf("Ice-cream") }
    var selectedCompanyName by remember { mutableStateOf("Hocco") }
    var enteredAmountText by remember { mutableStateOf("120") }
    var paymentMode by remember { mutableStateOf("CASH") }
    var udharCustomerName by remember { mutableStateOf("") }
    var showAddCompanyDialog by remember { mutableStateOf(false) }
    var newCompanyNameInput by remember { mutableStateOf("") }

    // Ensure selectedCategoryName is valid
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && categories.none { it.name.equals(selectedCategoryName, ignoreCase = true) }) {
            selectedCategoryName = categories.first().name
        }
    }

    val selectedCategoryObj = categories.firstOrNull { it.name.equals(selectedCategoryName, ignoreCase = true) }
    val isBoxTracked = selectedCategoryObj?.trackBoxes == true

    // Companies or items for this category
    val companiesForCategory = subCategories.filter { it.categoryName.equals(selectedCategoryName, ignoreCase = true) }
    val itemsForCategory = products.filter { it.category.equals(selectedCategoryName, ignoreCase = true) }

    // Ensure company selection
    LaunchedEffect(selectedCategoryName, companiesForCategory, itemsForCategory) {
        if (isBoxTracked) {
            if (companiesForCategory.isNotEmpty() && companiesForCategory.none { it.name.equals(selectedCompanyName, ignoreCase = true) }) {
                selectedCompanyName = companiesForCategory.first().name
            } else if (companiesForCategory.isEmpty()) {
                selectedCompanyName = "Hocco"
            }
        } else {
            if (itemsForCategory.isNotEmpty() && itemsForCategory.none { it.name.equals(selectedCompanyName, ignoreCase = true) }) {
                selectedCompanyName = itemsForCategory.first().name
                enteredAmountText = itemsForCategory.first().price.toInt().toString()
            } else if (itemsForCategory.isEmpty()) {
                selectedCompanyName = "General"
            }
        }
    }

    fun onKeypadClick(key: String) {
        when (key) {
            "⌫" -> {
                if (enteredAmountText.isNotEmpty()) {
                    enteredAmountText = enteredAmountText.dropLast(1)
                    if (enteredAmountText.isEmpty()) enteredAmountText = "0"
                }
            }
            "00" -> {
                if (enteredAmountText != "0" && enteredAmountText.length < 6) {
                    enteredAmountText += "00"
                }
            }
            "0" -> {
                if (enteredAmountText != "0" && enteredAmountText.length < 6) {
                    enteredAmountText += "0"
                }
            }
            else -> {
                if (enteredAmountText == "0" || enteredAmountText.isEmpty()) {
                    enteredAmountText = key
                } else if (enteredAmountText.length < 6) {
                    enteredAmountText += key
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 5-SECOND UNDO BANNER
                if (lastSaleForUndo != null && undoSecondsLeft > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ Sale saved: ₹" + lastSaleForUndo!!.totalAmount.toInt() + " (" + lastSaleForUndo!!.paymentMode + ") • " + undoSecondsLeft + "s",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                                Text("Undo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 1. TOP HEADER: Back Button & Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Naya Sale",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                }

                // 2. CATEGORY SELECTOR PILLS (Horizontal Scroll)
                val allCategoryNames = if (categories.isNotEmpty()) {
                    categories.map { it.name }
                } else {
                    listOf("Soda", "Ice-cream", "Milkshake", "Cold Drink")
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allCategoryNames) { catName ->
                        val isSelected = catName.equals(selectedCategoryName, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFFF5C7A9) else Color.White,
                            border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable {
                                selectedCategoryName = catName
                            }
                        ) {
                            Text(
                                text = catName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                // 3. SUBHEADER: COMPANY CHUNO
                Text(
                    text = "COMPANY CHUNO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )

                // 4. COMPANY / ITEM CHIPS
                val chipList = if (isBoxTracked) {
                    val names = companiesForCategory.map { it.name }.toMutableList()
                    if (names.isEmpty()) {
                        names.addAll(listOf("Hocco", "Vadilal", "Hajoori", "Amul", "Other"))
                    }
                    names
                } else {
                    val names = itemsForCategory.map { it.name }.toMutableList()
                    if (names.isEmpty()) {
                        names.addAll(listOf("Jeera", "Lemon", "Cola", "Orange", "Blueberry"))
                    }
                    names
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chipList) { itemOrComp ->
                        val isSelected = itemOrComp.equals(selectedCompanyName, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFF5C7A9).copy(alpha = 0.35f) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFE8A87C) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.clickable {
                                selectedCompanyName = itemOrComp
                                val matchedItem = itemsForCategory.firstOrNull { it.name.equals(itemOrComp, ignoreCase = true) }
                                if (matchedItem != null && matchedItem.price > 0) {
                                    enteredAmountText = matchedItem.price.toInt().toString()
                                }
                            }
                        ) {
                            Text(
                                text = itemOrComp,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // + Naya Button
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                            modifier = Modifier.clickable { showAddCompanyDialog = true }
                        ) {
                            Text(
                                text = "+ Naya",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                // 5. DISPLAY BOX: Selected Name on Left, Large Price on Right
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCompanyName + " · " + selectedCategoryName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "₹" + enteredAmountText.ifBlank { "0" },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }

                // UDHAAR CUSTOMER NAME INPUT (Visible only if Udhaar is chosen)
                if (paymentMode == "UDHAR") {
                    AppTextField(
                        value = udharCustomerName,
                        onValueChange = { udharCustomerName = it },
                        label = "Customer Name (for Khata entry)",
                        placeholder = "Type name..."
                    )
                }
            }

            // BOTTOM SECTION: KEYPAD + PAYMENT MODE + SAVE BUTTON
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 6. 4x3 TOUCH NUMERIC KEYPAD
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("00", "0", "⌫")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    keypadRows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowKeys.forEach { key ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .clickable { onKeypadClick(key) }
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (key == "⌫") {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Backspace",
                                                tint = Color(0xFF1E293B),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. PAYMENT MODE CHIPS: Cash (Green), UPI, Udhaar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cash
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (paymentMode == "CASH") Color(0xFF366A55) else Color.White,
                        border = if (paymentMode == "CASH") null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { paymentMode = "CASH" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Cash",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (paymentMode == "CASH") Color.White else Color(0xFF1E293B)
                            )
                        }
                    }

                    // UPI
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (paymentMode == "UPI") Color(0xFF366A55) else Color.White,
                        border = if (paymentMode == "UPI") null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { paymentMode = "UPI" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "UPI",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (paymentMode == "UPI") Color.White else Color(0xFF1E293B)
                            )
                        }
                    }

                    // Udhaar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (paymentMode == "UDHAR") Color(0xFF366A55) else Color.White,
                        border = if (paymentMode == "UDHAR") null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable { paymentMode = "UDHAR" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Udhaar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (paymentMode == "UDHAR") Color.White else Color(0xFF1E293B)
                            )
                        }
                    }
                }

                // 8. FULL-WIDTH SALE SAVE BUTTON (Warm Peach #F5C7A9)
                val finalAmt = enteredAmountText.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        if (finalAmt > 0) {
                            if (paymentMode == "UDHAR" && udharCustomerName.isBlank()) {
                                Toast.makeText(context, "Please enter customer name for Udhaar", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.recordDirectSale(
                                category = selectedCategoryName,
                                itemName = selectedCompanyName,
                                amount = finalAmt,
                                paymentMode = paymentMode,
                                customerName = if (paymentMode == "UDHAR") udharCustomerName.trim() else null
                            )
                            Toast.makeText(context, "Sale Saved: ₹" + finalAmt.toInt(), Toast.LENGTH_SHORT).show()
                            enteredAmountText = "0"
                            if (paymentMode == "UDHAR") udharCustomerName = ""
                        } else {
                            Toast.makeText(context, "Please enter an amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF5C7A9),
                        contentColor = Color(0xFF1E293B)
                    )
                ) {
                    Text(
                        text = "Sale Save karo · ₹" + enteredAmountText.ifBlank { "0" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showAddCompanyDialog) {
        AlertDialog(
            onDismissRequest = { showAddCompanyDialog = false },
            title = { Text("Add New Company / Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add for category: $selectedCategoryName", fontSize = 12.sp, color = Color(0xFF64748B))
                    AppTextField(
                        value = newCompanyNameInput,
                        onValueChange = { newCompanyNameInput = it },
                        label = "Name",
                        placeholder = "e.g. Havmor, Mango..."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCompanyNameInput.trim()
                        if (trimmed.isNotBlank()) {
                            if (isBoxTracked) {
                                viewModel.addSubCategory(selectedCategoryName, trimmed, 0.0) {
                                    selectedCompanyName = trimmed
                                    newCompanyNameInput = ""
                                    showAddCompanyDialog = false
                                }
                            } else {
                                viewModel.addProduct(
                                    name = trimmed,
                                    category = selectedCategoryName,
                                    price = 20.0,
                                    costPrice = 10.0,
                                    isSoda = true
                                )
                                selectedCompanyName = trimmed
                                enteredAmountText = "20"
                                newCompanyNameInput = ""
                                showAddCompanyDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF366A55))
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCompanyDialog = false }) { Text("Cancel") }
            }
        )
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
                                                if (categories.firstOrNull { it.name.equals(item.category, ignoreCase = true) }?.trackBoxes == true) {
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
    val subCategories by viewModel.subCategories.collectAsState()
    val inwardCategories = categories.filter { it.trackBoxes }

    var selectedBrand by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var billNumber by remember { mutableStateOf("") }
    var billDate by remember { mutableStateOf(viewModel.todayDateString) }
    var paymentStatus by remember { mutableStateOf("PAID") }
    var paymentMode by remember { mutableStateOf("UPI") }
    var paidAmountText by remember { mutableStateOf("") }
    var customBillText by remember { mutableStateOf("") }
    var showPasteSection by remember { mutableStateOf(false) }
    var showAddCompany by remember { mutableStateOf(false) }
    var newCompanyName by remember { mutableStateOf("") }
    var addGstToCost by remember { mutableStateOf(false) }

    val draftItems = remember { mutableStateListOf<InwardBillItemDraft>() }
    val lineAmountTexts = remember { mutableStateMapOf<String, String>() }

    var isScanning by remember { mutableStateOf(false) }

    LaunchedEffect(inwardCategories) {
        if (inwardCategories.none { it.name.equals(selectedCategory, ignoreCase = true) }) {
            selectedCategory = inwardCategories.firstOrNull()?.name.orEmpty()
        }
    }

    val categoryCompanies = subCategories.filter {
        it.categoryName.equals(selectedCategory, ignoreCase = true)
    }

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
                categoryCompanies.map { it.name },
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
    }?.trackBoxes ?: false
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
                // 1. COMPANY SELECTOR
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Ice-cream company", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (inwardCategories.isEmpty()) {
                            Text("Create an ice-cream type in category management first.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        } else if (categoryCompanies.isEmpty()) {
                            Text("No company yet, add one.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(categoryCompanies) { company ->
                                    FilterChip(
                                        selected = selectedBrand.equals(company.name, ignoreCase = true),
                                        onClick = { selectedBrand = company.name },
                                        label = { Text(company.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                        if (inwardCategories.isNotEmpty()) {
                            TextButton(onClick = { showAddCompany = true }) { Text("+ Add company") }
                        }
                    }
                }

                // 2. ICE-CREAM CATEGORY SELECTOR
                if (inwardCategories.size > 1) item {
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
                                items(inwardCategories) { category ->
                                    FilterChip(
                                        selected = selectedCategory.equals(category.name, ignoreCase = true),
                                        onClick = {
                                            selectedCategory = category.name
                                            selectedBrand = ""
                                        },
                                        label = { Text(category.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
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
                                Text("Scanning bill with ML Kit OCR...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        if (trackBoxes) "📦 Invoice Items & Boxes (${draftItems.size} items):"
                        else "Invoice Items (${draftItems.size} items):",
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
                                    Text("Total Boxes:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text("$totalBoxes Boxes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Purchase Cost:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(
                                    "₹${String.format(Locale.getDefault(), "%.2f", totalCost)}",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Estimated Sales (MRP):", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(
                                    "₹${String.format(Locale.getDefault(), "%.2f", totalSelling)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Estimated Net Profit:", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    if (selectedBrand.isBlank() || selectedCategory.isBlank() || !trackBoxes) {
                        Toast.makeText(context, "Choose an ice-cream company and category.", Toast.LENGTH_SHORT).show()
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF366A55))
            ) {
                Text("Save Stock", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showAddCompany) {
        AlertDialog(
            onDismissRequest = { showAddCompany = false },
            title = { Text("Add ice-cream company") },
            text = {
                OutlinedTextField(
                    value = newCompanyName,
                    onValueChange = { newCompanyName = it },
                    label = { Text("Company name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val companyName = newCompanyName.trim()
                        if (companyName.isNotBlank() && selectedCategory.isNotBlank()) {
                            viewModel.addSubCategory(selectedCategory, companyName, 0.0) {
                                selectedBrand = it.name
                                newCompanyName = ""
                                showAddCompany = false
                            }
                        }
                    },
                    enabled = newCompanyName.isNotBlank() && selectedCategory.isNotBlank()
                ) {
                    Text("Add company")
                }
            },
            dismissButton = { TextButton(onClick = { showAddCompany = false }) { Text("Cancel") } }
        )
    }
}

// ==============================================================================
// 4. GODOWN BOX-ONLY STOCK TRACKER SCREEN
// ==============================================================================
@Composable
fun InventoryScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val varietyStocks by viewModel.iceCreamVarietyStock.collectAsState()
    val products by viewModel.products.collectAsState()
    val iceCreamProfit by viewModel.iceCreamProfitStats.collectAsState()

    var activeTab by remember { mutableStateOf("ICE_CREAM") }
    var showInwardDialog by remember { mutableStateOf(false) }
    var showAddCompanyDialog by remember { mutableStateOf(false) }
    var newCompanyNameInput by remember { mutableStateOf("") }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProductNameInput by remember { mutableStateOf("") }
    var newProductPriceInput by remember { mutableStateOf("20") }

    val boxTrackedCategories = categories.filter { it.trackBoxes }
    val defaultIceCreamCategoryName = boxTrackedCategories.firstOrNull()?.name ?: "Ice-cream"

    val iceCreamCompanies = subCategories.filter { sub ->
        boxTrackedCategories.any { it.name.equals(sub.categoryName, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. TOP HEADER (Screen 3: Ice-cream Stock & Company ke hisaab se box)
        item {
            Column(modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) {
                Text(
                    text = "Ice-cream Stock",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = "Company ke hisaab se box",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
        }

        // 2. PRIMARY BUTTON (Forest Green #366A55: 📦 Maal aaya · Bill scan karo)
        item {
            Button(
                onClick = { showInwardDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF366A55),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "📦 Maal aaya · Bill scan karo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // TABS: ICE-CREAM BOXES vs SODA & ALL ITEMS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (activeTab == "ICE_CREAM") Color(0xFFF5C7A9) else Color.White,
                    border = if (activeTab == "ICE_CREAM") null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeTab = "ICE_CREAM" }
                ) {
                    Text(
                        text = "🍨 Ice-cream Boxes",
                        fontSize = 12.sp,
                        fontWeight = if (activeTab == "ICE_CREAM") FontWeight.Bold else FontWeight.Medium,
                        color = if (activeTab == "ICE_CREAM") Color(0xFF1E293B) else Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (activeTab == "SODA") Color(0xFFF5C7A9) else Color.White,
                    border = if (activeTab == "SODA") null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeTab = "SODA" }
                ) {
                    Text(
                        text = "🥤 Soda & Other Items",
                        fontSize = 12.sp,
                        fontWeight = if (activeTab == "SODA") FontWeight.Bold else FontWeight.Medium,
                        color = if (activeTab == "SODA") Color(0xFF1E293B) else Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }

        if (activeTab == "ICE_CREAM") {
            // Display default 3 companies (Hocco, Vadilal, Hajoori) or dynamically from DB
            val companiesToDisplay = if (iceCreamCompanies.isNotEmpty()) {
                iceCreamCompanies
            } else {
                listOf(
                    SubCategory(id = "comp_hocco", name = "Hocco", categoryName = defaultIceCreamCategoryName, sealedBoxes = 3, costPrice = 0.0),
                    SubCategory(id = "comp_vadilal", name = "Vadilal", categoryName = defaultIceCreamCategoryName, sealedBoxes = 2, costPrice = 0.0),
                    SubCategory(id = "comp_hajoori", name = "Hajoori", categoryName = defaultIceCreamCategoryName, sealedBoxes = 9, costPrice = 0.0)
                )
            }

            items(companiesToDisplay) { company ->
                val boxes = company.sealedBoxes
                val lowLimit = 5
                val isLow = boxes <= lowLimit

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header Row: Company Name + Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = company.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )

                            // Badge: Kam hai / Theek
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isLow) Color(0xFFFEECE8) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = if (isLow) "Kam hai" else "Theek",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLow) Color(0xFFC2410C) else Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Count row: 3 box · alert 5 se kam par
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "" + boxes,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLow) Color(0xFFC2410C) else Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "box · alert " + lowLimit + " se kam par",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Progress Bar Line
                        val progressFraction = (boxes.toFloat() / (lowLimit * 2).toFloat()).coerceIn(0.08f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isLow) Color(0xFFC2410C) else Color(0xFF366A55))
                            )
                        }

                        // Controls Row: +1 Box, -1 Open
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.addIceCreamVarietyBoxes(
                                        company.categoryName,
                                        company.name,
                                        "Assorted Variety",
                                        1
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF366A55).copy(alpha = 0.12f),
                                    contentColor = Color(0xFF366A55)
                                )
                            ) {
                                Text("+1 Box", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            FilledTonalButton(
                                onClick = {
                                    val stock = varietyStocks.firstOrNull {
                                        it.categoryName.equals(company.categoryName, ignoreCase = true) &&
                                        it.companyName.equals(company.name, ignoreCase = true)
                                    }
                                    if (stock != null) {
                                        viewModel.openIceCreamVarietyBox(stock)
                                    } else {
                                        Toast.makeText(context, "Box opened at counter", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                enabled = boxes > 0,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFF1F5F9),
                                    contentColor = Color(0xFF475569)
                                )
                            ) {
                                Text("-1 Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. BOTTOM DASHED BUTTON: + Nayi company jodo
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddCompanyDialog = true }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Nayi company jodo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }
                }
            }
        } else {
            // SODA & ALL ITEMS LIST TAB
            val sodaItems = products.filter {
                categories.none { cat -> cat.trackBoxes && cat.name.equals(it.category, ignoreCase = true) }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Soda & Counter Products (" + sodaItems.size + ")",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Button(
                        onClick = { showAddProductDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF366A55)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("+ Add Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(sodaItems) { product ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = product.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = product.category + " • Rate: ₹" + product.price.toInt(),
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteProduct(product.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ADD NEW COMPANY DIALOG
    if (showAddCompanyDialog) {
        AlertDialog(
            onDismissRequest = { showAddCompanyDialog = false },
            title = { Text("Add New Company", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add ice-cream company (e.g. Amul, Havmor):", fontSize = 12.sp, color = Color(0xFF64748B))
                    AppTextField(
                        value = newCompanyNameInput,
                        onValueChange = { newCompanyNameInput = it },
                        label = "Company Name",
                        placeholder = "e.g. Amul, Dinshaw's..."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCompanyNameInput.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.addSubCategory(defaultIceCreamCategoryName, trimmed, 0.0) {
                                newCompanyNameInput = ""
                                showAddCompanyDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF366A55))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCompanyDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ADD NEW SODA/COUNTER PRODUCT DIALOG
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Add New Soda Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(
                        value = newProductNameInput,
                        onValueChange = { newProductNameInput = it },
                        label = "Item Name",
                        placeholder = "e.g. Jeera Masala, Nimbu Soda..."
                    )
                    AppTextField(
                        value = newProductPriceInput,
                        onValueChange = { newProductPriceInput = it },
                        label = "Selling Price (₹)"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newProductNameInput.trim()
                        val price = newProductPriceInput.toDoubleOrNull() ?: 20.0
                        if (name.isNotBlank()) {
                            viewModel.addProduct(
                                name = name,
                                category = "Soda",
                                price = price,
                                costPrice = price / 2,
                                isSoda = true
                            )
                            newProductNameInput = ""
                            newProductPriceInput = "20"
                            showAddProductDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF366A55))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("Cancel") }
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
                                    val msg = "Hello ${first.name},\nYour outstanding balance at Refresh Soda is ₹${first.outstandingDebt.toInt()}. Please clear at your convenience. Thank you!"
                                    sendWhatsAppMessage(context, first.phone, msg)
                                } else {
                                    Toast.makeText(context, "$debtorCount customers with pending dues. Tap Send on card to message.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📢 Due Payment Reminders ($debtorCount customers)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                    Text("⚠️ 7+ days old", color = Color(0xFFDC2626), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
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
        "❄️ Ice",
        "🍋 Lemons & Flavors",
        "🥤 Syrup & CO2 Gas",
        "🥛 Milk & Shake Supplies",
        "🧻 Cups, Glasses & Straws",
        "💡 Electricity Bill",
        "🏪 Shop Rent",
        "👷 Staff Salary / Labor",
        "🔧 Machine Repair",
        "📦 Other Miscellaneous"
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
                        Text("Today's Expenses", fontSize = 12.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
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
                Text("Expense Breakdown:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            Text("Recent Expenses Log", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        if (expenses.isEmpty()) {
            item {
                Text("No expenses recorded today.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
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
            title = { Text("+ Record Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title / Note") }, singleLine = true)
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount (₹)") }, singleLine = true)

                    Text("Payment Source:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paymentMode == "CASH",
                            onClick = { paymentMode = "CASH" },
                            label = { Text("💵 Cash Galla") }
                        )
                        FilterChip(
                            selected = paymentMode == "UPI",
                            onClick = { paymentMode = "UPI" },
                            label = { Text("📱 Bank / UPI") }
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
                    Text("Save")
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
                    Text("Total Supplier Outstanding", color = Color(0xFFFECDD3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${totalDebt.toInt()} Due", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total balance payable to suppliers.", color = Color(0xFFFEE2E2), fontSize = 11.sp)
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
                        "No supplier bills added yet.\nIn the Stock screen, tap 'Maal aaya' to record supplier invoices.",
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
                }?.trackBoxes ?: false

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
                                    if (trackBoxes) "Total Bill: ₹${purchase.totalAmount.toInt()} (${purchase.totalBoxes} Boxes)"
                                    else "Total Bill: ₹${purchase.totalAmount.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!isPaid) {
                                    Text("Balance: ₹${balance.toInt()} pending", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
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
                var subCategoryName by remember(cat.id) { mutableStateOf("") }
                var subCategoryBoxes by remember(cat.id) { mutableStateOf("0") }
                var subCategoryLimit by remember(cat.id) { mutableStateOf("2") }
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
                                Text(
                                    if (cat.trackBoxes) "${categorySubCategories.size} companies" else "$count items attached",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (!cat.trackBoxes) {
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
                            Text("Ice-cream type (company + box stock)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = cat.trackBoxes,
                                onCheckedChange = { viewModel.updateCategory(cat.copy(trackBoxes = it)) }
                            )
                        }
                        Text(if (cat.trackBoxes) "Companies" else "Items (managed below)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (cat.trackBoxes) {
                            categorySubCategories.forEach { subCategory ->
                                var boxesText by remember(subCategory.id, subCategory.sealedBoxes) {
                                    mutableStateOf(subCategory.sealedBoxes.toString())
                                }
                                var limitText by remember(subCategory.id, subCategory.lowStockLimit) {
                                    mutableStateOf(subCategory.lowStockLimit.toString())
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${subCategory.name} • ${subCategory.sealedBoxes} box (limit ${subCategory.lowStockLimit})",
                                            modifier = Modifier.weight(1f),
                                            fontSize = 12.sp
                                        )
                                        IconButton(onClick = { viewModel.deleteSubCategory(subCategory) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete company", tint = Color(0xFFDC2626))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = boxesText,
                                            onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) boxesText = it },
                                            label = { Text("Current boxes") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = limitText,
                                            onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) limitText = it },
                                            label = { Text("Low stock limit") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = {
                                            viewModel.updateSubCategory(
                                                subCategory.copy(
                                                    sealedBoxes = boxesText.toIntOrNull() ?: subCategory.sealedBoxes,
                                                    lowStockLimit = limitText.toIntOrNull() ?: subCategory.lowStockLimit
                                                )
                                            )
                                        }) { Text("Save") }
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
                                    label = { Text("Company name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = subCategoryBoxes,
                                    onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) subCategoryBoxes = it },
                                    label = { Text("Current boxes") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = subCategoryLimit,
                                    onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) subCategoryLimit = it },
                                    label = { Text("Low stock limit") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            TextButton(
                                onClick = {
                                    viewModel.addSubCategory(
                                        cat.name,
                                        subCategoryName,
                                        0.0,
                                        sealedBoxes = subCategoryBoxes.toIntOrNull() ?: 0,
                                        lowStockLimit = subCategoryLimit.toIntOrNull() ?: 2
                                    )
                                    subCategoryName = ""
                                    subCategoryBoxes = "0"
                                    subCategoryLimit = "2"
                                },
                                enabled = subCategoryName.isNotBlank()
                            ) {
                                Text("+ Add company")
                            }
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
                val itemModeProducts = products.filter { product ->
                    categories.none { it.trackBoxes && it.name.equals(product.category, ignoreCase = true) }
                }
                Text("All Items (${itemModeProducts.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (categories.any { !it.trackBoxes }) {
                    Button(
                        onClick = {
                            selectedCatForProduct = categories.firstOrNull { !it.trackBoxes }?.name.orEmpty()
                            showAddProductDialog = true
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("+ Add Item") }
                }
            }
        }

        val itemModeProducts = products.filter { product ->
            categories.none { it.trackBoxes && it.name.equals(product.category, ignoreCase = true) }
        }
        if (itemModeProducts.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                    Text("No items added yet. Click '+ Add Item' or 'Smart Bulk Import'.", modifier = Modifier.padding(14.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(itemModeProducts) { prod ->
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
                                "Category: ${prod.category}",
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
        var priceText by remember { mutableStateOf("") }
        val trackBoxes = categories.firstOrNull {
            it.name.equals(category, ignoreCase = true)
        }?.trackBoxes ?: false

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("+ Add Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (trackBoxes) {
                        Text("Add an ice-cream company in category management instead of adding an item.")
                    }
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
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = priceText.toDoubleOrNull() ?: 0.0
                    if (!trackBoxes && name.isNotBlank() && category.isNotBlank() && p > 0) {
                        if (categories.none { it.name.equals(category.trim(), ignoreCase = true) }) {
                            viewModel.addCategory(category.trim())
                        }
                        viewModel.addProduct(name, category, "", p, 0, null)
                        showAddProductDialog = false
                    }
                }, enabled = !trackBoxes) {
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
        var priceText by remember(product.id) { mutableStateOf(product.price.toString()) }
        AlertDialog(
            onDismissRequest = { productBeingEdited = null },
            title = { Text("Edit Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item name") }, singleLine = true)
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price (₹)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val price = priceText.toDoubleOrNull()
                    if (name.isNotBlank() && price != null && price > 0) {
                        viewModel.updateProduct(
                            product.copy(
                                name = name.trim(),
                                price = price,
                                brand = "",
                                sealedBoxes = 0,
                                lowStockLimit = null
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
