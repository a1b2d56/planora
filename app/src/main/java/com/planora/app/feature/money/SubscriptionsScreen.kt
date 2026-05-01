package com.planora.app.feature.money

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.R
import com.planora.app.core.data.database.entities.Transaction
import com.planora.app.core.ui.components.*
import com.planora.app.core.ui.theme.ExpenseRed
import com.planora.app.core.utils.CategoryIcon
import com.planora.app.core.utils.DateUtils
import com.planora.app.core.utils.FormatUtils

@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    viewModel: MoneyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscriptions = uiState.transactions.filter { it.isRecurring }

    PlanoraScreen(
        title = "Subscriptions",
        onBack = onBack,
        scrollable = false
    ) {
        if (subscriptions.isEmpty()) {
            EmptyState(
                iconRes = R.drawable.ic_schedule,
                title = "No Subscriptions Found",
                subtitle = "When the app detects recurring charges, they will appear here automatically."
            )
        } else {
            // Group by merchant to show active subs, rather than individual transactions
            val groupedSubs = subscriptions.groupBy { it.merchant.ifBlank { it.category } }
            
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(groupedSubs.toList(), key = { it.first }) { (merchant, txns) ->
                    val latestTxt = txns.maxByOrNull { it.date }
                    if (latestTxt != null) {
                        SubscriptionCard(merchant, latestTxt, uiState.currencySymbol)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionCard(merchant: String, lastTxn: Transaction, sym: String) {
    PlanoraCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(ExpenseRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(CategoryIcon.forCategory("Subscription")), null, tint = ExpenseRed, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(merchant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Last billed: ${DateUtils.formatDate(lastTxn.date)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(FormatUtils.formatCurrency(lastTxn.amount, sym), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
        }
    }
}
