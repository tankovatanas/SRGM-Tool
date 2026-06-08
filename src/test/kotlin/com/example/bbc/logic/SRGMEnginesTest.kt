package com.example.bbc.logic

import com.example.bbc.domain.ReliabilityTestRecord
import com.example.bbc.ui.viewmodel.BBCViewModel
import com.example.bbc.roundToCustomHalf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SRGMEnginesTest {

    @Test
    fun testGoelOkumotoEngineFitting() {
        val records = listOf(
            ReliabilityTestRecord(1.0, 5),
            ReliabilityTestRecord(2.0, 10),
            ReliabilityTestRecord(3.0, 14),
            ReliabilityTestRecord(4.0, 18),
            ReliabilityTestRecord(5.0, 21),
            ReliabilityTestRecord(6.0, 23),
            ReliabilityTestRecord(7.0, 25),
            ReliabilityTestRecord(8.0, 26),
            ReliabilityTestRecord(9.0, 27),
            ReliabilityTestRecord(10.0, 28)
        )
        val engine = GoelOkumotoEngine()
        val params = engine.fit(records)
        
        assertTrue(params.alpha > 0)
        assertTrue(params.beta > 0)
        assertTrue(params.rSq in 0.0..1.0)
        assertTrue(params.adjRSq in 0.0..1.0)
        assertTrue(params.adjRSq <= params.rSq) // Adjusted R-squared should be less than or equal to R-squared
        
        val pred = engine.calculateExpectedFailures(5.0, params)
        assertTrue(pred > 0.0)
    }

    @Test
    fun testSchickWolvertonEngineFitting() {
        val records = listOf(
            ReliabilityTestRecord(1.0, 2),
            ReliabilityTestRecord(2.0, 8),
            ReliabilityTestRecord(3.0, 15),
            ReliabilityTestRecord(4.0, 22),
            ReliabilityTestRecord(5.0, 27),
            ReliabilityTestRecord(6.0, 30),
            ReliabilityTestRecord(7.0, 32),
            ReliabilityTestRecord(8.0, 33),
            ReliabilityTestRecord(9.0, 34),
            ReliabilityTestRecord(10.0, 35)
        )
        val engine = SchickWolvertonEngine()
        val params = engine.fit(records)
        
        assertTrue(params.alpha > 0)
        assertTrue(params.beta > 0)
        assertTrue(params.rSq in 0.0..1.0)
        assertTrue(params.adjRSq in 0.0..1.0)
        assertTrue(params.adjRSq <= params.rSq)
        
        val pred = engine.calculateExpectedFailures(5.0, params)
        assertTrue(pred > 0.0)
        
        // Test that expected failure matches formula alpha * (1 - exp(-beta * t^2))
        val expected = params.alpha * (1 - kotlin.math.exp(-params.beta * 5.0 * 5.0))
        assertEquals(expected, pred, 1e-6)
    }

    @Test
    fun testAdjustedRSquaredCalculation() {
        val records = listOf(
            ReliabilityTestRecord(1.0, 2),
            ReliabilityTestRecord(2.0, 4),
            ReliabilityTestRecord(3.0, 6)
        )
        // With 3 records and 2 parameters, n = 3, k = 2
        // If rSq is 0.9, adjRSq = 1 - (1 - 0.9) * (3 - 1) / (3 - 2) = 1 - 0.1 * 2 / 1 = 0.8
        val engine = GoelOkumotoEngine()
        val params = engine.fit(records)
        
        val n = 3.0
        val k = 2.0
        val expectedAdjRSq = (1.0 - (1.0 - params.rSq) * (n - 1.0) / (n - k)).coerceIn(0.0, 1.0)
        assertEquals(expectedAdjRSq, params.adjRSq, 1e-6)
    }

    @Test
    fun testT39Fitting() {
        val t39Records = listOf(
            ReliabilityTestRecord(62.0, 5),
            ReliabilityTestRecord(63.0, 25),
            ReliabilityTestRecord(66.0, 10),
            ReliabilityTestRecord(24.0, 7),
            ReliabilityTestRecord(8.0, 4),
            ReliabilityTestRecord(206.5, 4),
            ReliabilityTestRecord(60.0, 0),
            ReliabilityTestRecord(65.0, 2),
            ReliabilityTestRecord(59.0, 0),
            ReliabilityTestRecord(163.0, 1),
            ReliabilityTestRecord(62.5, 0),
            ReliabilityTestRecord(60.0, 6),
            ReliabilityTestRecord(61.5, 0),
            ReliabilityTestRecord(66.0, 5),
            ReliabilityTestRecord(64.0, 0)
        )
        val engines = listOf(
            GoelOkumotoEngine(),
            JelinskiMorandaEngine(),
            YamadaExponentialEngine(),
            DelayedSShapedEngine(),
            SchickWolvertonEngine()
        )
        for (engine in engines) {
            val params = engine.fit(t39Records)
            println("Engine: ${engine::class.simpleName}, alpha: ${params.alpha}, beta: ${params.beta}, phi: ${params.phi}, rSq: ${params.rSq}, adjRSq: ${params.adjRSq}, aic: ${params.aic}")
        }
    }

    @Test
    fun testT39AccumulatedFitting() {
        val t39Raw = listOf(
            ReliabilityTestRecord(62.0, 5),
            ReliabilityTestRecord(63.0, 25),
            ReliabilityTestRecord(66.0, 10),
            ReliabilityTestRecord(24.0, 7),
            ReliabilityTestRecord(8.0, 4),
            ReliabilityTestRecord(206.5, 4),
            ReliabilityTestRecord(60.0, 0),
            ReliabilityTestRecord(65.0, 2),
            ReliabilityTestRecord(59.0, 0),
            ReliabilityTestRecord(163.0, 1),
            ReliabilityTestRecord(62.5, 0),
            ReliabilityTestRecord(60.0, 6),
            ReliabilityTestRecord(61.5, 0),
            ReliabilityTestRecord(66.0, 5),
            ReliabilityTestRecord(64.0, 0)
        )
        val accumulated = mutableListOf<ReliabilityTestRecord>()
        var cumTime = 0.0
        var cumFailures = 0
        for (rec in t39Raw) {
            cumTime += rec.timeUnit
            cumFailures += rec.cumulativeFailures
            accumulated.add(ReliabilityTestRecord(cumTime, cumFailures))
        }
        val engines = listOf(
            GoelOkumotoEngine(),
            JelinskiMorandaEngine(),
            YamadaExponentialEngine(),
            DelayedSShapedEngine(),
            SchickWolvertonEngine()
        )
        for (engine in engines) {
            val params = engine.fit(accumulated)
            println("ACCUMULATED - Engine: ${engine::class.simpleName}, alpha: ${params.alpha}, beta: ${params.beta}, phi: ${params.phi}, rSq: ${params.rSq}, adjRSq: ${params.adjRSq}, aic: ${params.aic}")
        }
    }

    @Test
    fun testViewModelDataLoading() {
        val viewModel = BBCViewModel()
        val csv = "Time,Failures\n62.0,5\n8.0,25\n"
        viewModel.loadData(csv)
        val records = viewModel.records.value
        assertEquals(2, records.size)
        // Should be accumulated sequentially (Interpretation B):
        // Record 0: time = 62.0, failures = 5
        // Record 1: time = 70.0, failures = 30
        assertEquals(62.0, records[0].timeUnit, 1e-6)
        assertEquals(5, records[0].cumulativeFailures)
        assertEquals(70.0, records[1].timeUnit, 1e-6)
        assertEquals(30, records[1].cumulativeFailures)
    }

    @Test
    fun testCustomRounding() {
        // - if negative number - round to 0
        assertEquals(0f, (-2.5f).roundToCustomHalf(), 1e-6f)
        assertEquals(0f, (-0.2f).roundToCustomHalf(), 1e-6f)
        
        // - if multiple of 0.5 - keep it
        assertEquals(1.0f, 1.0f.roundToCustomHalf(), 1e-6f)
        assertEquals(1.5f, 1.5f.roundToCustomHalf(), 1e-6f)
        assertEquals(0.5f, 0.5f.roundToCustomHalf(), 1e-6f)
        
        // - if not multiple of 0.5:
        // lower than 0.5 fractional part -> round down to whole number
        assertEquals(1.0f, 1.2f.roundToCustomHalf(), 1e-6f)
        assertEquals(2.0f, 2.49f.roundToCustomHalf(), 1e-6f)
        
        // higher than 0.5 fractional part -> round up to whole number
        assertEquals(2.0f, 1.7f.roundToCustomHalf(), 1e-6f)
        assertEquals(3.0f, 2.51f.roundToCustomHalf(), 1e-6f)
    }

    @Test
    fun testT39SortedAccumulatedFitting() {
        val t39Raw = listOf(
            ReliabilityTestRecord(62.0, 5),
            ReliabilityTestRecord(63.0, 25),
            ReliabilityTestRecord(66.0, 10),
            ReliabilityTestRecord(24.0, 7),
            ReliabilityTestRecord(8.0, 4),
            ReliabilityTestRecord(206.5, 4),
            ReliabilityTestRecord(60.0, 0),
            ReliabilityTestRecord(65.0, 2),
            ReliabilityTestRecord(59.0, 0),
            ReliabilityTestRecord(163.0, 1),
            ReliabilityTestRecord(62.5, 0),
            ReliabilityTestRecord(60.0, 6),
            ReliabilityTestRecord(61.5, 0),
            ReliabilityTestRecord(66.0, 5),
            ReliabilityTestRecord(64.0, 0)
        )
        // Sort by timeUnit
        val sorted = t39Raw.sortedBy { it.timeUnit }
        val accumulated = mutableListOf<ReliabilityTestRecord>()
        var cumFailures = 0
        for (rec in sorted) {
            cumFailures += rec.cumulativeFailures
            accumulated.add(ReliabilityTestRecord(rec.timeUnit, cumFailures))
        }
        val engines = listOf(
            GoelOkumotoEngine(),
            JelinskiMorandaEngine(),
            YamadaExponentialEngine(),
            DelayedSShapedEngine(),
            SchickWolvertonEngine()
        )
        for (engine in engines) {
            val params = engine.fit(accumulated)
            println("SORTED ACCUMULATED - Engine: ${engine::class.simpleName}, alpha: ${params.alpha}, beta: ${params.beta}, phi: ${params.phi}, rSq: ${params.rSq}, adjRSq: ${params.adjRSq}, aic: ${params.aic}")
        }
    }

    @Test
    fun testT39FromUrlFitting() {
        val url = java.net.URL("https://raw.githubusercontent.com/Derek-Jones/Reliability-data/refs/heads/main/T39.csv")
        val csvContent = url.readText()
        
        // Parse raw records to verify
        val reader = java.io.StringReader(csvContent)
        val parser = org.apache.commons.csv.CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()
            .parse(reader)
            
        val records = mutableListOf<ReliabilityTestRecord>()
        for (record in parser) {
            val cpuHour = record.get("CPU_hour").trim().toDoubleOrNull() ?: 0.0
            val failures = if (record.isMapped("Failures") && record.size() > 2) {
                record.get("Failures").trim().toIntOrNull() ?: 0
            } else 0
            records.add(ReliabilityTestRecord(cpuHour, failures))
        }
        
        // Sort and accumulate
        val sorted = records.sortedBy { it.timeUnit }
        val accumulated = mutableListOf<ReliabilityTestRecord>()
        var cumFailures = 0
        for (rec in sorted) {
            cumFailures += rec.cumulativeFailures
            accumulated.add(ReliabilityTestRecord(rec.timeUnit, cumFailures))
        }
        
        val engines = listOf(
            GoelOkumotoEngine(),
            JelinskiMorandaEngine(),
            YamadaExponentialEngine(),
            DelayedSShapedEngine(),
            SchickWolvertonEngine()
        )
        for (engine in engines) {
            val params = engine.fit(accumulated)
            println("URL FIT SORTED - Engine: ${engine::class.simpleName}, alpha: ${params.alpha}, beta: ${params.beta}, phi: ${params.phi}, rSq: ${params.rSq}, adjRSq: ${params.adjRSq}, aic: ${params.aic}, bic: ${params.bic}")
        }
        
        // Chronological (Sequential) accumulated
        val accumulatedSeq = mutableListOf<ReliabilityTestRecord>()
        var cumTime = 0.0
        var cumFailuresSeq = 0
        for (rec in records) {
            cumTime += rec.timeUnit
            cumFailuresSeq += rec.cumulativeFailures
            accumulatedSeq.add(ReliabilityTestRecord(cumTime, cumFailuresSeq))
        }
        
        for (engine in engines) {
            val params = engine.fit(accumulatedSeq)
            println("URL FIT SEQ - Engine: ${engine::class.simpleName}, alpha: ${params.alpha}, beta: ${params.beta}, phi: ${params.phi}, rSq: ${params.rSq}, adjRSq: ${params.adjRSq}, aic: ${params.aic}, bic: ${params.bic}")
        }
    }

    @Test
    fun testPrintSortedAccumulated() {
        val url = java.net.URL("https://raw.githubusercontent.com/Derek-Jones/Reliability-data/refs/heads/main/T39.csv")
        val csvContent = url.readText()
        val reader = java.io.StringReader(csvContent)
        val parser = org.apache.commons.csv.CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()
            .parse(reader)
        val records = mutableListOf<ReliabilityTestRecord>()
        for (record in parser) {
            val cpuHour = record.get("CPU_hour").trim().toDoubleOrNull() ?: 0.0
            val failures = if (record.isMapped("Failures") && record.size() > 2) {
                record.get("Failures").trim().toIntOrNull() ?: 0
            } else 0
            records.add(ReliabilityTestRecord(cpuHour, failures))
        }
        val sorted = records.sortedBy { it.timeUnit }
        var cumFailures = 0
        for ((idx, rec) in sorted.withIndex()) {
            cumFailures += rec.cumulativeFailures
            println("SORTED KOTLIN $idx: time: ${rec.timeUnit}, failures: ${rec.cumulativeFailures}, cumFailures: $cumFailures")
        }
    }
}

