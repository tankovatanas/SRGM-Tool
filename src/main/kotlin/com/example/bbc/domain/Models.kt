package com.example.bbc.domain

data class ReliabilityTestRecord(
    val timeUnit: Double,
    val cumulativeFailures: Int
)

data class ModelParameters(
    val alpha: Double,
    val beta: Double,
    val aic: Double = 0.0,
    val bic: Double = 0.0,
    val rSq: Double = 0.0,
    val adjRSq: Double = 0.0,
    val confidenceIntervals: Map<String, Pair<Double, Double>> = emptyMap()
)

sealed class SRGMType(val name: String) {
    object GoelOkumoto : SRGMType("Goel-Okumoto")
    object JelinskiMoranda : SRGMType("Jelinski-Moranda")
    object YamadaExponential : SRGMType("Yamada Exponential")
    object DelayedSShaped : SRGMType("Delayed S-Shaped")
    object SchickWolverton : SRGMType("Schick-Wolverton")
}

interface SRGMEngine {
    fun calculateExpectedFailures(t: Double, params: ModelParameters): Double
    fun fit(records: List<ReliabilityTestRecord>): ModelParameters
}
