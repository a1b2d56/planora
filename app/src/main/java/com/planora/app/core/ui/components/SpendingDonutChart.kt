package com.planora.app.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.planora.app.core.ui.theme.chartColors
import com.planora.app.core.utils.FormatUtils



/** Two-panel spending chart with donut and category breakdown. */
@Composable
fun SpendingDonutChart(
    categorySpending: Map<String, Double>,
    totalExpense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    donutSize: Dp = 140.dp
) {
    if (categorySpending.isEmpty()) return

    // Stable derived value
    val sorted = remember(categorySpending) {
        val all = categorySpending.entries.sortedByDescending { it.value }
        if (all.size <= 8) {
            all.toList()
        } else {
            val top7 = all.take(7)
            val rest = all.drop(7)
            val othersValue = rest.sumOf { it.value }
            // We use AbstractMap.SimpleEntry as a stable way to create a Map.Entry
            top7 + listOf(java.util.AbstractMap.SimpleEntry("Misc", othersValue))
        }
    }

    val animProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(durationMillis = 900, easing = EaseOutCubic),
        label         = "donut_anim"
    )

    // Pre-compute sweep angles once per sort change, not per frame
    val strokeWidth = donutSize.value * 0.115f
    val gapDeg      = 3f
    val totalDeg    = 360f - gapDeg * sorted.size
    val donutStroke = remember(strokeWidth) { Stroke(strokeWidth, cap = StrokeCap.Butt) }
    val sweepAngles = remember(sorted, totalExpense) {
        sorted.map { (_, v) ->
            if (totalExpense > 0) (v / totalExpense * totalDeg).toFloat() else 0f
        }
    }

    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Donut Chart
        Box(modifier = Modifier.size(donutSize), contentAlignment = Alignment.Center) {
            val donutTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            Canvas(Modifier.fillMaxSize()) {
                val inset  = strokeWidth / 2f
                val oval   = Size(size.width - inset * 2, size.height - inset * 2)
                val origin = Offset(inset, inset)

                drawArc(donutTrackColor, -90f, 360f, false, origin, oval, style = donutStroke)

                var startAngle = -90f
                sweepAngles.forEachIndexed { i, sweep ->
                    val animated = sweep * animProgress
                    drawArc(chartColors[i % chartColors.size], startAngle, animated, false, origin, oval, style = donutStroke)
                    startAngle += animated + gapDeg
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    FormatUtils.formatCurrency(totalExpense, currencySymbol, compact = true),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text("Total", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }

        // Category breakdown list
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sorted.forEachIndexed { i, (cat, amount) ->
                CategoryDetailRow(
                    name      = cat,
                    amount    = amount,
                    total     = totalExpense,
                    color     = chartColors[i % chartColors.size]
                )
            }
        }
    }
}

@Composable
private fun CategoryDetailRow(
    name: String,
    amount: Double,
    total: Double,
    color: Color
) {
    val pct = remember(amount, total) {
        if (total > 0) (amount / total * 100).toInt() else 0
    }
    val barTarget = remember(amount, total) {
        if (total > 0) (amount / total).toFloat() else 0f
    }
    val bar by animateFloatAsState(
        targetValue   = barTarget,
        animationSpec = tween(900, easing = EaseOutCubic),
        label         = "bar_$name"
    )

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(name, style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                softWrap   = false,
                modifier   = Modifier.weight(1f))
            Text("$pct%", style = MaterialTheme.typography.labelSmall,
                color = color, fontWeight = FontWeight.SemiBold)

        }
        // Thin animated bar
        Box(
            Modifier.fillMaxWidth().height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.18f))
        ) {
            Box(
                Modifier.fillMaxWidth(bar).fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}
