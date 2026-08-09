package com.finnflow.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Category
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.ui.LocalCurrencyFormat
import com.finnflow.ui.components.ConfirmationDialog
import com.finnflow.ui.theme.FinnFlowTheme
import com.finnflow.ui.theme.rememberCategoryColor
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val TAG = "HomeScreen"

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onNavigateToProfile: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    SecureLogger.d(TAG, "HomeScreen rendered")
    val state by viewModel.uiState.collectAsState()
    val money = LocalCurrencyFormat.current
    val monthLabel = state.selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
            " " + state.selectedMonth.year

    // Held as the pending transaction (not a bare boolean) so the confirmation survives
    // recomposition and can never be shown against a stale row.
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(state.selectedMonth) {
        SecureLogger.d(TAG, "HomeUiState changed: month=${state.selectedMonth}, tx_count=${state.transactions.size}, balance=${state.balance}")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    SecureLogger.d(TAG, "User clicked add transaction FAB")
                    onAddTransaction()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Top bar: avatar + greeting ──────────────────────────────
            // Same start/end/top/bottom rhythm as the Stats and Yearly title rows, so the
            // three bottom-bar destinations start their content at the same offset.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .clickable(onClick = {
                            SecureLogger.d(TAG, "User clicked profile avatar")
                            onNavigateToProfile()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.initials,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Hello,",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        state.displayName.ifBlank { "there" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
                IconButton(onClick = {
                    SecureLogger.d(TAG, "User clicked previous month")
                    viewModel.previousMonth()
                }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Previous month",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    monthLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = {
                    SecureLogger.d(TAG, "User clicked next month")
                    viewModel.nextMonth()
                }) {
                    Icon(
                        Icons.Default.ArrowForward,
                        "Next month",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
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
                            .background(FinnFlowTheme.colors.heroGradient)
                    ) {
                        // Decorative watermark. Follows the active currency: a Taka glyph on a
                        // dollar user's balance card was part of the bug being fixed. Height is
                        // set by the font's line metrics, not the glyph, so the card does not
                        // resize when the symbol changes.
                        Text(
                            money.symbol,
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
                                    money.symbol,
                                    fontSize = 24.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = FinnFlowTheme.colors.heroOnSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, end = 4.dp)
                                )
                                Text(
                                    money.amount(state.balance),
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                transaction     = tx,
                                category        = cat,
                                subCategoryName = state.subCategoryNameOf(tx),
                                onEdit          = { onEditTransaction(tx.id) },
                                onDelete        = { pendingDelete = tx }
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    pendingDelete?.let { tx ->
        val label = state.categories[tx.categoryId]?.name ?: tx.type.name
        // Names the transaction the same way its row does, so the dialog is recognisably about it.
        val described = (listOf(label) + txDetailParts(state.subCategoryNameOf(tx), tx.note))
            .joinToString(" · ")
        val day = "${tx.date.dayOfMonth} " +
                tx.date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) +
                " ${tx.date.year}"
        ConfirmationDialog(
            title = "Delete transaction?",
            message = "$described — ${money.format(tx.amount)} on $day will be permanently " +
                    "deleted. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                SecureLogger.d(TAG, "User confirmed delete for transaction id=${tx.id}")
                viewModel.deleteTransaction(tx)
                pendingDelete = null
            },
            onDismiss = {
                SecureLogger.d(TAG, "User cancelled delete for transaction id=${tx.id}")
                pendingDelete = null
            }
        )
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
            LocalCurrencyFormat.current.format(value),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = FinnFlowTheme.colors.heroOnSurface
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
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 12.sp
            )
            Text(
                date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 12.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            LocalCurrencyFormat.current.format(kotlin.math.abs(dayTotal)),
            fontSize = 12.sp,
            color = if (dayTotal >= 0) FinnFlowTheme.colors.income
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A transaction's subcategory, or null when it has none or the subcategory was deleted. */
private fun HomeUiState.subCategoryNameOf(transaction: Transaction): String? =
    transaction.subCategoryId?.let { subCategories[it]?.name }

/** The secondary-line parts of a row, in order, skipping whatever is missing. */
private fun txDetailParts(subCategoryName: String?, note: String): List<String> =
    listOfNotNull(
        subCategoryName?.takeIf { it.isNotBlank() },
        note.takeIf { it.isNotBlank() }
    )

@Composable
private fun TxRow(
    transaction: Transaction,
    category: Category?,
    subCategoryName: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val catColor = rememberCategoryColor(category?.colorHex)
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
            // Subcategory and note share the line the note used to have to itself — two "Food"
            // rows are otherwise indistinguishable.
            val detail = txDetailParts(subCategoryName, transaction.note)
            if (detail.isNotEmpty()) {
                Text(
                    detail.joinToString(" · "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            LocalCurrencyFormat.current.format(transaction.amount),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (isIncome) FinnFlowTheme.colors.income
                    else MaterialTheme.colorScheme.onBackground
        )
        Box {
            IconButton(onClick = {
                SecureLogger.d(TAG, "User opened transaction menu for id=${transaction.id}")
                showMenu = true
            }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        SecureLogger.d(TAG, "User clicked edit for transaction id=${transaction.id}")
                        showMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        SecureLogger.d(TAG, "User clicked delete for transaction id=${transaction.id}")
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}
