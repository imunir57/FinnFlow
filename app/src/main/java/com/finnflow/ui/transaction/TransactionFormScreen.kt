package com.finnflow.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.finnflow.data.model.Category
import com.finnflow.data.model.TransactionType
import com.finnflow.ui.theme.ExpenseClay
import com.finnflow.ui.theme.IncomeGreen
import com.finnflow.ui.theme.Ink
import com.finnflow.ui.theme.InkFaint
import com.finnflow.ui.theme.InkMedium
import com.finnflow.ui.theme.Rule
import com.finnflow.ui.theme.WarmCard
import com.finnflow.ui.theme.WarmPaper
import com.finnflow.ui.theme.WarmSurface
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private fun parseCatColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) { Color(0xFF607D8B) }

private fun safeEval(expr: String): Double? = try {
    val tokens = buildList<String> {
        var i = 0
        val s = expr.filter { it != ' ' }
        while (i < s.length) {
            if (s[i].isDigit() || s[i] == '.') {
                val start = i
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                add(s.substring(start, i))
            } else { add(s[i].toString()); i++ }
        }
    }
    val terms = mutableListOf<Double>()
    var op = '+'
    for (tok in tokens) {
        when (tok) {
            "+", "-", "*", "/" -> op = tok[0]
            else -> {
                val n = tok.toDouble()
                when (op) {
                    '+' -> terms.add(n)
                    '-' -> terms.add(-n)
                    '*' -> if (terms.isNotEmpty()) terms[terms.lastIndex] = terms.last() * n else terms.add(n)
                    '/' -> if (terms.isNotEmpty()) terms[terms.lastIndex] = terms.last() / n else terms.add(n)
                }
            }
        }
    }
    if (terms.isEmpty()) null else terms.sum().takeIf { it.isFinite() }
} catch (_: Exception) { null }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showCalc by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    val today = LocalDate.now()
    val dateChips = listOf(
        "Today"     to today,
        "Yesterday" to today.minusDays(1),
        today.minusDays(2).dayOfWeek.name.take(3).lowercase()
            .replaceFirstChar { it.uppercase() } to today.minusDays(2),
        "Pick"      to state.date
    )

    val amountColor = if (state.type == TransactionType.INCOME) IncomeGreen else Ink

    // Date picker dialog
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmPaper)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = if (showCalc) { { showCalc = false } } else onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink)
            }
            Text(
                if (showCalc) "Calculator" else "New transaction",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            TextButton(
                onClick = viewModel::save,
                enabled = state.isValid && !state.isLoading && !showCalc
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(16.dp))
                else Text(
                    "Save",
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.isValid && !showCalc) Ink else InkFaint
                )
            }
        }

        if (showCalc) {
            CalculatorView(
                initial = state.amount,
                onUse   = { result -> viewModel.onAmountChange(result); showCalc = false },
                onBack  = { showCalc = false },
                modifier = Modifier.weight(1f)
            )
        } else {
            // ── Scrollable form ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Type toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(WarmSurface)
                            .padding(3.dp)
                    ) {
                        Row {
                            listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                                val active = state.type == type
                                TextButton(
                                    onClick = { viewModel.onTypeChange(type) },
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (active) Ink else Color.Transparent)
                                ) {
                                    Text(
                                        type.name.lowercase().replaceFirstChar { it.uppercase() },
                                        color = if (active) WarmPaper else InkMedium,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Hero amount display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "৳",
                        fontSize = 30.sp,
                        fontFamily = FontFamily.Serif,
                        color = amountColor.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 8.dp, end = 4.dp)
                    )
                    Text(
                        if (state.amount.isEmpty()) "0" else state.amount,
                        fontSize = 60.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        color = if (state.amount.isEmpty()) InkFaint else amountColor,
                        lineHeight = 60.sp
                    )
                }
                Text(
                    "Tap keypad below — or use calculator",
                    fontSize = 11.sp,
                    color = InkFaint,
                    letterSpacing = 0.3.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(20.dp))

                // Date chips
                FormLabel("Date")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dateChips.forEachIndexed { index, (label, date) ->
                        val active = state.dateChipIndex == index
                        ChipButton(
                            active = active,
                            onClick = {
                                if (index == 3) showDatePicker = true
                                else viewModel.onDateChipChange(index)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) WarmPaper else Ink
                                )
                                if (index < 3) {
                                    Text(
                                        date.format(DateTimeFormatter.ofPattern("MMM d")),
                                        fontSize = 10.sp,
                                        color = if (active) WarmPaper.copy(alpha = 0.7f) else InkFaint
                                    )
                                } else {
                                    Text(
                                        if (state.dateChipIndex == 3)
                                            state.date.format(DateTimeFormatter.ofPattern("MMM d"))
                                        else "···",
                                        fontSize = 10.sp,
                                        color = if (active) WarmPaper.copy(alpha = 0.7f) else InkFaint
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Category chips
                if (state.categories.isNotEmpty()) {
                    FormLabel("Category")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        state.categories.forEach { cat ->
                            val active = cat.id == state.categoryId
                            val catColor = parseCatColor(cat.colorHex)
                            CategoryChip(cat = cat, catColor = catColor, active = active) {
                                viewModel.onCategoryChange(cat.id)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Subcategory chips
                if (state.subCategories.isNotEmpty()) {
                    FormLabel("Sub-category")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        // None option
                        val noneActive = state.subCategoryId == null
                        SubChip(label = "None", active = noneActive) { viewModel.onSubCategoryChange(null) }
                        state.subCategories.forEach { sub ->
                            SubChip(label = sub.name, active = sub.id == state.subCategoryId) {
                                viewModel.onSubCategoryChange(sub.id)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Note
                FormLabel("Note — optional")
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::onNoteChange,
                    placeholder = { Text("e.g. Dinner with friends", color = InkFaint, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Ink,
                        unfocusedBorderColor = Rule,
                        focusedTextColor     = Ink,
                        unfocusedTextColor   = Ink
                    )
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Numpad ───────────────────────────────────────────────────
            Numpad(
                onDigit   = viewModel::onAmountDigit,
                onBack    = viewModel::onAmountBackspace,
                onCalc    = { showCalc = true }
            )
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 10.sp,
        letterSpacing = 1.sp,
        color = InkFaint,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun ChipButton(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Ink else WarmPaper)
            .then(if (!active) Modifier.border(1.dp, Rule, RoundedCornerShape(14.dp)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            modifier = Modifier.fillMaxWidth()
        ) { content() }
    }
}

@Composable
private fun CategoryChip(cat: Category, catColor: Color, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) catColor.copy(alpha = 0.14f) else WarmPaper)
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(catColor.copy(alpha = if (active) 0.22f else 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(cat.name.take(1).uppercase(), fontSize = 10.sp, color = catColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                cat.name,
                fontSize = 13.sp,
                color = if (active) catColor else InkMedium,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SubChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) Ink else WarmPaper)
            .then(if (!active) Modifier.border(1.dp, Rule, RoundedCornerShape(999.dp)) else Modifier)
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                label,
                fontSize = 12.5.sp,
                color = if (active) WarmPaper else InkMedium,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun Numpad(
    onDigit: (String) -> Unit,
    onBack: () -> Unit,
    onCalc: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("calc", "0", "back")
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarmCard)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = {
                                when (key) {
                                    "calc" -> onCalc()
                                    "back" -> onBack()
                                    else   -> onDigit(key)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            when (key) {
                                "back" -> Text("⌫", fontSize = 22.sp, color = InkMedium)
                                "calc" -> Text("⊞", fontSize = 20.sp, color = InkMedium)
                                else   -> Text(key, fontSize = 26.sp, fontFamily = FontFamily.Serif, color = Ink)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculatorView(
    initial: String,
    onUse: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expr by remember { mutableStateOf(initial) }
    val result = remember(expr) { safeEval(expr) }

    val calcRows = listOf(
        listOf("C", "(", ")", "/"),
        listOf("7", "8", "9", "*"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("0", ".", "⌫", "=")
    )

    Column(modifier = modifier.fillMaxWidth().background(WarmPaper)) {
        // Expression display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WarmCard)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    expr.ifEmpty { "0" },
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    color = Ink,
                    maxLines = 1
                )
                if (result != null && expr.isNotEmpty()) {
                    Text(
                        "= ${if (result == kotlin.math.floor(result)) "%,.0f".format(result) else "%,.2f".format(result)}",
                        fontSize = 14.sp,
                        color = InkFaint
                    )
                }
            }
        }
        // Use result button
        if (result != null && result > 0) {
            TextButton(
                onClick = {
                    val formatted = if (result == kotlin.math.floor(result))
                        result.toLong().toString()
                    else "%.2f".format(result)
                    onUse(formatted)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(containerColor = Ink, contentColor = WarmPaper)
            ) {
                Text("Use ৳ ${if (result == kotlin.math.floor(result)) "%,.0f".format(result) else "%,.2f".format(result)}", fontWeight = FontWeight.SemiBold)
            }
        }
        // Calc keypad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WarmCard)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            calcRows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.forEach { key ->
                        val isOp = key in listOf("+", "-", "*", "/", "(", ")", "=", "C")
                        Box(modifier = Modifier.weight(1f).height(52.dp), contentAlignment = Alignment.Center) {
                            TextButton(
                                onClick = {
                                    when (key) {
                                        "C"  -> expr = ""
                                        "⌫"  -> expr = expr.dropLast(1)
                                        "="  -> { result?.let { r ->
                                            expr = if (r == kotlin.math.floor(r)) r.toLong().toString()
                                                   else "%.2f".format(r)
                                        }}
                                        else -> expr += key
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    key,
                                    fontSize = if (key.length == 1 && !key[0].isDigit() && key != ".") 20.sp else 24.sp,
                                    fontFamily = if (key[0].isDigit() || key == ".") FontFamily.Serif else FontFamily.Default,
                                    color = if (isOp) InkMedium else Ink,
                                    fontWeight = if (isOp) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
