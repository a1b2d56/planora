package com.devil.taskzio.ui.screens.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.devil.taskzio.R
import com.devil.taskzio.data.database.entities.Priority
import com.devil.taskzio.data.database.entities.Task
import com.devil.taskzio.theme.PriorityHigh
import com.devil.taskzio.theme.PriorityLow
import com.devil.taskzio.theme.PriorityMedium
import com.devil.taskzio.ui.components.DetailTopBar
import com.devil.taskzio.ui.components.TaskzioTextField
import com.devil.taskzio.ui.viewmodels.TaskViewModel
import com.devil.taskzio.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("AssignedValueIsNeverRead")
@Composable
fun AddEditTaskScreen(
    taskId: Long?,
    onBack: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val isEditing = taskId != null && taskId > 0L  // single source of truth
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var category by remember { mutableStateOf("General") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (isEditing) {
            viewModel.getTaskById(taskId)?.let { task ->
                title = task.title; description = task.description
                priority = task.priority; category = task.category; dueDate = task.dueDate
            }
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate ?: System.currentTimeMillis())
    val dismissDatePicker = { showDatePicker = false }

    val datePickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showDatePicker) {
        ModalBottomSheet(
            onDismissRequest = dismissDatePicker,
            sheetState = datePickerSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = dismissDatePicker) { Text("Cancel") }
                    TextButton(onClick = { 
                        dueDate = datePickerState.selectedDateMillis
                        dismissDatePicker() 
                    }) { Text("Confirm") }
                }
                DatePicker(
                    state = datePickerState,
                    showModeToggle = false,
                    title = null,
                    headline = null,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DetailTopBar(if (isEditing) "Edit Task" else "New Task", onBack) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp)
            .imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(8.dp))
            TaskzioTextField(value = title, onValueChange = { title = it },
                label = "Task Title", placeholder = "What needs to be done?")
            TaskzioTextField(value = description, onValueChange = { description = it },
                label = "Description (Optional)", singleLine = false, maxLines = 4)

            Text("Priority", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Priority.entries.forEach { p ->
                    val (color, label) = when (p) {
                        Priority.LOW    -> PriorityLow    to "Low"
                        Priority.MEDIUM -> PriorityMedium to "Medium"
                        Priority.HIGH   -> PriorityHigh   to "High"
                    }
                    val selected = priority == p
                    Surface(modifier = Modifier.weight(1f).clickable { priority = p },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        border = if (selected) BorderStroke(1.5.dp, color) else null) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                            Text(label, style = MaterialTheme.typography.labelMedium,
                                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }

            TaskzioTextField(value = category, onValueChange = { category = it }, label = "Category",
                leadingIcon = { Icon(painterResource(R.drawable.ic_edit), null, tint = MaterialTheme.colorScheme.primary) })

            OutlinedCard(modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(R.drawable.ic_date_range), null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Due Date", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (dueDate != null) DateUtils.formatDate(dueDate!!) else "No due date set",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (dueDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (dueDate != null) {
                        IconButton(onClick = { dueDate = null }) {
                            Icon(painterResource(R.drawable.ic_close), "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    scope.launch {
                        if (isEditing) {
                            val existing = viewModel.getTaskById(taskId) ?: return@launch
                            viewModel.updateTask(existing.copy(title = title.trim(), description = description.trim(),
                                priority = priority, category = category.trim().ifBlank { "General" },
                                dueDate = dueDate, updatedAt = System.currentTimeMillis()))
                        } else {
                            viewModel.addTask(Task(title = title.trim(), description = description.trim(),
                                priority = priority, category = category.trim().ifBlank { "General" }, dueDate = dueDate))
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isEditing) "Update Task" else "Create Task",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
