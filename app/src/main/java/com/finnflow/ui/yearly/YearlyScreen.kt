package com.finnflow.ui.yearly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
//import androidx.compose.material.icons.filled.ChevronLeft
//import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.db.dao.MonthlyTotal
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyScreen(viewModel: YearlyViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Year selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = viewModel::previousYear) {
                Icon(Icons.Default.ArrowBack, "Previous year")
            }
            Text(
                text = state.year.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            IconButton(onClick = viewModel::nextYear) {
                Icon(Icons.Default.ArrowForward, "Next year")
            }
        }

        // Annual summary card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                YearlySummaryItem("Income", state.totalIncome, MaterialTheme.colorScheme.primary)
                YearlySummaryItem("Expense", state.totalExpense, MaterialTheme.colorScheme.error)
                YearlySummaryItem("Balance", state.netBalance, MaterialTheme.colorScheme.secondary)
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Monthly breakdown
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(12) { index ->
                    val monthNum = "%02d".format(index + 1)
                    val monthName = Month.of(index + 1).getDisplayName(TextStyle.FULL, Locale.getDefault())
                    val income = state.incomeByMonth.firstOrNull { it.month == monthNum }?.total ?: 0.0
                    val expense = state.expenseByMonth.firstOrNull { it.month == monthNum }?.total ?: 0.0
                    MonthRow(monthName = monthName, income = income, expense = expense)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun YearlySummaryItem(label: String, amount: Double, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            "%.2f".format(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun MonthRow(monthName: String, income: Double, expense: Double) {
    val balance = income - expense
    ListItem(
        headlineContent = { Text(monthName, style = MaterialTheme.typography.bodyMedium) },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("+%.2f".format(income), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("-%.2f".format(expense), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "%.2f".format(balance),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    )
}
