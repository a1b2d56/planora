package com.planora.app.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planora.app.core.data.database.entities.Priority
import com.planora.app.core.data.database.entities.Task
import com.planora.app.core.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TaskFilter { ALL, ACTIVE, COMPLETED, HIGH_PRIORITY }

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val filter: TaskFilter = TaskFilter.ALL,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val categories: List<String> = emptyList(),
    val activeCount: Int = 0,
    val completedCount: Int = 0
)

@HiltViewModel
class TaskViewModel @Inject constructor(private val repository: TaskRepository) : ViewModel() {

    private val _filter           = MutableStateFlow(TaskFilter.ALL)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _searchQuery      = MutableStateFlow("")

    // Debounce search so rapid typing doesn't re-filter the full task list every keystroke
    private val _debouncedSearch = _searchQuery.debounce { if (it.isBlank()) 0L else 200L }.distinctUntilChanged()

    val uiState: StateFlow<TaskUiState> = combine(
        _filter,
        _selectedCategory,
        _debouncedSearch,
        repository.getAllTasks().distinctUntilChanged()
    ) { filter, category, query, allTasks ->
        // Derive categories inline  --  avoids a 5th Room DB observer
        val categories = allTasks.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
        // One-pass: filter → category → search
        val tasks = allTasks
            .let { t -> when (filter) {
                TaskFilter.ALL           -> t
                TaskFilter.ACTIVE        -> t.filter { !it.isCompleted }
                TaskFilter.COMPLETED     -> t.filter { it.isCompleted }
                TaskFilter.HIGH_PRIORITY -> t.filter { it.priority == Priority.HIGH }
            }}
            .let { t -> if (category != null) t.filter { it.category == category } else t }
            .let { t -> if (query.isNotBlank()) t.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            } else t }
        // Derive counts from allTasks  --  removes 2 extra Room DB observers
        val activeCount    = allTasks.count { !it.isCompleted }
        val completedCount = allTasks.count { it.isCompleted }
        TaskUiState(tasks, filter, category, query, categories, activeCount, completedCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskUiState())

    fun setFilter(filter: TaskFilter)    { _filter.value = filter }
    fun setCategory(category: String?)   { _selectedCategory.value = category }
    fun setSearchQuery(query: String)    { _searchQuery.value = query }
    fun addTask(task: Task)              = viewModelScope.launch { repository.insertTask(task) }
    fun updateTask(task: Task)           = viewModelScope.launch { repository.updateTask(task) }
    fun deleteTask(task: Task)           = viewModelScope.launch { repository.deleteTask(task) }
    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        repository.updateTaskCompletion(task.id, !task.isCompleted)
    }
    suspend fun getTaskById(id: Long): Task? = repository.getTaskById(id)
}
