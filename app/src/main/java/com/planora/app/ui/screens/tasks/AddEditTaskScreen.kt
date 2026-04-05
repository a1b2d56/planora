package com.planora.app.ui.screens.tasks

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.planora.app.R
import com.planora.app.data.database.entities.Priority
import com.planora.app.data.database.entities.Task
import com.planora.app.theme.PriorityHigh
import com.planora.app.theme.PriorityLow
import com.planora.app.theme.PriorityMedium
import com.planora.app.ui.components.PlanoraTextField
import com.planora.app.ui.viewmodels.TaskViewModel
import com.planora.app.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun AddEditTaskScreen(
    taskId: Long?,
    onBack: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val isEditing = taskId != null && taskId > 0L
    val scope = rememberCoroutineScope()
    
    // Core state tokens for task definition
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority    by remember { mutableStateOf(Priority.MEDIUM) }
    var category    by remember { mutableStateOf("General") }
    var dueDate     by remember { mutableStateOf<Long?>(null) }

    // Initial hydration: load existing task data when in 'Edit' mode
    LaunchedEffect(taskId) {
        if (isEditing) {
            viewModel.getTaskById(taskId)?.let { task ->
                title = task.title; description = task.description
                priority = task.priority; category = task.category; dueDate = task.dueDate
            }
        }
    }

    PlanoraScreen(
        title = if (isEditing) "Edit Task" else "New Task",
        onBack = onBack,
        actionButtonLabel = if (isEditing) "Update Task" else "Create Task",
        onActionButtonClick = {
            // Validation: Title is the only strictly required field
            if (title.isBlank()) return@PlanoraScreen
            
            scope.launch {
                if (isEditing) {
                    val existing = viewModel.getTaskById(taskId) ?: return@launch
                    viewModel.updateTask(
                        existing.copy(
                            title = title.trim(), 
                            description = description.trim(),
                            priority = priority, 
                            category = category.trim().ifBlank { "General" },
                            dueDate = dueDate, 
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    viewModel.addTask(
                        Task(
                            title = title.trim(), 
                            description = description.trim(),
                            priority = priority, 
                            category = category.trim().ifBlank { "General" }, 
                            dueDate = dueDate
                        )
                    )
                }
                onBack()
            }
        }
    ) {
        PlanoraTextField(
            value = title, onValueChange = { title = it },
            label = "Task Title", placeholder = "What needs to be done?"
        )
        
        PlanoraTextField(
            value = description, onValueChange = { description = it },
            label = "Description (Optional)", singleLine = false, maxLines = 4
        )

        // Visual distinction for Priority selection
        Text("Priority", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
        
        PlanoraToggleGroup(
            options = listOf(
                Priority.LOW to "Low",
                Priority.MEDIUM to "Medium",
                Priority.HIGH to "High"
            ),
            selectedOption = priority,
            onOptionSelected = { priority = it },
            activeColor = when(priority) {
                Priority.LOW    -> PriorityLow
                Priority.MEDIUM -> PriorityMedium
                Priority.HIGH   -> PriorityHigh
            }
        )

        PlanoraTextField(
            value = category, onValueChange = { category = it }, 
            label = "Category",
            leadingIcon = { Icon(painterResource(R.drawable.ic_edit), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
        )

        PlanoraDatePickerField(
            selectedDate = dueDate,
            onDateSelected = { dueDate = it },
            label = "Due Date",
            placeholder = "No due date set"
        )
    }
}
