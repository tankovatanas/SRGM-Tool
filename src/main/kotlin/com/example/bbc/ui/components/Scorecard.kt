package com.example.bbc.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.example.bbc.domain.ModelParameters
import com.example.bbc.domain.SRGMType
import kotlin.math.*

@Composable
fun MorphingStar(
    modifier: Modifier = Modifier,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MorphingStar")
    val morphProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MorphProgress"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SlowRotation"
    )

    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotation }) {
        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = size.width / 2
        val innerRadius = outerRadius * 0.4f

        val path = Path()
        val segments = 120

        for (i in 0..segments) {
            val angle = (i.toDouble() / segments * 2 * PI) - PI / 2

            val star5Section = 2 * PI / 5
            val star5RelAngle = (angle % star5Section + star5Section) % star5Section
            val star5Dist = abs(star5RelAngle - star5Section / 2) / (star5Section / 2)
            val rStar = outerRadius * (1 - star5Dist.toFloat()) + innerRadius * star5Dist.toFloat()

            val hex6Section = 2 * PI / 6
            val hex6RelAngle = (angle % hex6Section + hex6Section) % hex6Section
            val hex6Dist = abs(hex6RelAngle - hex6Section / 2) / (hex6Section / 2)
            val rHex = outerRadius * 0.9f * (1 - 0.15f * hex6Dist.toFloat())

            val r = rStar * (1 - morphProgress) + rHex * morphProgress

            val x = center.x + r * cos(angle).toFloat()
            val y = center.y + r * sin(angle).toFloat()

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }
}

@Composable
fun ModelScorecard(
    results: Map<SRGMType, ModelParameters>,
    selectedModel: SRGMType,
    onSelect: (SRGMType) -> Unit,
    modifier: Modifier = Modifier
) {
    val bestModel = remember(results) {
        results.filter { it.value.aic > 0 }
            .minByOrNull { it.value.aic }?.key
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val allModels = listOf(
            SRGMType.GoelOkumoto,
            SRGMType.JelinskiMoranda,
            SRGMType.YamadaExponential,
            SRGMType.DelayedSShaped,
            SRGMType.SchickWolverton
        )
        
        allModels.forEach { type ->
            val params = results[type] ?: ModelParameters(0.0, 0.0)
            val isSelected = type == selectedModel
            val isBest = type == bestModel

            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val isFocused by interactionSource.collectIsFocusedAsState()

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.03f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "ScorecardScale"
            )

            val backgroundColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    isHovered || isFocused -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                },
                label = "ScorecardBackground"
            )

            val borderColor by animateColorAsState(
                targetValue = if (isSelected || isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "ScorecardBorder"
            )

            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(backgroundColor)
                    .border(
                        width = if (isSelected || isFocused) 2.dp else 1.dp,
                        color = if (isSelected || isFocused) borderColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelect(type) }
                    )
                    .hoverable(interactionSource)
                    .focusable(interactionSource = interactionSource)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = "Избери ${type.name} модел"
                        selected = isSelected
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            type.name,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (params.aic >= 0) {
                            Text(
                                "AIC: ${params.aic.format(2)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isBest) {
                        MorphingStar(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(24.dp),
                            color = Color(0xFFFFD700) // Gold
                        )
                    }
                }
            }
        }
    }
}

private fun Double.format(digits: Int): String {
    val s = this.toString()
    val dotIndex = s.indexOf(".")
    return if (dotIndex != -1 && dotIndex + digits + 1 <= s.length) {
        s.substring(0, dotIndex + digits + 1)
    } else {
        s
    }
}
