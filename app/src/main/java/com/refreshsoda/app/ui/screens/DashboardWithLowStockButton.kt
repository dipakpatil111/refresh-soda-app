package com.refreshsoda.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.refreshsoda.app.ui.viewmodel.ShopViewModel

@Composable
fun DashboardWithLowStockButton(
    viewModel: ShopViewModel,
    onNavigateToPOS: () -> Unit = {},
    onNavigateToMaalAavak: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToPurchaseReport: () -> Unit = {}
) {
    val categories by viewModel.categories.collectAsState()
    val varietyStocks by viewModel.iceCreamVarietyStock.collectAsState()
    var showLowStockDialog by remember { mutableStateOf(false) }

    val lowStockItems = varietyStocks.filter { stock ->
        stock.varietyName != "Opening stock (unassigned)" &&
            categories.any { category ->
                category.trackBoxes && category.name.equals(stock.categoryName, ignoreCase = true)
            } &&
            stock.sealedBoxes <= stock.lowStockLimit
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DashboardScreen(
            viewModel = viewModel,
            onNavigateToPOS = onNavigateToPOS,
            onNavigateToMaalAavak = onNavigateToMaalAavak,
            onNavigateToAnalytics = onNavigateToAnalytics,
            onNavigateToPurchaseReport = onNavigateToPurchaseReport
        )

        FloatingActionButton(
            onClick = { showLowStockDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp),
            containerColor = if (lowStockItems.isEmpty()) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Low stock warning"
            )
            if (lowStockItems.isNotEmpty()) {
                Text(" ${lowStockItems.size}")
            }
        }
    }

    if (showLowStockDialog) {
        LowStockWarningDialog(
            items = lowStockItems,
            onDismiss = { showLowStockDialog = false },
            onOpenStock = onNavigateToMaalAavak
        )
    }
}
