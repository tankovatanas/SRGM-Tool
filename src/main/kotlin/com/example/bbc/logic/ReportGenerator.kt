package com.example.bbc.logic

import com.example.bbc.domain.ModelParameters
import com.example.bbc.domain.ReliabilityTestRecord
import com.example.bbc.domain.SRGMType

class ReportGenerator {
    fun generateSummary(
        records: List<ReliabilityTestRecord>,
        selectedModel: SRGMType,
        params: ModelParameters
    ): String {
        val sb = StringBuilder()
        sb.append("SRGM - АНАЛИЗАТОРСКИ ОТЧЕТ\n")
        sb.append("====================================\n\n")
        
        sb.append("1. РЕЗЮМЕ ЗА РЪКОВОДСТВОТО\n")
        sb.append("---------------------------\n")
        sb.append("Анализът на системата чрез модела ${selectedModel.name} показва ")
        val status = if (params.beta < 0.05) "стабилизираща се" else "влошаваща се"
        sb.append("тенденция на надеждността, която в момента е $status.\n\n")
        
        sb.append("2. ХАРАКТЕРИСТИКИ НА ДАННИТЕ\n")
        sb.append("----------------------------\n")
        sb.append("Общ брой точки на наблюдение: ${records.size}\n")
        sb.append("Краен кумулативен брой откази: ${records.lastOrNull()?.cumulativeFailures ?: 0}\n")
        sb.append("Времеви диапазон: от ${records.firstOrNull()?.timeUnit ?: 0} до ${records.lastOrNull()?.timeUnit ?: 0} единици\n\n")
        
        sb.append("3. ПАРАМЕТРИ НА МОДЕЛА (MLE ОЦЕНКИ)\n")
        sb.append("----------------------------------\n")
        sb.append("Алфа (Очакван общ брой откази): ${params.alpha.format(2)}\n")
        sb.append("Бета (Скорост на откриване на грешки): ${params.beta.format(4)}\n")
        sb.append("\n")
        
        sb.append("4. КАЧЕСТВО НА ПРИЛЯГАНЕТО\n")
        sb.append("-------------------------------------------\n")
        sb.append("R² (Коефициент на детерминация): ${params.rSq.format(4)}\n")
        sb.append("Коригиран R²: ${params.adjRSq.format(4)}\n")
        sb.append("AIC критерий: ${params.aic.format(2)}\n")
        sb.append("BIC критерий: ${params.bic.format(2)}\n")
        sb.append("\n")
        
        sb.append("5. ЗАКЛЮЧЕНИЕ\n")
        sb.append("-------------\n")
        sb.append("Въз основа на резултата от R² и AIC, този модел се счита за ")
        sb.append(if (params.rSq > 0.9) "отлично прилягащ" else "умерено прилягащ")
        sb.append(" към предоставения набор от данни. Вероятността за бъдещи откази е ")
        sb.append(if (params.beta < 0.03) "ниска." else "значителна.")
        
        return sb.toString()
    }
    
    private fun Double.format(digits: Int): String {
        val s = this.toString()
        val dotIndex = s.indexOf(".")
        if (dotIndex == -1) return s
        return s.take(minOf(s.length, dotIndex + digits + 1))
    }
}
