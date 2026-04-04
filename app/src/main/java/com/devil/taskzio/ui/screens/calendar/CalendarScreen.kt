package com.devil.taskzio.ui.screens.calendar

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.taskzio.R
import com.devil.taskzio.data.database.entities.CalendarEvent
import com.devil.taskzio.data.database.entities.EventType
import com.devil.taskzio.data.database.entities.displayName
import com.devil.taskzio.theme.*
import com.devil.taskzio.ui.components.*
import com.devil.taskzio.ui.viewmodels.CalendarViewModel
import com.devil.taskzio.utils.DateUtils

@Suppress("AssignedValueIsNeverRead")
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddEvent  by remember { mutableStateOf(false) }
    var editingEvent  by remember { mutableStateOf<CalendarEvent?>(null) }
    val dismissDialog = { showAddEvent = false; editingEvent = null }

    if (showAddEvent || editingEvent != null) {
        AddEditEventDialog(
            event = editingEvent, selectedDate = uiState.selectedDate,
            onDismiss = dismissDialog,
            onSave = { event ->
                if (editingEvent != null) viewModel.updateEvent(event) else viewModel.addEvent(event)
                dismissDialog()
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            TaskzioFAB(onClick = { showAddEvent = true }, modifier = Modifier.padding(bottom = 88.dp + navBarPadding)) {
                Icon(painterResource(R.drawable.ic_add), "Add event")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 120.dp + navBarPadding)) {
            /* ── Elevated header: top bar + month nav ──────────────── */
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 0.dp
                ) {
                    Column {
                        TaskzioTopBar("Calendar", subtitle = DateUtils.formatMonthYear(uiState.currentMonthYear))
                        CalendarHeader(uiState.currentMonthYear,
                            viewModel::navigateToPreviousMonth, viewModel::navigateToNextMonth)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                CalendarGrid(uiState.currentMonthYear, uiState.selectedDate,
                    uiState.eventDatesInMonth, viewModel::selectDate)
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                SectionHeader("Events on ${DateUtils.formatDate(uiState.selectedDate)}")
            }
            if (uiState.eventsForSelectedDay.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No events on this day", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(uiState.eventsForSelectedDay, key = { it.id }) { event ->
                    EventItem(event, onClick = { editingEvent = event },
                        onDelete = { viewModel.deleteEvent(event) },
                        modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(monthYear: Long, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) { Icon(painterResource(R.drawable.ic_chevron_left), "Previous") }
        Text(DateUtils.formatMonthYear(monthYear), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        IconButton(onClick = onNext) { Icon(painterResource(R.drawable.ic_chevron_right), "Next") }
    }
}

@Composable
private fun CalendarGrid(monthYear: Long, selectedDate: Long, eventDates: Set<Long>, onDateSelect: (Long) -> Unit) {
    val days  = remember(monthYear) {
        val ym = java.time.YearMonth.from(
            java.time.Instant.ofEpochMilli(monthYear)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        )
        val raw = DateUtils.getCalendarDaysForMonth(ym.year, ym.monthValue - 1)
        // Only keep rows that have at least one actual day (trim trailing blank row)
        raw.chunked(7).filter { row -> row.any { it != null } }.flatten()
    }
    val today      = remember { DateUtils.getDayStart(System.currentTimeMillis()) }
    val primary    = MaterialTheme.colorScheme.primary
    val todayBg    = remember(primary) { primary.copy(alpha = 0.15f) }
    val dayLabels  = remember(days) { days.map { if (it != null) DateUtils.formatDayNumber(it) else "" } }
    val rows       = remember(days) { days.chunked(7) }
    val rowLabels  = remember(dayLabels) { dayLabels.chunked(7) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").forEach { d ->
                    Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            rows.forEachIndexed { ri, row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEachIndexed { ci, dayMillis ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (dayMillis == null) {
                                Spacer(Modifier.size(40.dp))
                            } else {
                                val dayLabel   = rowLabels[ri][ci]
                                val dayStart   = remember(dayMillis) { DateUtils.getDayStart(dayMillis) }
                                val isSelected = remember(dayMillis, selectedDate) { DateUtils.isSameDay(dayMillis, selectedDate) }
                                val isToday    = dayStart == today
                                val hasEvent   = dayStart in eventDates
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp).size(40.dp).clip(CircleShape)
                                        .background(when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday    -> todayBg
                                            else       -> Color.Transparent
                                        })
                                        .clickable { onDateSelect(dayMillis) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            dayLabel,
                                            style      = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color      = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isToday    -> MaterialTheme.colorScheme.primary
                                                else       -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (hasEvent) Box(
                                            modifier = Modifier.size(4.dp).clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventItem(event: CalendarEvent, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val eventColor = remember(event.color) { try { Color(event.color.toColorInt()) } catch (_: Exception) { TaskzioGreen } }
    val typeIcon   = when (event.type) {
        EventType.BIRTHDAY -> R.drawable.ic_cake
        EventType.REMINDER -> R.drawable.ic_alarm
        EventType.HOLIDAY  -> R.drawable.ic_beach
        EventType.EVENT    -> R.drawable.ic_calendar_month
    }
    SwipeToDeleteBox(onDelete = onDelete, modifier = Modifier.padding(horizontal = SpacingMedium)) {
        Card(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(eventColor))
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(eventColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(typeIcon), null, tint = eventColor, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(event.title, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    if (event.description.isNotBlank()) Text(event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    if (event.startTime != null) Text(DateUtils.formatTime(event.startTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = eventColor.copy(alpha = 0.15f)) {
                    Text(event.type.displayName, style = MaterialTheme.typography.labelSmall,
                        color = eventColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditEventDialog(event: CalendarEvent?, selectedDate: Long,
    onDismiss: () -> Unit, onSave: (CalendarEvent) -> Unit) {
    var title       by remember { mutableStateOf(event?.title ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var eventType   by remember { mutableStateOf(event?.type ?: EventType.EVENT) }
    var isAllDay    by remember { mutableStateOf(event?.isAllDay ?: true) }
    var isYearly    by remember { mutableStateOf(event?.isYearly ?: false) }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (event != null) "Edit Event" else "New Event",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("Event Title") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(), maxLines = 3)

                Spacer(Modifier.height(SpacingSmall))
                Text("Event Type", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
                    verticalArrangement = Arrangement.spacedBy(SpacingSmall)
                ) {
                    EventType.entries.forEach { type ->
                        FilterChip(selected = eventType == type, onClick = { eventType = type },
                            label = { Text(type.displayName) },
                            shape = RoundedCornerShape(12.dp))
                    }
                }

                Spacer(Modifier.height(SpacingSmall))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("All Day", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                }
                if (eventType == EventType.BIRTHDAY) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Repeats Yearly", style = MaterialTheme.typography.bodyMedium)
                            Text("Show every year on this day", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isYearly, onCheckedChange = { isYearly = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    onSave(event?.copy(title = title, description = description, type = eventType,
                        isAllDay = isAllDay, isYearly = isYearly)
                        ?: CalendarEvent(title = title, description = description, date = selectedDate,
                            type = eventType, isAllDay = isAllDay, isYearly = isYearly))
                },
                shape = RoundedCornerShape(12.dp)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
