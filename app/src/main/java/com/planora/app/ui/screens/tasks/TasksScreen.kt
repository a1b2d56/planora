package com.planora.app.ui.screens.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.planora.app.R
import com.planora.app.data.database.entities.Task
import com.planora.app.ui.components.*
import com.planora.app.theme.*
import com.planora.app.ui.viewmodels.TaskFilter
import com.planora.app.ui.viewmodels.TaskViewModel
import com.planora.app.utils.DateUtils

@Composable
fun TasksScreen(
    onNavigateToAddTask: () -> Unit,
    onNavigateToEditTask: (Long) -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            PlanoraFAB(
                onClick = onNavigateToAddTask,
                modifier = Modifier.padding(bottom = 88.dp + navBarPadding)
            ) {
                Icon(painter = painterResource(R.drawable.ic_add), contentDescription = "Add task",
                    modifier = Modifier.size(24.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 120.dp + navBarPadding)
        ) {
            /* â”€â”€ Elevated header: top bar + search + filters â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 0.dp
                ) {
                    Column {
                        PlanoraTopBar(title = "My Tasks",
                            subtitle = "${uiState.activeCount} active Â· ${uiState.completedCount} done")
                        Spacer(Modifier.height(4.dp))
                        PlanoraSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::setSearchQuery,
                            placeholder = "Search tasksâ€¦",
                            modifier = Modifier.padding(horizontal = SpacingMedium, vertical = 4.dp)
                        )
                        Spacer(Modifier.height(SpacingSmall))
                        TaskFilterRow(uiState.filter, viewModel::setFilter)
                        if (uiState.categories.isNotEmpty()) {
                            CategoryFilterRow(uiState.categories, uiState.selectedCategory, viewModel::setCategory)
                        }
                        Spacer(Modifier.height(SpacingSmall))
                    }
                }
            }

            item { Spacer(Modifier.height(SpacingMedium)) }
            item { TaskStatsRow(uiState.activeCount, uiState.completedCount) }
            item { Spacer(Modifier.height(SpacingSmall)) }

            if (uiState.tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize(0.5f)
                            .padding(top = SpacingLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            iconRes = R.drawable.ic_check_circle,
                            title = if (uiState.searchQuery.isBlank()) "No tasks yet" else "No results found",
                            subtitle = if (uiState.searchQuery.isBlank()) "Tap + to create your first task" else "Try a different search"
                        )
                    }
                }
            } else {
                items(uiState.tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onToggle = { viewModel.toggleTaskCompletion(task) },
                        onClick = { onNavigateToEditTask(task.id) },
                        onDelete = { viewModel.deleteTask(task) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskStatsRow(activeCount: Int, completedCount: Int) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(SpacingSmall)) {
        StatCard(modifier = Modifier.weight(1f), title = "Pending", value = activeCount.toString(),
            icon = { Icon(painterResource(R.drawable.ic_radio_button_unchecked), null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) })
        StatCard(modifier = Modifier.weight(1f), title = "Completed", value = completedCount.toString(),
            icon = { Icon(painterResource(R.drawable.ic_check_circle), null,
                tint = IncomeGreen, modifier = Modifier.size(18.dp)) })
    }
}

@Composable
private fun TaskFilterRow(currentFilter: TaskFilter, onFilterChange: (TaskFilter) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
        modifier = Modifier.padding(vertical = SpacingSmall)) {
        items(TaskFilter.entries) { filter ->
            val label = when (filter) {
                TaskFilter.ALL           -> "All"
                TaskFilter.ACTIVE        -> "Active"
                TaskFilter.COMPLETED     -> "Done"
                TaskFilter.HIGH_PRIORITY -> "High Priority"
            }
            CategoryChip(label = label, selected = currentFilter == filter, onClick = { onFilterChange(filter) })
        }
    }
}

@Composable
private fun CategoryFilterRow(categories: List<String>, selectedCategory: String?, onCategorySelect: (String?) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = SpacingMedium),
        horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
        modifier = Modifier.padding(vertical = 4.dp)) {
        item { CategoryChip("All Categories", selectedCategory == null) { onCategorySelect(null) } }
        items(categories) { category ->
            CategoryChip(category, selectedCategory == category) { onCategorySelect(category) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    SwipeToDeleteBox(onDelete = onDelete, modifier = Modifier.padding(horizontal = SpacingMedium, vertical = 4.dp)) {
        PlanoraCard(modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape)
                    .background(if (task.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(2.dp, if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = onToggle), contentAlignment = Alignment.Center) {
                    if (task.isCompleted) {
                        Icon(painterResource(R.drawable.ic_check), null,
                            tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None, maxLines = 1)
                    if (task.description.isNotBlank()) {
                        Text(task.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PriorityChip(task.priority)
                        if (task.dueDate != null) {
                            val isOverdue = remember(task.dueDate, task.isCompleted) { DateUtils.isOverdue(task.dueDate) && !task.isCompleted }
                            Surface(shape = RoundedCornerShape(6.dp),
                                color = (if (isOverdue) ExpenseRed else MaterialTheme.colorScheme.surfaceVariant).copy(alpha = 0.2f)) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Icon(painterResource(R.drawable.ic_schedule), null, modifier = Modifier.size(10.dp),
                                        tint = if (isOverdue) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(3.dp))
                                    Text(DateUtils.formatShortDate(task.dueDate), style = MaterialTheme.typography.labelSmall,
                                        color = if (isOverdue) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(task.category, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}
