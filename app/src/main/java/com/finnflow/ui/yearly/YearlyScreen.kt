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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.ui.theme.ExpenseClay
import com.finnflow.ui.theme.IncomeGreen
import com.finnflow.ui.theme.Ink
import com.finnflow.ui.theme.InkFaint
import com.finnflow.ui.theme.InkMedium
import com.finnflow.ui.theme.Rule
import com.finnflow.ui.theme.WarmPaper
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

private fun fmtAmount(amount: Double): String =
    if (amount == kotlin.math.floor(amount)) "%,.0f".format(amount)
    else "%,.2f".format(amount)

@Composable
fun YearlyScreen(viewModel: YearlyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val maxMonthVal = remember(state.incomeByMonth, state.expenseByMonth) {
        (state.incomeByMonth + state.expenseByMonth).maxOfOrNull { it.total }?.takeIf { it > 0 } ?: 1.0
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
                "Yearly",
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                color = Ink,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, "Menu", tint = InkMedium)
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
                Icon(Icons.Default.ArrowBack, "Previous year", tint = InkMedium, modifier = Modifier.size(18.dp))
            }
            Text(
                state.year.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            IconButton(onClick = viewModel::nextYear) {
                Icon(Icons.Default.ArrowForward, "Next year", tint = InkMedium, modifier = Modifier.size(18.dp))
            }
        }

        // ── Hero summary card ─────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF1A2820),
                            0.4f to Color(0xFF1E1916),
                            1.0f to Color(0xFF241410)
                        ),
                        start = Offset(0f, 0f),
                        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            Text(
                "৳",
                fontSize = 160.sp,
                fontFamily = FontFamily.Serif,
                color = Color.White.copy(alpha = 0.05f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 12.dp, y = (-28).dp)
            )
            Column(modifier = Modifier.padding(22.dp)) {
                Text(
                    "NET BALANCE",
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "৳",
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 8.dp, end = 4.dp)
                    )
                    Text(
                        fmtAmount(state.netBalance),
                        fontSize = 52.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        color = Color.White,
                        lineHeight = 52.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    HeroStat("INCOME",  state.totalIncome,  Color(0xFF78C898), Modifier.weight(1f))
                    HeroStat("EXPENSE", state.totalExpense, Color(0xFFDC9070), Modifier.weight(1f))
                }
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        // ── Month list ────────────────────────────────────────────────────
        // Column header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Month",   fontSize = 10.sp, color = InkFaint, letterSpacing = 1.sp)
            Text("Income / Expense  Net", fontSize = 10.sp, color = InkFaint, letterSpacing = 1.sp)
        }
        HorizontalDivider(color = Rule)

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
                    maxVal    = maxMonthVal
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
            Text(label, fontSize = 10.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.6f))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "৳ ${fmtAmount(value)}",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun MonthRow(
    monthName: String,
    income: Double,
    expense: Double,
    maxVal: Double
) {
    val balance = income - expense
    val hasData = income > 0 || expense > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                monthName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (hasData) Ink else InkFaint,
                modifier = Modifier.weight(1f)
            )
            if (hasData) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("+${fmtAmount(income)}", fontSize = 11.5.sp, color = IncomeGreen)
                    Text("-${fmtAmount(expense)}", fontSize = 11.5.sp, color = ExpenseClay)
                    Text(
                        (if (balance >= 0) "+" else "") + fmtAmount(balance),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (balance >= 0) IncomeGreen else ExpenseClay
                    )
                }
            } else {
                Text("—", fontSize = 13.sp, color = InkFaint)
            }
        }

        if (hasData) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Rule, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((income / maxVal).toFloat().coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(IncomeGreen.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                )
            }
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Rule, RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((expense / maxVal).toFloat().coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(ExpenseClay.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                )
            }
        }
    }
    HorizontalDivider(color = Rule)
}
