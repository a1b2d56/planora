package com.planora.app.feature.savings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.R
import com.planora.app.core.data.database.entities.SavingsGoal
import com.planora.app.core.ui.components.*
import com.planora.app.core.utils.FormatUtils
import kotlinx.coroutines.launch

@Composable
fun AddEditGoalScreen(
    goalId: Long?,
    onBack: () -> Unit,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val isEditing = goalId != null && goalId > 0L
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sym = uiState.currencySymbol
    
    var title           by remember { mutableStateOf("") }
    var targetAmount    by remember { mutableStateOf("") }
    var durationDays    by remember { mutableStateOf("30") }
    var reminderEnabled by remember { mutableStateOf(true) }

    // Initial hydration: Load existing goal data from the database if in 'Edit' mode
    LaunchedEffect(goalId) {
        if (goalId != null && isEditing) {
            viewModel.getGoalById(goalId)?.let { g ->
                title = g.title; targetAmount = g.targetAmount.toString()
                durationDays = g.durationDays.toString(); reminderEnabled = g.reminderEnabled
            }
        }
    }

    // Derived logic: Calculate recommendation based on real-time input
    val t = targetAmount.trim().toDoubleOrNull() ?: 0.0
    val d = durationDays.trim().toIntOrNull() ?: 0

    PlanoraScreen(
        title = if (isEditing) "Edit Goal" else "New Savings Goal",
        onBack = onBack,
        actionButtonLabel = if (isEditing) "Update Goal" else "Create Goal",
        onActionButtonClick = {
            // Validation: Ensure all fields are valid before persisting
            val trimmedTitle = title.trim()
            val target = targetAmount.trim().toDoubleOrNull() ?: return@PlanoraScreen
            val duration = durationDays.trim().toIntOrNull() ?: return@PlanoraScreen
            if (trimmedTitle.isBlank() || target <= 0 || duration <= 0) return@PlanoraScreen
            
            scope.launch {
                val existing = if (goalId != null && isEditing) viewModel.getGoalById(goalId) else null
                val goal = if (isEditing && existing != null) {
                    existing.copy(
                        title = trimmedTitle, targetAmount = target,
                        durationDays = duration, reminderEnabled = reminderEnabled
                    )
                } else {
                    SavingsGoal(
                        title = trimmedTitle, targetAmount = target,
                        durationDays = duration, reminderEnabled = reminderEnabled
                    )
                }
                
                if (isEditing && existing != null) viewModel.updateGoal(goal)
                else if (!isEditing) viewModel.addGoal(goal)
                
                onBack()
            }
        }
    ) {
        PlanoraTextField(
            value = title, onValueChange = { title = it },
            label = "Goal Title", placeholder = "e.g. Dream Vacation",
            leadingIcon = { Icon(painterResource(R.drawable.ic_savings), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
        )

        PlanoraTextField(
            value = targetAmount, onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) targetAmount = it },
            label = "Target Amount ($sym)",
            placeholder = "How much do you need?",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = { Text(sym, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp)) }
        )

        PlanoraTextField(
            value = durationDays, onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) durationDays = it },
            label = "Duration (days)",
            placeholder = "In how many days?",
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(painterResource(R.drawable.ic_schedule), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) }
        )

        // Recommendation Engine: Provide value-add insights during the planning phase
        if (t > 0 && d > 0) {
            Surface(
                shape = CardShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Daily saving recommendation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(FormatUtils.formatCurrency(t / d, sym), 
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Save this amount every day to reach your goal on time.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        PlanoraActionRow(
            label = "Daily Reminder",
            description = "Get notified to save daily",
            icon = { Icon(painterResource(R.drawable.ic_alarm), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
        ) {
            Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
        }
    }
}
