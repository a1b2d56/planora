package com.planora.app.ui.screens.savings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.R
import com.planora.app.data.database.entities.SavingsGoal
import com.planora.app.theme.*
import com.planora.app.ui.components.*
import com.planora.app.ui.viewmodels.SavingsViewModel
import com.planora.app.utils.DateUtils
import com.planora.app.utils.FormatUtils



@Composable
fun SavingsScreen(
    onNavigateToAddGoal: () -> Unit,
    onNavigateToEditGoal: (Long) -> Unit,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sym = uiState.currencySymbol

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            PlanoraFAB(onClick = onNavigateToAddGoal,
                modifier = Modifier.padding(bottom = 88.dp + navBarPadding)) {
                Icon(painterResource(R.drawable.ic_add), "Add goal")
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
                        PlanoraTopBar("Savings Goals", subtitle = "Track your financial goals")
                        Spacer(Modifier.height(SpacingSmall))
                    }
                }
            }
            if (uiState.goals.isNotEmpty()) {
                item { OverallProgressCard(uiState.totalTarget, uiState.totalSaved, sym) }
            }
            if (uiState.goals.isEmpty()) {
                item {
                    Spacer(Modifier.height(SpacingLarge))
                    EmptyState(R.drawable.ic_savings, "No savings goals", "Set a goal and start saving today!")
                }
            } else {
                items(uiState.goals, key = { it.id }) { goal ->
                    SavingsGoalCard(goal = goal, sym = sym,
                        onEdit   = { onNavigateToEditGoal(goal.id) },
                        onDelete = { viewModel.deleteGoal(goal) },
                        onAddSaving = { amount ->
                            viewModel.addToGoal(goal.id, amount, goal.currentAmount, goal.targetAmount, goal.title)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallProgressCard(totalTarget: Double, totalSaved: Double, sym: String) {
    val progress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f
    GradientCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        gradientStart = MaterialTheme.colorScheme.surfaceContainerLow,
        gradientEnd   = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Overall Progress", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(FormatUtils.formatCurrency(totalSaved, sym),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = PlanoraGreen)
                    Text("saved of ${FormatUtils.formatCurrency(totalTarget, sym)}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
                Text("${"%.1f".format(progress * 100)}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            AnimatedProgressBar(progress, progressColor = PlanoraGreen,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), height = 10.dp)
        }
    }
}

@Suppress("AssignedValueIsNeverRead")
@Composable
private fun SavingsGoalCard(goal: SavingsGoal, sym: String, onEdit: () -> Unit,
    onDelete: () -> Unit, onAddSaving: (Double) -> Unit) {
    var showAddSavingDialog by remember { mutableStateOf(false) }

    if (showAddSavingDialog) {
        AddSavingDialog(dailyAmount = goal.dailySaving, sym = sym,
            onDismiss = { showAddSavingDialog = false },
            onConfirm = { onAddSaving(it); showAddSavingDialog = false })
    }

    PlanoraCard(modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingMedium, vertical = 6.dp),
        shape = CardShape) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(goal.title, style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        if (goal.isCompleted) {
                            Surface(shape = CircleShape, color = PlanoraGreen.copy(alpha = 0.15f)) {
                                Text("Done!", color = PlanoraGreen, style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                    val subtitle = remember(goal.durationDays, goal.dailySaving, sym) { "${goal.durationDays} day goal Â· Save ${FormatUtils.formatCurrency(goal.dailySaving, sym)}/day" }
                    Text(subtitle,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onEdit) { Icon(painterResource(R.drawable.ic_edit), "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete) { Icon(painterResource(R.drawable.ic_delete), "Delete", tint = ExpenseRed.copy(alpha = 0.7f)) }
            }
            // Amount summary â€” 3 stats in a row, avoids duplication with a helper lambda
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(
                    Triple("Saved",     goal.currentAmount,  IncomeGreen),
                    Triple("Remaining", goal.remainingAmount, if (goal.isCompleted) IncomeGreen else ExpenseRed),
                    Triple("Target",    goal.targetAmount,    MaterialTheme.colorScheme.onSurface)
                ).forEach { (label, amount, color) ->
                    Column(horizontalAlignment = if (label == "Saved") Alignment.Start else Alignment.End) {
                        Text(label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(FormatUtils.formatCurrency(amount, sym),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = color)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progress", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${"%.1f".format(goal.progressPercent)}%",
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                        color = PlanoraGreen)
                }
                AnimatedProgressBar(goal.progressPercent / 100f,
                    progressColor = if (goal.isCompleted) PlanoraGreen else MaterialTheme.colorScheme.primary,
                    height = 8.dp)
            }
            val deadline = remember(goal.deadlineDate) { "Deadline: ${DateUtils.formatDate(goal.deadlineDate)}" }
            Text(deadline,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!goal.isCompleted) {
                Button(onClick = { showAddSavingDialog = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp), shape = CurvedShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Icon(painterResource(R.drawable.ic_add), null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Saving", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun AddSavingDialog(dailyAmount: Double, sym: String, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("%.2f".format(dailyAmount)) }
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        tonalElevation = 6.dp,
        title = { Text("Add Saving") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Suggested daily: ${FormatUtils.formatCurrency(dailyAmount, sym)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PlanoraTextField(value = amount, onValueChange = { amount = it },
                    label = "Amount ($sym)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amount.trim().toDoubleOrNull() ?: return@Button
                    if (value <= 0.0) return@Button
                    onConfirm(value)
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

