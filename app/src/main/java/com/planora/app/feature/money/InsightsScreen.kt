package com.planora.app.feature.money

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.R
import com.planora.app.core.ui.components.*
import com.planora.app.core.ui.theme.ExpenseRed
import com.planora.app.core.ui.theme.IncomeGreen
import com.planora.app.core.utils.FormatUtils
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sym = uiState.currencySymbol

    PlanoraScreen(title = "Financial Insights", onBack = onBack) {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            // Pacing Card
            uiState.monthlyPacing?.let { pacing ->
                SectionHeader("Budget Pacing")
                PlanoraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Safe to spend today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(FormatUtils.formatCurrency(pacing.safeToSpendToday, sym), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            CircularProgressIndicator(
                                progress = { (pacing.safeToSpendToday / pacing.dailyBudget).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.size(48.dp),
                                color = if (pacing.safeToSpendToday > 0) IncomeGreen else ExpenseRed,
                                strokeWidth = 6.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Projected Month End", style = MaterialTheme.typography.labelMedium)
                            Text(FormatUtils.formatCurrency(pacing.projectedMonthEnd, sym), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } ?: run {
                SectionHeader("Budget Pacing")
                EmptyState(R.drawable.ic_dashboard, "No Budgets Set", "Set a monthly budget to unlock pacing insights.")
            }

            // Top Merchants
            SectionHeader("Top Merchants")
            if (uiState.topMerchants.isNotEmpty()) {
                PlanoraCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.topMerchants.forEach { (merchant, amount) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                        Text(merchant.first().uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(merchant, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                                Text(FormatUtils.formatCurrency(-amount, sym), style = MaterialTheme.typography.labelLarge, color = ExpenseRed)
                            }
                        }
                    }
                }
            } else {
                EmptyState(R.drawable.ic_attach_money, "No data", "No merchant activity recorded this month.")
            }

            // Top Categories (Chart)
            SectionHeader("Top Categories")
            if (uiState.topCategories.isNotEmpty()) {
                PlanoraCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surface) {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp)) {
                        val bars = uiState.topCategories
                            .map { (cat, amt) ->
                                Bars(
                                    label = cat,
                                    values = listOf(
                                        Bars.Data(label = "Expense", value = amt, color = SolidColor(ExpenseRed))
                                    )
                                )
                            }
                        ColumnChart(
                            modifier = Modifier.fillMaxSize(),
                            data = bars,
                            barProperties = BarProperties(spacing = 4.dp, thickness = 20.dp),
                            animationSpec = androidx.compose.animation.core.tween(1000)
                        )
                    }
                }
            } else {
                EmptyState(R.drawable.ic_check_circle, "No Categories", "Categorize your expenses to view breakdown.")
            }
        }
    }
}
