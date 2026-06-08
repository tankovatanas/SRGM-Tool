package com.example.bbc.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.dp

@Composable
fun BentoBox(
    modifier: Modifier = Modifier,
    title: String? = null,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(10.dp)
            }
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(10.dp)
            ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .semantics { heading() }
                )
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                content = content
            )
        }
    }
}
