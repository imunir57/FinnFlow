package com.finnflow.ui.yearly

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.ui.AmountStyle
import com.finnflow.ui.CurrencyFormat
import com.finnflow.ui.LocalCurrencyFormat
import com.finnflow.ui.theme.FinnFlowTheme
import com.finnflow.ui.theme.adaptForDark
import com.finnflow.ui.theme.parseHexColor
import com.finnflow.ui.theme.rememberCategoryColor
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun YearlyScreen(viewModel: YearlyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val expandedMonths by viewModel.expandedMonths.collectAsState()
    val currentMonthIndex = remember(state.year) {
        val today = LocalDate.now()
        if (state.year == today.year) today.monthValue - 1 else -1
    }
    val expandableMonths = remember(state.topIncomeByMonth, state.topExpenseByMonth) {
        state.topIncomeByMonth.keys + state.topExpenseByMonth.keys
    }
    val allExpanded = expandableMonths.isNotEmpty() &&
        expandedMonths.containsAll(expandableMonths)

    // One scroll for the whole page: the hero and averages strip scroll away with the month
    // list rather than staying pinned above it.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item(key = "title") {
            Text(
                "Yearly",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 2.dp)
            )
        }

        item(key = "year-nav") {
            YearNavigator(
                year = state.year,
                onPrevious = viewModel::previousYear,
                onNext = viewModel::nextYear
            )
        }

        item(key = "hero") { HeroCard(state = state) }

        if (state.isLoading) {
            item(key = "loading") {
                Box(
                    Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            return@LazyColumn
        }

        item(key = "averages") {
            AvgStrip(avgIn = state.avgMonthlyIncome, avgOut = state.avgMonthlyExpense)
        }

        item(key = "column-header") {
            MonthListHeader(
                allExpanded = allExpanded,
                canExpand = expandableMonths.isNotEmpty(),
                onToggleAll = { viewModel.setAllExpanded(!allExpanded) }
            )
        }

        items(12, key = { "month-$it" }) { index ->
            val monthKey = "%02d".format(index + 1)
            MonthRow(
                monthName = Month.of(index + 1).getDisplayName(TextStyle.FULL, Locale.getDefault()),
                income = state.incomeByMonth.firstOrNull { it.month == monthKey }?.total ?: 0.0,
                expense = state.expenseByMonth.firstOrNull { it.month == monthKey }?.total ?: 0.0,
                topIncome = state.topIncomeByMonth[monthKey].orEmpty(),
                topExpense = state.topExpenseByMonth[monthKey].orEmpty(),
                isCurrent = index == currentMonthIndex,
                isExpanded = monthKey in expandedMonths,
                onToggle = { viewModel.toggleMonth(monthKey) }
            )
        }

        item(key = "bottom-spacer") { Spacer(Modifier.height(80.dp)) }
    }
}

// ── Year navigator ────────────────────────────────────────────────────────────

