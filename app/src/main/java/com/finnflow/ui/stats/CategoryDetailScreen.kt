package com.finnflow.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.model.SubCategorySummary
import com.finnflow.data.model.Transaction
import com.finnflow.ui.AmountStyle
import com.finnflow.ui.LocalCurrencyFormat
import com.finnflow.ui.theme.FinnFlowTheme
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryName: String,
    onNavigateBack: () -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val resolvedName by viewModel.categoryName.collectAsState()
    val displayName = resolvedName.ifBlank { categoryName }
    val shades = FinnFlowTheme.colors.subCategoryShades

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(displayName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Summary header card ───────────────────────────────────────
            item {
                CategoryHeaderCard(state = state)
            }

            // ── Donut chart ───────────────────────────────────────────────
            item {
                CategoryDonutChart(state = state)
            }

            // ── Legend ────────────────────────────────────────────────────
            item {
                SubCategoryLegend(state = state)
            }

            // ── List header ───────────────────────────────────────────────
            item {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subcategory", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Amount", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }

            // ── Subcategory rows with inline expansion ────────────────────
            if (state.summaries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No transactions in this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.summaries.size) { index ->
                    val summary = state.summaries[index]
                    val color = shades[index % shades.size]
                    val isExpanded = state.isExpanded(summary.subCategoryId)
                    val transactions = state.transactionsBySubCategory[summary.subCategoryId]

                    SubCategoryRow(
                        summary = summary,
                        color = color,
                        percent = state.percentOf(summary.totalAmount),
                        isExpanded = isExpanded,
                        onToggle = { viewModel.toggleSubCategory(summary.subCategoryId) }
                    )

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            if (transactions == null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                }
                            } else {
                                transactions.forEach { tx ->
                                    InlineTransactionRow(transaction = tx, accentColor = color)
                                }
                                if (transactions.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No transactions",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Header card ───────────────────────────────────────────────────────────────

@Composable
private fun CategoryHeaderCard(state: CategoryDetailUiState) {
    val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "${state.from.format(fmt)} – ${state.to.format(fmt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${state.summaries.sumOf { it.transactionCount }} transactions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                LocalCurrencyFormat.current.format(state.totalAmount, AmountStyle.Whole),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Donut chart (category shades) ─────────────────────────────────────────────

@Composable
private fun CategoryDonutChart(state: CategoryDetailUiState) {
    val strokeWidth = 48f
    val chartSize = 200.dp
    val ffColors = FinnFlowTheme.colors
    val shades = ffColors.subCategoryShades
    val emptyTrack = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
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

            if (state.summaries.isEmpty()) {
                drawArc(
                    color = emptyTrack,
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(width = strokeWidth)
                )
            } else {
                state.summaries.forEachIndexed { index, summary ->
                    val sweep = if (state.totalAmount > 0)
                        (summary.totalAmount / state.totalAmount * 360f).toFloat()
                    else 0f
                    val color = shades[index % shades.size]

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep - 1f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    if (sweep > 25f) {
                        val pct = state.percentOf(summary.totalAmount)
                        val midAngle = Math.toRadians((startAngle + sweep / 2).toDouble())
                        val radius = (canvasSize / 2 - strokeWidth / 2)
                        val labelX = size.width / 2 + (radius * cos(midAngle)).toFloat()
                        val labelY = size.height / 2 + (radius * sin(midAngle)).toFloat()
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                // The ramp runs light in dark mode, so a fixed white label
                                // would be unreadable on the pale end of it.
                                this.color = ffColors.onChartSlice(color).toArgb()
                                textSize = 26f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isFakeBoldText = true
                            }
                            drawText("$pct%", labelX, labelY + 8f, paint)
                        }
                    }
                    startAngle += sweep
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(LocalCurrencyFormat.current.format(state.totalAmount, AmountStyle.Whole),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
        }
    }
}

// ── Legend ────────────────────────────────────────────────────────────────────

@Composable
private fun SubCategoryLegend(state: CategoryDetailUiState) {
    val shades = FinnFlowTheme.colors.subCategoryShades
    val chunked = state.summaries.chunked(2)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        chunked.forEachIndexed { rowIndex, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                pair.forEachIndexed { colIndex, summary ->
                    val globalIndex = rowIndex * 2 + colIndex
                    val color = shades[globalIndex % shades.size]
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
                        Text(
                            "${summary.subCategoryName}  ${state.percentOf(summary.totalAmount)}%",
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

// ── Subcategory row ───────────────────────────────────────────────────────────

@Composable
private fun SubCategoryRow(
    summary: SubCategorySummary,
    color: Color,
    percent: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .background(
                if (isExpanded) FinnFlowTheme.colors.selectedRow else Color.Transparent
            )
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(38.dp)
                .background(color, RoundedCornerShape(2.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                summary.subCategoryName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${summary.transactionCount} transaction${if (summary.transactionCount != 1) "s" else ""} · $percent%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            LocalCurrencyFormat.current.format(summary.totalAmount, AmountStyle.Whole),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.width(4.dp))

        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                          else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Inline transaction row ────────────────────────────────────────────────────

@Composable
private fun InlineTransactionRow(transaction: Transaction, accentColor: Color) {
    val fmt = DateTimeFormatter.ofPattern("MMM d")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small accent dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(accentColor.copy(alpha = 0.6f), RoundedCornerShape(50))
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.note.ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                transaction.date.format(fmt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            LocalCurrencyFormat.current.format(transaction.amount, AmountStyle.Whole),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}
