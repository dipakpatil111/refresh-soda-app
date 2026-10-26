package com.refreshsoda.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.refreshsoda.app.ui.screens.*
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import com.refreshsoda.app.ui.viewmodel.ShopViewModelFactory

enum class Screen(val title: String, val icon: ImageVector) {
    Dashboard("Home", Icons.Default.Dashboard),
    POS("POS Sale", Icons.Default.PointOfSale),
    Inventory("Box Stock", Icons.Default.Inventory2),
    Khata("Udhaar", Icons.Default.MenuBook),
    MenuSetup("Menu Setup", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ShopViewModel by viewModels {
        ShopViewModelFactory((application as RefreshApp).database.shopDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Refresh Soda & Ice-cream", style = MaterialTheme.typography.titleMedium) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.White,
                                titleContentColor = Color(0xFF0F172A)
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(containerColor = Color.White) {
                            Screen.values().forEach { screen ->
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = TealPrimary,
                                        selectedTextColor = TealPrimary,
                                        indicatorColor = Color(0xFFCCFBF1)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            Screen.Dashboard -> DashboardScreen(viewModel)
                            Screen.POS -> POSScreen(viewModel)
                            Screen.Inventory -> InventoryScreen(viewModel)
                            Screen.Khata -> KhataScreen(viewModel)
                            Screen.MenuSetup -> MenuSetupScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
