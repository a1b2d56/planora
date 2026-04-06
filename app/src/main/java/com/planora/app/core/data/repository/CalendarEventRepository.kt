package com.planora.app.core.data.repository

import com.planora.app.core.data.database.dao.CalendarEventDao
import com.planora.app.core.data.database.entities.CalendarEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarEventRepository @Inject constructor(private val dao: CalendarEventDao) {
    fun getAllEvents(): Flow<List<CalendarEvent>>         = dao.getAllEvents()
    suspend fun insertEvent(e: CalendarEvent): Long      = dao.insertEvent(e)
    suspend fun getEventById(id: Long): CalendarEvent?   = dao.getEventById(id)
    suspend fun updateEvent(e: CalendarEvent)            = dao.updateEvent(e)
    suspend fun deleteEvent(e: CalendarEvent)            = dao.deleteEvent(e)
}
