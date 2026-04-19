package com.finnflow.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.model.Category
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.ui.theme.IncomeGreen
import com.finnflow.ui.theme.InkFaint
import com.finnflow.ui.theme.InkMedium
import com.finnflow.ui.theme.Rule
import com.finnflow.ui.theme.WarmCard
import com.finnflow.ui.theme.WarmPaper
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) { Color(0xFF607D8B) }

private fun fmtAmount(amount: Double): String =
    if (amount == kotlin.math.floor(amount)) "%,.0f".format(amount)
    else "%,.2f".format(amount)

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val monthLabel = state.selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
            " " + state.selectedMonth.year

    Scaffold(
        containerColor = WarmPaper,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = WarmPaper,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Top bar: avatar + greeting + 3-dot ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(IncomeGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MN", color = WarmPaper, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Hello,",
                        fontSize = 11.sp,
                        color = InkFaint,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        "Munir",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.MoreVert, "Settings", tint = InkMedium)
                }
            }

            // ── Month navigation ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Icon(Icons.Default.ArrowBack, "Previous month", tint = InkMedium, modifier = Modifier.size(18.dp))
                }
                Text(
                    monthLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = viewModel::nextMonth) {
                    Icon(Icons.Default.ArrowForward, "Next month", tint = InkMedium, modifier = Modifier.size(18.dp))
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ── Hero balance card ────────────────────────────────────
                item {
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
                        // Decorative ৳ watermark
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
                                    fmtAmount(state.balance),
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
                }

                // ── Daily groups ─────────────────────────────────────────
                if (state.dailyGroups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No transactions this month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkFaint)
                        }
                    }
                } else {
                    state.dailyGroups.entries.sortedByDescending { it.key }.forEach { (date, txList) ->
                        val dayTotal = txList.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
                        item(key = "hdr-$date") {
                            DaySectionHeader(date = date, dayTotal = dayTotal)
                        }
                        items(txList, key = { it.id }) { tx ->
                            val cat = state.categories[tx.categoryId]
                            TxRow(
                                transaction = tx,
                                category    = cat,
                                onEdit      = { onEditTransaction(tx.id) },
                                onDelete    = { viewModel.deleteTransaction(tx) }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
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
private fun DaySectionHeader(date: LocalDate, dayTotal: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            date.dayOfMonth.toString(),
            fontSize = 28.sp,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.width(34.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.width(28.dp)) {
            Text(
                date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                fontSize = 9.sp, letterSpacing = 0.5.sp, color = InkFaint, lineHeight = 12.sp
            )
            Text(
                date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                fontSize = 9.sp, letterSpacing = 0.5.sp, color = InkFaint, lineHeight = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = Rule)
        Spacer(Modifier.width(8.dp))
        Text(
            "৳ ${fmtAmount(kotlin.math.abs(dayTotal))}",
            fontSize = 12.sp,
            color = if (dayTotal >= 0) IncomeGreen else InkMedium
        )
    }
}

@Composable
private fun TxRow(
    transaction: Transaction,
    category: Category?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val catColor = parseColor(category?.colorHex ?: "#607D8B")
    val isIncome = transaction.type == TransactionType.INCOME

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon bubble
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(catColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (category?.name?.take(1) ?: "?").uppercase(),
                color = catColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                category?.name ?: transaction.type.name,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (transaction.note.isNotBlank()) {
                Text(
                    transaction.note,
                    fontSize = 12.sp,
                    color = InkFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "৳ ${fmtAmount(transaction.amount)}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onBackground
        )
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, "Options", tint = InkFaint, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Edit") },   onClick = { showMenu = false; onEdit() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
            }
        }
    }
}
