package com.example.bbc.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bbc.domain.ModelParameters
import com.example.bbc.domain.ReliabilityTestRecord
import com.example.bbc.domain.SRGMEngine

@Composable
fun ReliabilityGraph(
    records: List<ReliabilityTestRecord>,
    filteredRecords: List<ReliabilityTestRecord>,
    fittedParams: ModelParameters?,
    engine: SRGMEngine?,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = onSurfaceColor.copy(alpha = 0.7f),
        fontSize = 10.sp
    )
    val axisLabelStyle = TextStyle(
        color = onSurfaceColor,
        fontSize = 12.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
    )

    Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingLeft = 60.dp.toPx()
            val paddingBottom = 60.dp.toPx()
            val paddingTop = 20.dp.toPx()
            val paddingRight = 30.dp.toPx()

            val maxTime = records.maxOfOrNull { it.timeUnit.toDouble() } ?: 100.0
            val maxFailures = records.maxOfOrNull { it.cumulativeFailures.toDouble() } ?: 100.0

            val effectiveMaxTime = maxTime * 1.1
            val effectiveMaxFailures = maxFailures * 1.1

            fun toScreenX(t: Double) = (paddingLeft + (t / effectiveMaxTime) * (width - paddingLeft - paddingRight)).toFloat()
            fun toScreenY(f: Double) = (height - paddingBottom - (f / effectiveMaxFailures) * (height - paddingBottom - paddingTop)).toFloat()

            // Draw Axis Labels
            val xLabel = "Време (t)"
            val yLabel = "Откази (n)"
            
            val xLabelLayout = textMeasurer.measure(xLabel, axisLabelStyle)
            val yLabelLayout = textMeasurer.measure(yLabel, axisLabelStyle)
            
            drawText(
                xLabelLayout,
                topLeft = Offset(paddingLeft + (width - paddingLeft - paddingRight) / 2 - xLabelLayout.size.width / 2, height - 20.dp.toPx())
            )
            
            // Draw Y label rotated
            rotate(-90f, pivot = Offset(15.dp.toPx(), height / 2)) {
                drawText(
                    yLabelLayout,
                    topLeft = Offset(15.dp.toPx() - yLabelLayout.size.width / 2, height / 2 - yLabelLayout.size.height / 2)
                )
            }

            // Draw Grid and Scale Numbers
            val steps = 7

            // X Axis Ticks and Labels
            for (i in 0..steps) {
                val t = (effectiveMaxTime / steps) * i
                val x = toScreenX(t)
                drawLine(gridColor.copy(alpha = 0.3f), Offset(x, paddingTop), Offset(x, height - paddingBottom), strokeWidth = 1f)
                
                val xNumLabel = t.toInt().toString()
                val xNumLayout = textMeasurer.measure(xNumLabel, labelStyle)
                rotate(-45f, pivot = Offset(x, height - paddingBottom + 15.dp.toPx())) {
                    drawText(
                        xNumLayout,
                        topLeft = Offset(x - xNumLayout.size.width / 2, height - paddingBottom + 15.dp.toPx())
                    )
                }
            }

            // Y Axis Ticks and Labels
            for (i in 0..steps) {
                val f = (effectiveMaxFailures / steps) * i
                val y = toScreenY(f)
                drawLine(gridColor.copy(alpha = 0.3f), Offset(paddingLeft, y), Offset(width - paddingRight, y), strokeWidth = 1f)
                
                val yNumLabel = f.toInt().toString()
                val yNumLayout = textMeasurer.measure(yNumLabel, labelStyle)
                val yX = paddingLeft - yNumLayout.size.width - 15.dp.toPx()
                val yY = y - yNumLayout.size.height / 2
                rotate(-45f, pivot = Offset(yX + yNumLayout.size.width / 2, y)) {
                    drawText(
                        yNumLayout,
                        topLeft = Offset(yX, yY)
                    )
                }
            }

            // Draw ALL Observed Data
            records.forEach { record ->
                val x = toScreenX(record.timeUnit.toDouble())
                val y = toScreenY(record.cumulativeFailures.toDouble())
                val isFiltered = record in filteredRecords
                drawCircle(
                    if (isFiltered) primaryColor else primaryColor.copy(alpha = 0.2f),
                    radius = 5f,
                    center = Offset(x, y)
                )
            }

            // Draw Fitted Curve
            if (fittedParams != null && engine != null) {
                val path = Path()
                var started = false
                val curveSteps = 100
                for (i in 0..curveSteps) {
                    val t = (effectiveMaxTime / curveSteps) * i
                    val f = engine.calculateExpectedFailures(t, fittedParams)
                    val x = toScreenX(t)
                    val y = toScreenY(f)

                    if (x >= paddingLeft && x <= width && y >= paddingTop && y <= height - paddingBottom) {
                        if (!started) {
                            path.moveTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                }
                drawPath(path, secondaryColor, style = Stroke(width = 4f))
            }
            
            // Draw Axes
            drawLine(primaryColor, Offset(paddingLeft, height - paddingBottom), Offset(width - paddingRight, height - paddingBottom), strokeWidth = 2f)
            drawLine(primaryColor, Offset(paddingLeft, paddingTop), Offset(paddingLeft, height - paddingBottom), strokeWidth = 2f)
        }
    }
}
