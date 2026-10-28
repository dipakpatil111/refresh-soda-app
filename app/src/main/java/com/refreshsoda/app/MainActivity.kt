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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                            if (currentScreen != Screen.Dashboard && currentScreen != Screen.POS && currentScreen != Screen.Inventory) {
                                @OptIn(ExperimentalMaterial3Api::class)
                                TopAppBar(
                                    title = {
                                        Text(
                                            when (currentScreen) {
                                                Screen.Analytics -> "Analytics & Graphs"
                                                Screen.Khata -> "Udhaar / Khata"
                                                Screen.Hisaab -> "Daily Hisaab"
                                                Screen.Settings -> "Settings & Backup"
                                                else -> viewModel.getString(currentScreen.titleKey)
                                            },
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { currentScreen = Screen.Dashboard }) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                        }
                                    },
                                    actions = {
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
                                                contentDescription = "Theme"
                                            )
                                        }
                                        IconButton(onClick = { showLogoutDialog = true }) {
                                            Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        },
                        bottomBar = {
                            if (currentScreen != Screen.POS) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 6.dp,
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .height(60.dp)
                                            .padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        // 1. Home
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable { currentScreen = Screen.Dashboard }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = "Home",
                                                tint = if (currentScreen == Screen.Dashboard) Color(0xFF366A55) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                "Home",
                                                fontSize = 10.sp,
                                                fontWeight = if (currentScreen == Screen.Dashboard) FontWeight.Bold else FontWeight.Medium,
                                                color = if (currentScreen == Screen.Dashboard) Color(0xFF366A55) else Color(0xFF94A3B8)
                                            )
                                        }

                                        // 2. Stock
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable { currentScreen = Screen.Inventory }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Inventory2,
                                                contentDescription = "Stock",
                                                tint = if (currentScreen == Screen.Inventory) Color(0xFF366A55) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                "Stock",
                                                fontSize = 10.sp,
                                                fontWeight = if (currentScreen == Screen.Inventory) FontWeight.Bold else FontWeight.Medium,
                                                color = if (currentScreen == Screen.Inventory) Color(0xFF366A55) else Color(0xFF94A3B8)
                                            )
                                        }

                                        // 3. Center FAB (+)
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFF5C7A9))
                                                .clickable { currentScreen = Screen.POS },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "New Sale",
                                                tint = Color(0xFF1E293B),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        // 4. Udhaar
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable { currentScreen = Screen.Khata }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = "Udhaar",
                                                tint = if (currentScreen == Screen.Khata) Color(0xFF366A55) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                "Udhaar",
                                                fontSize = 10.sp,
                                                fontWeight = if (currentScreen == Screen.Khata) FontWeight.Bold else FontWeight.Medium,
                                                color = if (currentScreen == Screen.Khata) Color(0xFF366A55) else Color(0xFF94A3B8)
                                            )
                                        }

                                        // 5. Hisaab
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable { currentScreen = Screen.Hisaab }
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountBalanceWallet,
                                                contentDescription = "Hisaab",
                                                tint = if (currentScreen == Screen.Hisaab) Color(0xFF366A55) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                "Hisaab",
                                                fontSize = 10.sp,
                                                fontWeight = if (currentScreen == Screen.Hisaab) FontWeight.Bold else FontWeight.Medium,
                                                color = if (currentScreen == Screen.Hisaab) Color(0xFF366A55) else Color(0xFF94A3B8)
                                            )
                                        }
                                    }
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
                                    onNavigateToAnalytics = { currentScreen = Screen.Analytics },
                                    onNavigateToInventory = { currentScreen = Screen.Inventory },
                                    onNavigateToKhata = { currentScreen = Screen.Khata },
                                    onNavigateToHisaab = { currentScreen = Screen.Hisaab },
                                    onOpenSettings = { currentScreen = Screen.Settings },
                                    onLogout = { showLogoutDialog = true }
                                )
                                Screen.POS -> POSScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentScreen = Screen.Dashboard }
                                )
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
                            title = { Text("Exit Shop App?", fontWeight = FontWeight.Bold) },
                            text = { Text("Do you really want to close the app?") },
                            confirmButton = {
                                Button(
                                    onClick = { finish() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Yes, Exit")
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
