package org.xeyronox.ledger.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.xeyronox.ledger.LocalLedgerApp
import org.xeyronox.ledger.ui.theme.LocalLedgerTheme

enum class LedgerNavigation {
    DASHBOARD, TRANSACTIONS
}

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as LocalLedgerApp
                return DashboardViewModel(app.repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            LocalLedgerTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var currentNav by remember { mutableStateOf(LedgerNavigation.DASHBOARD) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showPermissionsGuide by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentNav == LedgerNavigation.DASHBOARD,
                    onClick = { currentNav = LedgerNavigation.DASHBOARD },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Overview") }
                )
                NavigationBarItem(
                    selected = currentNav == LedgerNavigation.TRANSACTIONS,
                    onClick = { currentNav = LedgerNavigation.TRANSACTIONS },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Transactions") },
                    label = { Text("Ledger (${uiState.transactions.size})") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentNav) {
                LedgerNavigation.DASHBOARD -> {
                    DashboardScreen(
                        uiState = uiState,
                        onNavigateToTransactions = { currentNav = LedgerNavigation.TRANSACTIONS },
                        onOpenExportDialog = { showExportDialog = true },
                        onOpenPermissionsGuide = { showPermissionsGuide = true }
                    )
                }
                LedgerNavigation.TRANSACTIONS -> {
                    TransactionListScreen(
                        transactions = uiState.transactions,
                        onDeleteTransaction = { id -> viewModel.deleteTransaction(id) }
                    )
                }
            }

            if (showExportDialog) {
                ExportDialog(
                    transactionCount = uiState.transactions.size,
                    isExporting = uiState.isExporting,
                    onExportRequested = { password ->
                        viewModel.exportTransactions(
                            context = context,
                            password = password
                        ) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            if (success) {
                                showExportDialog = false
                            }
                        }
                    },
                    onDismiss = { showExportDialog = false }
                )
            }

            if (showPermissionsGuide) {
                PermissionsGuideDialog(
                    onDismiss = { showPermissionsGuide = false }
                )
            }
        }
    }
}
