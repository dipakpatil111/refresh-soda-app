package com.refreshsoda.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.data.model.CustomerKhata
import com.refreshsoda.app.data.model.KhataRepayment
import com.refreshsoda.app.data.model.KhataCredit
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
import com.refreshsoda.app.data.model.Offer
import com.refreshsoda.app.data.model.IceCreamVarietyStock
import com.refreshsoda.app.data.model.VarietyInsightsItem
import com.refreshsoda.app.data.model.CompanyInsightsItem
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import com.refreshsoda.app.ui.viewmodel.InwardRequest
import com.refreshsoda.app.ui.viewmodel.MAX_INWARD_QTY
import com.refreshsoda.app.ui.viewmodel.UNASSIGNED_VARIETY
import com.refreshsoda.app.ui.viewmodel.loadPurchaseItems
import com.refreshsoda.app.ui.viewmodel.saveInwardPurchase
import com.refreshsoda.app.ui.viewmodel.saveSupplier
import com.refreshsoda.app.util.BillImages
import com.refreshsoda.app.util.ReceiptPdf
import com.refreshsoda.app.util.ShareUtils
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import com.refreshsoda.app.ui.theme.appColors

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
// APP UI UTILITIES: CLEAN, NON-OVERFLOWING TEXT FIELDS & FUZZY MATCHER
// ==============================================================================
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        label = label?.let { { Text(it, fontSize = 11.sp, maxLines = 1) } },
        placeholder = placeholder?.let { { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) } },
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        isError = isError,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

/** Older expense records were saved with Hindi category labels; show them with the English label. */
private val LEGACY_EXPENSE_LABELS = mapOf(
    "❄️ बर्फ (Ice)" to "❄️ Ice",
    "🍋 नींबू (Lemons)" to "🍋 Lemons",
    "🥤 सोडे का माल व गैस (Syrup & Gas)" to "🥤 Syrup & Gas",
    "🥛 दूध व शेक सामग्री (Milk & Shake)" to "🥛 Milk & Shake",
    "🧻 ग्लास व स्ट्रॉ (Glasses & Straws)" to "🧻 Glasses & Straws",
    "💡 बिजली बिल (Electricity Bill)" to "💡 Electricity Bill",
    "🏪 दुकान का किराया (Shop Rent)" to "🏪 Shop Rent",
    "👷 कारीगर / मज़दूरी (Labor / Salary)" to "👷 Labor / Salary",
    "🔧 मशीन मेंटेनेंस (Machine Repair)" to "🔧 Machine Repair",
    "📦 अन्य खर्च (Other Miscellaneous)" to "📦 Other Miscellaneous"
)

fun expenseCategoryLabel(stored: String): String = LEGACY_EXPENSE_LABELS[stored] ?: stored

fun levenshteinDistance(s1: String, s2: String): Int {
    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
    for (i in 0..s1.length) dp[i][0] = i
    for (j in 0..s2.length) dp[0][j] = j
    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
        }
    }
    return dp[s1.length][s2.length]
}

