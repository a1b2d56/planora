package com.planora.app.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planora.app.core.data.database.entities.Budget
import com.planora.app.core.data.database.entities.TransactionType
import com.planora.app.core.data.repository.BudgetRepository
import com.planora.app.core.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class BudgetProgress(
    val budget: Budget,
    val spent: Double,
    val ratio: Float
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepo: BudgetRepository,
    transactionRepo: TransactionRepository
) : ViewModel() {

    private val now = Calendar.getInstance()
    val currentMonth: Int = now.get(Calendar.MONTH) + 1
    val currentYear: Int  = now.get(Calendar.YEAR)

    val progress: StateFlow<List<BudgetProgress>> = combine(
        budgetRepo.getForMonth(currentMonth, currentYear),
        transactionRepo.getAllTransactions()
    ) { budgets, transactions ->
        val cal = Calendar.getInstance()
        val monthlyExpenses = transactions.filter { t ->
            t.type == TransactionType.EXPENSE && cal.apply { timeInMillis = t.date }.let {
                it.get(Calendar.MONTH) + 1 == currentMonth && it.get(Calendar.YEAR) == currentYear
            }
        }
        budgets.map { budget ->
            val spent = monthlyExpenses.filter { it.category == budget.category }.sumOf { it.amount }
            BudgetProgress(budget, spent, (spent / budget.monthlyLimit).toFloat().coerceIn(0f, 1f))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun upsert(budget: Budget) = viewModelScope.launch { budgetRepo.upsert(budget) }
    fun delete(budget: Budget) = viewModelScope.launch { budgetRepo.delete(budget) }
}
