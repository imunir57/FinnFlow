package com.finnflow.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

/** Fixed colour palette for up to 8 category slices (repeats after). */
private val sliceColors = listOf(
    Color(0xFFE24B4A), Color(0xFF378ADD), Color(0xFF1D9E75),
    Color(0xFFEF9F27), Color(0xFF7F77DD), Color(0xFFD4537E),
    Color(0xFF639922), Color(0xFF888780)
)

@Composable
fun StatsScreen(
    onNavigateToCategory: (categoryId: Long, from: LocalDate, to: LocalDate, type: TransactionType) -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Unified control bar ───────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Period chips
                StatsPeriod.entries.forEach { period ->
                    PeriodChip(
                        label = period.label,
                        selected = state.period == period,
                        onClick = { viewModel.onPeriodChange(period) }
                    )
                }

                Spacer(Modifier.weight(1f))

                // Visual separator
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Spacer(Modifier.width(4.dp))

                // Income / Expense toggle
                TypeToggle(
                    selectedType = state.selectedType,
                    onTypeChange = viewModel::onTypeChange
                )
            }
        }

        // Custom date range row (only visible when CUSTOM selected)
        if (state.period == StatsPeriod.CUSTOM) {
            CustomDateRangeRow(
                from = state.from,
                to = state.to,
                onRangeChange = viewModel::onCustomRangeChange
            )
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Donut chart ───────────────────────────────────────────────
            item {
                DonutChartSection(
                    summaries = state.activeSummary,
                    totalAmount = state.totalAmount,
                    percentOf = state::percentOf
                )
            }

            // ── Legend ────────────────────────────────────────────────────
            item {
                LegendRow(summaries = state.activeSummary, percentOf = state::percentOf)
            }

            // ── Category list header ──────────────────────────────────────
            item {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Category", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Amount", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }

            // ── Category rows ─────────────────────────────────────────────
            if (state.activeSummary.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data for selected period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(state.activeSummary.size) { index ->
                    val summary = state.activeSummary[index]
                    val color = sliceColors[index % sliceColors.size]
                    CategoryListRow(
                        summary = summary,
                        color = color,
                        percent = state.percentOf(summary.totalAmount),
                        onClick = {
                            onNavigateToCategory(
                                summary.categoryId,
                                state.from,
                                state.to,
                                state.selectedType
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Period chip ───────────────────────────────────────────────────────────────

private val StatsPeriod.label: String get() = when (this) {
    StatsPeriod.MONTHLY  -> "Month"
    StatsPeriod.ANNUALLY -> "Year"
    StatsPeriod.CUSTOM   -> "Custom"
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurface,
        border = if (!selected) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

// ── Income / Expense toggle ───────────────────────────────────────────────────

@Composable
private fun TypeToggle(
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row {
            listOf(TransactionType.EXPENSE to "Exp", TransactionType.INCOME to "Inc").forEach { (type, label) ->
                val selected = selectedType == type
                Surface(
                    onClick = { onTypeChange(type) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// ── Custom date range ─────────────────────────────────────────────────────────

@Composable
private fun CustomDateRangeRow(
    from: LocalDate,
    to: LocalDate,
    onRangeChange: (LocalDate, LocalDate) -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("From", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { /* TODO: DatePickerDialog */ }) {
            Text(from.format(fmt), style = MaterialTheme.typography.labelSmall)
        }
        Text("to", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { /* TODO: DatePickerDialog */ }) {
            Text(to.format(fmt), style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ── Donut chart ───────────────────────────────────────────────────────────────

@Composable
private fun DonutChartSection(
    summaries: List<CategorySummary>,
    totalAmount: Double,
    percentOf: (Double) -> Int
) {
    val strokeWidth = 120f
    val chartSize = 220.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(chartSize)) {
            val canvasSize = size.minDimension
            val topLeft = Offset(
                (size.width - canvasSize) / 2f + strokeWidth / 2,
                (size.height - canvasSize) / 2f + strokeWidth / 2
            )
            val arcSize = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)
            var startAngle = -90f

            if (summaries.isEmpty()) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                summaries.forEachIndexed { index, summary ->
                    val sweep = if (totalAmount > 0)
                        (summary.totalAmount / totalAmount * 360f).toFloat()
                    else 0f
                    val color = sliceColors[index % sliceColors.size]

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep - 1f, // 1° gap between slices
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // % label inside slice — only draw if slice is wide enough
                    val pct = percentOf(summary.totalAmount)
                    if (sweep > 25f) {
                        val midAngle = Math.toRadians((startAngle + sweep / 2).toDouble())
                        val radius = (canvasSize / 2 - strokeWidth / 2)
                        val labelX = size.width / 2 + (radius * cos(midAngle)).toFloat()
                        val labelY = size.height / 2 + (radius * sin(midAngle)).toFloat()
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                this.color = android.graphics.Color.WHITE
                                textSize = 28f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            drawText("$pct%", labelX, labelY + 9f, paint)
                        }
                    }
                    startAngle += sweep
                }
            }
        }

        // Centre label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "৳ ${"%,.0f".format(totalAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Legend ────────────────────────────────────────────────────────────────────

@Composable
private fun LegendRow(
    summaries: List<CategorySummary>,
    percentOf: (Double) -> Int
) {
    val chunked = summaries.chunked(2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        chunked.forEachIndexed { rowIndex, pair ->
            Row(modifier = Modifier.fillMaxWidth()) {
                pair.forEachIndexed { colIndex, summary ->
                    val globalIndex = rowIndex * 2 + colIndex
                    val color = sliceColors[globalIndex % sliceColors.size]
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "${summary.categoryName}  ${percentOf(summary.totalAmount)}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    percent: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colour bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(color, RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(summary.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
            Text(
                "${summary.transactionCount} transaction${if (summary.transactionCount != 1) "s" else ""} · $percent%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "৳ ${"%,.0f".format(summary.totalAmount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.width(4.dp))
        Text("›", style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
