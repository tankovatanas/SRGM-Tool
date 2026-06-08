package com.example.bbc.ui.viewmodel

import com.example.bbc.domain.*
import com.example.bbc.logic.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.apache.commons.csv.CSVFormat
import java.io.StringReader
import java.net.URL

enum class AppState {
    IDLE, DATA_LOADING, VALIDATION_COMPLETE, CALCULATING_PARAMETERS, READY_FOR_REPORT, ERROR
}

class BBCViewModel {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + Job())
    private var analysisJob: Job? = null

    private val _state = MutableStateFlow(AppState.IDLE)
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _records = MutableStateFlow<List<ReliabilityTestRecord>>(emptyList())
    val records: StateFlow<List<ReliabilityTestRecord>> = _records.asStateFlow()

    private val _selectedModel = MutableStateFlow<SRGMType>(SRGMType.GoelOkumoto)
    val selectedModel: StateFlow<SRGMType> = _selectedModel.asStateFlow()

    // Store results for all models in a map to prevent jumping values
    private val _allResults = MutableStateFlow<Map<SRGMType, ModelParameters>>(emptyMap())
    val allResults: StateFlow<Map<SRGMType, ModelParameters>> = _allResults.asStateFlow()
    
    private val _timeFilterRange = MutableStateFlow(0f..100f)
    val timeFilterRange: StateFlow<ClosedFloatingPointRange<Float>> = _timeFilterRange.asStateFlow()

    private val engines = mapOf(
        SRGMType.GoelOkumoto to GoelOkumotoEngine(),
        SRGMType.JelinskiMoranda to JelinskiMorandaEngine(),
        SRGMType.YamadaExponential to YamadaExponentialEngine(),
        SRGMType.DelayedSShaped to DelayedSShapedEngine(),
        SRGMType.SchickWolverton to SchickWolvertonEngine()
    )

    fun loadData(csvContent: String) {
        _state.value = AppState.DATA_LOADING
        try {
            val reader = StringReader(csvContent)
            val parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(reader)

            val newRecords = parser.mapNotNull { record ->
                val timeStr = if (record.isMapped("Time")) record.get("Time") else if (record.isMapped("time")) record.get("time") else record.get(0)
                val failuresStr = if (record.isMapped("Failures")) record.get("Failures") else if (record.isMapped("failures")) record.get("failures") else record.get(1)
                
                val time = timeStr?.trim()?.toDoubleOrNull()
                val failures = failuresStr?.trim()?.toIntOrNull()
                
                if (time != null && failures != null) {
                    ReliabilityTestRecord(time, failures)
                } else null
            }

            if (newRecords.isEmpty()) throw Exception("No valid data found")
            
            val preprocessed = preprocessRecords(newRecords)
            _records.value = preprocessed
            val maxTime = preprocessed.maxOf { it.timeUnit }.toFloat()
            _timeFilterRange.value = 0f..maxTime
            _state.value = AppState.VALIDATION_COMPLETE
            _allResults.value = emptyMap() // Reset results for new data
        } catch (e: Exception) {
            _state.value = AppState.ERROR
        }
    }

    fun loadDataFromUrl(urlString: String) {
        _state.value = AppState.DATA_LOADING
        viewModelScope.launch {
            try {
                val csvContent = withContext(Dispatchers.IO) {
                    URL(urlString).readText()
                }
                
                val reader = StringReader(csvContent)
                val parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader)

                val newRecords = mutableListOf<ReliabilityTestRecord>()

                for (record in parser) {
                    val cpuHourStr = if (record.isMapped("CPU_hour")) record.get("CPU_hour") else null
                    val failuresStr = if (record.isMapped("Failures") && record.size() > 2) {
                        record.get("Failures")
                    } else {
                        null
                    }

                    val cpuHour = cpuHourStr?.trim()?.toDoubleOrNull() ?: 0.0
                    val failures = failuresStr?.trim()?.toIntOrNull() ?: 0

                    newRecords.add(ReliabilityTestRecord(cpuHour, failures))
                }

                if (newRecords.isEmpty()) throw Exception("No valid data found")

                val preprocessed = preprocessRecords(newRecords)
                _records.value = preprocessed
                val maxTime = preprocessed.maxOf { it.timeUnit }.toFloat()
                _timeFilterRange.value = 0f..maxTime
                _state.value = AppState.VALIDATION_COMPLETE
                _allResults.value = emptyMap()
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = AppState.ERROR
            }
        }
    }

    fun setTimeFilter(range: ClosedFloatingPointRange<Float>) {
        _timeFilterRange.value = range
        if (_state.value == AppState.READY_FOR_REPORT || _state.value == AppState.CALCULATING_PARAMETERS) {
            analysisJob?.cancel()
            analysisJob = viewModelScope.launch {
                delay(200)
                runAnalysis()
            }
        }
    }

    fun selectModel(type: SRGMType) {
        _selectedModel.value = type
    }

    fun runAnalysis() {
        _state.value = AppState.CALCULATING_PARAMETERS
        
        val filteredRecords = getFilteredRecords()
        if (filteredRecords.isEmpty()) {
            _state.value = AppState.ERROR
            return
        }

        // Calculate results for all models at once
        val resultsMap = mutableMapOf<SRGMType, ModelParameters>()
        engines.forEach { (type, engine) ->
            resultsMap[type] = engine.fit(filteredRecords)
        }
        
        _allResults.value = resultsMap
        _state.value = AppState.READY_FOR_REPORT
    }
    
    fun reset() {
        _records.value = emptyList()
        _allResults.value = emptyMap()
        _state.value = AppState.IDLE
    }

    fun getEngine(type: SRGMType): SRGMEngine? = engines[type]
    
    fun getFilteredRecords(): List<ReliabilityTestRecord> {
        return _records.value.filter { 
            it.timeUnit.toFloat() >= _timeFilterRange.value.start && 
            it.timeUnit.toFloat() <= _timeFilterRange.value.endInclusive 
        }
    }

    private fun preprocessRecords(rawRecords: List<ReliabilityTestRecord>): List<ReliabilityTestRecord> {
        if (rawRecords.isEmpty()) return emptyList()
        var isCumulative = true
        for (i in 1 until rawRecords.size) {
            if (rawRecords[i].timeUnit <= rawRecords[i - 1].timeUnit ||
                rawRecords[i].cumulativeFailures < rawRecords[i - 1].cumulativeFailures) {
                isCumulative = false
                break
            }
        }
        if (isCumulative) {
            return rawRecords
        }
        val sorted = rawRecords.sortedBy { it.timeUnit }
        val accumulated = mutableListOf<ReliabilityTestRecord>()
        var cumFailures = 0
        for (rec in sorted) {
            cumFailures += rec.cumulativeFailures
            accumulated.add(ReliabilityTestRecord(rec.timeUnit, cumFailures))
        }
        return accumulated
    }
}
