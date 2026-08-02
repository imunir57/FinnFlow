package com.finnflow.ui.yearly

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.ui.theme.FinnFlowTheme
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToLong

private fun fmtAmount(amount: Double): String =
    if (amount == kotlin.math.floor(amount)) "%,.0f".format(amount)
    else "%,.2f".format(amount)

@Composable
fun YearlyScreen(viewModel: YearlyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val maxMonthVal = remember(state.incomeByMonth, state.expenseByMonth) {
        (state.incomeByMonth + state.expenseByMonth).maxOfOrNull { it.total }?.takeIf { it > 0 } ?: 1.0
    }
    val currentMonthIndex = remember(state.year) {
        val today = LocalDate.now()
        if (state.year == today.year) today.monthValue - 1 else -1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Title bar ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Yearly",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.MoreVert,
                    "Menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Year navigator ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = viewModel::previousYear) {
                Icon(
                    Icons.Default.ArrowBack,
                    "Previous year",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                state.year.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = viewModel::nextYear) {
                Icon(
                    Icons.Default.ArrowForward,
                    "Next year",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Hero summary card ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(FinnFlowTheme.colors.heroGradient)
        ) {
            Text(
                "৳",
                fontSize = 160.sp,
                fontFamily = FontFamily.Serif,
                color = FinnFlowTheme.colors.heroOnSurface.copy(alpha = 0.05f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 12.dp, y = (-28).dp)
            )
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    "NET BALANCE",
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = FinnFlowTheme.colors.heroOnSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "৳",
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        color = FinnFlowTheme.colors.heroOnSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, end = 4.dp)
                    )
                    Text(
                        fmtAmount(state.netBalance),
                        fontSize = 52.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        color = FinnFlowTheme.colors.heroOnSurface,
                        lineHeight = 52.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(
                    color = FinnFlowTheme.colors.heroOnSurface.copy(alpha = 0.12f)
                )
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    HeroStat(
                        "INCOME",
                        state.totalIncome,
                        FinnFlowTheme.colors.heroIncome,
                        Modifier.weight(1f)
                    )
                    HeroStat(
                        "EXPENSE",
                        state.totalExpense,
                        FinnFlowTheme.colors.heroExpense,
                        Modifier.weight(1f)
                    )
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // ── Monthly averages strip ─────────────────────────────────────────
        AvgStrip(avgIn = state.avgMonthlyIncome, avgOut = state.avgMonthlyExpense)

        // ── Month list ────────────────────────────────────────────────────
        // Column header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Month",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Text(
                "In · Out · Net",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(12) { index ->
                val monthNum  = "%02d".format(index + 1)
                val monthFull = Month.of(index + 1).getDisplayName(TextStyle.FULL, Locale.getDefault())
                val income  = state.incomeByMonth.firstOrNull  { it.month == monthNum }?.total ?: 0.0
                val expense = state.expenseByMonth.firstOrNull { it.month == monthNum }?.total ?: 0.0
                MonthRow(
                    monthName = monthFull,
                    income    = income,
                    expense   = expense,
                    maxVal    = maxMonthVal,
                    isCurrent = index == currentMonthIndex
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = FinnFlowTheme.colors.heroOnSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "৳ ${fmtAmount(value)}",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = FinnFlowTheme.colors.heroOnSurface
        )
    }
}

@Composable
private fun AvgStrip(avgIn: Double, avgOut: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AvgCell(label = "Avg / month in", value = avgIn, color = FinnFlowTheme.colors.income)
        AvgCell(label = "Avg / month out", value = avgOut, color = FinnFlowTheme.colors.expense)
    }
}

@Composable
private fun AvgCell(label: String, value: Double, color: Color) {
    Column {
        Text(
            label,
            fontSize = 10.sp,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "৳",
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                color = color.copy(alpha = 0.55f),
                modifier = Modifier.padding(end = 1.dp, bottom = 2.dp)
            )
            Text(
                fmtAmount(value.roundToLong().toDouble()),
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                color = color,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

@Composable
private fun MonthRow(
    monthName: String,
    income: Double,
    expense: Double,
    maxVal: Double,
    isCurrent: Boolean = false
) {
    val balance = income - expense
    val hasData = income > 0 || expense > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrent) FinnFlowTheme.colors.selectedRow else Color.Transparent
            )
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    monthName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    // Empty months are decorative filler, so the faint role is legitimate here.
                    color = if (hasData) MaterialTheme.colorScheme.onSurface
                            else FinnFlowTheme.colors.inkFaint
                )
                if (isCurrent) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.inverseSurface,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "NOW",
                            fontSize = 9.sp,
                            letterSpacing = 0.6.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    }
                }
            }
            if (hasData) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "+${fmtAmount(income)}",
                        fontSize = 11.5.sp,
                        color = FinnFlowTheme.colors.income
                    )
                    Text(
                        "-${fmtAmount(expense)}",
                        fontSize = 11.5.sp,
                        color = FinnFlowTheme.colors.expense
                    )
                    Text(
                        (if (balance >= 0) "+" else "") + fmtAmount(balance),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (balance >= 0) FinnFlowTheme.colors.income
                                else FinnFlowTheme.colors.expense
                    )
                }
            } else {
                Text("—", fontSize = 13.sp, color = FinnFlowTheme.colors.inkFaint)
            }
        }

        if (hasData) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(2.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((income / maxVal).toFloat().coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(
                            FinnFlowTheme.colors.income.copy(alpha = 0.7f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(2.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((expense / maxVal).toFloat().coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(
                            FinnFlowTheme.colors.expense.copy(alpha = 0.7f),
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
