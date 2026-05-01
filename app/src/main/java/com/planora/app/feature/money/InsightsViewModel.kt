package com.planora.app.feature.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType
import com.planora.app.core.data.repository.BudgetRepository
import com.planora.app.core.data.repository.TransactionRepository
import com.planora.app.core.utils.DateUtils
import com.planora.app.core.utils.PrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class InsightsUiState(
    val transactions: List<Transaction> = emptyList(),
    val topMerchants: List<Pair<String, Double>> = emptyList(),
    val topCategories: List<Pair<String, Double>> = emptyList(),
    val monthlyPacing: PacingInfo? = null,
    val currencySymbol: String = "$",
    val isLoading: Boolean = false
)

data class PacingInfo(
    val dailyBudget: Double,
    val safeToSpendToday: Double,
    val projectedMonthEnd: Double
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    repository: TransactionRepository,
    budgetRepository: BudgetRepository,
    prefsManager: PrefsManager
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.getAllTransactions(),
        budgetRepository.getAllBudgets(),
        prefsManager.currencySymbol
    ) { allTrans, budgets, sym ->
        val now = System.currentTimeMillis()
        val monthStart = DateUtils.getMonthStart(now)
        val monthTransactions = allTrans.filter { it.date >= monthStart }
        
        val expenses = monthTransactions.filter { it.type == TransactionType.EXPENSE }
        
        // Merchant Rankings
        val merchants = expenses.groupBy { it.merchant.ifBlank { "Unknown" } }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // Category Rankings
        val categories = expenses.groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // Pacing Logic
        val totalBudget = budgets.sumOf { it.monthlyLimit }
        val daysInMonth = 30 // Simplified or use calendar
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val dailyBudget = if (totalBudget > 0) totalBudget / daysInMonth else 0.0
        val spentSoFar = expenses.sumOf { it.amount }
        
        val pacing = if (totalBudget > 0) {
            val dailySpent = spentSoFar / currentDay
            PacingInfo(
                dailyBudget = dailyBudget,
                safeToSpendToday = (totalBudget - spentSoFar) / (daysInMonth - currentDay + 1).coerceAtLeast(1),
                projectedMonthEnd = dailySpent * daysInMonth
            )
        } else null

        InsightsUiState(
            transactions = monthTransactions,
            topMerchants = merchants,
            topCategories = categories,
            monthlyPacing = pacing,
            currencySymbol = sym
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState(isLoading = true))
}
