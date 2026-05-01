package com.planora.app.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planora.app.core.data.database.entities.Account
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType
import com.planora.app.core.data.repository.AccountRepository
import com.planora.app.core.data.repository.SavingsGoalRepository
import com.planora.app.core.data.repository.BudgetRepository
import com.planora.app.core.data.repository.TransactionRepository
import com.planora.app.core.utils.DateUtils
import com.planora.app.core.utils.PrefsManager
import com.planora.app.core.sms.PdfImportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MoneyPeriod { ALL, DAILY, WEEKLY, MONTHLY }

data class MoneyUiState(
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val balance: Double = 0.0,
    val period: MoneyPeriod = MoneyPeriod.ALL,
    val categories: List<String> = emptyList(),
    val categorySpending: Map<String, Double> = emptyMap(),
    val currencySymbol: String = "$",
    val accounts: List<Account> = emptyList(),
    val activeAccountId: Long = -1L
)

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val accountRepository: AccountRepository,
    val prefsManager: PrefsManager,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val pdfImportManager: PdfImportManager
) : ViewModel() {

    fun getRepository() = repository
    fun getAccountRepository() = accountRepository

    private val _period = MutableStateFlow(MoneyPeriod.ALL)
    private val _activeAccountId = MutableStateFlow(-1L)

    val uiState: StateFlow<MoneyUiState> = combine(
        _period,
        _activeAccountId,
        repository.getAllTransactions().distinctUntilChanged(),
        accountRepository.getAll().distinctUntilChanged(),
        prefsManager.currencySymbol.distinctUntilChanged()
    ) { period, accountId, allTrans, accounts, currency ->
        val now = System.currentTimeMillis()
        
        // Auto-select default account if none selected and accounts exist
        val activeId = if (accountId == -1L && accounts.isNotEmpty()) {
            val defaultAcc = accounts.firstOrNull { it.isDefault } ?: accounts.first()
            _activeAccountId.value = defaultAcc.id
            defaultAcc.id
        } else {
            accountId
        }

        // Filter by account and period
        val accountFilter = if (activeId != -1L) allTrans.filter { it.accountId == activeId } else allTrans
        
        val filtered = when (period) {
            MoneyPeriod.ALL     -> accountFilter
            MoneyPeriod.DAILY   -> accountFilter.filter { it.date in DateUtils.getDayStart(now)..DateUtils.getDayEnd(now) }
            MoneyPeriod.WEEKLY  -> accountFilter.filter { it.date >= DateUtils.getWeekStart(now) }
            MoneyPeriod.MONTHLY -> accountFilter.filter { it.date >= DateUtils.getMonthStart(now) }
        }
        
        var income = 0.0
        var expense = 0.0
        val categorySpending = mutableMapOf<String, Double>()
        
        for (t in filtered) {
            if (t.type == TransactionType.INCOME) {
                income += t.amount
            } else {
                expense += t.amount
                categorySpending[t.category] = (categorySpending[t.category] ?: 0.0) + t.amount
            }
        }

        val categories = allTrans.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
        
        MoneyUiState(
            transactions     = filtered,
            totalIncome      = income,
            totalExpense     = expense,
            balance          = income - expense,
            period           = period,
            categories       = categories,
            categorySpending = categorySpending,
            currencySymbol   = currency,
            accounts         = accounts,
            activeAccountId  = activeId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MoneyUiState())

    fun setPeriod(period: MoneyPeriod)    { _period.value = period }
    fun setActiveAccount(id: Long)        { _activeAccountId.value = id }
    
    fun addTransaction(t: Transaction)    = viewModelScope.launch { repository.insertTransaction(t) }
    fun saveTransaction(t: Transaction)   = viewModelScope.launch { repository.insertTransaction(t) }
    fun updateTransaction(t: Transaction) = viewModelScope.launch { repository.updateTransaction(t) }
    fun deleteTransaction(t: Transaction) = viewModelScope.launch {
        repository.deleteTransaction(t)
        
        if (t.linkedSavingsGoalId != null) {
            val goal = savingsGoalRepository.getGoalById(t.linkedSavingsGoalId)
            if (goal != null) {
                val newAmount = (goal.currentAmount - t.amount).coerceAtLeast(0.0)
                savingsGoalRepository.updateCurrentAmount(goal.id, newAmount)
                if (goal.isCompleted && newAmount < goal.targetAmount) {
                    savingsGoalRepository.updateCompleted(goal.id, false)
                }
            }
        }
    }
    
    suspend fun getTransactionById(id: Long): Transaction? = repository.getTransactionById(id)

    fun importPdf(uri: android.net.Uri, context: android.content.Context) = viewModelScope.launch {
        val result = pdfImportManager.importFromPdf(uri)
        if (result.isSuccess) {
            val count = result.getOrNull() ?: 0
            android.widget.Toast.makeText(context, "Imported $count transactions", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

