package com.devil.taskzio.data.repository

import com.devil.taskzio.data.database.dao.CalendarEventDao
import com.devil.taskzio.data.database.entities.CalendarEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarEventRepository @Inject constructor(private val dao: CalendarEventDao) {
    fun getAllEvents(): Flow<List<CalendarEvent>>         = dao.getAllEvents()
    suspend fun insertEvent(e: CalendarEvent): Long      = dao.insertEvent(e)
    suspend fun updateEvent(e: CalendarEvent)            = dao.updateEvent(e)
    suspend fun deleteEvent(e: CalendarEvent)            = dao.deleteEvent(e)
}
