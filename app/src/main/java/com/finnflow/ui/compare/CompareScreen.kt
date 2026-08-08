package com.finnflow.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.model.TransactionType
import com.finnflow.ui.AmountStyle
import com.finnflow.ui.CurrencyFormat
import com.finnflow.ui.LocalCurrencyFormat
import com.finnflow.ui.components.categoryIconFor
import com.finnflow.ui.theme.FinnFlowTheme
import com.finnflow.ui.theme.onChartSlice
import com.finnflow.ui.theme.rememberCategoryColor

/** Width reserved for the period label gutter down the left of every chart. */
private val AxisGutter = 58.dp

/** A bar shorter than this can't hold its own amount label, so the label sits outside it. */
private val MinWidthForInsideLabel = 88.dp

@Composable
fun CompareScreen(
    onNavigateBack: () -> Unit,
    viewModel: CompareViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val categories by viewModel.pickerCategories.collectAsState()
    val subCategories by viewModel.subCategoriesByCategory.collectAsState()

    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showItemPicker by remember { mutableStateOf(false) }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            initial = state.periods.lastOrNull() ?: ComparePeriod.recent(ComparePeriodMode.MONTH, 1).first(),
            disabledKeys = state.periods.map { it.key }.toSet(),
            onConfirm = { viewModel.addPeriod(it); showMonthPicker = false },
            onDismiss = { showMonthPicker = false }
        )
    }
    if (showYearPicker) {
        YearPickerDialog(
            initial = state.periods.lastOrNull()?.year ?: java.time.LocalDate.now().year,
            disabledKeys = state.periods.map { it.key }.toSet(),
            onConfirm = { viewModel.addPeriod(it); showYearPicker = false },
            onDismiss = { showYearPicker = false }
        )
    }
    if (showItemPicker) {
        ItemPickerSheet(
            categories = categories,
            subCategoriesByCategory = subCategories,
            isSelected = viewModel::isSelected,
            isWholeSelected = viewModel::isWholeCategorySelected,
            selectedSubCount = viewModel::selectedSubCount,
            remainingSlots = MAX_COMPARE_ITEMS - state.items.size,
            onToggle = viewModel::toggleItem,
            onDismiss = { showItemPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompareTopBar(onNavigateBack = onNavigateBack)

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            CompareTypeToggle(selected = state.type, onChange = viewModel::setType)
        }

        PeriodSection(
            mode = state.mode,
            periods = state.periods,
            canAdd = state.canAddPeriod,
            onModeChange = viewModel::setMode,
            onRemove = viewModel::removePeriod,
            onAdd = {
                when (state.mode) {
                    ComparePeriodMode.MONTH -> showMonthPicker = true
                    ComparePeriodMode.YEAR -> showYearPicker = true
                }
            }
        )

        ItemSection(
            items = state.items,
            canAdd = state.canAddItem,
            onRemove = viewModel::removeItem,
            onAdd = { showItemPicker = true }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when {
            state.isLoading -> CenteredBox { CircularProgressIndicator() }

            state.items.isEmpty() -> CompareMessage(
                title = "Nothing to compare yet",
                body = "Add up to $MAX_COMPARE_ITEMS categories — or drill into one to compare " +
                    "its subcategories — and see them side by side across the periods above."
            )

            state.items.size < 2 -> CompareMessage(
                title = "Add one more",
                body = "Comparison needs at least two. \"${state.items.first().name}\" is picked " +
                    "so far."
            )

            state.allSeriesEmpty -> CompareMessage(
                title = "No activity in these periods",
                body = "Nothing was recorded for the selected " +
                    (if (state.type == TransactionType.EXPENSE) "expenses" else "income") +
                    " in " + state.periods.joinToString(", ") { it.label } + "."
            )

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.series, key = { it.item.key }) { series ->
                    SeriesChart(series = series, periods = state.periods, type = state.type)
                }
                item { Spacer(Modifier.height(90.dp)) }
            }
        }
    }
}

// ── Chrome ────────────────────────────────────────────────────────────────────

@Composable
private fun CompareTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 18.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "Compare",
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CompareTypeToggle(selected: TransactionType, onChange: (TransactionType) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp)
    ) {
        Row {
            listOf(
                TransactionType.EXPENSE to "Expense",
                TransactionType.INCOME to "Income"
            ).forEach { (type, label) ->
                val active = selected == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { onChange(type) }
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

// ── Selection sections ────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodSection(
    mode: ComparePeriodMode,
    periods: List<ComparePeriod>,
    canAdd: Boolean,
    onModeChange: (ComparePeriodMode) -> Unit,
    onRemove: (ComparePeriod) -> Unit,
    onAdd: () -> Unit
) {
    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ComparePeriodMode.entries.forEach { candidate ->
                val active = candidate == mode
                Text(
                    if (candidate == ComparePeriodMode.MONTH) "By month" else "By year",
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onModeChange(candidate) }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
    // One line that scrolls sideways rather than wrapping: five chips plus an add button can
    // reflow to three rows and shove the chart off screen just from picking a period.
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Add sits first so it stays put: trailing it would push it further right with every
        // chip added, and off screen entirely once the row starts scrolling.
        item(key = "add-period") {
            AddButton(
                enabled = canAdd,
                contentDescription = if (mode == ComparePeriodMode.MONTH) "Add a month"
                                     else "Add a year",
                onClick = onAdd
            )
        }
        items(periods, key = { it.key }) { period ->
            RemovableChip(label = period.label, onRemove = { onRemove(period) })
        }
    }
}

@Composable
private fun ItemSection(
    items: List<CompareItem>,
    canAdd: Boolean,
    onRemove: (CompareItem) -> Unit,
    onAdd: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item(key = "add-item") {
            AddButton(
                enabled = canAdd,
                contentDescription = "Add a category to compare",
                onClick = onAdd
            )
        }
        items(items, key = { it.key }) { item ->
            val swatch = rememberCategoryColor(item.colorHex)
            RemovableChip(
                label = item.name,
                onRemove = { onRemove(item) },
                leading = {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(swatch)
                    )
                }
            )
        }
    }
}

