package com.refreshsoda.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.ui.theme.appColors
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dashboard card: how much cash is in the shop and how much money is online, with an entry dialog. */
@Composable
fun ShopBalanceCard(viewModel: ShopViewModel) {
    val balance by viewModel.shopBalance.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val sinceFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    fun rs(v: Double) = "₹${"%,.0f".format(Locale.US, v)}"

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Shop Balance", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                TextButton(onClick = { showDialog = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(if (balance.hasBase) "Set / Adjust" else "Enter balance", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            if (!balance.hasBase) {
                Text(
                    "Shop me abhi kitna cash hai aur online kitna hai, ye ek baar enter karo. Uske baad sales, kharche, udhaar recovery aur supplier payment se ye apne aap update hota rahega.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.appColors.successContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("💵 Cash in shop", fontSize = 11.sp, color = MaterialTheme.appColors.onSuccessContainer, maxLines = 1)
                            Text(rs(balance.cash), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.appColors.onSuccessContainer)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.appColors.infoContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("📱 Online", fontSize = 11.sp, color = MaterialTheme.appColors.info, maxLines = 1)
                            Text(rs(balance.online), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.appColors.info)
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total (cash + online)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rs(balance.total), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    "Entered on ${sinceFormat.format(Date(balance.setAt))}; uske baad ki sab entries judi hain.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDialog) {
        var cashText by remember { mutableStateOf(if (balance.hasBase) balance.cash.toLong().toString() else "") }
        var onlineText by remember { mutableStateOf(if (balance.hasBase) balance.online.toLong().toString() else "") }
        val cash = cashText.toDoubleOrNull()
        val online = onlineText.toDoubleOrNull()
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Shop balance (abhi)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cashText,
                        onValueChange = { cashText = it },
                        label = { Text("Cash in shop ₹") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = onlineText,
                        onValueChange = { onlineText = it },
                        label = { Text("Online money ₹ (bank + Paytm)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Text(
                        "Ye amounts abhi ke asli hisaab se bharo. Aage ka calculation yahin se chalega.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = cash != null && online != null && cash >= 0 && online >= 0,
                    onClick = {
                        viewModel.setShopBalance(cash ?: 0.0, online ?: 0.0)
                        showDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}
