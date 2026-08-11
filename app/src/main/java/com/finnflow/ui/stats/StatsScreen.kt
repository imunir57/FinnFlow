package com.finnflow.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.TransactionType
import com.finnflow.ui.AmountStyle
import com.finnflow.ui.LocalCurrencyFormat
import com.finnflow.ui.components.PeriodSwipeBox
import com.finnflow.ui.theme.FinnFlowTheme
import com.finnflow.ui.theme.rememberCategoryColor
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateToCategory: (categoryId: Long, from: LocalDate, to: LocalDate, type: TransactionType) -> Unit,
    onNavigateToInsights: (YearMonth) -> Unit = {},
    onNavigateToCompare: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker   by remember { mutableStateOf(false) }

    // One picker per end of a CUSTOM range. They are independent — picking a new start must not
    // drag the user through the end picker as well, since either end is edited on its own.
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
                }) { Text("OK") }
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Title bar ─────────────────────────────────────────────────────
        // Padding replaces the height the removed overflow IconButton used to contribute
        // (48.dp touch target), so the title keeps its original vertical rhythm.
        Text(
            "Statistics",
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp)
        )

        // ── Range tabs (underline style) ──────────────────────────────────
        RangeTabsRow(
            period = state.period,
            onPeriodChange = viewModel::onPeriodChange,
            onNavigateToCompare = onNavigateToCompare
        )

        // Everything below the tabs belongs to the period on screen, so it travels as one panel
        // under the swipe. The title and tabs stay put — they do not change with the range.
        // A custom range has nothing to step to, so the gesture is off there.
        PeriodSwipeBox(
            onNext = viewModel::nextPeriod,
            onPrevious = viewModel::previousPeriod,
            enabled = state.period != StatsPeriod.CUSTOM,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Range display: < label >   /   [from] – [to] ──────────
                RangeDisplayRow(
                    period     = state.period,
                    from       = state.from,
                    to         = state.to,
                    onPrev     = viewModel::previousPeriod,
                    onNext     = viewModel::nextPeriod,
                    onPickFrom = { showFromPicker = true },
                    onPickTo   = { showToPicker   = true }
                )

                // ── Income / Expense toggle (centered) ────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TypeToggle(
                        selectedType = state.selectedType,
                        onTypeChange = viewModel::onTypeChange
                    )
                }

                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    return@Column
                }

                StatsContentList(
                    state = state,
                    onNavigateToCategory = onNavigateToCategory,
                    onNavigateToInsights = onNavigateToInsights
                )
            }
        }
    }
}

