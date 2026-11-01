package com.refreshsoda.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.data.model.IceCreamVarietyStock

@Composable
fun LowStockWarningDialog(
    items: List<IceCreamVarietyStock>,
    onDismiss: () -> Unit,
    onOpenStock: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Low Stock Warning", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (items.isEmpty()) {
                Text("All ice-cream varieties have sufficient stock.")
            } else {
                Column {
                    Text("${items.size} varieties need attention.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(items) { stock ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stock.varietyName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(stock.companyName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "${stock.sealedBoxes}/${stock.lowStockLimit} boxes",
                                    fontWeight = FontWeight.Bold,
                                    color = if (stock.sealedBoxes == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDismiss(); onOpenStock() }) { Text("Open Stock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
