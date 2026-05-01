@file:Suppress("UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
package com.planora.app.feature.notes

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.planora.app.R
import com.planora.app.core.ui.theme.AppTheme
import com.planora.app.core.data.database.entities.*
import com.planora.app.core.ui.components.richtext.FormatToolbar
import com.planora.app.core.ui.components.richtext.RichTextEditor
import com.planora.app.core.ui.components.richtext.RichTextState
import com.planora.app.feature.notes.handwriting.*
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NoteEditorScreen(
    noteId: Long?,
    onBack: () -> Unit,
    viewModel: NoteViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }

    val richTextState = remember { RichTextState() }
    val themeChoice by viewModel.appTheme.collectAsState(initial = AppTheme.MIDNIGHT)
    
    var selectedColor by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    var showDropdown by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var contentBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }


    var isHandwritingMode by remember { mutableStateOf(false) }
    var handwritingViewRef by remember { mutableStateOf<HandwritingView?>(null) }
    var handwritingDataRaw by remember { mutableStateOf<String?>(null) }
    
    var penType by remember { mutableStateOf(PenType.PEN) }
    var penWidth by remember { mutableFloatStateOf(8f) }
    var handwritingColor by remember { mutableStateOf(Color.Black) }
    var paperType by remember { mutableStateOf(PaperType.PLAIN) }
    
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }

    LaunchedEffect(noteId, themeChoice) {
        if (noteId != null && noteId > 0L) {
            viewModel.getNoteById(noteId)?.let { note ->
                title = note.title
                richTextState.fromHtml(note.content)
                selectedColor = note.color
                isPinned = note.isPinned
                handwritingDataRaw = note.handwritingData
                note.paperType.let { pType -> 
                    try { paperType = PaperType.valueOf(pType) } catch (_: Exception) {} 
                }
                

                if (note.handwritingData?.isNotBlank() == true) {
                    isHandwritingMode = true
                }
            }
        } else if (selectedColor.isEmpty()) {
            selectedColor = if (themeChoice == AppTheme.LIGHT) "#FFFFFF" else "#000000"
        }
        
        val bgLuminance = try { Color(selectedColor.toColorInt()).luminance() } catch(_: Exception) { 0f }
        handwritingColor = if (bgLuminance > 0.5f) Color.Black else Color.White
    }

    val noteColor = remember(selectedColor) { try { Color(selectedColor.toColorInt()) } catch (_: Exception) { Color.Black } }
    val onNoteColor = remember(noteColor) { if (noteColor.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White }
    val onNoteVariant = remember(onNoteColor) { onNoteColor.copy(alpha = 0.6f) }

    Scaffold(
        containerColor = noteColor,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(noteColor)) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", tint = onNoteColor)
                    }
                    Spacer(Modifier.weight(1f))
                    // Handwriting toggle
                    IconButton(onClick = { isHandwritingMode = !isHandwritingMode }) {
                        Icon(painterResource(R.drawable.ic_edit), "Handwriting", 
                            tint = if (isHandwritingMode) MaterialTheme.colorScheme.primary else onNoteVariant)
                    }
                    
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            painter = painterResource(if (isPinned) R.drawable.ic_push_pin else R.drawable.ic_push_pin_outlined),
                            contentDescription = "Pin",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else onNoteVariant
                        )
                    }
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val view = androidx.compose.ui.platform.LocalView.current
                    
                    Box {
                        IconButton(onClick = { showDropdown = true }) {
                            Icon(painterResource(R.drawable.ic_more_vert), "Options", tint = onNoteVariant)
                        }
                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            modifier = Modifier.background(noteColor.copy(alpha = 0.95f))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Background", color = onNoteColor) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_palette), null, tint = onNoteColor) },
                                onClick = {
                                    showDropdown = false
                                    showColorPicker = !showColorPicker
                                    isHandwritingMode = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share as Image", color = onNoteColor) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_share), null, tint = onNoteColor) },
                                onClick = {
                                    showDropdown = false
                                    scope.launch {
                                        isSharing = true
                                        delay(100)
                                        try {
                                            val fullBitmap = androidx.core.graphics.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
                                            val fullCanvas = android.graphics.Canvas(fullBitmap)
                                            fullCanvas.drawColor(noteColor.toArgb())
                                            view.draw(fullCanvas)
                                            
                                            val bounds = contentBounds
                                            val croppedBitmap = if (bounds != null) {
                                                android.graphics.Bitmap.createBitmap(
                                                    fullBitmap,
                                                    bounds.left.toInt().coerceAtLeast(0),
                                                    bounds.top.toInt().coerceAtLeast(0),
                                                    bounds.width.toInt().coerceAtMost(fullBitmap.width),
                                                    bounds.height.toInt().coerceAtMost(fullBitmap.height)
                                                )
                                            } else fullBitmap

                                            val finalBitmap = croppedBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
                                            
                                            val strokes = handwritingViewRef?.getStrokes() ?: emptyList()
                                            var minX = finalBitmap.width.toFloat()
                                            var minY = finalBitmap.height.toFloat()
                                            var maxX = 0f
                                            var maxY = 0f
                                            
                                            var hasContent = false
                                            strokes.forEach { stroke ->
                                                stroke.points.forEach { pt ->
                                                    minX = minOf(minX, pt.x); minY = minOf(minY, pt.y)
                                                    maxX = maxOf(maxX, pt.x); maxY = maxOf(maxY, pt.y)
                                                    hasContent = true
                                                }
                                            }
                                            
                                            if (title.isNotEmpty() || richTextState.text.isNotEmpty()) {
                                                minX = 0f; minY = 0f
                                                maxX = maxOf(maxX, finalBitmap.width.toFloat())
                                                maxY = maxOf(maxY, finalBitmap.height.toFloat())
                                                hasContent = true
                                            }

                                            val cropPadding = 60f
                                            val shareBitmap = if (hasContent && strokes.isNotEmpty() && title.isEmpty() && richTextState.text.isEmpty()) {
                                                val contentWidth = (maxX - minX + cropPadding * 2).toInt().coerceIn(100, finalBitmap.width)
                                                val contentHeight = (maxY - minY + cropPadding * 2).toInt().coerceIn(100, finalBitmap.height)
                                                android.graphics.Bitmap.createBitmap(
                                                    finalBitmap,
                                                    (minX - cropPadding).toInt().coerceIn(0, finalBitmap.width - contentWidth),
                                                    (minY - cropPadding).toInt().coerceIn(0, finalBitmap.height - contentHeight),
                                                    contentWidth,
                                                    contentHeight
                                                )
                                            } else finalBitmap

                                            val shareCanvas = android.graphics.Canvas(shareBitmap)
                                            val watermarkAlpha = 60
                                            val watermarkColor = if (noteColor.luminance() > 0.5f) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                                            
                                            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                                color = watermarkColor
                                                alpha = watermarkAlpha
                                                textSize = 36f
                                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.NORMAL)
                                            }
                                            
                                            val text = "Planora"
                                            val textWidth = paint.measureText(text)
                                            val margin = 48f
                                            val textX = shareCanvas.width - textWidth - margin
                                            val textY = shareCanvas.height - margin
                                            
                                            val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_splash_icon)
                                            if (drawable != null) {
                                                val iconSize = 44
                                                val iconX = (textX - iconSize - 12).toInt()
                                                val iconY = (textY - 34).toInt()
                                                drawable.setBounds(iconX, iconY, iconX + iconSize, iconY + iconSize)
                                                drawable.setTint(watermarkColor)
                                                drawable.alpha = watermarkAlpha
                                                drawable.draw(shareCanvas)
                                            }
                                            shareCanvas.drawText(text, textX, textY, paint)
                                            
                                            val file = java.io.File(context.cacheDir, "shared_note.png")
                                            val out = java.io.FileOutputStream(file)
                                            shareBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                            out.flush()
                                            out.close()
                                            
                                            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(intent, "Share Note"))
                                        } catch (e: Exception) { e.printStackTrace() }
                                        finally { isSharing = false }
                                    }
                                }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val now = System.currentTimeMillis()
                                val trimmedTitle = title.trim()
                                val htmlContent = richTextState.toHtml()
                                
                                val currentGson = Gson()
                                val currentHwData = handwritingViewRef?.let { view ->
                                    val strokes = view.getStrokes()
                                    if (strokes.isNotEmpty()) {
                                        currentGson.toJson(HandwritingData(strokes, paperType.name))
                                    } else null
                                } ?: handwritingDataRaw
                                
                                if (trimmedTitle.isEmpty() && htmlContent.isBlank() && currentHwData.isNullOrBlank()) return@launch
                                
                                if (noteId != null && noteId > 0L) {
                                    viewModel.getNoteById(noteId)?.let { existing ->
                                        viewModel.updateNote(
                                            existing.copy(
                                                title = trimmedTitle,
                                                content = htmlContent,
                                                color = selectedColor,
                                                isPinned = isPinned,
                                                updatedAt = now,
                                                handwritingData = currentHwData,
                                                paperType = paperType.name
                                            )
                                        )
                                    }
                                } else {
                                    viewModel.addNote(
                                        Note(
                                            title = trimmedTitle,
                                            content = htmlContent,
                                            color = selectedColor,
                                            isPinned = isPinned,
                                            handwritingData = currentHwData,
                                            paperType = paperType.name
                                        )
                                    )
                                }
                                onBack()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Save") }
                }

                if (showColorPicker && !isHandwritingMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NOTE_COLORS.forEach { (hex, name) ->
                            val c = try { Color(hex.toColorInt()) } catch (_: Exception) { Color.Gray }
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape).background(c)
                                    .border(
                                        width = if (selectedColor == hex) 2.dp else 1.dp,
                                        color = if (selectedColor == hex) MaterialTheme.colorScheme.primary else onNoteColor.copy(alpha = 0.2f),
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

                if (!isHandwritingMode) {
                    FormatToolbar(state = richTextState, tintColor = onNoteVariant)
                    HorizontalDivider(color = onNoteColor.copy(alpha = 0.1f))
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isHandwritingMode,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                HandwritingToolbar(
                    selectedPen = penType,
                    onPenSelect = { penType = it },
                    penColor = handwritingColor,
                    onColorChange = { handwritingColor = it },
                    penWidth = penWidth,
                    onWidthChange = { penWidth = it },
                    canUndo = canUndo,
                    canRedo = canRedo,
                    onUndo = {
                        handwritingViewRef?.undo()
                        canUndo = handwritingViewRef?.canUndo == true
                        canRedo = handwritingViewRef?.canRedo == true
                    },
                    onRedo = {
                        handwritingViewRef?.redo()
                        canUndo = handwritingViewRef?.canUndo == true
                        canRedo = handwritingViewRef?.canRedo == true
                    },
                    modifier = Modifier.navigationBarsPadding()
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).onGloballyPositioned { contentBounds = it.boundsInRoot() }) {
            Column(
                modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 20.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = onNoteColor),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    decorationBox = { inner ->
                        Box {
                            if (title.isEmpty() && !isSharing) Text("Title", style = MaterialTheme.typography.headlineSmall, color = onNoteVariant, fontWeight = FontWeight.Bold)
                            inner()
                        }
                    }
                )

                if (!isHandwritingMode) {
                    RichTextEditor(
                        state = richTextState,
                        textColor = onNoteColor,
                        placeholderColor = if (isSharing) Color.Transparent else onNoteVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
            
            if (isHandwritingMode) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        HandwritingView(context).apply {
                            handwritingViewRef = this
                            this.penType = penType
                            this.penColor = handwritingColor.toArgb()
                            this.penWidth = penWidth
                            this.paperType = paperType
                            
                            this.onStrokeCompleted = {
                                canUndo = this.canUndo
                                canRedo = this.canRedo
                            }
                            
                            handwritingDataRaw?.let { json ->
                                try {
                                    val data = Gson().fromJson(json, HandwritingData::class.java)
                                    this.setStrokes(data.strokes)
                                    try { 
                                        this.paperType = PaperType.valueOf(data.paperType)
                                        paperType = this.paperType 
                                    } catch (_: Exception) {}
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    },
                    update = { view ->
                        view.penType = penType
                        view.penColor = handwritingColor.toArgb()
                        view.penWidth = penWidth
                        view.paperType = paperType
                        view.invalidate()
                    }
                )
            }
        }
    }
}