fun findBestMatchingVariety(scannedText: String, candidateVarieties: List<String>): Pair<String?, Float> {
    if (candidateVarieties.isEmpty() || scannedText.isBlank()) return Pair(null, 0f)
    val cleanScanned = scannedText.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]"), "")
    var bestMatch: String? = null
    var bestScore = 0f

    for (candidate in candidateVarieties) {
        val cleanCandidate = candidate.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]"), "")
        if (cleanCandidate == cleanScanned) {
            return Pair(candidate, 1.0f)
        }
        if (cleanScanned.contains(cleanCandidate) || cleanCandidate.contains(cleanScanned)) {
            val score = cleanCandidate.length.toFloat() / maxOf(cleanScanned.length, cleanCandidate.length).toFloat()
            if (score > bestScore) {
                bestScore = score
                bestMatch = candidate
            }
        }
        val dist = levenshteinDistance(cleanScanned, cleanCandidate)
        val maxLen = maxOf(cleanScanned.length, cleanCandidate.length)
        if (maxLen > 0) {
            val ratio = 1f - (dist.toFloat() / maxLen.toFloat())
            if (ratio > bestScore) {
                bestScore = ratio
                bestMatch = candidate
            }
        }
    }
    return Pair(bestMatch, bestScore)
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
    val isOwner by viewModel.isOwner.collectAsState()

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
                                color = if (currentUser?.role == "OWNER") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.appColors.warningContainer,
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
                                        TextButton(onClick = { viewModel.logout() }) { // switching user always goes through the password screen
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

        // 4. CLOUD BACKUP & RESTORE SECTION (owner only)
        if (isOwner) item {
            Text(
                "☁️ Cloud Backup & Data Recovery",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isOwner) item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Keep your shop data safe, or restore it instantly on a new phone:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                            Text("Cloud Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

        // 4b. OWNER TOOLS: local backup, CSV export, bulk prices, offers
        if (isOwner) item { OwnerToolsSection(viewModel = viewModel) }

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
    onNavigateToMaalAavak: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToPurchaseReport: () -> Unit = {}
) {
    val context = LocalContext.current
    val totalSales by viewModel.totalSalesAmount.collectAsState()
    val cashSales by viewModel.cashSalesAmount.collectAsState()
    val upiSales by viewModel.upiSalesAmount.collectAsState()
    val udharSales by viewModel.udharSalesAmount.collectAsState()
    val totalExpenses by viewModel.totalExpensesAmount.collectAsState()
    val netProfit by viewModel.todayNetProfit.collectAsState()
    val dashboardFilter by viewModel.dashboardFilter.collectAsState()
    val filteredSummary by viewModel.filteredSummary.collectAsState()
    val todaySummary by viewModel.todaySummary.collectAsState()
    // Category sales / top sellers follow the picked dates when a filter is on, otherwise today.
    val activeSummary = filteredSummary ?: todaySummary
    val categorySalesMap = activeSummary.categorySales
    val topItem = activeSummary.topItem
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val varietyStocks by viewModel.iceCreamVarietyStock.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val topSodaIce = Pair(activeSummary.topSoda, activeSummary.topIceCream)

    var showInwardDialog by remember { mutableStateOf(false) }
    var selectedSaleForReceipt by remember { mutableStateOf<SaleTransaction?>(null) }

    val boxTrackedSubCategories = subCategories.filter { subCategory ->
        categories.any { it.trackBoxes && it.name.equals(subCategory.categoryName, ignoreCase = true) }
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.heroTeal),
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
                            color = MaterialTheme.appColors.onHeroMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(viewModel.todayDateString, color = MaterialTheme.appColors.onHero.copy(alpha = 0.8f), fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${totalSales.toInt()}", color = MaterialTheme.appColors.onHero, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)

                    Spacer(modifier = Modifier.height(8.dp))

                    // Net Profit & Expense Pill Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.appColors.onHero.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Shop Expenses", color = MaterialTheme.appColors.onHero.copy(alpha = 0.8f), fontSize = 10.sp)
                                Text("₹${totalExpenses.toInt()}", color = MaterialTheme.appColors.heroNegative, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(viewModel.getString("net_profit"), color = MaterialTheme.appColors.onHero.copy(alpha = 0.8f), fontSize = 10.sp)
                                if (currentUser?.role == "STAFF") {
                                    Text("₹ ***", color = MaterialTheme.appColors.onHero.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                } else {
                                    Text("₹${netProfit.toInt()}", color = if (netProfit >= 0) MaterialTheme.appColors.heroPositive else MaterialTheme.appColors.heroNegative, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.appColors.onHero.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(viewModel.getString("cash_galla"), color = MaterialTheme.appColors.onHeroMuted, fontSize = 11.sp)
                            Text("₹${cashSales.toInt()}", color = MaterialTheme.appColors.onHero, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text(viewModel.getString("upi_paytm"), color = MaterialTheme.appColors.onHeroMuted, fontSize = 11.sp)
                            Text("₹${upiSales.toInt()}", color = MaterialTheme.appColors.onHero, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Column {
                            Text(viewModel.getString("udhar_khata"), color = MaterialTheme.appColors.onHeroMuted, fontSize = 11.sp)
                            Text("₹${udharSales.toInt()}", color = MaterialTheme.appColors.heroCaution, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.success, contentColor = MaterialTheme.appColors.onSuccess),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(viewModel.getString("quick_new_sale"), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Button(
                    onClick = { onNavigateToMaalAavak() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(viewModel.getString("inward_stock_btn"), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        // ANALYTICS + COMPANY PURCHASE REPORT (OWNER ONLY)
        if (currentUser?.role != "STAFF") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToAnalytics,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Analytics", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onNavigateToPurchaseReport,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Company Report", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }

        // THIS MONTH'S BUSINESS
        item { MonthSummaryCard(viewModel = viewModel, hideProfit = currentUser?.role == "STAFF") }

        // DATE FILTER (Yesterday / Last 7 days / custom range)
        item { DashboardDateFilter(viewModel = viewModel, hideProfit = currentUser?.role == "STAFF") }

        item { AlertsCard(viewModel = viewModel, includeLowStock = false) }

        // CATEGORY-WISE SALES BREAKDOWN
        if (categorySalesMap.isNotEmpty()) {
            item {
                Text(
                    if (dashboardFilter != null) "Category-wise Sales (${dashboardFilter?.label})" else viewModel.getString("category_sales"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
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
                Text(
                    if (dashboardFilter != null) "Top Selling Items (${dashboardFilter?.label})" else viewModel.getString("top_items"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
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
                    Text(if (dashboardFilter != null) "📊 Top Sellers (${dashboardFilter?.label}):" else "📊 Today's Top Sellers:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
        if (boxTrackedSubCategories.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(viewModel.getString("godown_box_status"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    val totalBoxes = boxTrackedSubCategories.sumOf { it.sealedBoxes }
                    Text("Total: $totalBoxes Box", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(boxTrackedSubCategories) { subCategory ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.width(115.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(subCategory.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "${subCategory.sealedBoxes} Box",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (subCategory.sealedBoxes <= subCategory.lowStockLimit) MaterialTheme.appColors.danger else MaterialTheme.colorScheme.primary
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
                                    "CASH" -> MaterialTheme.appColors.success
                                    "UPI" -> MaterialTheme.appColors.info
                                    else -> MaterialTheme.appColors.warning
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
                                Icon(Icons.Default.ReceiptLong, contentDescription = "Share Receipt", tint = MaterialTheme.appColors.success, modifier = Modifier.size(18.dp))
                            }

                            // PDF receipt (opens any app: WhatsApp, print, Drive ...)
                            IconButton(
                                onClick = {
                                    try {
                                        val shop = viewModel.userProfile.value
                                        val pdf = ReceiptPdf.create(context, shop.shopName, shop.address, shop.phone, sale)
                                        ShareUtils.shareFile(context, pdf, "application/pdf", "Share bill PDF")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not create PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Receipt", tint = MaterialTheme.appColors.info, modifier = Modifier.size(18.dp))
                            }

                            // Delete Sale Button
                            IconButton(
                                onClick = { viewModel.deleteSale(sale) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.appColors.danger, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInwardDialog) {
        InwardBillDialog(viewModel = viewModel, onDismiss = { showInwardDialog = false })
    }
}

// ==============================================================================
// 3. POS SALES ENTRY SCREEN
// ==============================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun POSScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val khataList by viewModel.khataList.collectAsState()
    val discount by viewModel.discount.collectAsState()
    val topFavorites by viewModel.topFavoriteProducts.collectAsState(initial = emptyList())
    val lastSaleForUndo by viewModel.lastSaleForUndo.collectAsState()

    val shopProfile by viewModel.userProfile.collectAsState()
    var showUpiQr by remember { mutableStateOf(false) }
    val cartRawTotal = cart.sumOf { it.total }
    if (showUpiQr) {
        UpiQrDialog(
            upiId = shopProfile.upiId,
            shopName = shopProfile.shopName,
            amount = (cartRawTotal - discount).coerceAtLeast(0.0),
            onPaid = {
                showUpiQr = false
                viewModel.completeOrder("UPI")
            },
            onDismiss = { showUpiQr = false }
        )
    }

    var undoSecondsLeft by remember { mutableStateOf(0) }
    LaunchedEffect(lastSaleForUndo?.id) {
        val sale = lastSaleForUndo ?: return@LaunchedEffect
        while (true) {
            val remainingMillis = (sale.timestamp + 5_000L - System.currentTimeMillis()).coerceAtLeast(0L)
            undoSecondsLeft = ((remainingMillis + 999L) / 1_000L).toInt()
            if (remainingMillis == 0L) break
            kotlinx.coroutines.delay(minOf(remainingMillis, 1_000L))
        }
    }

    var selectedCategoryName by remember { mutableStateOf("All") }
    var selectedCompany by remember { mutableStateOf<SubCategory?>(null) }
    var companyAmountText by remember { mutableStateOf("") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var posProductActions by remember { mutableStateOf<Product?>(null) }
    var posProductToEdit by remember { mutableStateOf<Product?>(null) }
    var posProductToDelete by remember { mutableStateOf<Product?>(null) }
    var showSmartImportDialog by remember { mutableStateOf(false) }
    var showUdharDialog by remember { mutableStateOf(false) }
    var udharCustomerName by remember { mutableStateOf("") }
    var showEditItemDialog by remember { mutableStateOf<Int?>(null) }

    val rawTotal = cart.sumOf { it.total }
    val finalTotal = (rawTotal - discount).coerceAtLeast(0.0)
    val itemModeProducts = products.filter { product ->
        categories.none { it.trackBoxes && it.name.equals(product.category, ignoreCase = true) }
    }
    val itemModeFavorites = topFavorites.filter { product ->
        categories.none { it.trackBoxes && it.name.equals(product.category, ignoreCase = true) }
    }
    val selectedCategoryIsIceCream = categories.firstOrNull {
        it.name.equals(selectedCategoryName, ignoreCase = true)
    }?.trackBoxes == true
    val selectedItemFavorites = itemModeFavorites.filter { product ->
        selectedCategoryName == "All" || product.category.equals(selectedCategoryName, ignoreCase = true)
    }
    val selectedItemProducts = itemModeProducts.filter { product ->
        selectedCategoryName == "All" || product.category.equals(selectedCategoryName, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // 5-SECOND SALE UNDO BANNER
        if (lastSaleForUndo != null && undoSecondsLeft > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
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
                        Column {
                            Text("Sale saved • ₹${lastSaleForUndo!!.totalAmount.toInt()}", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Undo available for ${undoSecondsLeft}s", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f), fontSize = 10.sp)
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.undoLastSale { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.danger, contentColor = MaterialTheme.appColors.onDanger),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Undo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            color = MaterialTheme.appColors.danger,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearCart() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (cart.isEmpty()) {
                    Text(viewModel.getString("cart_empty_hint"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 132.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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

                if (cart.isNotEmpty()) {
                    OfferChips(viewModel = viewModel, rawTotal = cartRawTotal, discount = discount)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (discount > 0) {
                            Text("Discount: -₹${discount.toInt()}", fontSize = 10.sp, color = MaterialTheme.appColors.danger, fontWeight = FontWeight.Bold)
                        }
                        Text("Total: ₹${finalTotal.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { viewModel.completeOrder("CASH") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.success, contentColor = MaterialTheme.appColors.onSuccess),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            enabled = cart.isNotEmpty()
                        ) {
                            Text(viewModel.getString("btn_cash"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (shopProfile.upiId.isBlank()) {
                                    Toast.makeText(context, "Add your UPI ID in Settings to show a payment QR for the bill amount.", Toast.LENGTH_SHORT).show()
                                    viewModel.completeOrder("UPI")
                                } else {
                                    showUpiQr = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.info, contentColor = MaterialTheme.appColors.onInfo),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            enabled = cart.isNotEmpty()
                        ) {
                            Text(viewModel.getString("btn_upi"), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showUdharDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.warning, contentColor = MaterialTheme.appColors.onWarning),
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
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
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
                    label = { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                )
            }
        }

        // TOP 8 FAVOURITES (QUICK 1-TAP COUNTER)
        if (!selectedCategoryIsIceCream &&
            (selectedItemFavorites.isNotEmpty() || selectedItemProducts.isNotEmpty())
        ) {
            val favsToShow = if (selectedItemFavorites.isNotEmpty()) selectedItemFavorites.take(8) else selectedItemProducts.take(6)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                    Text("⭐ Best Sellers (1-tap +1):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryName == "All",
                    onClick = {
                        selectedCategoryName = "All"
                        selectedCompany = null
                        companyAmountText = ""
                    },
                    label = { Text("All", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategoryName == cat.name,
                    onClick = {
                        selectedCategoryName = cat.name
                        selectedCompany = null
                        companyAmountText = ""
                    },
                    label = { Text(cat.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
                FilledTonalButton(
                    onClick = { showSmartImportDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Import", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                FilledTonalButton(
                    onClick = { showAddCategoryDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Cat", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Button(
                    onClick = { showAddProductDialog = true },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Item", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
            val companyCategories = categories.filter { category ->
                category.trackBoxes &&
                    (selectedCategoryName == "All" || category.name.equals(selectedCategoryName, ignoreCase = true))
            }
            val itemProducts = selectedItemProducts
            Column(modifier = Modifier.fillMaxWidth()) {
                companyCategories.forEach { category ->
                    val companies = subCategories.filter {
                        it.categoryName.equals(category.name, ignoreCase = true)
                    }
                    Text(
                        "${category.name} • Company",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    if (companies.isEmpty()) {
                        Text(
                            "Add companies in category management.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(companies) { company ->
                                FilterChip(
                                    selected = selectedCompany?.id == company.id,
                                    onClick = {
                                        selectedCompany = company
                                        companyAmountText = company.defaultPrice
                                            .takeIf { it > 0.0 }
                                            ?.toString()
                                            .orEmpty()
                                    },
                                    label = { Text(company.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }

                selectedCompany?.let { company ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = companyAmountText,
                            onValueChange = { companyAmountText = it },
                            label = { Text("Amount (₹)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val amount = companyAmountText.toDoubleOrNull()
                                if (amount != null && amount > 0) {
                                    viewModel.addToCart(
                                        Product(
                                            id = "company-${company.id}-${UUID.randomUUID()}",
                                            name = company.name,
                                            category = company.categoryName,
                                            brand = company.name,
                                            price = amount
                                        )
                                    )
                                    selectedCompany = null
                                    companyAmountText = ""
                                }
                            },
                            enabled = companyAmountText.toDoubleOrNull()?.let { it > 0 } == true
                        ) { Text("Add to cart") }
                    }
                }

                if (itemProducts.isEmpty()) {
                    if (companyCategories.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("No items in this category yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (selectedCategoryName != "All" &&
                                    categories.firstOrNull { it.name.equals(selectedCategoryName, ignoreCase = true) }?.trackBoxes != true
                                ) {
                                    Button(onClick = { showAddProductDialog = true }) { Text("+ Add Item Here") }
                                }
                            }
                        }
                    }
                } else {
                    val gridRows = (itemProducts.size + 2) / 3
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((gridRows * 98 + (gridRows - 1).coerceAtLeast(0) * 8 + 16).dp)
                    ) {
                        items(itemProducts) { product ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(98.dp)
                                    .combinedClickable(
                                        onClick = { viewModel.addToCart(product) },
                                        onLongClick = { posProductActions = product }
                                    ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            product.category.uppercase(),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Edit or delete item",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { posProductActions = product }
                                        )
                                    }
                                    Text(product.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, textAlign = TextAlign.Center)
                                    Text("₹${product.price.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                }
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

    posProductActions?.let { product ->
        AlertDialog(
            onDismissRequest = { posProductActions = null },
            title = { Text(product.name, fontWeight = FontWeight.Bold) },
            text = { Text("Category: ${product.category}  •  ₹${product.price.toInt()}", fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = { posProductToEdit = product; posProductActions = null }) { Text("Edit") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { posProductActions = null }) { Text("Cancel") }
                    TextButton(
                        onClick = { posProductToDelete = product; posProductActions = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.appColors.danger)
                    ) { Text("Delete") }
                }
            }
        )
    }
    posProductToEdit?.let { product ->
        EditProductDialog(viewModel = viewModel, product = product, onDismiss = { posProductToEdit = null })
    }
    posProductToDelete?.let { product ->
        DeleteProductDialog(viewModel = viewModel, product = product, onDismiss = { posProductToDelete = null })
    }

    if (showAddProductDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember {
            mutableStateOf(
                if (selectedCategoryName != "All") selectedCategoryName
                else categories.firstOrNull { !it.trackBoxes }?.name.orEmpty()
            )
        }
        var priceText by remember { mutableStateOf("") }
        val trackBoxes = categories.firstOrNull {
            it.name.equals(category, ignoreCase = true)
        }?.trackBoxes ?: false

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text(viewModel.getString("add_item_btn"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (trackBoxes) {
                        Text("Add an ice-cream company in category management instead of adding an item.")
                    }
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

    if (showUdharDialog) {
        AlertDialog(
            onDismissRequest = { showUdharDialog = false },
            title = { Text("Credit Sale (Customer Ledger)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = udharCustomerName,
                        onValueChange = { udharCustomerName = it },
                        label = { Text("Customer Name") },
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
                            if (udharCustomerName.isBlank()) "Recent Customers (tap to select):" else "Matching Customers:",
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
                                                "Due: ₹${cust.outstandingDebt.toInt()}",
                                                fontSize = 11.sp,
                                                color = if (cust.outstandingDebt > 0) MaterialTheme.appColors.danger else MaterialTheme.appColors.success,
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
                            color = MaterialTheme.appColors.successContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.appColors.success, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("✨ New customer '${udharCustomerName}' will be added to the ledger", fontSize = 11.sp, color = MaterialTheme.appColors.onSuccessContainer, fontWeight = FontWeight.Bold)
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.warning, contentColor = MaterialTheme.appColors.onWarning)
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
                                    Text("Quantity", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    Text("Rate (₹)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("₹${customRateText.ifBlank { "0" }}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // Touch Numeric Keypad for fast 1-hand operation
                        Text("Quick Keypad:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.updateCartItemQuantity(idx, 0)
                        showEditItemDialog = null
                    }) {
                        Text("Remove", color = MaterialTheme.appColors.danger)
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
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.appColors.danger, modifier = Modifier.size(16.dp))
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.success, contentColor = MaterialTheme.appColors.onSuccess)
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
    onDismiss: () -> Unit,
    editPurchase: SupplierPurchase? = null
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val inwardCategories = categories.filter { cat ->
        cat.trackBoxes || (editPurchase != null && cat.name.equals(editPurchase.category, ignoreCase = true))
    }
    val suppliers by viewModel.suppliers.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val varietyStocks by viewModel.iceCreamVarietyStock.collectAsState()

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
    var printedTotalText by remember { mutableStateOf("") }
    var updateMenuPrice by remember { mutableStateOf(false) }
    var updateStockBoxes by remember { mutableStateOf(true) }
    var billImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val draftItems = remember { mutableStateListOf<InwardBillItemDraft>() }
    val lineAmountTexts = remember { mutableStateMapOf<String, String>() }

    var isScanning by remember { mutableStateOf(false) }

    LaunchedEffect(inwardCategories) {
        if (inwardCategories.none { it.name.equals(selectedCategory, ignoreCase = true) }) {
            selectedCategory = inwardCategories.firstOrNull()?.name.orEmpty()
        }
    }

    val trackBoxes = categories.firstOrNull {
        it.name.equals(selectedCategory, ignoreCase = true)
    }?.trackBoxes ?: false
    val categoryCompanies = subCategories.filter {
        it.categoryName.equals(selectedCategory, ignoreCase = true)
    }
    // Ice-cream categories buy from a company; other categories use the supplier list.
    val companyChoices = if (trackBoxes) categoryCompanies.map { it.name } else suppliers.map { it.name }

    val companyVarieties = remember(varietyStocks, selectedBrand, selectedCategory) {
        varietyStocks.filter {
            it.categoryName.equals(selectedCategory, ignoreCase = true) &&
            it.companyName.equals(selectedBrand, ignoreCase = true)
        }.map { it.varietyName }
    }

    LaunchedEffect(editPurchase?.id) {
        val purchase = editPurchase ?: return@LaunchedEffect
        selectedCategory = purchase.category
        selectedBrand = purchase.brand
        billNumber = purchase.billNumber
        billDate = purchase.billDate
        viewModel.loadPurchaseItems(purchase) { rows ->
            draftItems.clear()
            lineAmountTexts.clear()
            rows.forEach { row ->
                val draft = InwardBillItemDraft(
                    name = row.varietyName,
                    boxes = row.quantity,
                    costPrice = row.costPerBox,
                    sellingPrice = row.mrp,
                    piecesPerBox = row.piecesPerBox
                )
                draftItems.add(draft)
                lineAmountTexts[draft.id] = row.lineAmount.toString()
            }
            val linesSum = rows.sumOf { it.lineAmount }
            if (kotlin.math.abs(purchase.totalAmount - linesSum) > 0.5) {
                printedTotalText = purchase.totalAmount.toString()
            }
        }
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
            billImageUri = uri
            isScanning = true
            Toast.makeText(context, "Scanning bill with Google ML Kit...", Toast.LENGTH_SHORT).show()
            BillScannerService.scanInvoiceImage(
                context,
                uri,
                companyChoices,
                addGstToCost
            ) { success, _, parsed ->
                isScanning = false
                if (success && parsed.items.isNotEmpty()) {
                    appendParsedInvoice(parsed)
                    Toast.makeText(context, "ML Kit: Added ${parsed.items.size} invoice items.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not read the bill. Please add items manually.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val totalBoxes = if (trackBoxes) draftItems.sumOf { it.boxes.toLong() } else 0L
    val totalCost = draftItems.sumOf { draft ->
        lineAmountTexts[draft.id]?.toDoubleOrNull()
            ?: draft.boxes.toDouble() * draft.costPrice
    }
    val billTotal = printedTotalText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: totalCost
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
                Text(
                    if (editPurchase != null) "Edit bill" else viewModel.getString("inward_stock_title"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
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
                        Text(if (trackBoxes) "Ice-cream company" else "Supplier", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (inwardCategories.isEmpty()) {
                            Text("Please create a category in Category Management first.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        } else if (companyChoices.isEmpty()) {
                            Text("No company yet, add one.", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(companyChoices) { companyName ->
                                    FilterChip(
                                        selected = selectedBrand.equals(companyName, ignoreCase = true),
                                        onClick = { selectedBrand = companyName },
                                        label = { Text(companyName, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
                        AppTextField(
                            value = billNumber,
                            onValueChange = { billNumber = it },
                            label = viewModel.getString("bill_number_label"),
                            singleLine = true
                        )
                        AppTextField(
                            value = billDate,
                            onValueChange = { billDate = it },
                            label = viewModel.getString("bill_date_label"),
                            singleLine = true
                        )
                    }
                }

                // 4. PAYMENT STATUS
                if (editPurchase == null) item {
                    Text(viewModel.getString("payment_status_label"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = paymentStatus == "PAID",
                            onClick = { paymentStatus = "PAID" },
                            label = { Text(viewModel.getString("payment_status_paid"), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                        )
                        FilterChip(
                            selected = paymentStatus == "UNPAID",
                            onClick = { paymentStatus = "UNPAID" },
                            label = { Text(viewModel.getString("payment_status_unpaid"), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                        )
                        FilterChip(
                            selected = paymentStatus == "PARTIAL",
                            onClick = { paymentStatus = "PARTIAL" },
                            label = { Text("Partial", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                // 5. PAYMENT MODE & GALLA NOTE
                if (editPurchase == null && paymentStatus == "PARTIAL") {
                    item {
                        AppTextField(
                            value = paidAmountText,
                            onValueChange = { paidAmountText = it },
                            label = "Amount paid now ₹",
                            keyboardType = KeyboardType.Decimal,
                            singleLine = true
                        )
                    }
                }
                if (editPurchase == null && paymentStatus != "UNPAID") {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                if (draftItems.isEmpty()) "Upload Bill Image" else "Upload second page",
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
                                Text("Google ML Kit OCR is reading the bill...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (showPasteSection) {
                    item {
                        Column {
                            AppTextField(
                                value = customBillText,
                                onValueChange = { customBillText = it },
                                placeholder = "Paste invoice rows containing description, MRP, Qty, Rate, tax and Amount",
                                singleLine = false,
                                modifier = Modifier.height(90.dp)
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
                                        Toast.makeText(context, "Could not read the bill. Please add items manually.", Toast.LENGTH_LONG).show()
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add 5% GST to ex-GST cost rate", fontSize = 11.sp)
                    }
                }

                // GODOWN STOCK CHECKBOX
                if (trackBoxes) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (updateStockBoxes) MaterialTheme.appColors.successContainer else MaterialTheme.appColors.warningContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Add boxes to godown stock",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (updateStockBoxes) MaterialTheme.appColors.onSuccessContainer else MaterialTheme.appColors.onWarningContainer
                                    )
                                }
                                Switch(
                                    checked = updateStockBoxes,
                                    onCheckedChange = { updateStockBoxes = it }
                                )
                            }
                        }
                    }
                }

                // 7. EDITABLE ITEMS TABLE
                item {
                    Text(
                        "Items (${draftItems.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(draftItems, key = { it.id }) { draft ->
                    val idx = draftItems.indexOfFirst { it.id == draft.id }
                    val bestMatch = remember(draft.name, companyVarieties) {
                        if (trackBoxes && draft.name.isNotBlank() && companyVarieties.none { it.equals(draft.name, ignoreCase = true) }) {
                            findBestMatchingVariety(draft.name, companyVarieties).takeIf { it.second >= 0.4f }
                        } else null
                    }

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
                                AppTextField(
                                    value = draft.name,
                                    onValueChange = { draftItems[idx] = draft.copy(name = it) },
                                    label = if (trackBoxes) "Variety / Product name" else "Product name",
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { draftItems.removeAt(idx) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.appColors.danger, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (bestMatch?.first != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.appColors.infoContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            draftItems[idx] = draft.copy(name = bestMatch.first!!)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(bestMatch.first!!, fontSize = 10.sp, color = MaterialTheme.appColors.info, maxLines = 1)
                                    }
                                }
                            }

                            if (trackBoxes && draft.name.isBlank() && companyVarieties.isNotEmpty()) {
                                Column(modifier = Modifier.padding(top = 2.dp)) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(companyVarieties) { cv ->
                                            SuggestionChip(
                                                onClick = { draftItems[idx] = draft.copy(name = cv) },
                                                label = { Text(cv, fontSize = 9.sp) }
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppTextField(
                                    value = draft.boxesInput,
                                    onValueChange = {
                                        draftItems[idx] = draft.copy(
                                            boxesInput = it,
                                            boxes = it.toIntOrNull() ?: 0
                                        )
                                    },
                                    label = if (trackBoxes) "Boxes" else "Qty",
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f)
                                )
                                AppTextField(
                                    value = draft.costPriceInput,
                                    onValueChange = {
                                        draftItems[idx] = draft.copy(
                                            costPriceInput = it,
                                            costPrice = it.toDoubleOrNull() ?: 0.0
                                        )
                                    },
                                    label = "Cost ₹",
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.weight(1f)
                                )
                                AppTextField(
                                    value = draft.sellingPriceInput,
                                    onValueChange = {
                                        draftItems[idx] = draft.copy(
                                            sellingPriceInput = it,
                                            sellingPrice = it.toDoubleOrNull() ?: 0.0
                                        )
                                    },
                                    label = "MRP ₹",
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppTextField(
                                    value = lineAmountTexts[draft.id]
                                        ?.takeIf { it.isNotBlank() }
                                        ?: (draft.boxes * draft.costPrice).toString(),
                                    onValueChange = { lineAmountTexts[draft.id] = it },
                                    label = "Amount ₹",
                                    keyboardType = KeyboardType.Decimal,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                AppTextField(
                                    value = draft.piecesPerBoxInput,
                                    onValueChange = {
                                        draftItems[idx] = draft.copy(
                                            piecesPerBoxInput = it,
                                            piecesPerBox = it.toIntOrNull() ?: 0
                                        )
                                    },
                                    label = "Pcs/box",
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f)
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

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppTextField(
                            value = printedTotalText,
                            onValueChange = { printedTotalText = it },
                            label = "Total printed on bill ₹ (optional)",
                            keyboardType = KeyboardType.Decimal,
                            singleLine = true
                        )
                        val printed = printedTotalText.toDoubleOrNull()
                        if (printed != null && kotlin.math.abs(printed - totalCost) > 1.0) {
                            Text(
                                "⚠️ Items total ₹${totalCost.toInt()} differs from the bill total ₹${printed.toInt()}.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = updateMenuPrice, onCheckedChange = { updateMenuPrice = it })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update menu selling price from the bill MRP", fontSize = 11.sp)
                        }
                        if (billImageUri != null) {
                            Text("📷 Bill photo will be saved", fontSize = 11.sp, color = MaterialTheme.appColors.success)
                        }
                    }
                }

                // 8. BILL SUMMARY CARD
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.appColors.heroSlate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (trackBoxes) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Boxes:", color = MaterialTheme.appColors.onHeroMuted, fontSize = 11.sp)
                                    Text("$totalBoxes Boxes", color = MaterialTheme.appColors.onHero, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Bill Amount:", color = MaterialTheme.appColors.onHeroMuted, fontSize = 11.sp)
                                Text(
                                    "₹${String.format(Locale.getDefault(), "%.2f", totalCost)}",
                                    color = Color(0xFF7DD3FC),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                            if (isOwner) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Estimated Total Sales (MRP):", color = MaterialTheme.appColors.onHeroMuted, fontSize = 11.sp)
                                    Text(
                                        "₹${String.format(Locale.getDefault(), "%.2f", totalSelling)}",
                                        color = MaterialTheme.appColors.onHero,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.appColors.onHero.copy(alpha = 0.18f), modifier = Modifier.padding(vertical = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Estimated Net Profit on this Stock:", color = MaterialTheme.appColors.heroPositive, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        "+₹${String.format(Locale.getDefault(), "%.2f", totalProjectedProfit)} (${overallMargin.toInt()}%)",
                                        color = MaterialTheme.appColors.heroPositive,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isSaving) return@Button
                    if (selectedBrand.isBlank() || selectedCategory.isBlank()) {
                        Toast.makeText(context, "Select a supplier / company and a category.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (draftItems.isEmpty() || draftItems.any {
                            it.name.isBlank() || it.boxes !in 1..MAX_INWARD_QTY ||
                                it.costPrice !in 0.01..100000.0 || it.sellingPrice !in 0.0..100000.0
                        }) {
                        Toast.makeText(context, "Enter name, quantity, cost and MRP for every item.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val paid: Double? = when {
                        editPurchase != null -> 0.0
                        paymentStatus == "PAID" -> billTotal
                        paymentStatus == "PARTIAL" -> paidAmountText.toDoubleOrNull()?.takeIf { it in 0.0..billTotal }
                        else -> 0.0
                    }
                    if (paid == null) {
                        Toast.makeText(context, "Paid amount must be between ₹0 and the bill total.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    val savedImagePath = billImageUri?.let { BillImages.copyToAppStorage(context, it) }
                    viewModel.saveInwardPurchase(
                        InwardRequest(
                            purchaseId = editPurchase?.id,
                            brand = selectedBrand,
                            category = selectedCategory,
                            billNumber = billNumber,
                            billDate = billDate,
                            printedTotal = printedTotalText.toDoubleOrNull()?.takeIf { it > 0.0 },
                            paidAmount = paid,
                            paymentMode = paymentMode,
                            items = draftItems.map { it.copy(lineAmount = lineAmountTexts[it.id]?.toDoubleOrNull()) },
                            billImagePath = savedImagePath,
                            updateMenuPrice = updateMenuPrice,
                            updateStockBoxes = updateStockBoxes
                        )
                    ) { error ->
                        isSaving = false
                        if (error == null) {
                            Toast.makeText(context, viewModel.getString("inward_success_msg"), Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.success, contentColor = MaterialTheme.appColors.onSuccess)
            ) {
                Text(if (editPurchase != null) "Update bill" else "Save Inward Stock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showAddCompany) {
        AlertDialog(
            onDismissRequest = { showAddCompany = false },
            title = { Text(if (trackBoxes) "Add ice-cream company" else "Add supplier") },
            text = {
                AppTextField(
                    value = newCompanyName,
                    onValueChange = { newCompanyName = it },
                    label = "Company name",
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val companyName = newCompanyName.trim()
                        if (companyName.isNotBlank() && selectedCategory.isNotBlank()) {
                            if (trackBoxes) {
                                viewModel.addSubCategory(selectedCategory, companyName, 0.0) {
                                    selectedBrand = it.name
                                    newCompanyName = ""
                                    showAddCompany = false
                                }
                            } else {
                                viewModel.saveSupplier(companyName, "", "", "")
                                selectedBrand = companyName
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
// 4. GODOWN BOX-ONLY STOCK TRACKER SCREEN & DIALOGS
// ==============================================================================
@Composable
fun InventoryScreen(
    viewModel: ShopViewModel,
    onNavigateToMaalAavak: () -> Unit = {}
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val subCategories by viewModel.subCategories.collectAsState()
    val varietyStocks by viewModel.iceCreamVarietyStock.collectAsState()
    val iceCreamProfit by viewModel.iceCreamProfitStats.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var expandedCompanies by remember { mutableStateOf(setOf<String>()) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Dialog states
    var addVarietiesTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // (category, company)
    var mergeTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // (category, company)
    var stockForCorrection by remember { mutableStateOf<IceCreamVarietyStock?>(null) }
    var stockForDelete by remember { mutableStateOf<IceCreamVarietyStock?>(null) }
    var showInsightsDialog by remember { mutableStateOf(false) }
    var showWhatsAppOrderDialog by remember { mutableStateOf(false) }
    var showInwardDialog by remember { mutableStateOf(false) }

    val boxTrackedCategories = categories.filter { it.trackBoxes }
    val boxTrackedSubCategories = subCategories.filter { subCategory ->
        categories.any { it.trackBoxes && it.name.equals(subCategory.categoryName, ignoreCase = true) }
    }

    val totalBoxes = varietyStocks.sumOf { it.sealedBoxes }
    val lowStockCount = varietyStocks.count { it.varietyName != UNASSIGNED_VARIETY && it.sealedBoxes <= it.lowStockLimit }

    val companiesList = remember(boxTrackedSubCategories, varietyStocks) {
        val names = (boxTrackedSubCategories.map { it.name } + varietyStocks.map { it.companyName })
            .distinctBy { it.lowercase(Locale.getDefault()) }
            .sorted()
        names
    }

    val effectiveExpanded = remember(expandedCompanies, searchQuery, companiesList) {
        if (searchQuery.isNotBlank()) companiesList.toSet() else expandedCompanies
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TOP ACTION BAR
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToMaalAavak,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inward Stock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = { showInsightsDialog = true },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text("🍨 Top Sellers", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = { showWhatsAppOrderDialog = true },
                    modifier = Modifier.weight(1.1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text("📲 Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // GODOWN BOX STATUS BANNER
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.heroSlate),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("📦 " + viewModel.getString("godown_box_status"), color = MaterialTheme.appColors.onHeroMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$totalBoxes Sealed Boxes", color = MaterialTheme.appColors.onHero, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        if (lowStockCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFDC2626).copy(alpha = 0.2f),
                                modifier = Modifier.clickable { showWhatsAppOrderDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("⚠️ $lowStockCount Low Stock", color = MaterialTheme.appColors.heroNegative, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Tap '-1 Open' on a counter whenever you open a box. When new stock arrives, add boxes from an Inward Stock bill.",
                        color = MaterialTheme.appColors.onHeroMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // SEARCH & FILTER BAR
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search variety or company...",
                    leadingIcon = Icons.Default.Search,
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    } else null
                )

                if (boxTrackedCategories.size > 1) {
                    val filterOptions = listOf("All") + boxTrackedCategories.map { it.name }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(filterOptions) { opt ->
                            FilterChip(
                                selected = selectedCategoryFilter == opt,
                                onClick = { selectedCategoryFilter = opt },
                                label = { Text(opt, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        }

        // COMPANY-WISE EXPANDABLE ACCORDIONS
        if (companiesList.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No ice-cream companies yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showInwardDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Add First Inward Stock Bill")
                        }
                    }
                }
            }
        } else {
            items(companiesList) { companyName ->
                val companyStocks = varietyStocks.filter {
                    it.companyName.equals(companyName, ignoreCase = true) &&
                    (selectedCategoryFilter == "All" || it.categoryName.equals(selectedCategoryFilter, ignoreCase = true)) &&
                    (searchQuery.isBlank() || it.varietyName.contains(searchQuery, ignoreCase = true) || it.companyName.contains(searchQuery, ignoreCase = true))
                }
                val totalCompanyBoxes = companyStocks.sumOf { it.sealedBoxes }
                val companyLowCount = companyStocks.count { it.varietyName != UNASSIGNED_VARIETY && it.sealedBoxes <= it.lowStockLimit }
                val isExpanded = effectiveExpanded.contains(companyName)
                val defaultCategory = companyStocks.firstOrNull()?.categoryName
                    ?: boxTrackedSubCategories.firstOrNull { it.name.equals(companyName, ignoreCase = true) }?.categoryName
                    ?: boxTrackedCategories.firstOrNull()?.name
                    ?: "Ice-cream"

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Company Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedCompanies = if (isExpanded) {
                                        expandedCompanies - companyName
                                    } else {
                                        expandedCompanies + companyName
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🏢", fontSize = 18.sp)
                                Column {
                                    Text(companyName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${companyStocks.size} varieties",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (companyLowCount > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.appColors.dangerContainer
                                    ) {
                                        Text(
                                            "$companyLowCount Low",
                                            color = MaterialTheme.appColors.danger,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "$totalCompanyBoxes Boxes",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Expanded View
                        if (isExpanded) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { addVarietiesTarget = Pair(defaultCategory, companyName) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Add Varieties", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (companyStocks.size > 1) {
                                    OutlinedButton(
                                        onClick = { mergeTarget = Pair(defaultCategory, companyName) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("🔀 Merge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (companyStocks.isEmpty()) {
                                Text(
                                    "This company has no varieties yet. Tap '+ Add Varieties' to add some.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    companyStocks.forEach { stock ->
                                        val isAssigned = stock.varietyName != UNASSIGNED_VARIETY
                                        val isLow = isAssigned && stock.sealedBoxes <= stock.lowStockLimit

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isLow) MaterialTheme.appColors.dangerContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                            border = if (isLow) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.appColors.danger.copy(alpha = 0.4f)) else null,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(stock.varietyName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            if (stock.lastMrp > 0) {
                                                                Text("MRP: ₹${stock.lastMrp.toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                            if (stock.lastCostPrice > 0) {
                                                                Text("Cost: ₹${stock.lastCostPrice.toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                            Text("Alert Limit: ${stock.lowStockLimit}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = when {
                                                            stock.sealedBoxes == 0 -> MaterialTheme.appColors.dangerContainer
                                                            isLow -> MaterialTheme.appColors.warningContainer
                                                            else -> MaterialTheme.appColors.successContainer
                                                        }
                                                    ) {
                                                        Text(
                                                            "${stock.sealedBoxes} Box",
                                                            color = when {
                                                                stock.sealedBoxes == 0 -> MaterialTheme.appColors.onDangerContainer
                                                                isLow -> MaterialTheme.appColors.onWarningContainer
                                                                else -> MaterialTheme.appColors.onSuccessContainer
                                                            },
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }

                                                if (isLow) {
                                                    Text(
                                                        if (stock.sealedBoxes == 0) "⚠️ Out of stock! 0 boxes left."
                                                        else "⚠️ Low stock! Only ${stock.sealedBoxes} boxes left (limit: ${stock.lowStockLimit}).",
                                                        color = if (stock.sealedBoxes == 0) MaterialTheme.appColors.danger else MaterialTheme.appColors.warning,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }

                                                // Action row: [-1 Open] [⚙️ Sudhaar] [Wastage] [🗑️]
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.openIceCreamVarietyBox(stock)
                                                            Toast.makeText(context, "${stock.varietyName}: 1 box opened at the counter", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        enabled = stock.sealedBoxes > 0,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("-1 Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    OutlinedButton(
                                                        onClick = { stockForCorrection = stock },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.weight(1.1f)
                                                    ) {
                                                        Text("⚙️ Correct", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    WastageButton(viewModel = viewModel, stock = stock)

                                                    IconButton(
                                                        onClick = { stockForDelete = stock },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOGS
    addVarietiesTarget?.let { (cat, comp) ->
        AddVarietiesDialog(
            companyName = comp,
            categoryName = cat,
            onDismiss = { addVarietiesTarget = null },
            onAdd = { text, limit, cost, mrp ->
                viewModel.addVarietiesBulk(
                    categoryName = cat,
                    companyName = comp,
                    varietiesText = text,
                    defaultLimit = limit,
                    costPrice = cost,
                    mrp = mrp
                )
                Toast.makeText(context, "Varieties added!", Toast.LENGTH_SHORT).show()
                addVarietiesTarget = null
            }
        )
    }

    mergeTarget?.let { (cat, comp) ->
        val compVarieties = varietyStocks.filter { it.companyName.equals(comp, ignoreCase = true) }
        MergeVarietiesDialog(
            companyName = comp,
            categoryName = cat,
            varieties = compVarieties,
            onDismiss = { mergeTarget = null },
            onMerge = { src, tgt ->
                viewModel.mergeVarieties(cat, comp, src, tgt) { err ->
                    if (err == null) {
                        Toast.makeText(context, "Merged '$src' into '$tgt'!", Toast.LENGTH_SHORT).show()
                        mergeTarget = null
                    } else {
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    stockForCorrection?.let { stock ->
        StockCorrectionDialog(
            stock = stock,
            onDismiss = { stockForCorrection = null },
            onConfirm = { newBoxes, reason ->
                viewModel.recordStockCorrection(stock, newBoxes, reason) { err ->
                    if (err == null) {
                        Toast.makeText(context, "Stock correction recorded!", Toast.LENGTH_SHORT).show()
                        stockForCorrection = null
                    } else {
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    stockForDelete?.let { stock ->
        AlertDialog(
            onDismissRequest = { stockForDelete = null },
            title = { Text("Delete Variety?") },
            text = { Text("Remove '${stock.varietyName}' (${stock.sealedBoxes} boxes) from the stock list?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVariety(stock) { err ->
                            if (err == null) {
                                Toast.makeText(context, "Variety removed", Toast.LENGTH_SHORT).show()
                                stockForDelete = null
                            } else {
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.danger, contentColor = MaterialTheme.appColors.onDanger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { stockForDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showInsightsDialog) {
        IceCreamInsightsDialog(viewModel = viewModel, onDismiss = { showInsightsDialog = false })
    }

    if (showWhatsAppOrderDialog) {
        WhatsAppOrderDialog(viewModel = viewModel, onDismiss = { showWhatsAppOrderDialog = false })
    }

    if (showInwardDialog) {
        InwardBillDialog(viewModel = viewModel, onDismiss = { showInwardDialog = false })
    }
}

// ==============================================================================
// 4B. ADD VARIETIES DIALOG (BULK ADDITION)
// ==============================================================================
@Composable
fun AddVarietiesDialog(
    companyName: String,
    categoryName: String,
    onDismiss: () -> Unit,
    onAdd: (varietiesText: String, defaultLimit: Int, defaultCost: Double, defaultMrp: Double) -> Unit
) {
    var varietiesText by remember { mutableStateOf("") }
    var defaultLimitText by remember { mutableStateOf("2") }
    var defaultCostText by remember { mutableStateOf("") }
    var defaultMrpText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("+ Add Varieties", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Company: $companyName", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter variety names separated by commas or new lines:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AppTextField(
                    value = varietiesText,
                    onValueChange = { varietiesText = it },
                    placeholder = "e.g. Vanilla Cup 10, Choco Bar 20, Matka Kulfi",
                    singleLine = false,
                    modifier = Modifier.height(100.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(
                        value = defaultLimitText,
                        onValueChange = { if (it.all(Char::isDigit)) defaultLimitText = it },
                        label = "Low Alert Limit",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f)
                    )
                    AppTextField(
                        value = defaultMrpText,
                        onValueChange = { defaultMrpText = it },
                        label = "MRP ₹ (Optional)",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = defaultLimitText.toIntOrNull() ?: 2
                    val cost = defaultCostText.toDoubleOrNull() ?: 0.0
                    val mrp = defaultMrpText.toDoubleOrNull() ?: 0.0
                    if (varietiesText.isNotBlank()) {
                        onAdd(varietiesText, limit, cost, mrp)
                    }
                },
                enabled = varietiesText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Varieties")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ==============================================================================
// 4C. MERGE VARIETIES DIALOG
// ==============================================================================
@Composable
fun MergeVarietiesDialog(
    companyName: String,
    categoryName: String,
    varieties: List<IceCreamVarietyStock>,
    onDismiss: () -> Unit,
    onMerge: (source: String, target: String) -> Unit
) {
    var sourceVariety by remember { mutableStateOf(varieties.firstOrNull()?.varietyName.orEmpty()) }
    var targetVariety by remember { mutableStateOf(varieties.getOrNull(1)?.varietyName.orEmpty()) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("🔀 Merge Duplicate Varieties", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Company: $companyName", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.appColors.warningContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "All boxes of the misspelled or duplicate variety will be moved into the correct one, and the duplicate will be deleted.",
                        fontSize = 11.sp,
                        color = MaterialTheme.appColors.onWarningContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Source selector
                Column {
                    Text("1. Select the duplicate / wrong variety (to be removed):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { sourceExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(sourceVariety.ifBlank { "Select Duplicate" }, maxLines = 1)
                        }
                        DropdownMenu(
                            expanded = sourceExpanded,
                            onDismissRequest = { sourceExpanded = false }
                        ) {
                            varieties.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("${v.varietyName} (${v.sealedBoxes} box)") },
                                    onClick = {
                                        sourceVariety = v.varietyName
                                        sourceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Target selector
                Column {
                    Text("2. Select the correct master variety (to merge into):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { targetExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(targetVariety.ifBlank { "Select Target Master" }, maxLines = 1)
                        }
                        DropdownMenu(
                            expanded = targetExpanded,
                            onDismissRequest = { targetExpanded = false }
                        ) {
                            varieties.filter { it.varietyName != sourceVariety }.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("${v.varietyName} (${v.sealedBoxes} box)") },
                                    onClick = {
                                        targetVariety = v.varietyName
                                        targetExpanded = false
                                    }
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
                    if (sourceVariety.isNotBlank() && targetVariety.isNotBlank() && sourceVariety != targetVariety) {
                        onMerge(sourceVariety, targetVariety)
                    }
                },
                enabled = sourceVariety.isNotBlank() && targetVariety.isNotBlank() && sourceVariety != targetVariety,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.info, contentColor = MaterialTheme.appColors.onInfo)
            ) {
                Text("Merge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ==============================================================================
// 4D. STOCK CORRECTION DIALOG (SUDAAR)
// ==============================================================================
@Composable
fun StockCorrectionDialog(
    stock: IceCreamVarietyStock,
    onDismiss: () -> Unit,
    onConfirm: (newBoxes: Int, reason: String) -> Unit
) {
    var newBoxesText by remember { mutableStateOf(stock.sealedBoxes.toString()) }
    var reasonText by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf(stock.lowStockLimit.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("⚙️ Stock Correction", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${stock.companyName} • ${stock.varietyName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Current stock: ${stock.sealedBoxes} boxes. Only change the count when the physical godown count differs. The correction will be recorded in the audit log.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                AppTextField(
                    value = newBoxesText,
                    onValueChange = { if (it.all(Char::isDigit)) newBoxesText = it },
                    label = "Correct Number of Boxes (New Total)",
                    keyboardType = KeyboardType.Number
                )

                AppTextField(
                    value = limitText,
                    onValueChange = { if (it.all(Char::isDigit)) limitText = it },
                    label = "Low Stock Alert Limit",
                    keyboardType = KeyboardType.Number
                )

                AppTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = "Reason for Correction (required)*",
                    placeholder = "e.g. Physical count difference, melted / damaged, etc."
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Physical count difference", "Damaged / Melted", "Earlier wrong entry").forEach { r ->
                        SuggestionChip(
                            onClick = { reasonText = r },
                            label = { Text(r, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = newBoxesText.toIntOrNull()
                    if (count != null && reasonText.isNotBlank()) {
                        onConfirm(count, reasonText.trim())
                    }
                },
                enabled = newBoxesText.isNotBlank() && reasonText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.warning, contentColor = MaterialTheme.appColors.onWarning)
            ) {
                Text("Save Correction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ==============================================================================
// 4E. ICE-CREAM INSIGHTS & RANKING DIALOG
// ==============================================================================
@Composable
fun IceCreamInsightsDialog(
    viewModel: ShopViewModel,
    onDismiss: () -> Unit
) {
    var selectedDays by remember { mutableStateOf(7) }
    val (companyInsights, varietyInsights) = remember(selectedDays) {
        viewModel.getIceCreamInsights(selectedDays)
    }

    val topSellers = varietyInsights.filter { it.totalConsumed > 0 }.take(10)
    val deadStock = varietyInsights.filter { it.totalConsumed == 0 && it.currentBoxes > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🍨", fontSize = 20.sp)
                    Text("Top Sellers & Insights", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // FILTER TABS: 7 Din / 30 Din / All Time
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(Pair(7, "7 Din"), Pair(30, "30 Din"), Pair(0, "All Time")).forEach { (days, label) ->
                            FilterChip(
                                selected = selectedDays == days,
                                onClick = { selectedDays = days },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                // 1. TOP SELLING VARIETIES LEADERBOARD
                item {
                    Text("🏆 Best-Selling Varieties:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (topSellers.isEmpty()) {
                    item {
                        Text(
                            "No variety sales or box-opening records in this period.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(topSellers) { item ->
                        val rank = topSellers.indexOf(item) + 1
                        val medal = when (rank) {
                            1 -> "🥇"
                            2 -> "🥈"
                            3 -> "🥉"
                            else -> "#$rank"
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(medal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Column {
                                        Text(item.varietyName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${item.companyName} • ${item.currentBoxes} boxes left",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${item.totalConsumed} Open/Bika",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    item.estimatedDaysLeft?.let { days ->
                                        Text(
                                            "~$days days of stock",
                                            fontSize = 9.sp,
                                            color = if (days <= 3) MaterialTheme.appColors.danger else MaterialTheme.appColors.success
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. COMPANY SHARE
                if (companyInsights.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("📊 Company-wise Sales Share:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    items(companyInsights) { ci ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ci.companyName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("${ci.totalConsumed} units (${ci.percentage.toInt()}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val progressFraction = (ci.percentage / 100f).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }

                // 3. DEAD STOCK / SLOW MOVING
                if (deadStock.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("⚠️ Not Selling (Slow Moving):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.appColors.warning)
                    }
                    items(deadStock) { ds ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.appColors.warningContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(ds.varietyName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(ds.companyName, fontSize = 9.sp, color = MaterialTheme.appColors.onWarningContainer)
                                }
                                Text("${ds.currentBoxes} boxes stuck in godown", fontSize = 10.sp, color = MaterialTheme.appColors.onWarningContainer, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("OK") }
        }
    )
}

// ==============================================================================
// 4F. WHATSAPP ORDER GENERATOR DIALOG
// ==============================================================================
@Composable
fun WhatsAppOrderDialog(
    viewModel: ShopViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val varietyStocks by viewModel.iceCreamVarietyStock.collectAsState()

    val lowStockVarieties = remember(varietyStocks) {
        varietyStocks.filter { it.varietyName != UNASSIGNED_VARIETY && it.sealedBoxes <= it.lowStockLimit }
    }
    val groupedByCompany = remember(lowStockVarieties) {
        lowStockVarieties.groupBy { it.companyName }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📋 Items to Reorder (Low Stock)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            if (groupedByCompany.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✅ All items are sufficiently stocked!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.appColors.success)
                    Text("No variety is below its alert limit.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Send an order on WhatsApp per company in 1 tap:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    groupedByCompany.forEach { (compName, itemsList) ->
                        item(key = compName) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(compName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("${itemsList.size} items low", fontSize = 10.sp, color = MaterialTheme.appColors.danger, fontWeight = FontWeight.Bold)
                                    }

                                    itemsList.forEach { stock ->
                                        val suggestedOrder = maxOf(5, (stock.lowStockLimit * 2) - stock.sealedBoxes)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("• ${stock.varietyName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text(
                                                "${stock.sealedBoxes} left (order: ${suggestedOrder} boxes)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.appColors.onInfoContainer
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val orderPairs = itemsList.map {
                                                val suggested = maxOf(5, (it.lowStockLimit * 2) - it.sealedBoxes)
                                                Pair(it.varietyName, suggested)
                                            }
                                            val message = viewModel.generateWhatsAppOrderText(compName, orderPairs)
                                            sendWhatsAppMessage(context, null, message)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.success, contentColor = MaterialTheme.appColors.onSuccess),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 6.dp)
                                    ) {
                                        Text("📲 Send WhatsApp Order ($compName)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
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
    var creditCustomer by remember { mutableStateOf<CustomerKhata?>(null) }
    var ledgerCustomerId by remember { mutableStateOf<String?>(null) }
    val allSales by viewModel.sales.collectAsState()
    val allRepayments by viewModel.repayments.collectAsState()
    val allCredits by viewModel.khataCredits.collectAsState()

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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.heroAmber),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(viewModel.getString("total_market_outstanding"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.appColors.heroCaution, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${totalOutstanding.toInt()}", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.appColors.onHero)

                    val debtorCount = khataList.count { it.outstandingDebt > 0 }
                    if (debtorCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val debtorsWithPhone = khataList.filter { it.outstandingDebt > 0 && it.phone.isNotBlank() }
                                val first = debtorsWithPhone.firstOrNull()
                                if (first != null) {
                                    val msg = "Hello ${first.name},\nYour previous balance of ₹${first.outstandingDebt.toInt()} at Refresh Soda is pending. Please pay soon. Thank you!"
                                    sendWhatsAppMessage(context, first.phone, msg)
                                } else {
                                    Toast.makeText(context, "$debtorCount customers have dues. Tap Send on a customer card.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.appColors.onHero),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.appColors.onHero.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📢 Payment Reminder ($debtorCount customers)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                if (isKhataOverdue(customer)) {
                                    Surface(
                                        color = MaterialTheme.appColors.dangerContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("⚠️ Overdue", color = MaterialTheme.appColors.onDangerContainer, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            if (customer.phone.isNotBlank()) {
                                Text("📱 ${customer.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "Due: ₹${customer.outstandingDebt.toInt()}",
                                color = if (customer.outstandingDebt > 0) MaterialTheme.appColors.danger else MaterialTheme.appColors.success,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (customer.outstandingDebt > 0) KhataDueDateRow(viewModel = viewModel, customer = customer)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = { ledgerCustomerId = customer.id },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = "Ledger / history", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    val msg = "Hello ${customer.name} ji,\nYour outstanding balance at Refresh Soda is ₹${customer.outstandingDebt.toInt()}. Please clear it at your earliest convenience.\nThank you!"
                                    sendWhatsAppMessage(context, customer.phone, msg)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "WhatsApp", tint = MaterialTheme.appColors.success)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { creditCustomer = customer },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.appColors.danger),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.appColors.danger.copy(alpha = 0.6f))
                        ) {
                            Text("+ Give Credit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                repayCustomer = customer
                                repayMode = "CASH"
                                repayAmountText = customer.outstandingDebt.toInt().toString()
                            },
                            enabled = customer.outstandingDebt > 0,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.success, contentColor = MaterialTheme.appColors.onSuccess),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Receive Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ---------- Receive payment (cash / online) ----------
    if (repayCustomer != null) {
        val cust = repayCustomer!!
        val amt = repayAmountText.toDoubleOrNull() ?: 0.0
        val overpay = amt > cust.outstandingDebt
        AlertDialog(
            onDismissRequest = { repayCustomer = null },
            title = { Text("Payment from ${cust.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total Due: ₹${cust.outstandingDebt.toInt()}", color = MaterialTheme.appColors.danger, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = repayAmountText,
                        onValueChange = { repayAmountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Received Amount (₹)") },
                        singleLine = true,
                        isError = overpay,
                        supportingText = { if (overpay) Text("Cannot be more than the due amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    Text("Received via:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = repayMode == "CASH",
                            onClick = { repayMode = "CASH" },
                            label = { Text("Cash") }
                        )
                        FilterChip(
                            selected = repayMode == "UPI",
                            onClick = { repayMode = "UPI" },
                            label = { Text("Online (UPI / Paytm)") }
                        )
                    }
                    Text(
                        if (repayMode == "CASH") "Cash is added to today's till (Day Closing)."
                        else "Online payments are not added to till cash.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = amt > 0 && !overpay,
                    onClick = {
                        viewModel.recordKhataRepayment(cust.id, cust.name, amt, repayMode)
                        repayCustomer = null
                        repayAmountText = ""
                    }
                ) {
                    Text("Save Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { repayCustomer = null }) { Text("Cancel") }
            }
        )
    }

    // ---------- Give credit (udhaar) ----------
    creditCustomer?.let { cust ->
        var amountText by remember(cust.id) { mutableStateOf("") }
        var noteText by remember(cust.id) { mutableStateOf("") }
        val amt = amountText.toDoubleOrNull() ?: 0.0
        val newDue = cust.outstandingDebt + amt
        AlertDialog(
            onDismissRequest = { creditCustomer = null },
            title = { Text("Give Credit to ${cust.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current due: ₹${cust.outstandingDebt.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Credit Amount (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note (e.g. 2 sodas, 1 ice-cream)") },
                        singleLine = true
                    )
                    if (amt > 0) {
                        Text("New due will be ₹${newDue.toInt()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    if (amt > 0 && newDue > cust.debtLimit) {
                        Text(
                            "⚠️ This is above the credit limit of ₹${cust.debtLimit.toInt()}.",
                            fontSize = 11.sp,
                            color = MaterialTheme.appColors.warning
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = amt > 0,
                    onClick = {
                        viewModel.recordKhataCredit(cust.id, cust.name, amt, noteText)
                        creditCustomer = null
                    }
                ) { Text("Save Credit") }
            },
            dismissButton = {
                TextButton(onClick = { creditCustomer = null }) { Text("Cancel") }
            }
        )
    }

    // ---------- Customer ledger (credits + payments) ----------
    val ledgerCust = ledgerCustomerId?.let { id -> khataList.firstOrNull { it.id == id } }
    if (ledgerCust != null) {
        var rowToDelete by remember(ledgerCust.id) { mutableStateOf<KhataLedgerRow?>(null) }
        val rows = remember(allSales, allRepayments, allCredits, ledgerCust.id, ledgerCust.name) {
            val list = mutableListOf<KhataLedgerRow>()
            allSales.filter {
                it.paymentMode == "UDHAR" && it.customerName?.trim().equals(ledgerCust.name.trim(), ignoreCase = true)
            }.forEach {
                list += KhataLedgerRow(it.timestamp, "Credit sale", it.itemsSummary, it.totalAmount, true)
            }
            allCredits.filter { it.customerId == ledgerCust.id }.forEach {
                list += KhataLedgerRow(it.timestamp, "Credit given", it.note, it.amount, true, credit = it)
            }
            allRepayments.filter { it.customerId == ledgerCust.id }.forEach {
                list += KhataLedgerRow(
                    it.timestamp,
                    if (it.paymentMode == "UPI") "Payment received • Online" else "Payment received • Cash",
                    "",
                    it.amount,
                    false,
                    repayment = it
                )
            }
            list.sortedByDescending { it.timestamp }
        }
        val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { ledgerCustomerId = null },
            title = { Text("Ledger: ${ledgerCust.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Current due: ₹${ledgerCust.outstandingDebt.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        color = if (ledgerCust.outstandingDebt > 0) MaterialTheme.appColors.danger else MaterialTheme.appColors.success
                    )
                    if (rows.isEmpty()) {
                        Text("No entries yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(rows) { row ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(row.title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            if (row.subtitle.isNotBlank()) {
                                                Text(row.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                            }
                                            Text(
                                                dateFormat.format(java.util.Date(row.timestamp)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            (if (row.isCredit) "+ ₹" else "− ₹") + row.amount.toInt(),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = if (row.isCredit) MaterialTheme.appColors.danger else MaterialTheme.appColors.success
                                        )
                                        if (row.credit != null || row.repayment != null) {
                                            IconButton(onClick = { rowToDelete = row }, modifier = Modifier.size(32.dp)) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete entry",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { ledgerCustomerId = null }) { Text("Close") }
            }
        )

        rowToDelete?.let { row ->
            AlertDialog(
                onDismissRequest = { rowToDelete = null },
                title = { Text("Delete this entry?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        if (row.isCredit) "The ₹${row.amount.toInt()} credit entry will be removed and the customer's due will go down."
                        else "The ₹${row.amount.toInt()} payment entry will be removed and the customer's due will go up."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            row.credit?.let { viewModel.deleteKhataCreditEntry(it) }
                            row.repayment?.let { viewModel.deleteKhataRepaymentEntry(it) }
                            rowToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.appColors.danger,
                            contentColor = MaterialTheme.appColors.onDanger
                        )
                    ) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { rowToDelete = null }) { Text("Cancel") } }
            )
        }
    }

    // ---------- Add customer (once) ----------
    if (showAddCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var limitText by remember { mutableStateOf("2000") }
        var openingDueText by remember { mutableStateOf("") }
        var addError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Add Customer", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it; addError = null }, label = { Text("Customer Name") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number (WhatsApp)") }, singleLine = true)
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Credit Limit (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = openingDueText,
                        onValueChange = { openingDueText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount already owed (₹, optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    addError?.let {
                        Text(it, fontSize = 12.sp, color = MaterialTheme.appColors.danger)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val limit = limitText.toDoubleOrNull() ?: 2000.0
                    val openingDue = openingDueText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        viewModel.addNewCustomer(name, phone, limit, openingDue) { ok, message ->
                            if (ok) showAddCustomerDialog = false else addError = message
                        }
                    } else {
                        addError = "Customer name is required."
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

private data class KhataLedgerRow(
    val timestamp: Long,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val isCredit: Boolean,
    val credit: KhataCredit? = null,
    val repayment: KhataRepayment? = null
)

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

    val isOwner by viewModel.isOwner.collectAsState()
    val subTabs = listOf(
        viewModel.getString("day_closing_title"),
        viewModel.getString("daily_expenses"),
        viewModel.getString("paytm_settlement_title"),
        viewModel.getString("supplier_bills_title"),
        "Category & Menu Setup"
    )
    // Staff must not see supplier ledger or profit analytics.
    val visibleTabs = subTabs.indices.filter { isOwner || (it != 3) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(visibleTabs) { index ->
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
            3 -> if (isOwner) SupplierPurchasesView(viewModel, context)
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
    val shopExpensesCash by viewModel.shopExpensesCashToday.collectAsState(initial = 0.0)
    val supplierCashPaid by viewModel.supplierCashPaidToday.collectAsState(initial = 0.0)

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
                        Text("• (+) Cash Sales:", fontSize = 12.sp, color = MaterialTheme.appColors.success)
                        Text("+ ₹${cashSales.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.appColors.success)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• (+) Credit Cash Recovery:", fontSize = 12.sp, color = MaterialTheme.appColors.success)
                        Text("+ ₹${udharCashRepay.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.appColors.success)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• (-) Shop Cash Expenses:", fontSize = 12.sp, color = MaterialTheme.appColors.danger)
                        Text("- ₹${shopExpensesCash.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.appColors.danger)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("• (-) Cash Paid to Suppliers:", fontSize = 12.sp, color = MaterialTheme.appColors.danger)
                        Text("- ₹${supplierCashPaid.toInt()}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.appColors.danger)
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
                    AppTextField(
                        value = actualCashText,
                        onValueChange = { actualCashText = it },
                        label = "Counted Cash in Till (₹)",
                        keyboardType = KeyboardType.Decimal
                    )

                    val diffColor = if (difference == 0.0) MaterialTheme.appColors.success else if (difference < 0) MaterialTheme.appColors.danger else MaterialTheme.appColors.info
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.success, contentColor = MaterialTheme.appColors.onSuccess),
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
        "🍋 Lemons",
        "🥤 Syrup & Gas",
        "🥛 Milk & Shake",
        "🧻 Glasses & Straws",
        "💡 Electricity Bill",
        "🏪 Shop Rent",
        "👷 Labor / Salary",
        "🔧 Machine Repair",
        "📦 Other Miscellaneous"
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // TOP EXPENSE SUMMARY CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.dangerContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Expenses Today", fontSize = 12.sp, color = MaterialTheme.appColors.onDangerContainer, fontWeight = FontWeight.Bold)
                        Text("₹${totalExpenses.toInt()}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.appColors.danger)
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.appColors.danger, contentColor = MaterialTheme.appColors.onDanger),
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
                    items(categoryExpMap.entries.groupBy({ expenseCategoryLabel(it.key) }, { it.value }).mapValues { it.value.sum() }.entries.toList()) { entry ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(entry.key, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${entry.value.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.appColors.danger)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Recent Expenses", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                            Text("${expenseCategoryLabel(exp.category)} • Mode: ${exp.paymentMode}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹${exp.amount.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.appColors.danger)
                            IconButton(onClick = { viewModel.deleteExpense(exp) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.appColors.danger)
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
            title = { Text("+ Add Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select category:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title / Note") }, singleLine = true)
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount (₹)") }, singleLine = true)

                    Text("Paid from", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paymentMode == "CASH",
                            onClick = { paymentMode = "CASH" },
                            label = { Text("💵 Cash (Till)") }
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
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }

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
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.appColors.danger)
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
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete company", tint = MaterialTheme.appColors.danger)
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
                            IconButton(onClick = { productPendingDelete = prod }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.appColors.danger)
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
        EditProductDialog(viewModel = viewModel, product = product, onDismiss = { productBeingEdited = null })
    }
    productPendingDelete?.let { product ->
        DeleteProductDialog(viewModel = viewModel, product = product, onDismiss = { productPendingDelete = null })
    }
}

@Composable
fun EditProductDialog(viewModel: ShopViewModel, product: Product, onDismiss: () -> Unit) {
    var name by remember(product.id) { mutableStateOf(product.name) }
    var priceText by remember(product.id) { mutableStateOf(product.price.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    viewModel.updateProduct(product.copy(name = name.trim(), price = price))
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DeleteProductDialog(viewModel: ShopViewModel, product: Product, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete item?", fontWeight = FontWeight.Bold) },
        text = {
            Text("'${product.name}' will be removed from the menu and POS. Past sales and reports are not affected.")
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.deleteProductWithLog(product)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.appColors.danger,
                    contentColor = MaterialTheme.appColors.onDanger
                )
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
