package com.devil.taskzio.ui.screens.notes


import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*

import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devil.taskzio.R
import com.devil.taskzio.data.database.entities.Note
import com.devil.taskzio.theme.ExpenseRed
import com.devil.taskzio.ui.components.*
import com.devil.taskzio.ui.viewmodels.NoteViewModel
import com.devil.taskzio.utils.DateUtils
import com.devil.taskzio.utils.stripContentForPreview

@Composable
fun NotesScreen(
    onNavigateToNote: (Long?) -> Unit,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            TaskzioFAB(
                onClick = { onNavigateToNote(null) },
                modifier = Modifier.padding(bottom = 88.dp + navBarPadding)
            ) { Icon(painter = painterResource(R.drawable.ic_add), contentDescription = "New note") }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            /* ── Elevated header: top bar + search ─────────────────── */
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 0.dp
            ) {
                Column {
                    TaskzioTopBar(title = "Notes", subtitle = "${uiState.notes.size} notes")
                    Spacer(Modifier.height(4.dp))
                    TaskzioSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::setSearchQuery,
                        placeholder = "Search notes…",
                        modifier = Modifier.padding(horizontal = SpacingMedium, vertical = SpacingSmall)
                    )
                    Spacer(Modifier.height(SpacingSmall))
                }
            }

            if (uiState.notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        iconRes = R.drawable.ic_edit,
                        title = if (uiState.searchQuery.isBlank()) "No notes yet"
                                else "No results found",
                        subtitle = if (uiState.searchQuery.isBlank()) "Tap + to create your first note"
                                   else "Try a different search"
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(start = SpacingMedium, end = SpacingMedium, top = SpacingMedium, bottom = 120.dp + navBarPadding),
                    horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
                    verticalItemSpacing = SpacingSmall
                ) {
                    items(uiState.notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNavigateToNote(note.id) },
                            onPin = { viewModel.togglePin(note) },
                            onDelete = { viewModel.deleteNote(note) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit) {
    val surface  = MaterialTheme.colorScheme.surface
    val bgLum    = MaterialTheme.colorScheme.background.luminance()
    val isDark   = bgLum < 0.1f

    val noteColor = remember(note.color, isDark, surface) {
        val raw = try { Color(note.color.toColorInt()) } catch (_: Exception) { null }
        when {
            raw == null                           -> surface
            isDark && raw.luminance() > 0.8f      -> surface
            else                                  -> raw
        }
    }
    val textColor    = remember(noteColor) {
        if (noteColor.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White
    }
    val subtextColor = remember(textColor) { textColor.copy(alpha = 0.6f) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = noteColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    if (note.title.isNotBlank()) {
                        Text(note.title, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold, color = textColor,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (note.isPinned) {
                    Icon(painterResource(R.drawable.ic_push_pin), "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp).padding(start = 4.dp))
                }
            }
            if (note.content.isNotBlank()) {
                val preview = remember(note.content) { stripContentForPreview(note.content) }
                Text(
                    preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtextColor,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(DateUtils.relativeTime(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall, color = subtextColor)
                Row {
                    IconButton(onClick = onPin, modifier = Modifier.size(24.dp)) {
                        Icon(
                            painter = painterResource(if (note.isPinned) R.drawable.ic_push_pin else R.drawable.ic_push_pin_outlined),
                            contentDescription = null, tint = subtextColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null,
                            tint = ExpenseRed.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