/**
 * Bare "+" for adding a period or a category.
 *
 * Disabled rather than hidden at the cap: a control that vanishes reads as a glitch, whereas a
 * greyed one says the limit was reached. Sizes match the chips beside it so the row stays even.
 */
@Composable
private fun AddButton(
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    val tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
               else FinnFlowTheme.colors.disabledText
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(
                1.dp,
                if (enabled) MaterialTheme.colorScheme.outline
                else FinnFlowTheme.colors.disabledContainer,
                RoundedCornerShape(999.dp)
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ── Chart ─────────────────────────────────────────────────────────────────────

/**
 * One item's bars, one per period.
 *
 * Bars are scaled against this series' own maximum rather than a figure shared across every
 * series: the point is the shape of one category over time, and a shared scale would flatten
 * a small category to invisibility next to a large one.
 */
@Composable
private fun SeriesChart(
    series: CompareSeries,
    periods: List<ComparePeriod>,
    type: TransactionType
) {
    val money = LocalCurrencyFormat.current
    val barColor = rememberCategoryColor(series.item.colorHex)
    val max = series.max

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        SeriesHeader(
            item = series.item,
            barColor = barColor,
            series = series,
            periods = periods,
            type = type
        )

        periods.forEachIndexed { index, period ->
            BarRow(
                label = period.label,
                amount = series.amounts.getOrElse(index) { 0.0 },
                max = max,
                color = barColor,
                money = money
            )
            if (index != periods.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Icon, name and what the row covers, with the change badge trailing.
 *
 * The breadcrumb is what separates "Food & Dining as a whole" from "Haircut, within Personal
 * Care" — without it two rows can look like peers when one is nested inside the other.
 */
@Composable
private fun SeriesHeader(
    item: CompareItem,
    barColor: Color,
    series: CompareSeries,
    periods: List<ComparePeriod>,
    type: TransactionType
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(barColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                categoryIconFor(item.iconName),
                contentDescription = null,
                tint = barColor,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.breadcrumb,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        ChangeBadge(series = series, periods = periods, type = type)
    }
}

/**
 * The first-to-last change. Shown for any number of periods, but explicitly labelled with its
 * two endpoints whenever there are more than two — a bare percentage across three or more
 * points reads as a trend it does not actually describe.
 */
@Composable
private fun ChangeBadge(
    series: CompareSeries,
    periods: List<ComparePeriod>,
    type: TransactionType
) {
    val change = series.endpointChangePercent ?: return
    val rising = change >= 0
    // Direction alone doesn't say good or bad: spending more is the clay colour, but earning
    // more is the green one.
    val favourable = if (type == TransactionType.EXPENSE) !rising else rising
    val color = if (favourable) FinnFlowTheme.colors.income else FinnFlowTheme.colors.expense

    Column(horizontalAlignment = Alignment.End) {
        if (periods.size > 2) {
            Text(
                "${periods.first().label} → ${periods.last().label}",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            (if (rising) "▲ " else "▼ ") + "${kotlin.math.abs(change)}%",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun BarRow(
    label: String,
    amount: Double,
    max: Double,
    color: Color,
    money: CurrencyFormat
) {
    val fraction = if (max > 0.0) (amount / max).toFloat().coerceIn(0f, 1f) else 0f
    var barWidth by remember { mutableStateOf<Dp?>(null) }
    val density = LocalDensity.current
    // Null until the bar has been measured. Both branches wait for that rather than guessing,
    // otherwise the first frame draws the label in the wrong place and visibly jumps.
    val labelFitsInside = barWidth != null && barWidth!! >= MinWidthForInsideLabel
    val amountText = money.format(amount, AmountStyle.Whole, spaced = false)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(AxisGutter)
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                    .height(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .onSizeChanged { barWidth = with(density) { it.width.toDp() } },
                contentAlignment = Alignment.CenterEnd
            ) {
                // Keep the figure inside the bar where it fits; a bar too short to hold it
                // gets the label just past its end instead of an overflowing, clipped one.
                if (labelFitsInside) {
                    Text(
                        amountText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onChartSlice(color),
                        maxLines = 1,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            val measured = barWidth
            if (measured != null && !labelFitsInside) {
                Text(
                    amountText,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.padding(start = measured + 8.dp)
                )
            }
        }
    }
}

// ── States ────────────────────────────────────────────────────────────────────

@Composable
private fun CompareMessage(title: String, body: String) {
    CenteredBox {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Text(
                title,
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