@Composable
private fun YearNavigator(year: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.Default.ArrowBack,
                "Previous year",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            year.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = onNext) {
            Icon(
                Icons.Default.ArrowForward,
                "Next year",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(state: YearlyUiState) {
    val money = LocalCurrencyFormat.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(FinnFlowTheme.colors.heroGradient)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // The year, not the currency symbol — this card is the only place the figures aren't
        // labelled with their period, and four digits fill the corner better than one glyph.
        Text(
            state.year.toString(),
            fontSize = 92.sp,
            fontFamily = FontFamily.Serif,
            color = FinnFlowTheme.colors.heroOnSurface.copy(alpha = 0.05f),
            maxLines = 1,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 14.dp, y = (-30).dp)
        )
        Column {
            Text(
                "NET BALANCE · ${state.year}",
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                color = FinnFlowTheme.colors.heroOnSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                money.format(state.netBalance),
                fontSize = 36.sp,
                fontFamily = FontFamily.Serif,
                color = FinnFlowTheme.colors.heroOnSurface,
                lineHeight = 40.sp
            )

            Spacer(Modifier.height(12.dp))
            HeroDivider()
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                HeroStat(
                    label = "Income",
                    value = state.totalIncome,
                    valueColor = FinnFlowTheme.colors.heroIncome,
                    alignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
                HeroStat(
                    label = "Expense",
                    value = state.totalExpense,
                    valueColor = FinnFlowTheme.colors.heroExpense,
                    alignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                )
            }

            if (state.topIncome.isNotEmpty() || state.topExpense.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HeroDivider()
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TopCategoryCard(
                        title = "Income",
                        categories = state.topIncome,
                        modifier = Modifier.weight(1f)
                    )
                    TopCategoryCard(
                        title = "Expense",
                        categories = state.topExpense,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroDivider() {
    HorizontalDivider(color = FinnFlowTheme.colors.heroOnSurface.copy(alpha = 0.14f))
}

@Composable
private fun HeroStat(
    label: String,
    value: Double,
    valueColor: Color,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    val money = LocalCurrencyFormat.current
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(label, fontSize = 12.sp, color = FinnFlowTheme.colors.heroOnSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            money.format(value),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * One of the two translucent panels on the hero listing the year's biggest categories.
 * The fill is drawn from the hero's own foreground rather than a theme surface, because the
 * gradient underneath is fixed dark in both themes.
 */
@Composable
private fun TopCategoryCard(
    title: String,
    categories: List<CategoryShare>,
    modifier: Modifier = Modifier
) {
    val onHero = FinnFlowTheme.colors.heroOnSurface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(onHero.copy(alpha = 0.055f))
            .border(1.dp, onHero.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 9.dp)
    ) {
        Text(
            title.uppercase(),
            fontSize = 9.5.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.SemiBold,
            color = onHero.copy(alpha = 0.55f)
        )
        Spacer(Modifier.height(6.dp))
        if (categories.isEmpty()) {
            Text("—", fontSize = 11.5.sp, color = onHero.copy(alpha = 0.55f))
        } else {
            categories.forEach { HeroCategoryRow(it) }
        }
    }
}

@Composable
private fun HeroCategoryRow(share: CategoryShare) {
    val money = LocalCurrencyFormat.current
    val onHero = FinnFlowTheme.colors.heroOnSurface
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The hero gradient is dark in *both* themes, so these swatches always take the
        // dark-adapted colour — the stored Material-500 values are too dark to read on it,
        // and following the light theme here would make them vanish.
        val swatch = remember(share.colorHex) { adaptForDark(parseHexColor(share.colorHex)) }
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(swatch)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            share.name,
            fontSize = 11.5.sp,
            color = onHero.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            money.format(share.amount, AmountStyle.Whole, spaced = false),
            fontSize = 11.5.sp,
            color = onHero.copy(alpha = 0.95f),
            maxLines = 1
        )
    }
}

// ── Averages strip ────────────────────────────────────────────────────────────

@Composable
private fun AvgStrip(avgIn: Double, avgOut: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AvgCell(
            label = "Avg / month in",
            value = avgIn,
            color = FinnFlowTheme.colors.income,
            modifier = Modifier.weight(1f)
        )
        AvgCell(
            label = "Avg / month out",
            value = avgOut,
            color = FinnFlowTheme.colors.expense,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AvgCell(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    val money = LocalCurrencyFormat.current
    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            fontSize = 10.sp,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            money.format(value.roundToLong().toDouble(), AmountStyle.Whole),
            fontSize = 20.sp,
            fontFamily = FontFamily.Serif,
            color = color,
            maxLines = 1
        )
    }
}

// ── Month list ────────────────────────────────────────────────────────────────

@Composable
private fun MonthListHeader(
    allExpanded: Boolean,
    canExpand: Boolean,
    onToggleAll: () -> Unit
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "MONTH",
                fontSize = 10.sp,
                letterSpacing = 0.9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "IN · OUT · NET",
                    fontSize = 10.sp,
                    letterSpacing = 0.9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (canExpand) {
                    Text(
                        if (allExpanded) "Collapse all" else "Expand all",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onToggleAll)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthRow(
    monthName: String,
    income: Double,
    expense: Double,
    topIncome: List<CategoryShare>,
    topExpense: List<CategoryShare>,
    isCurrent: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val money = LocalCurrencyFormat.current
    val balance = income - expense
    val hasData = income > 0 || expense > 0
    val canExpand = topIncome.isNotEmpty() || topExpense.isNotEmpty()
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "monthChevron"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrent) FinnFlowTheme.colors.selectedRow else Color.Transparent
            )
            .then(if (canExpand) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(horizontal = 22.dp, vertical = 11.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    monthName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    // An empty month is decorative filler, so the faint role is legitimate.
                    color = if (hasData) MaterialTheme.colorScheme.onSurface
                            else FinnFlowTheme.colors.inkFaint
                )
                if (isCurrent) {
                    Spacer(Modifier.width(7.dp))
                    NowPill()
                }
            }

            if (hasData) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "+${money.amount(income, AmountStyle.Whole)}",
                        fontSize = 11.sp,
                        color = FinnFlowTheme.colors.income
                    )
                    Text(
                        "−${money.amount(expense, AmountStyle.Whole)}",
                        fontSize = 11.sp,
                        color = FinnFlowTheme.colors.expense
                    )
                    Text(
                        (if (balance >= 0) "+" else "−") +
                            money.amount(kotlin.math.abs(balance), AmountStyle.Whole),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        color = if (balance >= 0) FinnFlowTheme.colors.income
                                else FinnFlowTheme.colors.expense
                    )
                    if (canExpand) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse $monthName"
                                                 else "Expand $monthName",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp).rotate(chevronRotation)
                        )
                    }
                }
            } else {
                Text(
                    "—",
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Serif,
                    color = FinnFlowTheme.colors.inkFaint
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded && canExpand,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    MonthBreakdownColumn(
                        title = "Top income",
                        categories = topIncome,
                        money = money,
                        modifier = Modifier.weight(1f)
                    )
                    MonthBreakdownColumn(
                        title = "Top expense",
                        categories = topExpense,
                        money = money,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun NowPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            "NOW",
            fontSize = 9.sp,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.inverseOnSurface
        )
    }
}

@Composable
private fun MonthBreakdownColumn(
    title: String,
    categories: List<CategoryShare>,
    money: CurrencyFormat,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            title.uppercase(),
            fontSize = 9.sp,
            letterSpacing = 0.9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        if (categories.isEmpty()) {
            Text(
                "—",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            categories.forEach { share ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(rememberCategoryColor(share.colorHex))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        share.name,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        money.format(share.amount, AmountStyle.Whole, spaced = false),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
