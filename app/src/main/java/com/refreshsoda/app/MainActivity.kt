package com.refreshsoda.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.ui.i18n.AppLanguage
import com.refreshsoda.app.ui.screens.*
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import com.refreshsoda.app.ui.viewmodel.ShopViewModelFactory

enum class Screen(val titleKey: String, val icon: ImageVector) {
    Dashboard("tab_dashboard", Icons.Default.Dashboard),
    POS("tab_pos", Icons.Default.PointOfSale),
    Inventory("tab_stock", Icons.Default.Inventory2),
    Khata("tab_khata", Icons.Default.MenuBook),
    Hisaab("tab_hisaab", Icons.Default.AccountBalanceWallet)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ShopViewModel by viewModels {
        ShopViewModelFactory((application as RefreshApp).database.shopDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentLanguage by viewModel.language.collectAsState()
            var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
            var showLanguageMenu by remember { mutableStateOf(false) }

            MaterialTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = {
                                Text(
                                    "Refresh Soda & Ice-cream",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                Box {
                                    FilledTonalButton(
                                        onClick = { showLanguageMenu = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Language,
                                            contentDescription = "Language",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            currentLanguage.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showLanguageMenu,
                                        onDismissRequest = { showLanguageMenu = false }
                                    ) {
                                        AppLanguage.values().forEach { lang ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        lang.displayName,
                                                        fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (lang == currentLanguage) TealPrimary else Color.Unspecified
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.setLanguage(lang)
                                                    showLanguageMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.White,
                                titleContentColor = Color(0xFF0F172A)
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(containerColor = Color.White) {
                            Screen.values().forEach { screen ->
                                val labelText = viewModel.getString(screen.titleKey)
                                NavigationBarItem(
                                    selected = currentScreen == screen,
                                    onClick = { currentScreen = screen },
                                    icon = { Icon(screen.icon, contentDescription = labelText) },
                                    label = { Text(labelText, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
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
                            Screen.Dashboard -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToPOS = { currentScreen = Screen.POS }
                            )
                            Screen.POS -> POSScreen(viewModel = viewModel)
                            Screen.Inventory -> InventoryScreen(viewModel = viewModel)
                            Screen.Khata -> KhataScreen(viewModel = viewModel)
                            Screen.Hisaab -> HisaabScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
