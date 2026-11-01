package com.refreshsoda.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
    Inventory("tab_stock", Icons.Default.Inventory2),
    MaalAavak("tab_maal_aavak", Icons.Default.LocalShipping),
    Khata("tab_khata", Icons.Default.MenuBook),
    Hisaab("tab_hisaab", Icons.Default.AccountBalanceWallet),
    Settings("tab_settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: ShopViewModel by viewModels {
        ShopViewModelFactory((application as RefreshApp).database.shopDao(), applicationContext)
    }
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val userProfile by viewModel.userProfile.collectAsState()
            val currentUser by viewModel.currentUser.collectAsState()
            val sessionChecked by viewModel.sessionChecked.collectAsState()
            var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
            var showLogoutDialog by remember { mutableStateOf(false) }
            var showExitDialog by remember { mutableStateOf(false) }

            BackHandler(enabled = currentUser != null) {
                if (currentScreen != Screen.Dashboard) currentScreen = Screen.Dashboard else showExitDialog = true
            }

            RefreshSodaTheme(themeMode = themeMode) {
                if (!sessionChecked) {
                    Box(modifier = Modifier.fillMaxSize())
                } else if (currentUser == null) {
                    AuthScreen(viewModel = viewModel, onLoginSuccess = { currentScreen = Screen.Dashboard })
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            @OptIn(ExperimentalMaterial3Api::class)
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(userProfile.shopName.ifBlank { currentUser?.shopName?.takeIf { it.isNotBlank() } ?: "Refresh Soda & Ice-cream" }, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(if (currentUser?.role == "OWNER") "👑 ${currentUser?.name}" else "👤 ${currentUser?.name} (Staff)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (currentUser?.role == "OWNER") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                                    }
                                },
                                actions = {
                                    IconButton(onClick = {
                                        viewModel.setThemeMode(when (themeMode) {
                                            AppThemeMode.LIGHT -> AppThemeMode.DARK
                                            AppThemeMode.DARK -> AppThemeMode.SYSTEM
                                            else -> AppThemeMode.LIGHT
                                        })
                                    }) {
                                        Icon(if (themeMode == AppThemeMode.LIGHT) Icons.Default.LightMode else if (themeMode == AppThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.BrightnessAuto, contentDescription = "Theme Toggle")
                                    }
                                    IconButton(onClick = { currentScreen = Screen.Settings }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                                    IconButton(onClick = { showLogoutDialog = true }) { Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error) }
                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                listOf(Screen.Dashboard, Screen.POS, Screen.Inventory, Screen.MaalAavak, Screen.Khata, Screen.Hisaab).forEach { screen ->
                                    val labelText = viewModel.getString(screen.titleKey)
                                    NavigationBarItem(selected = currentScreen == screen, onClick = { currentScreen = screen }, icon = { Icon(screen.icon, contentDescription = labelText) }, label = { Text(labelText, fontSize = 10.sp, fontWeight = FontWeight.Bold) })
                                }
                            }
                        }
                    ) { innerPadding ->
                        Surface(modifier = Modifier.padding(innerPadding), color = MaterialTheme.colorScheme.background) {
                            when (currentScreen) {
                                Screen.Dashboard -> DashboardWithLowStockButton(viewModel, { currentScreen = Screen.POS }, { currentScreen = Screen.Inventory })
                                Screen.POS -> POSScreen(viewModel)
                                Screen.Inventory -> InventoryScreen(viewModel, { currentScreen = Screen.MaalAavak })
                                Screen.MaalAavak -> MaalAavakScreen(viewModel)
                                Screen.Khata -> KhataScreen(viewModel)
                                Screen.Hisaab -> HisaabScreen(viewModel) { currentScreen = Screen.Settings }
                                Screen.Settings -> SettingsScreen(viewModel)
                            }
                        }
                    }
                    if (showLogoutDialog) {
                        AlertDialog(onDismissRequest = { showLogoutDialog = false }, title = { Text(viewModel.getString("logout_btn"), fontWeight = FontWeight.Bold) }, text = { Text("${viewModel.getString("logged_in_as")} ${currentUser?.name}\n\n${viewModel.getString("switch_user_btn")}?") }, confirmButton = { Button(onClick = { showLogoutDialog = false; viewModel.logout() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(viewModel.getString("logout_btn")) } }, dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text(viewModel.getString("cancel")) } })
                    }
                    if (showExitDialog) {
                        AlertDialog(onDismissRequest = { showExitDialog = false }, title = { Text("Exit app?", fontWeight = FontWeight.Bold) }, text = { Text("Do you really want to leave the app?") }, confirmButton = { Button(onClick = { finish() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Yes, exit") } }, dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text(viewModel.getString("cancel")) } })
                    }
                }
            }
        }
    }
}
