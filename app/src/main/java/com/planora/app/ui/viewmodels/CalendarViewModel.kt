package com.planora.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planora.app.data.database.entities.CalendarEvent
import com.planora.app.data.repository.CalendarEventRepository
import com.planora.app.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

data class CalendarUiState(
    val events: List<CalendarEvent> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val currentMonthYear: Long = System.currentTimeMillis(),
    val eventsForSelectedDay: List<CalendarEvent> = emptyList(),
    val eventDatesInMonth: Set<Long> = emptySet()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarEventRepository
) : ViewModel() {

    private val zone = ZoneId.systemDefault()
    private val _selectedDate     = MutableStateFlow(System.currentTimeMillis())
    private val _currentMonthYear = MutableStateFlow(System.currentTimeMillis())

    private val allEvents = repository.getAllEvents().distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month-level data: expensive set computation, only runs when month or events change
    private val monthData = combine(_currentMonthYear, allEvents) { monthYear, events ->
        val start = DateUtils.getMonthStart(monthYear)
        val end   = DateUtils.getMonthEnd(monthYear)
        val dates = events
            .filter { it.date in start..end }
            .mapTo(mutableSetOf()) { DateUtils.getDayStart(it.date) }
        monthYear to dates
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedDate, monthData, allEvents
    ) { selectedDate, (monthYear, eventDatesInMonth), events ->
        val dayStart = DateUtils.getDayStart(selectedDate)
        val dayEnd   = DateUtils.getDayEnd(selectedDate)
        CalendarUiState(
            events               = events,
            selectedDate         = selectedDate,
            currentMonthYear     = monthYear,
            eventsForSelectedDay = events.filter { it.date in dayStart..dayEnd },
            eventDatesInMonth    = eventDatesInMonth
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun selectDate(date: Long) { _selectedDate.value = date }

    private fun navigateMonth(offset: Int) {
        // Use java.time  --  no Calendar needed
        val current = java.time.Instant.ofEpochMilli(_currentMonthYear.value).atZone(zone).toLocalDate()
        val next    = current.plusMonths(offset.toLong()).withDayOfMonth(1)
        val millis  = next.atStartOfDay(zone).toInstant().toEpochMilli()
        _currentMonthYear.value = millis
        _selectedDate.value     = millis
    }

    fun navigateToPreviousMonth() = navigateMonth(-1)
    fun navigateToNextMonth()     = navigateMonth(1)

    fun addEvent(event: CalendarEvent)    = viewModelScope.launch { repository.insertEvent(event) }
    suspend fun getEventById(id: Long): CalendarEvent?    = repository.getEventById(id)
    fun updateEvent(event: CalendarEvent) = viewModelScope.launch { repository.updateEvent(event) }
    fun deleteEvent(event: CalendarEvent) = viewModelScope.launch { repository.deleteEvent(event) }
}
