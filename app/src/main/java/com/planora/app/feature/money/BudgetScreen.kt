@file:Suppress("UNUSED_VALUE")
package com.planora.app.feature.money

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import com.planora.app.core.ui.components.*
import com.planora.app.core.ui.theme.ExpenseRed
import com.planora.app.core.ui.theme.IncomeGreen
import com.planora.app.core.utils.CategoryIcon
import com.planora.app.core.utils.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun BudgetScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val progressList by viewModel.progress.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showAddSheet by remember { mutableStateOf(false) }

    PlanoraScreen(
        title = "Monthly Budgets",
        onBack = onBack,
        scrollable = false,
        actionButtonLabel = "New Budget",
        onActionButtonClick = { showAddSheet = true }
    ) {
        if (progressList.isEmpty()) {
            EmptyState(R.drawable.ic_attach_money, "No Budgets Yet", "Set limits for your spending categories.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(progressList, key = { it.budget.id }) { item ->
                    SwipeToDeleteBox(
                        onDelete = { viewModel.delete(item.budget) }
                    ) {
                        BudgetCard(item)
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            AddBudgetSheet(
                viewModel = viewModel,
                onDismiss = {
                    scope.launch { bottomSheetState.hide() }.invokeOnCompletion { showAddSheet = false }
                }
            )
        }
    }
}

@Composable
private fun BudgetCard(progress: BudgetProgress) {
    val budget = progress.budget
    val isOver = progress.ratio >= 1f
    val isAlert = progress.ratio >= budget.alertAt && !isOver
    
    val color = when {
        isOver -> ExpenseRed
        isAlert -> MaterialTheme.colorScheme.tertiary
        else -> IncomeGreen
    }

    val animatedRatio by animateFloatAsState(targetValue = progress.ratio, animationSpec = tween(500), label = "progress")

    PlanoraCard(modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(CategoryIcon.forCategory(budget.category)), null, tint = color, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(budget.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${FormatUtils.formatCurrency(progress.spent, compact = true)} spent of ${FormatUtils.formatCurrency(budget.monthlyLimit, compact = true)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(progress.ratio * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color
                )
            }
            
            // Progress Bar
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedRatio)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
    }
}
