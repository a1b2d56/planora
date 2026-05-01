package com.planora.app.feature.calendar

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
import com.planora.app.R
import com.planora.app.core.data.database.entities.CalendarEvent
import com.planora.app.core.data.database.entities.EventType
import com.planora.app.core.data.database.entities.displayName
import com.planora.app.core.ui.theme.*
import com.planora.app.core.ui.components.*
import com.planora.app.core.utils.DateUtils

@Composable
fun CalendarScreen(
    onNavigateToAddEvent: (Long?) -> Unit,
    onNavigateToEditEvent: (Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            PlanoraFAB(onClick = { onNavigateToAddEvent(uiState.selectedDate) }, modifier = Modifier.padding(bottom = 88.dp + navBarPadding)) {
                Icon(painterResource(R.drawable.ic_add), "Add event")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 120.dp + navBarPadding)) {
            /* Elevated header: top bar + month nav */
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 0.dp
                ) {
                    Column {
                        PlanoraTopBar("Events", subtitle = DateUtils.formatMonthYear(uiState.currentMonthYear))
                        CalendarHeader(uiState.currentMonthYear,
                            viewModel::navigateToPreviousMonth, viewModel::navigateToNextMonth)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                CalendarGrid(uiState.currentMonthYear, uiState.selectedDate,
                    viewModel::selectDate)
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
                    EventItem(event, onClick = { onNavigateToEditEvent(event.id) },
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
private fun CalendarGrid(monthYear: Long, selectedDate: Long, onDateSelect: (Long) -> Unit) {
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
        shape    = CardShape,
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
    val eventColor = remember(event.color) { try { Color(event.color.toColorInt()) } catch (_: Exception) { PlanoraGreen } }
    val typeIcon   = when (event.type) {
        EventType.BIRTHDAY -> R.drawable.ic_cake
        EventType.REMINDER -> R.drawable.ic_alarm
        EventType.HOLIDAY  -> R.drawable.ic_beach
        EventType.EVENT    -> R.drawable.ic_calendar_month
    }
    SwipeToDeleteBox(onDelete = onDelete, modifier = Modifier.padding(horizontal = SpacingMedium)) {
        Card(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clickable(onClick = onClick),
            shape = CurvedShape,
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

