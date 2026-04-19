package com.planora.app.feature.notes.handwriting

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.planora.app.R

private val PEN_COLORS = listOf(
    Color.White,
    Color(0xFFFF5252),
    Color(0xFFFFAB40),
    Color(0xFFFFFF00),
    Color(0xFF69F0AE),
    Color(0xFF40C4FF),
    Color(0xFFE040FB)
)

@Composable
fun HandwritingToolbar(
    modifier: Modifier = Modifier,
    selectedPen: PenType,
    onPenSelect: (PenType) -> Unit,
    penColor: Color,
    onColorChange: (Color) -> Unit,
    penWidth: Float,
    onWidthChange: (Float) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showWidthPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Width picker popup
        if (showWidthPicker) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2A2A2A),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Stroke Width", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = penWidth,
                        onValueChange = onWidthChange,
                        valueRange = 2f..24f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Color picker popup
        if (showColorPicker) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2A2A2A),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PEN_COLORS.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (color == penColor) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                )
                                .clickable {
                                    onColorChange(color)
                                    showColorPicker = false
                                }
                        )
                    }
                }
            }
        }

        // Main toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A1A1A).copy(alpha = 0.95f), Color(0xFF111111))
                    ),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Color solid circle preview
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(penColor)
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    .clickable { showColorPicker = !showColorPicker; showWidthPicker = false }
            )

            // Pen tools
            ToolButton("Pen", selectedPen == PenType.PEN, R.drawable.ic_pen_tool) {
                if (selectedPen == PenType.PEN) { showWidthPicker = !showWidthPicker; showColorPicker = false }
                else { showWidthPicker = false; showColorPicker = false; onPenSelect(PenType.PEN) }
            }
            ToolButton("Pencil", selectedPen == PenType.PENCIL, R.drawable.ic_pencil_tool) {
                if (selectedPen == PenType.PENCIL) { showWidthPicker = !showWidthPicker; showColorPicker = false }
                else { showWidthPicker = false; showColorPicker = false; onPenSelect(PenType.PENCIL) }
            }
            ToolButton("Marker", selectedPen == PenType.MARKER, R.drawable.ic_marker_tool) {
                if (selectedPen == PenType.MARKER) { showWidthPicker = !showWidthPicker; showColorPicker = false }
                else { showWidthPicker = false; showColorPicker = false; onPenSelect(PenType.MARKER) }
            }
            ToolButton("Eraser", selectedPen == PenType.ERASER, R.drawable.ic_eraser_tool) {
                if (selectedPen == PenType.ERASER) { showWidthPicker = !showWidthPicker; showColorPicker = false }
                else { showWidthPicker = false; showColorPicker = false; onPenSelect(PenType.ERASER) }
            }

            // Divider
            Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.15f)))

            // Undo/Redo
            IconButton(onClick = onUndo, enabled = canUndo, modifier = Modifier.size(36.dp)) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left),
                    "Undo",
                    tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo, modifier = Modifier.size(36.dp)) {
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    "Redo",
                    tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolButton(label: String, selected: Boolean, iconRes: Int, onClick: () -> Unit) {
    val elevation by animateFloatAsState(
        targetValue = if (selected) -8f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 350f),
        label = "toolElevation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset(y = elevation.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Icon(
            painterResource(iconRes),
            label,
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
        if (selected) {
            Spacer(Modifier.height(2.dp))
            Box(
                Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
