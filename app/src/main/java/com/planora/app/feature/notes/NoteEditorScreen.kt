package com.planora.app.feature.notes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.planora.app.R
import com.planora.app.core.ui.theme.AppTheme
import com.planora.app.core.data.database.entities.*
import com.planora.app.core.ui.components.richtext.FormatToolbar
import com.planora.app.core.ui.components.richtext.RichTextEditor
import com.planora.app.core.ui.components.richtext.RichTextState
import kotlinx.coroutines.launch


@Composable
fun NoteEditorScreen(
    noteId: Long?,
    onBack: () -> Unit,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }

    // Rich text state for the content editor
    val richTextState = remember { RichTextState() }

    // Theme awareness for creation-time sticky context
    val themeChoice by viewModel.appTheme.collectAsState(initial = AppTheme.MIDNIGHT)
    
    var selectedColor by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    LaunchedEffect(noteId, themeChoice) {
        if (noteId != null && noteId > 0L) {
            viewModel.getNoteById(noteId)?.let { note ->
                title = note.title
                richTextState.fromHtml(note.content)
                selectedColor = note.color
                isPinned = note.isPinned
            }
        } else if (selectedColor.isEmpty()) {
            // New Note Intelligence: Default to true black/white based on theme context
            selectedColor = if (themeChoice == AppTheme.LIGHT) "#FFFFFF" else "#000000"
        }
    }

    // Humanized derived state: Recompute core visual tokens only on color shift.
    val noteColor    = remember(selectedColor) {
        try { Color(selectedColor.toColorInt()) } catch (_: Exception) { Color.Black }
    }
    val onNoteColor  = remember(noteColor) { 
        if (noteColor.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White 
    }
    val onNoteVariant = remember(onNoteColor) { onNoteColor.copy(alpha = 0.6f) }

    Scaffold(
        containerColor = noteColor,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(noteColor)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", tint = onNoteColor)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                painter = painterResource(if (isPinned) R.drawable.ic_push_pin else R.drawable.ic_push_pin_outlined),
                                contentDescription = "Pin",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else onNoteVariant
                            )
                        }
                        IconButton(onClick = { showColorPicker = !showColorPicker }) {
                            Icon(painterResource(R.drawable.ic_palette), "Color", tint = onNoteVariant)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val now = System.currentTimeMillis()
                                    val trimmedTitle = title.trim()
                                    val htmlContent = richTextState.toHtml()
                                    if (trimmedTitle.isEmpty() && htmlContent.isBlank()) return@launch
                                    if (noteId != null && noteId > 0L) {
                                        viewModel.getNoteById(noteId)?.let { existing ->
                                            viewModel.updateNote(
                                                existing.copy(
                                                    title = trimmedTitle,
                                                    content = htmlContent,
                                                    color = selectedColor,
                                                    isPinned = isPinned,
                                                    updatedAt = now
                                                )
                                            )
                                        }
                                    } else {
                                        viewModel.addNote(
                                            Note(
                                                title = trimmedTitle,
                                                content = htmlContent,
                                                color = selectedColor,
                                                isPinned = isPinned
                                            )
                                        )
                                    }
                                    onBack()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text("Save") }
                    }

                    if (showColorPicker) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NOTE_COLORS.forEach { (hex, name) ->
                                val c = try { Color(hex.toColorInt()) } catch (_: Exception) { Color.Gray }
                                Box(
                                    modifier = Modifier.size(28.dp).clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (selectedColor == hex) 2.dp else 1.dp,
                                            color = if (selectedColor == hex) MaterialTheme.colorScheme.primary
                                                    else onNoteColor.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = hex; showColorPicker = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedColor == hex) {
                                        Icon(painter = painterResource(R.drawable.ic_check), contentDescription = name,
                                            tint = if (c.luminance() > 0.5f) Color.Black else Color.White,
                                            modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    FormatToolbar(state = richTextState, tintColor = onNoteVariant)
                    HorizontalDivider(color = onNoteColor.copy(alpha = 0.1f))
                }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = onNoteColor
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                decorationBox = { inner ->
                    Box {
                        if (title.isEmpty()) Text(
                            "Title", style = MaterialTheme.typography.headlineSmall,
                            color = onNoteVariant, fontWeight = FontWeight.Bold
                        )
                        inner()
                    }
                }
            )

            // Rich text content editor — real formatting, no visible markers
            RichTextEditor(
                state = richTextState,
                textColor = onNoteColor,
                placeholderColor = onNoteVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}
