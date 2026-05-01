package com.planora.app.feature.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.R
import com.planora.app.core.data.database.entities.*
import com.planora.app.core.ui.theme.*
import com.planora.app.core.ui.components.*
import com.planora.app.core.utils.CategoryIcon
import com.planora.app.core.utils.DateUtils
import com.planora.app.core.utils.FormatUtils

@Composable
fun MoneyScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToSmsImport: () -> Unit,
    viewModel: MoneyViewModel = hiltViewModel()
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sym = uiState.currencySymbol
    val context = androidx.compose.ui.platform.LocalContext.current

    val pdfImportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importPdf(uri, context)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            PlanoraFAB(onClick = onNavigateToAddTransaction, modifier = Modifier.padding(bottom = 104.dp + navBarPadding)) {
                Icon(painterResource(R.drawable.ic_add), "Add transaction")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(bottom = 140.dp + navBarPadding)) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 0.dp
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PlanoraTopBar("Money", subtitle = "Track income & expenses")
                        }
                        
                        if (uiState.accounts.size > 1) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                                item { CategoryChip("All Wallets", uiState.activeAccountId == -1L) { viewModel.setActiveAccount(-1L) } }
                                items(uiState.accounts, key = { it.id }) { acc ->
                                    CategoryChip(acc.name, uiState.activeAccountId == acc.id) { viewModel.setActiveAccount(acc.id) }
                                }
                            }
                        } else {
                             Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToAccounts() }.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                 Text(if (uiState.accounts.isEmpty()) "Add Wallet" else uiState.accounts.first().name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                 Icon(painterResource(R.drawable.ic_chevron_right), null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                             }
                        }
                    }
                }
            }
            item { BalanceSummaryCard(uiState.balance, uiState.totalIncome, uiState.totalExpense, sym, onNavigateToAccounts) }
            
            item {
                QuickToolsSection(
                    onNavigateToBudgets = onNavigateToBudgets,
                    onNavigateToAccounts = onNavigateToAccounts,
                    onNavigateToInsights = onNavigateToInsights,
                    onNavigateToSubscriptions = onNavigateToSubscriptions,
                    onNavigateToSmsImport = onNavigateToSmsImport,
                    onLaunchPdfImport = { pdfImportLauncher.launch("application/pdf") }
                )
            }

            item { PeriodFilterRow(uiState.period, viewModel::setPeriod) }
            if (uiState.categorySpending.isNotEmpty()) {
                item { SpendingBreakdown(uiState.categorySpending, uiState.totalExpense, sym) }
            }
            item { SectionHeader("Transactions") }
            if (uiState.transactions.isEmpty()) {
                item { EmptyState(R.drawable.ic_attach_money, "No transactions", "Add your first income or expense") }
            } else {
                items(uiState.transactions, key = { it.id }) { txn ->
                    TransactionItem(txn, sym, onClick = { onNavigateToEditTransaction(txn.id) }, onDelete = { viewModel.deleteTransaction(txn) })
                }
            }
        }
    }
}

@Composable
private fun BalanceSummaryCard(balance: Double, income: Double, expense: Double, sym: String, onNavigateToAccounts: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val gradEnd = remember(primary) { primary.copy(alpha = 0.15f) }
    GradientCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).clickable { onNavigateToAccounts() },
        gradientStart = MaterialTheme.colorScheme.surfaceContainerLow, gradientEnd   = gradEnd) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Total Balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(FormatUtils.formatCurrency(balance, sym, forcePlus = true), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = if (balance >= 0) IncomeGreen else ExpenseRed)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("Income" to income to (IncomeGreen to R.drawable.ic_arrow_upward),
                       "Expenses" to expense to (ExpenseRed  to R.drawable.ic_arrow_downward)
                ).forEach { (labelAmount, colorIcon) ->
                    val (label, amount) = labelAmount; val (color, iconRes) = colorIcon
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(painterResource(iconRes), null, tint = color, modifier = Modifier.size(16.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        }
                        Text(FormatUtils.formatCurrency(amount, sym), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodFilterRow(currentPeriod: MoneyPeriod, onPeriodChange: (MoneyPeriod) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = SpacingMedium), horizontalArrangement = Arrangement.spacedBy(SpacingSmall), modifier = Modifier.padding(vertical = SpacingSmall)) {
        items(MoneyPeriod.entries) { period ->
            val label = when (period) { MoneyPeriod.ALL -> "All Time"; MoneyPeriod.DAILY -> "Today"; MoneyPeriod.WEEKLY -> "This Week"; MoneyPeriod.MONTHLY -> "This Month" }
            CategoryChip(label, currentPeriod == period) { onPeriodChange(period) }
        }
    }
}

@Composable
private fun SpendingBreakdown(categorySpending: Map<String, Double>, totalExpense: Double, sym: String) {
    PlanoraCard(modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingMedium, vertical = SpacingSmall)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Spending by Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            SpendingDonutChart(categorySpending = categorySpending, totalExpense = totalExpense, currencySymbol = sym, donutSize = 140.dp)
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction, sym: String, onClick: () -> Unit, onDelete: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME
    val color    = if (isIncome) IncomeGreen else ExpenseRed
    val iconRes  = CategoryIcon.forCategory(transaction.category)

    SwipeToDeleteBox(onDelete = onDelete, modifier = Modifier.padding(horizontal = SpacingMedium)) {
        PlanoraCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), containerColor = MaterialTheme.colorScheme.surface) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(iconRes), null, tint = color, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(transaction.merchant.ifBlank { transaction.category }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (transaction.isRecurring) {
                        Text("Recurring", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    } else if (transaction.note.isNotBlank()) {
                         Text(transaction.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Text(DateUtils.formatDate(transaction.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(FormatUtils.formatCurrency(if (isIncome) transaction.amount else -transaction.amount, sym, forcePlus = isIncome), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
private fun QuickToolsSection(
    onNavigateToBudgets: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToSmsImport: () -> Unit,
    onLaunchPdfImport: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        SectionHeader("Tools & Insights")
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile(
                "Budgets", 
                "Limits", 
                R.drawable.ic_dashboard, 
                MaterialTheme.colorScheme.primary, 
                Modifier.weight(1f),
                onNavigateToBudgets
            )
            ToolTile(
                "Insights", 
                "Trends", 
                R.drawable.ic_account_balance, 
                IncomeGreen, 
                Modifier.weight(1f),
                onNavigateToInsights
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile(
                "SMS Import", 
                "Auto", 
                R.drawable.ic_notifications, 
                MaterialTheme.colorScheme.tertiary, 
                Modifier.weight(1f),
                onNavigateToSmsImport
            )
            ToolTile(
                "Subscriptions", 
                "Recurring", 
                R.drawable.ic_cat_subscription, 
                ExpenseRed, 
                Modifier.weight(1f),
                onNavigateToSubscriptions
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ToolTile(
                "Wallets", 
                "Accounts", 
                R.drawable.ic_wallet, 
                MaterialTheme.colorScheme.primary, 
                Modifier.weight(1f),
                onNavigateToAccounts
            )
            ToolTile(
                "PDF Import", 
                "Statements", 
                R.drawable.ic_sticky_note, 
                MaterialTheme.colorScheme.secondary, 
                Modifier.weight(1f),
                onLaunchPdfImport 
            )
        }
    }
}

@Composable
private fun ToolTile(
    title: String,
    subtitle: String,
    iconRes: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    PlanoraCard(
        modifier = modifier.clickable { onClick() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(iconRes), null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

