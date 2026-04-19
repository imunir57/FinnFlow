package com.finnflow.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.TransactionType
import com.finnflow.ui.theme.Ink
import com.finnflow.ui.theme.InkFaint
import com.finnflow.ui.theme.InkMedium
import com.finnflow.ui.theme.Rule
import com.finnflow.ui.theme.WarmCard
import com.finnflow.ui.theme.WarmPaper
import com.finnflow.ui.theme.WarmSurface
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

private val sliceColors = listOf(
    Color(0xFFE24B4A), Color(0xFF378ADD), Color(0xFF1D9E75),
    Color(0xFFEF9F27), Color(0xFF7F77DD), Color(0xFFD4537E),
    Color(0xFF639922), Color(0xFF888780)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateToCategory: (categoryId: Long, from: LocalDate, to: LocalDate, type: TransactionType) -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showPicker     by remember { mutableStateOf(false) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker   by remember { mutableStateOf(false) }

    // Single date picker — used for MONTHLY (jump to month) and ANNUALLY (jump to year)
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.from.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.onPickedDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }

    // Two-step pickers for CUSTOM range
    if (showFromPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.from.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val d = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.onCustomRangeChange(d, state.to)
                    }
                    showFromPicker = false
                    showToPicker   = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
    if (showToPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.to.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val d = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.onCustomRangeChange(state.from, d)
                    }
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmPaper)
    ) {
        // ── Title bar ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Statistics",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = Ink,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, "Menu", tint = InkMedium)
            }
        }

        // ── Range tabs (underline style) ──────────────────────────────────
        RangeTabsRow(period = state.period, onPeriodChange = viewModel::onPeriodChange)

        // ── Range display: < label >  [Pick] ─────────────────────────────
        RangeDisplayRow(
            period = state.period,
            from   = state.from,
            to     = state.to,
            onPrev = viewModel::previousPeriod,
            onNext = viewModel::nextPeriod,
            onPick = {
                when (state.period) {
                    StatsPeriod.MONTHLY, StatsPeriod.ANNUALLY -> showPicker = true
                    StatsPeriod.CUSTOM -> showFromPicker = true
                }
            }
        )

        // ── Income / Expense toggle (centered) ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            TypeToggle(selectedType = state.selectedType, onTypeChange = viewModel::onTypeChange)
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                DonutChartSection(
                    summaries   = state.activeSummary,
                    totalAmount = state.totalAmount,
                    label       = if (state.selectedType == TransactionType.EXPENSE) "Total out" else "Total in",
                    percentOf   = state::percentOf
                )
            }
            item {
                LegendRow(summaries = state.activeSummary, percentOf = state::percentOf)
            }
            item {
                HorizontalDivider(color = Rule, modifier = Modifier.padding(top = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Category", fontSize = 10.sp, color = InkFaint, letterSpacing = 1.sp)
                    Text("Amount",   fontSize = 10.sp, color = InkFaint, letterSpacing = 1.sp)
                }
            }
            if (state.activeSummary.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data for selected period", fontSize = 14.sp, color = InkFaint)
                    }
                }
            } else {
                items(state.activeSummary.size) { index ->
                    val summary = state.activeSummary[index]
                    val color   = sliceColors[index % sliceColors.size]
                    CategoryListRow(
                        summary = summary,
                        color   = color,
                        percent = state.percentOf(summary.totalAmount).toFloat() /
                                  100f * state.totalAmount.let {
                                      if (it > 0) (summary.totalAmount / it * 100f).toFloat() else 0f
                                  }.let { 1f }, // keep raw percent for bar
                        percentInt = state.percentOf(summary.totalAmount),
                        onClick = {
                            onNavigateToCategory(summary.categoryId, state.from, state.to, state.selectedType)
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Range tabs (underline indicator) ─────────────────────────────────────────

private val StatsPeriod.label: String get() = when (this) {
    StatsPeriod.MONTHLY  -> "Month"
    StatsPeriod.ANNUALLY -> "Year"
    StatsPeriod.CUSTOM   -> "Custom"
}

@Composable
private fun RangeTabsRow(period: StatsPeriod, onPeriodChange: (StatsPeriod) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            StatsPeriod.entries.forEach { p ->
                val active = period == p
                val inkColor = Ink
                Box(
                    modifier = Modifier
                        .clickable { onPeriodChange(p) }
                        .padding(top = 14.dp, bottom = 12.dp)
                        .drawBehind {
                            if (active) drawLine(
                                color = inkColor,
                                start = Offset(0f, size.height + 1.dp.toPx()),
                                end   = Offset(size.width, size.height + 1.dp.toPx()),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                ) {
                    Text(
                        p.label,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active) Ink else InkFaint
                    )
                }
            }
        }
        HorizontalDivider(color = Rule)
    }
}

// ── Range display row ─────────────────────────────────────────────────────────

@Composable
private fun RangeDisplayRow(
    period: StatsPeriod,
    from: LocalDate,
    to: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPick: () -> Unit
) {
    val label = when (period) {
        StatsPeriod.MONTHLY  -> YearMonth.from(from).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        StatsPeriod.ANNUALLY -> from.year.toString()
        StatsPeriod.CUSTOM   -> {
            val fmt  = DateTimeFormatter.ofPattern("MMM d")
            val fmtY = DateTimeFormatter.ofPattern("MMM d, yyyy")
            if (from.year == to.year) "${from.format(fmt)} – ${to.format(fmtY)}"
            else "${from.format(fmtY)} – ${to.format(fmtY)}"
        }
    }
    val canNavigate = period != StatsPeriod.CUSTOM

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 18.dp, top = 14.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: chevron nav + label
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onPrev,
                enabled = canNavigate,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack, "Previous",
                    tint = if (canNavigate) InkMedium else Rule,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onNext,
                enabled = canNavigate,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ArrowForward, "Next",
                    tint = if (canNavigate) InkMedium else Rule,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Right: Pick pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, Rule, RoundedCornerShape(999.dp))
                .clickable(onClick = onPick)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("Pick", fontSize = 11.sp, color = InkMedium, letterSpacing = 0.3.sp)
        }
    }
}

