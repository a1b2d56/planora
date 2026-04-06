package com.planora.app.feature.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.planora.app.core.data.database.entities.SavingsGoal
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType
import com.planora.app.core.data.repository.SavingsGoalRepository
import com.planora.app.core.data.repository.TransactionRepository
import com.planora.app.core.utils.PrefsManager
import com.planora.app.core.worker.SavingsReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavingsUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val totalTarget: Double = 0.0,
    val totalSaved: Double = 0.0,
    val currencySymbol: String = "$"
)

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val repository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val workManager: WorkManager,
    prefsManager: PrefsManager
) : ViewModel() {

    val uiState: StateFlow<SavingsUiState> = combine(
        repository.getAllGoals().distinctUntilChanged(),
        prefsManager.currencySymbol.distinctUntilChanged()
    ) { goals, currency ->
        SavingsUiState(
            goals          = goals,
            totalTarget    = goals.sumOf { it.targetAmount },
            totalSaved     = goals.sumOf { it.currentAmount },
            currencySymbol = currency
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavingsUiState())

    fun addGoal(goal: SavingsGoal) = viewModelScope.launch {
        repository.insertGoal(goal)
        if (goal.reminderEnabled) SavingsReminderWorker.schedule(workManager)
    }

    suspend fun getGoalById(id: Long): SavingsGoal? = repository.getGoalById(id)

    fun updateGoal(goal: SavingsGoal) = viewModelScope.launch { repository.updateGoal(goal) }
    fun deleteGoal(goal: SavingsGoal) = viewModelScope.launch {
        transactionRepository.deleteByLinkedGoalId(goal.id)
        repository.deleteGoal(goal)
    }

    /** Adds amount to goal, records a deposit transaction, and auto-completes if target reached. */
    fun addToGoal(goalId: Long, amount: Double, currentAmount: Double, targetAmount: Double, goalTitle: String) =
        viewModelScope.launch {
            val newAmount = (currentAmount + amount).coerceAtLeast(0.0)
            repository.updateCurrentAmount(goalId, newAmount)

            // Auto-complete when target is reached
            if (newAmount >= targetAmount) {
                repository.updateCompleted(goalId, true)
            }

            transactionRepository.insertTransaction(
                Transaction(
                    amount              = amount,
                    type                = TransactionType.INCOME,
                    category            = "Deposit",
                    note                = "Savings: $goalTitle",
                    linkedSavingsGoalId = goalId
                )
            )
        }
}
