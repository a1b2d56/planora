package com.planora.app.feature.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.planora.app.R
import com.planora.app.core.data.database.entities.CalendarEvent
import com.planora.app.core.data.database.entities.EventType
import com.planora.app.core.data.database.entities.displayName
import com.planora.app.core.ui.components.*
import com.planora.app.core.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditEventScreen(
    eventId: Long?,
    selectedDate: Long?,
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val isEditing = eventId != null && eventId > 0L
    
    // Localized state tokens for event configuration
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var eventType   by remember { mutableStateOf(EventType.EVENT) }
    var isAllDay    by remember { mutableStateOf(true) }
    var isYearly    by remember { mutableStateOf(false) }
    var eventDate   by remember { mutableLongStateOf(selectedDate ?: System.currentTimeMillis()) }

    // Lifecycle Synchronization: Hydrate form with existing event data
    LaunchedEffect(eventId) {
        if (eventId != null && eventId > 0L) {
            viewModel.getEventById(eventId)?.let { e ->
                title = e.title; description = e.description
                eventType = e.type; isAllDay = e.isAllDay
                isYearly = e.isYearly; eventDate = e.date
            }
        }
    }

    PlanoraScreen(
        title = if (isEditing) "Edit Event" else "New Event",
        onBack = onBack,
        actionButtonLabel = if (isEditing) "Update Event" else "Create Event",
        onActionButtonClick = {
            // Validation: Title is the only strictly required attribute
            if (title.isBlank()) return@PlanoraScreen
            
            scope.launch {
                val existing = if (eventId != null && isEditing) viewModel.getEventById(eventId) else null
                val event = if (isEditing && existing != null) {
                    existing.copy(
                        title = title.trim(), description = description.trim(), 
                        type = eventType, isAllDay = isAllDay, 
                        isYearly = isYearly, date = eventDate
                    )
                } else {
                    CalendarEvent(
                        title = title.trim(), description = description.trim(), 
                        date = eventDate, type = eventType, 
                        isAllDay = isAllDay, isYearly = isYearly
                    )
                }
                
                if (isEditing && existing != null) viewModel.updateEvent(event)
                else if (!isEditing) viewModel.addEvent(event)
                
                onBack()
            }
        }
    ) {
        PlanoraTextField(
            value = title, onValueChange = { title = it },
            label = "Event Title", placeholder = "What's happening?"
        )

        PlanoraTextField(
            value = description, onValueChange = { description = it },
            label = "Description (Optional)", singleLine = false, maxLines = 3
        )

        // Visual distinction for the Event Type classification
        Text("Event Type", style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
            verticalArrangement = Arrangement.spacedBy(SpacingSmall),
            modifier = Modifier.fillMaxWidth()
        ) {
            EventType.entries.forEach { type ->
                CategoryChip(
                    label = type.displayName,
                    selected = eventType == type,
                    onClick = { eventType = type }
                )
            }
        }

        // Modularized configuration options
        PlanoraActionRow(
            label = "All Day",
            icon = { Icon(painterResource(R.drawable.ic_alarm), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
        ) {
            Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
        }
        
        // Intelligent surfacing: Only show yearly repetition for specific event types
        if (eventType == EventType.BIRTHDAY) {
            PlanoraActionRow(
                label = "Repeats Yearly",
                description = "Show every year in calendar",
                icon = { Icon(painterResource(R.drawable.ic_calendar_month), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
            ) {
                Switch(checked = isYearly, onCheckedChange = { isYearly = it })
            }
        }
    }
}