// ── Type toggle ───────────────────────────────────────────────────────────────

@Composable
private fun TypeToggle(selectedType: TransactionType, onTypeChange: (TransactionType) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(WarmSurface)
            .padding(3.dp)
    ) {
        Row {
            listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (type, label) ->
                val active = selectedType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) Ink else Color.Transparent)
                        .clickable { onTypeChange(type) }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        color = if (active) WarmPaper else InkMedium,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Donut chart ───────────────────────────────────────────────────────────────

@Composable
private fun DonutChartSection(
    summaries: List<CategorySummary>,
    totalAmount: Double,
    label: String,
    percentOf: (Double) -> Int
) {
    val strokeWidth = 80f
    val chartSize   = 220.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(chartSize)) {
            val canvasSize = size.minDimension
            val topLeft = Offset(
                (size.width  - canvasSize) / 2f + strokeWidth / 2,
                (size.height - canvasSize) / 2f + strokeWidth / 2
            )
            val arcSize = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)
            var startAngle = -90f

            if (summaries.isEmpty()) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                summaries.forEachIndexed { index, summary ->
                    val sweep = if (totalAmount > 0)
                        (summary.totalAmount / totalAmount * 360f).toFloat() else 0f
                    drawArc(
                        color = sliceColors[index % sliceColors.size],
                        startAngle = startAngle, sweepAngle = sweep - 1f, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )
                    val pct = percentOf(summary.totalAmount)
                    if (sweep > 20f) {
                        val midAngle = Math.toRadians((startAngle + sweep / 2).toDouble())
                        val radius   = canvasSize / 2 - strokeWidth / 2
                        val lx = size.width  / 2 + (radius * cos(midAngle)).toFloat()
                        val ly = size.height / 2 + (radius * sin(midAngle)).toFloat()
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = if (sweep < 30f) 22f else 28f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            drawText("$pct%", lx, ly + 9f, paint)
                        }
                    }
                    startAngle += sweep
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label.uppercase(),
                fontSize = 9.5.sp, letterSpacing = 0.8.sp, color = InkFaint
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("৳", fontSize = 11.sp, color = InkFaint, modifier = Modifier.padding(end = 2.dp))
                Text(
                    "%,.0f".format(totalAmount),
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    color = Ink,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Legend ────────────────────────────────────────────────────────────────────

@Composable
private fun LegendRow(summaries: List<CategorySummary>, percentOf: (Double) -> Int) {
    val chunked = summaries.chunked(2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        chunked.forEachIndexed { rowIndex, pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEachIndexed { colIndex, summary ->
                    val globalIndex = rowIndex * 2 + colIndex
                    val color = sliceColors[globalIndex % sliceColors.size]
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
                        Text(
                            summary.categoryName,
                            fontSize = 11.5.sp,
                            color = InkMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${percentOf(summary.totalAmount)}%",
                            fontSize = 11.sp,
                            color = InkFaint
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ── Category list row ─────────────────────────────────────────────────────────

@Composable
private fun CategoryListRow(
    summary: CategorySummary,
    color: Color,
    percent: Float,
    percentInt: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, RoundedCornerShape(99.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(summary.categoryName, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, color = Ink)
                Text(
                    "${summary.transactionCount} txn${if (summary.transactionCount != 1) "s" else ""} · $percentInt%",
                    fontSize = 11.5.sp,
                    color = InkFaint
                )
            }
            Text(
                "৳ ${"%,.0f".format(summary.totalAmount)}",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = Ink
            )
        }
        Spacer(Modifier.height(8.dp))
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Rule, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentInt / 100f)
                    .height(3.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
        HorizontalDivider(color = Rule, modifier = Modifier.padding(top = 12.dp))
    }
}
