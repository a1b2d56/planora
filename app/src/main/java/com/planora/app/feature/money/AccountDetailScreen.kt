package com.planora.app.feature.money

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.R
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.data.database.entities.TransactionType
import com.planora.app.core.ui.components.*
import com.planora.app.core.ui.theme.ExpenseRed
import com.planora.app.core.ui.theme.IncomeGreen
import com.planora.app.core.utils.BrandIcons
import com.planora.app.core.utils.CategoryIcon
import com.planora.app.core.utils.DateUtils
import com.planora.app.core.utils.FormatUtils

@Composable
fun AccountDetailScreen(
    accountId: Long,
    onBack: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(accountId) {
        viewModel.setAccountId(accountId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sym = uiState.currencySymbol
    var showTransferSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DetailTopBar(uiState.account?.name ?: "Account", onBack) }
    ) { paddingValues ->
        if (showTransferSheet) {
            TransferSheet(onDismiss = { showTransferSheet = false })
        }
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    // Premium Hero Card
                    PatternedCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val brandIcon = remember(uiState.account?.name) { 
                            uiState.account?.name?.let { BrandIcons.getIconForBank(it) } 
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (brandIcon != null) {
                                        Icon(painterResource(brandIcon), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(painterResource(R.drawable.ic_account_balance), null, modifier = Modifier.size(20.dp), tint = Color.White)
                                    }
                                    Text(uiState.account?.type?.name ?: "Personal", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                                
                                // Transfer Action Chip
                                Surface(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showTransferSheet = true },
                                    color = Color.White.copy(alpha = 0.2f),
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(painterResource(R.drawable.ic_shortcut_transfer), null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Text("Transfer", style = MaterialTheme.typography.labelSmall, color = Color.White)
                                    }
                                }
                            }
                            Text(FormatUtils.formatCurrency(uiState.balance, sym, forcePlus = true), 
                                style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color.White)
                            Text("Current Balance", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }

                item {
                    // Stats Row
                    StatSummaryRow(
                        income = uiState.totalIncome,
                        expense = uiState.totalExpense,
                        currencySymbol = sym,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                item {
                    // Trend Chart
                    PlanoraCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Balance Trend (30d)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            BalanceTrendChart(
                                points = uiState.trendPoints,
                                modifier = Modifier.fillMaxWidth().height(160.dp)
                            )
                        }
                    }
                }

                item {
                    SectionHeader("Recent Activity", modifier = Modifier.padding(top = 16.dp))
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        EmptyState(R.drawable.ic_attach_money, "Clean Slate", "No transactions recorded for this account yet.")
                    }
                } else {
                    items(uiState.transactions, key = { it.id }) { txn ->
                        TransactionItem(
                            transaction = txn,
                            sym = sym,
                            onClick = { onNavigateToEditTransaction(txn.id) },
                            onDelete = { viewModel.deleteTransaction(txn) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(transaction: Transaction, sym: String, onClick: () -> Unit, onDelete: () -> Unit) {
    val isIncome = transaction.type == TransactionType.INCOME
    val color    = if (isIncome) IncomeGreen else ExpenseRed
    val iconRes  = CategoryIcon.forCategory(transaction.category)

    SwipeToDeleteBox(onDelete = onDelete, modifier = Modifier.padding(horizontal = 16.dp)) {
        PlanoraCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), containerColor = MaterialTheme.colorScheme.surface) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(iconRes), null, tint = color, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(transaction.merchant.ifBlank { transaction.category }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (transaction.note.isNotBlank()) {
                         Text(transaction.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Text(DateUtils.formatDate(transaction.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(FormatUtils.formatCurrency(if (isIncome) transaction.amount else -transaction.amount, sym, forcePlus = isIncome), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}
