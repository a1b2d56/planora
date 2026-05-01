package com.planora.app.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planora.app.core.data.database.entities.Account
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType
import com.planora.app.core.data.repository.AccountRepository
import com.planora.app.core.data.repository.TransactionRepository
import com.planora.app.core.ui.components.TrendPoint
import com.planora.app.core.utils.DateUtils
import com.planora.app.core.utils.PrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountDetailUiState(
    val account: Account? = null,
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val trendPoints: List<TrendPoint> = emptyList(),
    val currencySymbol: String = "$",
    val isLoading: Boolean = true
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val prefsManager: PrefsManager
) : ViewModel() {

    private val _accountId = MutableStateFlow(-1L)
    
    val uiState: StateFlow<AccountDetailUiState> = _accountId.flatMapLatest { id ->
        if (id == -1L) flowOf(AccountDetailUiState(isLoading = false))
        else {
            combine(
                accountRepository.getAll().map { list -> list.find { it.id == id } },
                transactionRepository.getAllTransactions().map { list -> list.filter { it.accountId == id } },
                prefsManager.currencySymbol
            ) { account, transactions, currency ->
                
                var income = 0.0
                var expense = 0.0
                transactions.forEach {
                    if (it.type == TransactionType.INCOME) income += it.amount
                    else expense += it.amount
                }

                AccountDetailUiState(
                    account = account,
                    transactions = transactions.sortedByDescending { it.date },
                    totalIncome = income,
                    totalExpense = expense,
                    balance = income - expense,
                    trendPoints = calculateTrend(account, transactions),
                    currencySymbol = currency,
                    isLoading = false
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountDetailUiState())

    fun setAccountId(id: Long) {
        _accountId.value = id
    }

    private fun calculateTrend(account: Account?, transactions: List<Transaction>): List<TrendPoint> {
        if (account == null) return emptyList()
        val now = System.currentTimeMillis()
        val startOfToday = DateUtils.getDayStart(now)

        val last30Days = (0..29).map { day ->
            val time = startOfToday - (day * 86_400_000L)
            val dayStart = DateUtils.getDayStart(time)
            val dayEnd = DateUtils.getDayEnd(time)
            
            val dayIncome = transactions.filter { it.type == TransactionType.INCOME && it.date <= dayEnd }.sumOf { it.amount }
            val dayExpense = transactions.filter { it.type == TransactionType.EXPENSE && it.date <= dayEnd }.sumOf { it.amount }
            
            TrendPoint(
                label = DateUtils.formatShortDate(dayStart),
                value = dayIncome - dayExpense
            )
        }.reversed()
        return last30Days
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        transactionRepository.deleteTransaction(transaction)
    }
}