@Composable
private fun StatsContentList(
    state: StatsUiState,
    onNavigateToCategory: (categoryId: Long, from: LocalDate, to: LocalDate, type: TransactionType) -> Unit,
    onNavigateToInsights: (YearMonth) -> Unit
) {
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
        // Insights is scoped to a single calendar month and takes no range argument, so
        // offering it under Year or Custom would open numbers unrelated to what is on screen.
        if (state.period == StatsPeriod.MONTHLY && state.selectedType == TransactionType.EXPENSE) {
            // Hands over the month on screen, so Insights opens on the same figures the
            // user was just looking at rather than snapping back to today.
            item { InsightsEntryCard(onClick = { onNavigateToInsights(YearMonth.from(state.from)) }) }
        }
        item {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Category",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    "Amount",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }
        if (state.activeSummary.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data for selected period",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.activeSummary.size) { index ->
                val summary = state.activeSummary[index]
                CategoryListRow(
                    summary = summary,
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

// ── Range tabs (underline indicator) ─────────────────────────────────────────

private val StatsPeriod.label: String get() = when (this) {
    StatsPeriod.MONTHLY  -> "Month"
    StatsPeriod.ANNUALLY -> "Year"
    StatsPeriod.CUSTOM   -> "Custom"
}

@Composable
private fun RangeTabsRow(
    period: StatsPeriod,
    onPeriodChange: (StatsPeriod) -> Unit,
    onNavigateToCompare: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            val activeColor = MaterialTheme.colorScheme.onSurface
            val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
            StatsPeriod.entries.forEach { p ->
                val active = period == p
                Box(
                    modifier = Modifier
                        .clickable { onPeriodChange(p) }
                        .padding(top = 14.dp, bottom = 12.dp)
                        .drawBehind {
                            if (active) drawLine(
                                color = activeColor,
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
                        color = if (active) activeColor else inactiveColor
                    )
                }
            }
            // Sits in the tab row per the design, but it is a destination rather than a
            // period — it never takes the underline, and tapping it leaves the screen.
            Box(
                modifier = Modifier
                    .clickable(onClick = onNavigateToCompare)
                    .padding(top = 14.dp, bottom = 12.dp)
            ) {
                Text(
                    "Compare",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = inactiveColor
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ── Range display row ─────────────────────────────────────────────────────────

private val rangeEndFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
private fun RangeDisplayRow(
    period: StatsPeriod,
    from: LocalDate,
    to: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 18.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (period == StatsPeriod.CUSTOM) {
            // Stepping through arbitrary ranges is meaningless, so the chevrons give way to the
            // two end buttons — they are both the affordance and the range label.
            Spacer(Modifier.width(8.dp))
            RangeEndButton(date = from, onClick = onPickFrom, clickLabel = "Change start date")
            Text(
                " – ",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RangeEndButton(date = to, onClick = onPickTo, clickLabel = "Change end date")
        } else {
            val label = when (period) {
                StatsPeriod.ANNUALLY -> from.year.toString()
                else -> YearMonth.from(from).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            }
            IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, "Previous",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, "Next",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RangeEndButton(date: LocalDate, onClick: () -> Unit, clickLabel: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
            .clickable(onClickLabel = clickLabel, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            date.format(rangeEndFormatter),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Type toggle ───────────────────────────────────────────────────────────────

@Composable
private fun TypeToggle(selectedType: TransactionType, onTypeChange: (TransactionType) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp)
    ) {
        Row {
            listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (type, label) ->
                val active = selectedType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { onTypeChange(type) }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        color = if (active) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
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
    val ffColors    = FinnFlowTheme.colors
    val emptyTrack  = MaterialTheme.colorScheme.outlineVariant
    // Resolved outside the Canvas: the draw lambda is not a composable scope. Each slice uses its
    // own category's stored colour, so the palette can never wrap and repeat.
    val sliceColors = summaries.map { rememberCategoryColor(it.colorHex) }

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
                    color = emptyTrack,
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                summaries.forEachIndexed { index, summary ->
                    val sliceColor = sliceColors[index]
                    val sweep = if (totalAmount > 0)
                        (summary.totalAmount / totalAmount * 360f).toFloat() else 0f
                    drawArc(
                        color = sliceColor,
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
                                // Dark-mode slices are lifted to be readable on near-black, so a
                                // fixed white label would disappear on them.
                                color = ffColors.onChartSlice(sliceColor).toArgb()
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
                fontSize = 9.5.sp,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            val money = LocalCurrencyFormat.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    money.symbol,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 2.dp)
                )
                Text(
                    money.amount(totalAmount, AmountStyle.Whole),
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
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
        chunked.forEach { pair ->
            // Gap between the two legend columns — without it the left column's percentage
            // collides with the right column's swatch and clips its label.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                pair.forEach { summary ->
                    val color = rememberCategoryColor(summary.colorHex)
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
                        Text(
                            summary.categoryName,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${percentOf(summary.totalAmount)}%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ── Insights entry card ───────────────────────────────────────────────────────

@Composable
private fun InsightsEntryCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FinnFlowTheme.colors.income.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = FinnFlowTheme.colors.income,
                modifier = Modifier.size(17.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "View insights",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Trends, highlights & spending patterns",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Category list row ─────────────────────────────────────────────────────────

@Composable
private fun CategoryListRow(
    summary: CategorySummary,
    percentInt: Int,
    onClick: () -> Unit
) {
    val color = rememberCategoryColor(summary.colorHex)
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
                Text(
                    summary.categoryName,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${summary.transactionCount} txn${if (summary.transactionCount != 1) "s" else ""} · $percentInt%",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                LocalCurrencyFormat.current.format(summary.totalAmount, AmountStyle.Whole),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
