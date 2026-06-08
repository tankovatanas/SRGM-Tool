package com.example.bbc

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bbc.domain.SRGMType
import com.example.bbc.ui.components.*
import com.example.bbc.ui.theme.BBCTheme
import com.example.bbc.ui.viewmodel.BBCViewModel
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import com.example.bbc.logic.rememberPdfExporter
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

data class MathParticle(
    val symbol: String,
    val initialX: Float,
    val initialY: Float,
    val speed: Float,
    val fontSize: TextUnit,
    val opacity: Float
)

internal fun Float.roundToCustomHalf(): Float {
    if (this < 0f) return 0f
    val floorVal = kotlin.math.floor(this.toDouble()).toFloat()
    val frac = this - floorVal
    val epsilon = 1e-6f
    return when {
        kotlin.math.abs(frac - 0.5f) < epsilon -> floorVal + 0.5f
        kotlin.math.abs(frac) < epsilon -> floorVal
        frac < 0.5f -> floorVal
        else -> floorVal + 1f
    }
}

@Composable
fun App() {
    val viewModel = remember { BBCViewModel() }
    val state by viewModel.state.collectAsState()
    val records by viewModel.records.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val results by viewModel.allResults.collectAsState()
    val timeFilter by viewModel.timeFilterRange.collectAsState()

    var showToast by remember { mutableStateOf(false) }
    var toastTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        if (state == com.example.bbc.ui.viewmodel.AppState.VALIDATION_COMPLETE) {
            toastTrigger++
        }
    }

    LaunchedEffect(toastTrigger) {
        if (toastTrigger > 0) {
            showToast = true
            delay(2000.milliseconds)
            showToast = false
        }
    }

    BBCTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            MathBackground()

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
                color = Color.Transparent
            ) {
                ResponsiveLayout { screenSize ->
                    MainDashboard(
                        screenSize = screenSize,
                        viewModel = viewModel,
                        state = state,
                        records = records,
                        selectedModel = selectedModel,
                        results = results,
                        timeFilter = timeFilter
                    )
                }
            }

            AnimatedVisibility(
                visible = showToast,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .pointerInput(Unit) {
                            // Consumes touch events to ensure they don't affect the toast's duration or propagate
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(size.width * 0.2f, size.height * 0.5f)
                                lineTo(size.width * 0.45f, size.height * 0.75f)
                                lineTo(size.width * 0.8f, size.height * 0.25f)
                            }
                            drawPath(path, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
                        }
                        Text(
                            "Данните са заредени успешно!",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MathBackground() {
    val symbols = listOf("λ", "Σ", "∫", "∞", "μ", "σ", "∂", "√", "π", "Δ", "≈", "≠", "≤", "≥")
    val textMeasurer = rememberTextMeasurer()
    val infiniteTransition = rememberInfiniteTransition()

    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val particleCount = 25
    val particles = remember {
        List(particleCount) {
            MathParticle(
                symbol = symbols.random(),
                initialX = Random.nextFloat(),
                initialY = Random.nextFloat(),
                speed = 0.05f + Random.nextFloat() * 0.1f,
                fontSize = (14 + Random.nextFloat() * 20).sp,
                opacity = 0.05f + Random.nextFloat() * 0.1f
            )
        }
    }

    val symbolColor = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
        particles.forEach { particle ->
            val yPos = (particle.initialY * size.height + (t * particle.speed * 50f)) % size.height
            val xPos = particle.initialX * size.width

            drawText(
                textMeasurer = textMeasurer,
                text = particle.symbol,
                topLeft = Offset(xPos, yPos),
                style = TextStyle(
                    color = symbolColor.copy(alpha = particle.opacity),
                    fontSize = particle.fontSize
                )
            )
        }
    }
}

@Composable
fun MainDashboard(
    screenSize: ScreenSize,
    viewModel: BBCViewModel,
    state: com.example.bbc.ui.viewmodel.AppState,
    records: List<com.example.bbc.domain.ReliabilityTestRecord>,
    selectedModel: SRGMType,
    results: Map<SRGMType, com.example.bbc.domain.ModelParameters>,
    timeFilter: ClosedFloatingPointRange<Float>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (screenSize == ScreenSize.COMPACT || screenSize == ScreenSize.MEDIUM) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Text(
                    text = "BBC Надеждност",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (state != com.example.bbc.ui.viewmodel.AppState.IDLE && state != com.example.bbc.ui.viewmodel.AppState.DATA_LOADING) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FluidButton(
                        text = "Нулиране",
                        onClick = { viewModel.reset() },
                        baseColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(180.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BBC Надеждност",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (state != com.example.bbc.ui.viewmodel.AppState.IDLE && state != com.example.bbc.ui.viewmodel.AppState.DATA_LOADING) {
                    FluidButton(
                        text = "Нулиране",
                        onClick = { viewModel.reset() },
                        baseColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(180.dp)
                    )
                }
            }
        }

        when (screenSize) {
            ScreenSize.COMPACT -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GraphSection(records, viewModel.getFilteredRecords(), results[selectedModel], viewModel, Modifier.height(300.dp))
                    FilterSection(viewModel, records, timeFilter, Modifier.fillMaxWidth())
                    ControlSection(viewModel, state, Modifier.fillMaxWidth())
                    ScorecardSection(viewModel, state, selectedModel, results, Modifier.fillMaxWidth())
                    DetailsSection(results[selectedModel], Modifier.fillMaxWidth())
                    ReportSection(viewModel.getFilteredRecords(), selectedModel, results[selectedModel], Modifier.fillMaxWidth())
                }
            }
            ScreenSize.MEDIUM -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GraphSection(records, viewModel.getFilteredRecords(), results[selectedModel], viewModel, Modifier.height(400.dp))
                    FilterSection(viewModel, records, timeFilter, Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ControlSection(viewModel, state, Modifier.weight(1f).fillMaxHeight())
                        ScorecardSection(viewModel, state, selectedModel, results, Modifier.weight(1f).fillMaxHeight())
                    }
                    DetailsSection(results[selectedModel], Modifier.fillMaxWidth())
                    ReportSection(viewModel.getFilteredRecords(), selectedModel, results[selectedModel], Modifier.fillMaxWidth())
                }
            }
            ScreenSize.LARGE, ScreenSize.EXTRA_LARGE -> {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GraphSection(
                            records,
                            viewModel.getFilteredRecords(),
                            results[selectedModel],
                            viewModel,
                            Modifier.weight(1.8f).fillMaxHeight()
                        )
                        Column(
                            modifier = Modifier.weight(1.2f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ScorecardSection(viewModel, state, selectedModel, results, Modifier.weight(1f).fillMaxWidth())
                            ControlSection(viewModel, state, Modifier.weight(1f).fillMaxWidth())
                        }
                    }
                    DetailsSection(results[selectedModel], Modifier.fillMaxWidth())
                    FilterSection(viewModel, records, timeFilter, Modifier.fillMaxWidth())
                    ReportSection(viewModel.getFilteredRecords(), selectedModel, results[selectedModel], Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun GraphSection(
    records: List<com.example.bbc.domain.ReliabilityTestRecord>,
    filteredRecords: List<com.example.bbc.domain.ReliabilityTestRecord>,
    results: com.example.bbc.domain.ModelParameters?,
    viewModel: BBCViewModel,
    modifier: Modifier
) {
    BentoBox(
        title = "Прогностична визуализация",
        modifier = modifier,
        contentDescription = "Графика на наблюдаваните и прогнозираните софтуерни откази във времето"
    ) {
        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Няма заредени данни. Използвайте контролния панел.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            ReliabilityGraph(
                records = records,
                filteredRecords = filteredRecords,
                fittedParams = results,
                engine = viewModel.getEngine(viewModel.selectedModel.value),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun FilterSection(
    viewModel: BBCViewModel,
    records: List<com.example.bbc.domain.ReliabilityTestRecord>,
    timeFilter: ClosedFloatingPointRange<Float>,
    modifier: Modifier
) {
    if (records.isNotEmpty()) {
        val maxTime = records.maxOf { it.timeUnit }.toFloat()
        
        fun formatTimeValue(value: Float): String {
            return if (value % 1.0f == 0.0f) {
                value.toInt().toString()
            } else {
                value.toString()
            }
        }

        fun Float.roundToHalfStep(): Float {
            return kotlin.math.round(this * 2f) / 2f
        }



        var startInput by remember(timeFilter.start) { mutableStateOf(formatTimeValue(timeFilter.start)) }
        var endInput by remember(timeFilter.endInclusive) { mutableStateOf(formatTimeValue(timeFilter.endInclusive)) }

        BentoBox(
            title = "Филтър за времеви диапазон",
            modifier = modifier,
            contentDescription = "Филтриране на данни по време"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Плъзнете за филтриране или въведете точни стойности по-долу:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                RangeSlider(
                    value = timeFilter,
                    onValueChange = { range ->
                        val start = range.start.roundToHalfStep()
                        val end = range.endInclusive.roundToHalfStep()
                        viewModel.setTimeFilter(start..end)
                    },
                    valueRange = 0f..maxTime,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startInput,
                        onValueChange = { newVal ->
                            startInput = newVal
                            val startVal = newVal.toFloatOrNull()
                            if (startVal != null) {
                                val roundedStart = startVal.roundToCustomHalf()
                                val appliedStart = roundedStart.coerceAtMost(timeFilter.endInclusive)
                                viewModel.setTimeFilter(appliedStart..timeFilter.endInclusive)
                            }
                        },
                        label = { Text("Начало") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                    OutlinedTextField(
                        value = endInput,
                        onValueChange = { newVal ->
                            endInput = newVal
                            val endVal = newVal.toFloatOrNull()
                            if (endVal != null) {
                                val roundedEnd = endVal.roundToCustomHalf()
                                val appliedEnd = roundedEnd.coerceIn(timeFilter.start, maxTime)
                                viewModel.setTimeFilter(timeFilter.start..appliedEnd)
                            }
                        },
                        label = { Text("Край") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        )
                    )
                }

                Text(
                    text = "Анализ на записи от t=${formatTimeValue(timeFilter.start)} до t=${formatTimeValue(timeFilter.endInclusive)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ControlSection(viewModel: BBCViewModel, state: com.example.bbc.ui.viewmodel.AppState, modifier: Modifier) {
    if (state != com.example.bbc.ui.viewmodel.AppState.READY_FOR_REPORT) {
        BentoBox(title = "Панел за управление", modifier = modifier) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedVisibility(
                    visible = state == com.example.bbc.ui.viewmodel.AppState.IDLE || state == com.example.bbc.ui.viewmodel.AppState.ERROR || state == com.example.bbc.ui.viewmodel.AppState.DATA_LOADING,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FluidButton(
                        text = if (state == com.example.bbc.ui.viewmodel.AppState.DATA_LOADING) "Зареждане..." else "Зареди T39 от GitHub",
                        onClick = {
                            viewModel.loadDataFromUrl("https://raw.githubusercontent.com/Derek-Jones/Reliability-data/main/T39.csv")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state == com.example.bbc.ui.viewmodel.AppState.IDLE || state == com.example.bbc.ui.viewmodel.AppState.ERROR
                    )
                }

                if (state == com.example.bbc.ui.viewmodel.AppState.VALIDATION_COMPLETE || state == com.example.bbc.ui.viewmodel.AppState.CALCULATING_PARAMETERS) {
                    FluidButton(
                        text = if (state == com.example.bbc.ui.viewmodel.AppState.CALCULATING_PARAMETERS) "Изчисляване..." else "Стартирай анализ",
                        onClick = { viewModel.runAnalysis() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (state == com.example.bbc.ui.viewmodel.AppState.ERROR) {
                    Text(
                        text = "Грешка при обработката. Проверете формата.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun ScorecardSection(
    viewModel: BBCViewModel,
    state: com.example.bbc.ui.viewmodel.AppState,
    selectedModel: SRGMType,
    results: Map<SRGMType, com.example.bbc.domain.ModelParameters>,
    modifier: Modifier
) {
    BentoBox(title = "Сравнение на модели", modifier = modifier) {
        val isAnalyzed = state == com.example.bbc.ui.viewmodel.AppState.READY_FOR_REPORT
        val displayResults = mapOf(
            SRGMType.GoelOkumoto to (results[SRGMType.GoelOkumoto] ?: com.example.bbc.domain.ModelParameters(0.0, 0.0, aic = if (isAnalyzed) 0.0 else -1.0)),
            SRGMType.JelinskiMoranda to (results[SRGMType.JelinskiMoranda] ?: com.example.bbc.domain.ModelParameters(0.0, 0.0, aic = if (isAnalyzed) 0.0 else -1.0)),
            SRGMType.YamadaExponential to (results[SRGMType.YamadaExponential] ?: com.example.bbc.domain.ModelParameters(0.0, 0.0, aic = if (isAnalyzed) 0.0 else -1.0)),
            SRGMType.DelayedSShaped to (results[SRGMType.DelayedSShaped] ?: com.example.bbc.domain.ModelParameters(0.0, 0.0, aic = if (isAnalyzed) 0.0 else -1.0)),
            SRGMType.SchickWolverton to (results[SRGMType.SchickWolverton] ?: com.example.bbc.domain.ModelParameters(0.0, 0.0, aic = if (isAnalyzed) 0.0 else -1.0))
        )
        ModelScorecard(
            results = displayResults,
            selectedModel = selectedModel,
            onSelect = { viewModel.selectModel(it) }
        )
    }
}

@Composable
fun DetailsSection(results: com.example.bbc.domain.ModelParameters?, modifier: Modifier) {
    BentoBox(title = "Детайли за параметрите", modifier = modifier) {
        ParameterDetails(params = results)
    }
}

@Composable
fun ReportSection(
    filteredRecords: List<com.example.bbc.domain.ReliabilityTestRecord>,
    selectedModel: SRGMType,
    results: com.example.bbc.domain.ModelParameters?,
    modifier: Modifier
) {
    if (results != null) {
        val reportGenerator = remember { com.example.bbc.logic.ReportGenerator() }
        val summary = reportGenerator.generateSummary(filteredRecords, selectedModel, results)

        BentoBox(title = "Автоматизирано резюме", modifier = modifier) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    val exporter = rememberPdfExporter()
                    FluidButton(
                        text = "Експортирай отчет",
                        onClick = { exporter.exportReport(filteredRecords, selectedModel, results, summary) },
                        modifier = Modifier.width(180.dp),
                        contentDescription = "Експортиране на отчета као PDF"
                    )
                }
            }
        }
    }
}
