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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun FluidButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    // Accessibility: Visual feedback for both hover and focus
    val fillFactor by animateFloatAsState(
        targetValue = if ((isHovered || isFocused || isPressed) && enabled) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ButtonFillFactor"
    )

    // Infinite transition for the wave horizontal motion
    val infiniteTransition = rememberInfiniteTransition(label = "WaveTransition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // Smooth color transition for text when background fills
    val textColor by animateColorAsState(
        targetValue = if (fillFactor > 0.5f) MaterialTheme.colorScheme.onPrimary else baseColor,
        animationSpec = tween(durationMillis = 300),
        label = "ButtonTextColor"
    )

    // Accessibility: Clear focus border
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        baseColor.copy(alpha = 0.4f)
    }
    val borderWidth = if (isFocused) 2.4.dp else 1.2.dp

    Box(
        modifier = modifier
            .widthIn(max = 320.dp)
            .height(48.dp) // Accessibility: Standard touch target height
            .clip(RoundedCornerShape(12.dp))
            .background(baseColor.copy(alpha = 0.1f))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom visual feedback via fillFactor
                enabled = enabled,
                onClick = onClick
            )
            .hoverable(interactionSource)
            .focusable(enabled, interactionSource)
            .semantics { 
                role = Role.Button 
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
                // Accessibility: State description
                if (!enabled) {
                    stateDescription = "Disabled"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Liquid Fill Effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val amplitude = 6.dp.toPx()

            val verticalRange = height + amplitude * 2
            val baseLevel = height + amplitude - (fillFactor * verticalRange)

            val path = Path().apply {
                moveTo(0f, height)
                lineTo(width, height)
                lineTo(width, baseLevel)

                val segments = 40
                for (i in segments downTo 0) {
                    val x = (i.toFloat() / segments) * width
                    val waveY = baseLevel + sin(x * (3.5f * PI.toFloat() / width) + wavePhase) * amplitude
                    lineTo(x, waveY)
                }
                close()
            }

            drawPath(path, baseColor)
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
