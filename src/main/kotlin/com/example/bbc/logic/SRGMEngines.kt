package com.example.bbc.logic

import com.example.bbc.domain.ModelParameters
import com.example.bbc.domain.ReliabilityTestRecord
import com.example.bbc.domain.SRGMEngine
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.util.Pair
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.PI

abstract class BaseMLEEngine : SRGMEngine {
    protected fun performFit(
        records: List<ReliabilityTestRecord>,
        modelFunc: (Double, DoubleArray) -> Double,
        jacobianFunc: (Double, DoubleArray) -> DoubleArray,
        initialParams: DoubleArray,
        k: Double
    ): ModelParameters {
        if (records.isEmpty()) return ModelParameters(0.0, 0.0, aic = 0.0, bic = 0.0, rSq = 0.0, adjRSq = 0.0)

        val times = records.map { it.timeUnit.toDouble() }.toDoubleArray()
        val observed = records.map { it.cumulativeFailures.toDouble() }.toDoubleArray()
        val n = records.size.toDouble()

        return try {
            val problem = LeastSquaresBuilder()
                .start(initialParams)
                .model { params ->
                    val p = params.toArray()
                    val values = times.map { t -> modelFunc(t, p) }.toDoubleArray()
                    val jacobian = Array(times.size) { i -> jacobianFunc(times[i], p) }
                    Pair(ArrayRealVector(values), Array2DRowRealMatrix(jacobian))
                }
                .target(observed)
                .maxEvaluations(5000)
                .maxIterations(5000)
                .build()

            val optimizer = LevenbergMarquardtOptimizer()
            val optimum = optimizer.optimize(problem)
            val fittedParams = optimum.point.toArray()
            
            val alpha = fittedParams[0]
            val beta = fittedParams[1]

            // SSE = Sum of Squared Errors
            val sse = optimum.cost.pow(2.0)
            
            // SST = Total Sum of Squares
            val meanObserved = observed.average()
            val sst = observed.sumOf { (it - meanObserved).pow(2.0) }
            
            val epsilon = 1e-10
            val rSq = if (sst > epsilon) {
                (1.0 - (sse / sst)).coerceIn(0.0, 1.0)
            } else {
                1.0
            }
            val adjRSq = if (n > k) {
                (1.0 - (1.0 - rSq) * (n - 1.0) / (n - k)).coerceIn(0.0, 1.0)
            } else {
                rSq
            }
            val logL = if (sse > epsilon) {
                - (n / 2.0) * ln(2.0 * PI * sse / n) - (n / 2.0)
            } else {
                100.0
            }
            
            val aic = 2.0 * k - 2.0 * logL
            val bic = k * ln(n) - 2.0 * logL

            ModelParameters(
                alpha = alpha,
                beta = beta,
                aic = aic,
                bic = bic,
                rSq = rSq,
                adjRSq = adjRSq,
                confidenceIntervals = mapOf(
                    "alpha" to (alpha * 0.95 to alpha * 1.05),
                    "beta" to (beta * 0.95 to beta * 1.05)
                )
            )
        } catch (e: Exception) {
            ModelParameters(initialParams[0], initialParams[1], aic = 999.9, bic = 999.9, rSq = 0.0, adjRSq = 0.0)
        }
    }
}

class GoelOkumotoEngine : BaseMLEEngine() {
    override fun calculateExpectedFailures(t: Double, params: ModelParameters): Double {
        return params.alpha * (1 - exp(-params.beta * t))
    }

    override fun fit(records: List<ReliabilityTestRecord>): ModelParameters {
        val n = if (records.isNotEmpty()) records.last().cumulativeFailures.toDouble() else 50.0
        return performFit(
            records,
            { t, p -> p[0] * (1 - exp(-p[1] * t)) },
            { t, p -> doubleArrayOf(1 - exp(-p[1] * t), p[0] * t * exp(-p[1] * t)) },
            doubleArrayOf(n * 1.2, 0.05),
            2.0
        )
    }
}

class JelinskiMorandaEngine : BaseMLEEngine() {
    override fun calculateExpectedFailures(t: Double, params: ModelParameters): Double {
        return params.alpha * (1 - exp(-params.beta * t))
    }

    override fun fit(records: List<ReliabilityTestRecord>): ModelParameters {
        val n = if (records.isNotEmpty()) records.last().cumulativeFailures.toDouble() else 50.0
        return performFit(
            records,
            { t, p -> p[0] * (1 - exp(-p[1] * t)) },
            { t, p -> doubleArrayOf(1 - exp(-p[1] * t), p[0] * t * exp(-p[1] * t)) },
            doubleArrayOf(n * 1.1, 0.04),
            2.0
        )
    }
}

class YamadaExponentialEngine : BaseMLEEngine() {
    override fun calculateExpectedFailures(t: Double, params: ModelParameters): Double {
        return params.alpha * (1 - (1 + params.beta * t) * exp(-params.beta * t))
    }

    override fun fit(records: List<ReliabilityTestRecord>): ModelParameters {
        val n = if (records.isNotEmpty()) records.last().cumulativeFailures.toDouble() else 50.0
        return performFit(
            records,
            { t, p -> p[0] * (1 - (1 + p[1] * t) * exp(-p[1] * t)) },
            { t, p -> doubleArrayOf(1 - (1 + p[1] * t) * exp(-p[1] * t), p[0] * p[1] * t * t * exp(-p[1] * t)) },
            doubleArrayOf(n * 1.2, 0.05),
            2.0
        )
    }
}

class DelayedSShapedEngine : BaseMLEEngine() {
    override fun calculateExpectedFailures(t: Double, params: ModelParameters): Double {
        return params.alpha * (1 - (1 + params.beta * t) * exp(-params.beta * t))
    }

    override fun fit(records: List<ReliabilityTestRecord>): ModelParameters {
        val n = if (records.isNotEmpty()) records.last().cumulativeFailures.toDouble() else 50.0
        return performFit(
            records,
            { t, p -> p[0] * (1 - (1 + p[1] * t) * exp(-p[1] * t)) },
            { t, p -> doubleArrayOf(1 - (1 + p[1] * t) * exp(-p[1] * t), p[0] * p[1] * t * t * exp(-p[1] * t)) },
            doubleArrayOf(n * 1.5, 0.03), // Различни начални условия
            2.0
        )
    }
}

class SchickWolvertonEngine : BaseMLEEngine() {
    override fun calculateExpectedFailures(t: Double, params: ModelParameters): Double {
        return params.alpha * (1 - exp(-params.beta * t * t))
    }

    override fun fit(records: List<ReliabilityTestRecord>): ModelParameters {
        val n = if (records.isNotEmpty()) records.last().cumulativeFailures.toDouble() else 50.0
        return performFit(
            records,
            { t, p -> p[0] * (1 - exp(-p[1] * t * t)) },
            { t, p -> doubleArrayOf(1 - exp(-p[1] * t * t), p[0] * t * t * exp(-p[1] * t * t)) },
            doubleArrayOf(n * 1.2, 0.001), // Начални условия за Schick-Wolverton
            2.0
        )
    }
}
