package com.devil.taskzio.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.taskzio.data.database.entities.CalendarEvent
import com.devil.taskzio.data.database.entities.Task
import com.devil.taskzio.data.database.entities.TransactionType
import com.devil.taskzio.data.repository.*
import com.devil.taskzio.utils.DateUtils
import com.devil.taskzio.utils.PrefsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class DashboardUiState(
    val userName: String = "",
    val activeTasks: Int = 0,
    val completedTasks: Int = 0,
    val nextTask: Task? = null,
    val todayEvents: List<CalendarEvent> = emptyList(),
    val balance: Double = 0.0,
    val currencySymbol: String = "$",
    val savingsProgress: Float = 0f,
    val totalSaved: Double = 0.0,
    val totalTarget: Double = 0.0,
    val categorySpending: Map<String, Double> = emptyMap(),
    val totalMonthExpense: Double = 0.0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    taskRepo: TaskRepository,
    calendarRepo: CalendarEventRepository,
    transactionRepo: TransactionRepository,
    savingsRepo: SavingsGoalRepository,
    prefsManager: PrefsManager
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        prefsManager.userName.distinctUntilChanged(),
        prefsManager.currencySymbol.distinctUntilChanged(),
        taskRepo.getAllTasks().distinctUntilChanged(),
        calendarRepo.getAllEvents().distinctUntilChanged(),
        combine(
            transactionRepo.getAllTransactions().distinctUntilChanged(),
            savingsRepo.getAllGoals().distinctUntilChanged()
        ) { transactions, goals -> transactions to goals }
    ) { name, currency, tasks, events, (transactions, goals) ->
        val now   = System.currentTimeMillis()
        val zone  = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        // Single pass: derive active list and counts together
        var activeCount   = 0
        var completedCount = 0
        val active = mutableListOf<Task>()
        for (t in tasks) {
            if (!t.isCompleted) { active.add(t); activeCount++ } else completedCount++
        }
        val nextTask = active.filter { it.dueDate != null && it.dueDate > now }
            .minByOrNull { it.dueDate!! } ?: active.firstOrNull()

        val todayEvents = events.filter { event ->
            if (event.isYearly) {
                val d = java.time.Instant.ofEpochMilli(event.date).atZone(zone).toLocalDate()
                d.month == today.month && d.dayOfMonth == today.dayOfMonth
            } else {
                event.date in DateUtils.getDayStart(now)..DateUtils.getDayEnd(now)
            }
        }

        val totalTarget = goals.sumOf { it.targetAmount }
        val totalSaved  = goals.sumOf { it.currentAmount }

        val monthStart = DateUtils.getMonthStart(now)
        // Single-pass: partition transactions once, derive all aggregates from the two halves
        var allIncome = 0.0
        var allExpense = 0.0
        val monthExpenseMap = mutableMapOf<String, Double>()
        var totalMonthExpense = 0.0

        for (t in transactions) {
            if (t.type == TransactionType.INCOME) {
                allIncome += t.amount
            } else {
                allExpense += t.amount
                if (t.date >= monthStart) {
                    totalMonthExpense += t.amount
                    monthExpenseMap[t.category] = (monthExpenseMap[t.category] ?: 0.0) + t.amount
                }
            }
        }

        DashboardUiState(
            userName          = name,
            activeTasks       = activeCount,
            completedTasks    = completedCount,
            nextTask          = nextTask,
            todayEvents       = todayEvents,
            balance           = allIncome - allExpense,
            currencySymbol    = currency,
            savingsProgress   = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f,
            totalSaved        = totalSaved,
            totalTarget       = totalTarget,
            categorySpending  = monthExpenseMap,
            totalMonthExpense = totalMonthExpense
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
