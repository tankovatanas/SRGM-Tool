package com.example.bbc.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bbc.domain.ModelParameters

@Composable
fun ParameterDetails(
    params: ModelParameters?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (params == null) {
            Text(
                text = "Все още няма резултати от анализа.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            ParameterRow("Алфа (Мащаб)", params.alpha.toString())
            ParameterRow("Бета (Форма)", params.beta.toString())
            params.phi?.let { ParameterRow("Фи (Инфлексия)", it.toString()) }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Качество на модела",
                style = MaterialTheme.typography.labelMedium, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            ParameterRow("R² (Коефициент на детерминация)", params.rSq.format(4))
            ParameterRow("Коригиран R²", params.adjRSq.format(4))
            ParameterRow("AIC", params.aic.toString())
            ParameterRow("BIC", params.bic.toString())
        }
    }
}

@Composable
private fun ParameterRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label, 
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value, 
            style = MaterialTheme.typography.bodySmall, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun Double.format(digits: Int): String {
    val s = this.toString()
    val dotIndex = s.indexOf(".")
    if (dotIndex == -1) return s
    return s.take(minOf(s.length, dotIndex + digits + 1))
}
