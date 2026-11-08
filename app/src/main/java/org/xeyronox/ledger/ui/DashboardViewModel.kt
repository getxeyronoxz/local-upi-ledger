package org.xeyronox.ledger.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.xeyronox.ledger.data.repository.TransactionRepository
import org.xeyronox.ledger.domain.model.Transaction
import org.xeyronox.ledger.export.ExportManager

data class DashboardUiState(
    val creditTotal: Double = 0.0,
    val debitTotal: Double = 0.0,
    val netBalance: Double = 0.0,
    val transactions: List<Transaction> = emptyList(),
    val isExporting: Boolean = false,
    val exportStatusMessage: String? = null
)

class DashboardViewModel(
    private val repository: TransactionRepository,
    private val exportManager: ExportManager = ExportManager
) : ViewModel() {

    private val _isExporting = MutableStateFlow(false)
    private val _exportStatusMessage = MutableStateFlow<String?>(null)

    private val ledgerStatsFlow = combine(
        repository.getCreditTotal(),
        repository.getDebitTotal(),
        repository.getNetBalance(),
        repository.getAllTransactions()
    ) { credit, debit, net, transactions ->
        DashboardUiState(
            creditTotal = credit,
            debitTotal = debit,
            netBalance = net,
            transactions = transactions
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        ledgerStatsFlow,
        _isExporting,
        _exportStatusMessage
    ) { baseState, isExporting, exportMsg ->
        baseState.copy(
            isExporting = isExporting,
            exportStatusMessage = exportMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DashboardUiState()
    )

    fun exportTransactions(
        context: Context,
        password: CharSequence,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (password.length < 6) {
            onComplete(false, "Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _isExporting.value = true
            _exportStatusMessage.value = "Encrypting ledger export..."
            try {
                val currentTransactions = uiState.value.transactions
                val exportedPath = exportManager.exportToDownloadsFolder(
                    context = context,
                    transactions = currentTransactions,
                    password = password.toString()
                )
                _exportStatusMessage.value = "Exported ${currentTransactions.size} transactions to Downloads/LocalLedger"
                onComplete(true, "Saved: $exportedPath")
            } catch (e: Exception) {
                _exportStatusMessage.value = "Export failed: ${e.localizedMessage}"
                onComplete(false, e.localizedMessage ?: "Error during export")
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun clearExportStatus() {
        _exportStatusMessage.value = null
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
}
