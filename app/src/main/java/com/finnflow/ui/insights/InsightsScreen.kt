package com.finnflow.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.ui.theme.*
import kotlin.math.roundToInt

private fun fmtAmount(n: Double): String = "%,.0f".format(n)

private val WEEKDAY_ABBREV = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val WEEKDAY_FULL = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmPaper)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = InkMedium)
            }
            Text(
                "Insights",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = Ink,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, contentDescription = null, tint = InkMedium)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 18.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = viewModel::previousMonth, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.ArrowBack, contentDescription = "Previous month",
                    tint = InkMedium, modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(state.monthLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = viewModel::nextMonth, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.ArrowForward, contentDescription = "Next month",
                    tint = InkMedium, modifier = Modifier.size(16.dp)
                )
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { HeroCard(state) }
            item { IncomeExpenseBarCard(state) }
            item { DailyTrendCard(state) }
            item { HighlightsCard(state) }
            item { DayOfWeekCard(state) }
            item { FooterText() }
        }
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(state: InsightsUiState) {
    val rate = state.savingsRate
    val net = state.net
    val monthName = state.monthLabel.substringBefore(" ")

    val heroGradient = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFF1A2820),
            0.4f to Color(0xFF1E1916),
            1.0f to Color(0xFF241410)
        )
    )
    val rateColor = if (rate >= 0) Color(0xFF7EC8A0) else Color(0xFFE8A880)
    val message = when {
        rate >= 30 -> "Strong month — you held back nearly a third of what came in."
        rate >= 10 -> "Net ৳${fmtAmount(net)} saved so far. On track for a positive close."
        rate >= 0  -> "Thin margin — most of what you earned went out again."
        else       -> "Overspent by ৳${fmtAmount(-net)} — review big-ticket items below."
    }

    Box(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(heroGradient)
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                "Savings rate · $monthName",
                fontSize = 10.5.sp,
                color = WarmPaper.copy(alpha = 0.65f),
                letterSpacing = 1.2.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${if (rate < 0) "−" else ""}${Math.abs(rate).roundToInt()}",
                    fontFamily = FontFamily.Serif,
                    fontSize = 56.sp,
                    lineHeight = 56.sp,
                    letterSpacing = (-1.2).sp,
                    color = rateColor
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "%",
                    fontFamily = FontFamily.Serif,
                    fontSize = 28.sp,
                    color = WarmPaper.copy(alpha = 0.70f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                fontSize = 12.5.sp,
                color = WarmPaper.copy(alpha = 0.70f),
                lineHeight = 18.sp
            )
        }
    }
}

// ── Shared card chrome ────────────────────────────────────────────────────────

@Composable
private fun InsightCard(
    title: String,
    caption: String = "",
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Rule, RoundedCornerShape(18.dp))
            .background(WarmCard)
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                letterSpacing = 0.1.sp
            )
            Spacer(Modifier.weight(1f))
            if (caption.isNotEmpty()) {
                Text(
                    caption,
                    fontSize = 11.5.sp,
                    color = InkFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .widthIn(max = 160.dp)
                        .padding(start = 12.dp)
                )
            }
        }
        content()
    }
}

// ── Income vs Expense bar card ────────────────────────────────────────────────

@Composable
private fun IncomeExpenseBarCard(state: InsightsUiState) {
    val max = maxOf(state.income, state.expense, 1.0)
    val net = state.net
    val savingsRate = state.savingsRate
    val caption = if (savingsRate > 0)
        "Saved ${savingsRate.roundToInt()}% of income"
    else
        "Overspent — net ৳${fmtAmount(-net)}"

    InsightCard(title = "Income vs Expense", caption = caption) {
        Spacer(Modifier.height(10.dp))
        BarLine(label = "Income", value = state.income, max = max, color = IncomeGreen)
        Spacer(Modifier.height(8.dp))
        BarLine(label = "Expense", value = state.expense, max = max, color = ExpenseClay)
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Rule)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("NET BALANCE", fontSize = 11.sp, color = InkFaint, letterSpacing = 0.6.sp)
            val netColor = if (net >= 0) IncomeGreen else ExpenseClay
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (net >= 0) "+" else "−",
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    color = netColor,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    "৳",
                    fontSize = 13.sp,
                    color = netColor.copy(alpha = 0.55f),
                    modifier = Modifier.padding(horizontal = 1.dp)
                )
                Text(
                    fmtAmount(Math.abs(net)),
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    color = netColor,
                    letterSpacing = (-0.3).sp
                )
            }
        }
    }
}

