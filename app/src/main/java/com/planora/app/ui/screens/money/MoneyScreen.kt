package com.planora.app.ui.screens.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.R
import com.planora.app.data.database.entities.*
import com.planora.app.theme.*
import com.planora.app.ui.components.*
import com.planora.app.ui.viewmodels.MoneyPeriod
import com.planora.app.ui.viewmodels.MoneyViewModel
import com.planora.app.utils.DateUtils
import com.planora.app.utils.FormatUtils



@Composable
fun MoneyScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    viewModel: MoneyViewModel = hiltViewModel()
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sym = uiState.currencySymbol

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            PlanoraFAB(onClick = onNavigateToAddTransaction,
                modifier = Modifier.padding(bottom = 88.dp + navBarPadding)) {
                Icon(painterResource(R.drawable.ic_add), "Add transaction")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 120.dp + navBarPadding)) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 0.dp
                ) {
                    Column {
                        PlanoraTopBar("Money Tracker", subtitle = "Track income & expenses")
                        Spacer(Modifier.height(SpacingSmall))
                    }
                }
            }
            item { BalanceSummaryCard(uiState.balance, uiState.totalIncome, uiState.totalExpense, sym) }
            item { PeriodFilterRow(uiState.period, viewModel::setPeriod) }
            if (uiState.categorySpending.isNotEmpty()) {
                item { SpendingBreakdown(uiState.categorySpending, uiState.totalExpense, sym) }
            }
            item { SectionHeader("Transactions") }
            if (uiState.transactions.isEmpty()) {
                item {
                    EmptyState(R.drawable.ic_attach_money,
                        "No transactions", "Add your first income or expense")
                }
            } else {
                items(uiState.transactions, key = { it.id }) { txn ->
                    TransactionItem(txn, sym,
                        onClick = { onNavigateToEditTransaction(txn.id) },
                        onDelete = { viewModel.deleteTransaction(txn) })
                }
            }
        }
    }
}

@Composable
private fun BalanceSummaryCard(balance: Double, income: Double, expense: Double, sym: String) {
    val primary = MaterialTheme.colorScheme.primary
    val gradEnd = remember(primary) { primary.copy(alpha = 0.15f) }
    GradientCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        gradientStart = MaterialTheme.colorScheme.surfaceContainerLow,
        gradientEnd   = gradEnd) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Total Balance", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(FormatUtils.formatCurrency(balance, sym, forcePlus = true),
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = if (balance >= 0) IncomeGreen else ExpenseRed)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Income and expense as a pair â€” one loop, no duplication
                listOf("Income" to income to (IncomeGreen to R.drawable.ic_arrow_upward),
                       "Expenses" to expense to (ExpenseRed  to R.drawable.ic_arrow_downward)
                ).forEach { (labelAmount, colorIcon) ->
                    val (label, amount) = labelAmount
                    val (color, iconRes) = colorIcon
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(painterResource(iconRes), null, tint = color, modifier = Modifier.size(16.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        }
                        Text(FormatUtils.formatCurrency(amount, sym),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, color = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodFilterRow(currentPeriod: MoneyPeriod, onPeriodChange: (MoneyPeriod) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(SpacingSmall), modifier = Modifier.padding(vertical = SpacingSmall)) {
        items(MoneyPeriod.entries) { period ->
            val label = when (period) {
                MoneyPeriod.ALL     -> "All Time"
                MoneyPeriod.DAILY   -> "Today"
                MoneyPeriod.WEEKLY  -> "This Week"
                MoneyPeriod.MONTHLY -> "This Month"
            }
            CategoryChip(label, currentPeriod == period) { onPeriodChange(period) }
        }
    }
}

@Composable
private fun SpendingBreakdown(categorySpending: Map<String, Double>, totalExpense: Double, sym: String) {
    PlanoraCard(modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingMedium, vertical = SpacingSmall)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Spending by Category", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            SpendingDonutChart(
                categorySpending = categorySpending,
                totalExpense     = totalExpense,
                currencySymbol   = sym,
                donutSize        = 140.dp
            )
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction, sym: String, onClick: () -> Unit, onDelete: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME
    val color    = if (isIncome) IncomeGreen else ExpenseRed
    val iconRes  = if (isIncome) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward

    SwipeToDeleteBox(onDelete = onDelete, modifier = Modifier.padding(horizontal = SpacingMedium)) {
        PlanoraCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.surface) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(iconRes), null, tint = color, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(transaction.category, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                    if (transaction.note.isNotBlank()) Text(transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text(DateUtils.formatDate(transaction.date), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(FormatUtils.formatCurrency(if (isIncome) transaction.amount else -transaction.amount, sym, forcePlus = isIncome),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}
