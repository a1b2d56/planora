package com.planora.app.feature.notes.handwriting

import androidx.annotation.Keep
import android.graphics.Color as AndroidColor

/** A single sampled point from a touch event. */
@Keep
data class DrawPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestamp: Long = 0L
)

/** A complete stroke drawn by the user. */
@Keep
data class DrawStroke(
    val points: List<DrawPoint>,
    val color: Int = AndroidColor.WHITE,
    val width: Float = 8f,
    val penType: PenType = PenType.PEN,
    val alpha: Int = 255
)

@Keep
enum class PenType {
    PEN,
    PENCIL,
    MARKER,
    ERASER
}

/** Paper background style. */
@Keep
enum class PaperType {
    PLAIN,
    DOTTED,
    GRID
}

/** Container for serializing all handwriting data for a note. */
@Keep
data class HandwritingData(
    val strokes: List<DrawStroke>,
    val paperType: String = PaperType.PLAIN.name
)