@Composable
private fun BarLine(label: String, value: Double, max: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = InkMedium)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("৳", fontSize = 11.sp, color = InkFaint, modifier = Modifier.padding(end = 1.dp))
            Text(fmtAmount(value), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink)
        }
    }
    Spacer(Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Rule)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((value / max).toFloat().coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
    }
}

// ── Daily spend trend card ────────────────────────────────────────────────────

@Composable
private fun DailyTrendCard(state: InsightsUiState) {
    if (state.dailyTotals.isEmpty()) return
    val monthAbbrev = state.monthLabel.substringBefore(" ").take(3)
    val peakDay = state.peakDayIndex + 1
    val peakAmount = state.dailyTotals.getOrElse(state.peakDayIndex) { 0.0 }
    val todayAmount = state.dailyTotals.getOrElse(state.todayDayIndex) { 0.0 }
    val quietestIdx = state.quietestDayIndex

    InsightCard(
        title = "Daily spend trend",
        caption = "Avg ৳${fmtAmount(state.avgDailySpend)} / day · ${state.daysRecorded} days in"
    ) {
        Spacer(Modifier.height(10.dp))
        SparklineCanvas(
            dailyTotals = state.dailyTotals,
            todayIndex = state.todayDayIndex,
            peakIndex = state.peakDayIndex,
            avgSpend = state.avgDailySpend,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val mid = (state.daysInMonth / 2).coerceAtLeast(1)
            val todayDay = state.todayDayIndex + 1
            listOf(
                "$monthAbbrev 1",
                "$monthAbbrev $mid",
                "$monthAbbrev $todayDay",
                "$monthAbbrev ${state.daysInMonth}"
            ).forEachIndexed { i, label ->
                Text(
                    label,
                    fontSize = 9.5.sp,
                    color = if (i == 3) InkFaint.copy(alpha = 0.5f) else InkFaint
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Rule)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MiniStat(
                label = "Peak day",
                value = "$monthAbbrev $peakDay",
                sub = "৳${fmtAmount(peakAmount)}",
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(1.dp)
                    .height(40.dp)
                    .background(Rule)
            )
            MiniStat(
                label = "Quietest",
                value = if (quietestIdx >= 0) "$monthAbbrev ${quietestIdx + 1}" else "—",
                sub = if (quietestIdx >= 0) "৳${fmtAmount(state.dailyTotals[quietestIdx])}" else "",
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(1.dp)
                    .height(40.dp)
                    .background(Rule)
            )
            MiniStat(
                label = "Today",
                value = "৳${fmtAmount(todayAmount)}",
                sub = if (todayAmount > state.avgDailySpend) "above avg" else "below avg",
                valueColor = if (todayAmount > state.avgDailySpend) ExpenseClay else IncomeGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SparklineCanvas(
    dailyTotals: List<Double>,
    todayIndex: Int,
    peakIndex: Int,
    avgSpend: Double,
    modifier: Modifier = Modifier
) {
    val clay = ExpenseClay
    val inkColor = Ink
    val faintColor = InkFaint
    val paperColor = WarmPaper

    Canvas(modifier = modifier) {
        val visible = dailyTotals.take(todayIndex + 1)
        if (visible.size < 2) return@Canvas

        val w = size.width
        val h = size.height
        val vPad = 6.dp.toPx()
        val max = visible.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val n = dailyTotals.size.coerceAtLeast(2)
        val stepX = w / (n - 1).toFloat()

        fun xOf(i: Int) = i * stepX
        fun yOf(v: Double) = (h - (v / max).toFloat() * (h - vPad) - vPad / 2).coerceIn(0f, h)

        val pts = visible.mapIndexed { i, v -> Offset(xOf(i), yOf(v)) }

        // Dotted average line
        val avgY = yOf(avgSpend)
        drawLine(
            color = faintColor.copy(alpha = 0.6f),
            start = Offset(0f, avgY),
            end = Offset(w, avgY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        )

        // Gradient fill
        val fillPath = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            lineTo(pts.last().x, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(clay.copy(alpha = 0.22f), clay.copy(alpha = 0f)),
                startY = 0f, endY = h
            )
        )

        // Sparkline
        val linePath = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
        }
        drawPath(
            linePath,
            color = clay,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Peak dot (hollow)
        val peakPt = pts.getOrElse(peakIndex.coerceIn(0, pts.lastIndex)) { pts.last() }
        drawCircle(color = paperColor, radius = 3.5.dp.toPx(), center = peakPt)
        drawCircle(
            color = clay, radius = 3.5.dp.toPx(), center = peakPt,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Today dot (solid)
        drawCircle(color = inkColor, radius = 3.dp.toPx(), center = pts.last())
    }
}

// ── Highlights card ───────────────────────────────────────────────────────────

@Composable
private fun HighlightsCard(state: InsightsUiState) {
    val dateFmt = java.time.format.DateTimeFormatter.ofPattern("MMM d")

    InsightCard(title = "Highlights", caption = "What stood out this month") {
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.biggestExpenseAmount > 0) {
                HighlightRow(
                    label = "Biggest single expense",
                    primary = "৳${fmtAmount(state.biggestExpenseAmount)}",
                    sub = buildString {
                        if (state.biggestExpenseCategoryName.isNotEmpty()) append(state.biggestExpenseCategoryName)
                        state.biggestExpenseDate?.let { if (isNotEmpty()) append(" · "); append(it.format(dateFmt)) }
                    }
                )
            }
            if (state.mostFrequentCategoryName.isNotEmpty()) {
                HighlightRow(
                    label = "Most frequent",
                    primary = state.mostFrequentCategoryName,
                    sub = "${state.mostFrequentCategoryCount} transaction${if (state.mostFrequentCategoryCount != 1) "s" else ""}"
                )
            }
            if (state.biggestMoMCategoryName.isNotEmpty() && state.biggestMoMDelta > 0) {
                HighlightRow(
                    label = "Biggest jump vs last month",
                    primary = state.biggestMoMCategoryName,
                    sub = "+৳${fmtAmount(state.biggestMoMDelta)} vs previous month",
                    arrowUp = true
                )
            }
        }
    }
}

@Composable
private fun HighlightRow(
    label: String,
    primary: String,
    sub: String,
    arrowUp: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Rule)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 10.5.sp,
                color = InkFaint,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(1.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    primary,
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp,
                    color = Ink
                )
                if (arrowUp) {
                    Text(
                        "▲",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ExpenseClay,
                        letterSpacing = 0.4.sp
                    )
                }
            }
            Text(
                sub,
                fontSize = 11.5.sp,
                color = InkFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Day-of-week heatmap card ──────────────────────────────────────────────────

@Composable
private fun DayOfWeekCard(state: InsightsUiState) {
    val totals = state.weekdayTotals
    val max = totals.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val peakIdx = totals.indices.maxByOrNull { totals[it] } ?: 0

    InsightCard(
        title = "Spending by day of week",
        caption = "Heaviest on ${WEEKDAY_FULL[peakIdx]}"
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            totals.forEachIndexed { i, v ->
                val intensity = (v / max).toFloat()
                val isPeak = i == peakIdx
                val bgColor = if (v == 0.0) Rule
                else lerp(Color(0xFFF5E8E4), ExpenseClay, intensity)
                val borderColor = if (isPeak) ExpenseClay else Rule
                val borderWidth = if (isPeak) 1.5.dp else 1.dp

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        if (v > 0) "${(v / 1000).roundToInt()}k" else "—",
                        fontSize = 9.5.sp,
                        color = if (isPeak) ExpenseClay else InkFaint,
                        fontWeight = if (isPeak) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
                    )
                    Text(
                        WEEKDAY_ABBREV[i],
                        fontSize = 10.sp,
                        color = if (isPeak) Ink else InkMedium,
                        fontWeight = if (isPeak) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

// ── Shared sub-composables ────────────────────────────────────────────────────

@Composable
private fun MiniStat(
    label: String,
    value: String,
    sub: String = "",
    valueColor: Color = Ink,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 9.5.sp, color = InkFaint, letterSpacing = 0.8.sp)
        Text(
            value,
            fontFamily = FontFamily.Serif,
            fontSize = 15.sp,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 1.dp)
        )
        if (sub.isNotEmpty()) {
            Text(
                sub,
                fontSize = 10.5.sp,
                color = InkFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FooterText() {
    Text(
        "Insights are computed from your local transactions only.\nNothing leaves your device.",
        fontSize = 13.sp,
        color = InkFaint.copy(alpha = 0.7f),
        fontFamily = FontFamily.Serif,
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        textAlign = TextAlign.Center,
        lineHeight = 19.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp)
    )
}
