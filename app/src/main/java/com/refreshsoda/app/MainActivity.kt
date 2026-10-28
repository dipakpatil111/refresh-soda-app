package com.refreshsoda.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.refreshsoda.app.ui.screens.*
import com.refreshsoda.app.ui.theme.AppThemeMode
import com.refreshsoda.app.ui.theme.RefreshSodaTheme
import com.refreshsoda.app.ui.viewmodel.ShopViewModel
import com.refreshsoda.app.ui.viewmodel.ShopViewModelFactory

enum class Screen(val titleKey: String, val icon: ImageVector) {
    Dashboard("tab_dashboard", Icons.Default.Dashboard),
    POS("tab_pos", Icons.Default.PointOfSale),
    Analytics("tab_analytics", Icons.Default.InsertChart),
    Inventory("tab_stock", Icons.Default.Inventory2),
    Khata("tab_khata", Icons.Default.MenuBook),
    Hisaab("tab_hisaab", Icons.Default.AccountBalanceWallet),
    Settings("tab_settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: ShopViewModel by viewModels {
        ShopViewModelFactory((application as RefreshApp).database.shopDao(), applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val userProfile by viewModel.userProfile.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()
            val sessionChecked by viewModel.sessionChecked.collectAsState()
            var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
            var showLogoutDialog by remember { mutableStateOf(false) }
            var showExitDialog by remember { mutableStateOf(false) }

            BackHandler(enabled = currentUser != null) {
                if (currentScreen != Screen.Dashboard) {
                    currentScreen = Screen.Dashboard
                } else {
                    showExitDialog = true
                }
            }

            RefreshSodaTheme(themeMode = themeMode) {
                if (!sessionChecked) {
                    Box(modifier = Modifier.fillMaxSize())
                } else if (currentUser == null) {
                    AuthScreen(
                        viewModel = viewModel,
                        onLoginSuccess = { currentScreen = Screen.Dashboard }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            @OptIn(ExperimentalMaterial3Api::class)
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            userProfile.shopName.ifBlank {
                                                currentUser?.shopName?.takeIf { it.isNotBlank() } ?: "Refresh Soda & Ice-cream"
                                            },
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (currentUser?.role == "OWNER") "👑 ${currentUser?.name}" else "👤 ${currentUser?.name} (Staff)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (currentUser?.role == "OWNER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    // Quick Theme Toggle Icon
                                    IconButton(onClick = {
                                        val nextMode = when (themeMode) {
                                            AppThemeMode.LIGHT -> AppThemeMode.DARK
                                            AppThemeMode.DARK -> AppThemeMode.SYSTEM
                                            else -> AppThemeMode.LIGHT
                                        }
                                        viewModel.setThemeMode(nextMode)
                                    }) {
                                        Icon(
                                            imageVector = when (themeMode) {
                                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                                AppThemeMode.DARK -> Icons.Default.DarkMode
                                                else -> Icons.Default.BrightnessAuto
                                            },
                                            contentDescription = "Theme Toggle"
                                        )
                                    }

                                    // Settings Gear Icon
                                    IconButton(onClick = { currentScreen = Screen.Settings }) {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = if (currentScreen == Screen.Settings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Logout / Switch User Icon
                                    IconButton(onClick = { showLogoutDialog = true }) {
                                        Icon(
                                            Icons.Default.Logout,
                                            contentDescription = "Logout",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                val bottomTabs = listOf(
                                    Screen.Dashboard,
                                    Screen.POS,
                                    Screen.Analytics,
                                    Screen.Inventory,
                                    Screen.Khata,
                                    Screen.Hisaab
                                )
                                bottomTabs.forEach { screen ->
                                    val labelText = viewModel.getString(screen.titleKey)
                                    NavigationBarItem(
                                        selected = currentScreen == screen,
                                        onClick = { currentScreen = screen },
                                        icon = { Icon(screen.icon, contentDescription = labelText) },
                                        label = { Text(labelText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier.padding(innerPadding),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            when (currentScreen) {
                                Screen.Dashboard -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToPOS = { currentScreen = Screen.POS },
                                    onNavigateToAnalytics = { currentScreen = Screen.Analytics }
                                )
                                Screen.POS -> POSScreen(viewModel = viewModel)
                                Screen.Analytics -> AnalyticsScreen(viewModel = viewModel)
                                Screen.Inventory -> InventoryScreen(viewModel = viewModel)
                                Screen.Khata -> KhataScreen(viewModel = viewModel)
                                Screen.Hisaab -> HisaabScreen(
                                    viewModel = viewModel,
                                    onOpenSettings = { currentScreen = Screen.Settings }
                                )
                                Screen.Settings -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }

                    if (showLogoutDialog) {
                        AlertDialog(
                            onDismissRequest = { showLogoutDialog = false },
                            title = { Text(viewModel.getString("logout_btn"), fontWeight = FontWeight.Bold) },
                            text = {
                                Text("${viewModel.getString("logged_in_as")} ${currentUser?.name}\n\n${viewModel.getString("switch_user_btn")}?")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showLogoutDialog = false
                                        viewModel.logout()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(viewModel.getString("logout_btn"))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLogoutDialog = false }) {
                                    Text(viewModel.getString("cancel"))
                                }
                            }
                        )
                    }

                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            title = { Text("दुकान ऐप बंद करें?", fontWeight = FontWeight.Bold) },
                            text = { Text("क्या आप सचमुच ऐप से बाहर जाना चाहते हैं?") },
                            confirmButton = {
                                Button(
                                    onClick = { finish() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("हां, बंद करें (Exit)")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExitDialog = false }) {
                                    Text(viewModel.getString("cancel"))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
