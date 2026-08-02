package com.finnflow.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Category
import com.finnflow.data.model.TransactionType
import com.finnflow.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val TAG = "TransactionForm"

/**
 * Sections the form auto-advances through, in layout order. Completing a selection in
 * one scrolls the next into view, so the user never has to hunt past the category grid
 * by hand. Hand-off is explicit per call site, since Category skips straight to Note
 * when the chosen category has no subcategories.
 */
private enum class FormSection { Amount, Date, Category, SubCategory, Note }

/** Headroom left above a section header when it is scrolled to. */
private val SectionHeadroom = 12.dp

/** Window-space bounds of a section, refreshed on every layout and scroll pass. */
private data class SectionBounds(val top: Float, val height: Int)

private fun Modifier.sectionAnchor(
    section: FormSection,
    anchors: MutableMap<FormSection, SectionBounds>
) = onGloballyPositioned {
    anchors[section] = SectionBounds(it.positionInWindow().y, it.size.height)
}

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
        if (state.isSaved) {
            SecureLogger.d(TAG, "Transaction saved, navigating back")
            onNavigateBack()
        }
    }

    // ── Auto-advance ────────────────────────────────────────────────────────
    // Anchors are measured in window space and refreshed on every scroll, so the
    // target is computed against live positions rather than a stale layout pass.
    val scrollState = rememberScrollState()
    val anchors = remember { mutableStateMapOf<FormSection, SectionBounds>() }
    var viewportTop by remember { mutableFloatStateOf(0f) }
    var viewportHeight by remember { mutableIntStateOf(0) }
    val headroomPx = with(LocalDensity.current) { SectionHeadroom.toPx() }

    // Set by a selection, cleared once the scroll runs. Only user taps set this, so
    // opening an existing transaction for edit never yanks the form around.
    var pendingSection by remember { mutableStateOf<FormSection?>(null) }

    // Amount is the first field, so the keypad starts open. Every interaction outside the
    // amount section dismisses it; tapping the amount back brings it up.
    var showNumpad by remember { mutableStateOf(true) }

    /** Leaves the amount section for [next], collapsing the keypad on the way out. */
    fun advanceTo(next: FormSection) {
        showNumpad = false
        pendingSection = next
    }

    /** Returns to the amount section and reopens the keypad. */
    fun focusAmount() {
        showNumpad = true
        pendingSection = FormSection.Amount
    }

    suspend fun scrollToSection(section: FormSection) {
        // The sub-category section is composed in the same frame its data arrives, so its
        // anchor is stale (or missing) right now. Wait out the frame so layout has run.
        withFrameNanos { }
        val bounds = anchors[section] ?: return
        val relativeTop = bounds.top - viewportTop
        // Already fully on screen — moving would be noise, not help.
        if (relativeTop >= 0f && relativeTop + bounds.height <= viewportHeight) return
        val target = scrollState.value + relativeTop - headroomPx
        scrollState.animateScrollTo(
            target.roundToInt().coerceIn(0, scrollState.maxValue)
        )
    }

    LaunchedEffect(pendingSection, state.isLoadingSubCategories, state.subCategories) {
        when (val section = pendingSection) {
            null -> Unit
            FormSection.SubCategory -> {
                // The list arrives asynchronously; wait for it to settle, then skip
                // straight to the note if this category has no subcategories at all.
                if (!state.isLoadingSubCategories) {
                    scrollToSection(
                        if (state.subCategories.isEmpty()) FormSection.Note else FormSection.SubCategory
                    )
                    pendingSection = null
                }
            }
            else -> {
                scrollToSection(section)
                pendingSection = null
            }
        }
    }

    val today = LocalDate.now()
    val dateChips = listOf(
        "Today"     to today,
        "Yesterday" to today.minusDays(1),
        today.minusDays(2).dayOfWeek.name.take(3).lowercase()
            .replaceFirstChar { it.uppercase() } to today.minusDays(2),
        "Pick"      to state.date
    )

    val amountColor = if (state.type == TransactionType.INCOME) FinnFlowTheme.colors.income else MaterialTheme.colorScheme.onSurface

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
                    advanceTo(FormSection.Category)
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                if (showCalc) "Calculator" else "New transaction",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = {
                    SecureLogger.d(TAG, "User clicked Save button: isValid=${state.isValid}")
                    viewModel.save()
                },
                enabled = state.isValid && !state.isLoading && !showCalc
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(16.dp))
                else Text(
                    "Save",
                    fontWeight = FontWeight.SemiBold,
                    color = if (state.isValid && !showCalc) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showCalc) {
            CalculatorView(
                initial = state.amount,
                onUse   = { result -> viewModel.onAmountChange(result); showCalc = false },
                modifier = Modifier.weight(1f)
            )
        } else {
            // ── Scrollable form ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned {
                        viewportTop = it.positionInWindow().y
                        viewportHeight = it.size.height
                    }
                    .verticalScroll(scrollState)
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
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(3.dp)
                    ) {
                        Row {
                            listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                                val active = state.type == type
                                TextButton(
                                    onClick = {
                                        SecureLogger.d(TAG, "User clicked type toggle: $type")
                                        viewModel.onTypeChange(type)
                                        // Type sits above the amount, so the natural next
                                        // step is entering the amount, not the date.
                                        focusAmount()
                                    },
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                                ) {
                                    Text(
                                        type.name.lowercase().replaceFirstChar { it.uppercase() },
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Hero amount display — tapping it reopens the keypad
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sectionAnchor(FormSection.Amount, anchors)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { focusAmount() }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                            color = if (state.amount.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else amountColor,
                            lineHeight = 60.sp
                        )
                    }
                    Text(
                        if (showNumpad) "Tap keypad below — or use calculator"
                        else "Tap the amount to edit",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.3.sp
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Date chips
                Column(Modifier.sectionAnchor(FormSection.Date, anchors)) {
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
                                    SecureLogger.d(TAG, "User selected date: label=$label, index=$index")
                                    showNumpad = false
                                    // "Pick" advances only once the dialog is confirmed.
                                    if (index == 3) showDatePicker = true
                                    else {
                                        viewModel.onDateChipChange(index)
                                        pendingSection = FormSection.Category
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (index < 3) {
                                        Text(
                                            date.format(DateTimeFormatter.ofPattern("MMM d")),
                                            fontSize = 10.sp,
                                            color = if (active) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            if (state.dateChipIndex == 3)
                                                state.date.format(DateTimeFormatter.ofPattern("MMM d"))
                                            else "···",
                                            fontSize = 10.sp,
                                            color = if (active) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Category chips
                if (state.categories.isNotEmpty()) {
                    Column(Modifier.sectionAnchor(FormSection.Category, anchors)) {
                        FormLabel("Category")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            state.categories.forEach { cat ->
                                val active = cat.id == state.categoryId
                                val catColor = rememberCategoryColor(cat.colorHex)
                                CategoryChip(cat = cat, catColor = catColor, active = active) {
                                    SecureLogger.d(TAG, "User selected category: name=${cat.name}, id=${cat.id}")
                                    viewModel.onCategoryChange(cat.id)
                                    advanceTo(FormSection.SubCategory)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Subcategory chips
                if (state.subCategories.isNotEmpty()) {
                    Column(Modifier.sectionAnchor(FormSection.SubCategory, anchors)) {
                        FormLabel("Sub-category")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            // None option
                            val noneActive = state.subCategoryId == null
                            SubChip(label = "None", active = noneActive) {
                                SecureLogger.d(TAG, "User selected: None (no sub-category)")
                                viewModel.onSubCategoryChange(null)
                                advanceTo(FormSection.Note)
                            }
                            state.subCategories.forEach { sub ->
                                SubChip(label = sub.name, active = sub.id == state.subCategoryId) {
                                    SecureLogger.d(TAG, "User selected sub-category: name=${sub.name}, id=${sub.id}")
                                    viewModel.onSubCategoryChange(sub.id)
                                    advanceTo(FormSection.Note)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Note
                Column(Modifier.sectionAnchor(FormSection.Note, anchors)) {
                    FormLabel("Note — optional")
                    OutlinedTextField(
                        value = state.note,
                        onValueChange = viewModel::onNoteChange,
                        placeholder = { Text("e.g. Dinner with friends", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            // The soft keyboard and the numpad must never fight for the
                            // bottom of the screen.
                            .onFocusChanged { if (it.isFocused) showNumpad = false },
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor   = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Numpad ───────────────────────────────────────────────────
            // Shown only while the amount has focus. Kept un-animated on purpose: the
            // scroll math below measures the viewport, and a mid-flight height would
            // make it target the wrong offset.
            if (showNumpad) {
                Numpad(
                    onDigit     = viewModel::onAmountDigit,
                    onDecimal   = viewModel::onAmountDecimal,
                    onBackspace = viewModel::onAmountBackspace,
                    onClear     = viewModel::onAmountClear,
                    onCalc      = { showCalc = true },
                    onDone      = { advanceTo(FormSection.Date) }
                )
            }
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 10.sp,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
            .then(if (!active) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)) else Modifier),
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
            .background(if (active) catColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.background)
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
                color = if (active) catColor else MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
            .then(if (!active) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp)) else Modifier)
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                label,
                fontSize = 12.5.sp,
                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

/** One cell of the 4×4 keypad. [Blank] holds the gap open so the grid stays aligned. */
private sealed interface NumKey {
    data class Digit(val value: String) : NumKey
    data object Decimal : NumKey
    data object Backspace : NumKey
    data object Clear : NumKey
    data object Calc : NumKey
    data object Done : NumKey
    data object Blank : NumKey
}

private val NumpadKeys = listOf(
    listOf(NumKey.Digit("1"), NumKey.Digit("2"), NumKey.Digit("3"), NumKey.Backspace),
    listOf(NumKey.Digit("4"), NumKey.Digit("5"), NumKey.Digit("6"), NumKey.Clear),
    listOf(NumKey.Digit("7"), NumKey.Digit("8"), NumKey.Digit("9"), NumKey.Calc),
    listOf(NumKey.Blank,      NumKey.Digit("0"), NumKey.Decimal,    NumKey.Done)
)

@Composable
private fun Numpad(
    onDigit: (String) -> Unit,
    onDecimal: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onCalc: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        NumpadKeys.forEach { row ->
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
                        when (key) {
                            NumKey.Blank -> Unit

                            NumKey.Done -> TextButton(
                                onClick = onDone,
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    containerColor = MaterialTheme.colorScheme.onSurface,
                                    contentColor = MaterialTheme.colorScheme.background
                                )
                            ) {
                                Text(
                                    "Done",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.3.sp
                                )
                            }

                            else -> TextButton(
                                onClick = {
                                    when (key) {
                                        is NumKey.Digit -> onDigit(key.value)
                                        NumKey.Decimal   -> onDecimal()
                                        NumKey.Backspace -> onBackspace()
                                        NumKey.Clear     -> onClear()
                                        NumKey.Calc      -> onCalc()
                                        else             -> Unit
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                when (key) {
                                    is NumKey.Digit -> Text(
                                        key.value,
                                        fontSize = 26.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    NumKey.Decimal -> Text(
                                        ".",
                                        fontSize = 26.sp,
                                        fontFamily = FontFamily.Serif,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    NumKey.Backspace -> Text("⌫", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    NumKey.Clear -> Text(
                                        "C",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    NumKey.Calc -> Text("⊞", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Grouped, for display: 1234.5 → "1,234.50". */
private fun formatCalcDisplay(value: Double): String =
    if (value == kotlin.math.floor(value)) "%,.0f".format(value) else "%,.2f".format(value)

/** Ungrouped, for handing back to the amount field, which parses with toDouble(). */
private fun formatCalcAmount(value: Double): String =
    if (value == kotlin.math.floor(value)) value.toLong().toString() else "%.2f".format(value)

private const val CalcKeyDone = "Done"

private val CalculatorKeys = listOf(
    listOf("C", "(", ")", "/"),
    listOf("7", "8", "9", "*"),
    listOf("4", "5", "6", "-"),
    listOf("1", "2", "3", "+"),
    listOf("0", ".", "⌫", CalcKeyDone)
)

@Composable
private fun CalculatorView(
    initial: String,
    onUse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expr by remember { mutableStateOf(initial) }
    val result = remember(expr) { safeEval(expr) }
    // Done both evaluates and applies, so it only lights up on a usable result.
    val canApply = result != null && result > 0

    Column(modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
        // ── Expression display, pinned to the top ────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                Text(
                    expr.ifEmpty { "0" },
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (result != null && expr.isNotEmpty()) {
                    Text("= ${formatCalcDisplay(result)}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Pushes the keypad to the bottom of the screen instead of letting it
        // float directly under the display.
        Spacer(Modifier.weight(1f))

        // ── Keypad, pinned to the bottom ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CalculatorKeys.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    row.forEach { key ->
                        Box(modifier = Modifier.weight(1f).height(52.dp), contentAlignment = Alignment.Center) {
                            if (key == CalcKeyDone) {
                                TextButton(
                                    onClick = { result?.let { onUse(formatCalcAmount(it)) } },
                                    enabled = canApply,
                                    modifier = Modifier.fillMaxSize(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = MaterialTheme.colorScheme.onSurface,
                                        contentColor = MaterialTheme.colorScheme.background,
                                        disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        CalcKeyDone,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.3.sp
                                    )
                                }
                            } else {
                                val isOp = key in listOf("+", "-", "*", "/", "(", ")", "C")
                                TextButton(
                                    onClick = {
                                        when (key) {
                                            "C" -> expr = ""
                                            "⌫" -> expr = expr.dropLast(1)
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
                                        color = if (isOp) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
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
}
