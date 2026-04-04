package com.devil.taskzio.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devil.taskzio.data.database.entities.Note
import com.devil.taskzio.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteUiState(val notes: List<Note> = emptyList(), val searchQuery: String = "")

@HiltViewModel
class NoteViewModel @Inject constructor(private val repository: NoteRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<NoteUiState> = _searchQuery
        .debounce { if (it.isBlank()) 0L else 300L }  // skip DB round-trip on every keystroke
        .distinctUntilChanged()
        .flatMapLatest { query ->
        // Capture query in lambda scope — fixes race condition where .value could differ
        val source = if (query.isBlank()) repository.getAllNotes() else repository.searchNotes(query)
        source.map { NoteUiState(notes = it, searchQuery = query) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NoteUiState())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun addNote(note: Note)           = viewModelScope.launch { repository.insertNote(note) }
    fun updateNote(note: Note)        = viewModelScope.launch { repository.updateNote(note) }
    fun deleteNote(note: Note)        = viewModelScope.launch { repository.deleteNote(note) }
    fun togglePin(note: Note)         = viewModelScope.launch { repository.updatePinned(note.id, !note.isPinned) }
    suspend fun getNoteById(id: Long): Note? = repository.getNoteById(id)
}
