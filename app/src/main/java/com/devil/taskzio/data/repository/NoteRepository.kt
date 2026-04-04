package com.devil.taskzio.data.repository

import com.devil.taskzio.data.database.dao.NoteDao
import com.devil.taskzio.data.database.entities.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val dao: NoteDao) {
    fun getAllNotes(): Flow<List<Note>> = dao.getAllNotes()
    fun searchNotes(query: String): Flow<List<Note>> = dao.searchNotes(query)

    suspend fun getNoteById(id: Long): Note? = dao.getNoteById(id)
    suspend fun insertNote(note: Note): Long = dao.insertNote(note)
    suspend fun updateNote(note: Note) = dao.updateNote(note)
    suspend fun deleteNote(note: Note) = dao.deleteNote(note)
    suspend fun updatePinned(id: Long, pinned: Boolean) = dao.updatePinned(id, pinned)
}
