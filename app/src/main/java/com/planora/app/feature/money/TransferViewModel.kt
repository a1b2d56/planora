package com.planora.app.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planora.app.core.data.database.entities.Account
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType
import com.planora.app.core.data.repository.AccountRepository
import com.planora.app.core.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransferUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.getAll().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun performTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String
    ) {
        if (fromAccountId == toAccountId) {
            _uiState.update { it.copy(error = "Source and destination accounts must be different.") }
            return
        }
        if (amount <= 0) {
            _uiState.update { it.copy(error = "Amount must be greater than zero.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val transferTxn = Transaction(
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    category = "Transfer",
                    note = note,
                    accountId = fromAccountId,
                    toAccountId = toAccountId,
                    merchant = "Internal Transfer"
                )
                transactionRepository.insertTransaction(transferTxn)
                _uiState.update { it.copy(isLoading = false, success = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Transfer failed") }
            }
        }
    }
}
