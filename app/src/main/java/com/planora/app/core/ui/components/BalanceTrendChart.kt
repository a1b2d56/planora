package com.planora.app.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class TrendPoint(val label: String, val value: Double)

/**
 * Premium cubic line chart for balance trends.
 */
@Composable
fun BalanceTrendChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = lineColor.copy(alpha = 0.12f)
) {
    if (points.isEmpty()) return

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1200, easing = EaseInOutQuart),
        label = "trend_anim"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val maxVal = points.maxOf { it.value }.coerceAtLeast(1.0)
        val minVal = points.minOf { it.value }.coerceAtMost(0.0)
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        val xStep = if (points.size > 1) width / (points.size - 1) else width
        
        val path = Path()
        val fillPath = Path()

        val getY: (Double) -> Float = { value ->
            (height - ((value - minVal) / range * height)).toFloat()
        }

        points.forEachIndexed { i, pt ->
            val x = i * xStep
            val y = getY(pt.value)
            
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * xStep
                val prevY = getY(points[i - 1].value)
                
                // Cubic bezier for smoothness
                val controlX1 = prevX + (x - prevX) / 2
                val controlX2 = prevX + (x - prevX) / 2
                
                path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }
            
            if (i == points.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Clip the drawing based on animation progress (reveal from left)
        drawContext.canvas.save()
        drawContext.canvas.clipRect(0f, 0f, width * animProgress, height)

        // Draw under-fill gradient
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw main line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        drawContext.canvas.restore()
    }
}
