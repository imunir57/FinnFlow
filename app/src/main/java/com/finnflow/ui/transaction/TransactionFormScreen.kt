package com.finnflow.ui.transaction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.TransactionType
import com.finnflow.ui.LocalCurrencyFormat
import com.finnflow.ui.components.ConfirmationDialog
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
    var showDiscardPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            SecureLogger.d(TAG, "Transaction saved, navigating back")
            onNavigateBack()
        }
    }

    // Leaving with a form that *could* have been saved throws away real work, so it asks
    // first. A half-filled form has nothing worth keeping and leaves without comment.
    fun requestBack() {
        if (state.isValid && !state.isSaved) {
            SecureLogger.d(TAG, "Back requested with a saveable form — confirming discard")
            showDiscardPrompt = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler {
        if (showCalc) showCalc = false else requestBack()
    }

    if (showDiscardPrompt) {
        ConfirmationDialog(
            title = "Discard transaction?",
            message = "This transaction hasn't been saved. Leaving now discards what you entered.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            onConfirm = {
                SecureLogger.d(TAG, "User confirmed discarding the unsaved transaction")
                showDiscardPrompt = false
                onNavigateBack()
            },
            onDismiss = { showDiscardPrompt = false }
        )
    }

    // ── Auto-advance ────────────────────────────────────────────────────────
    // Anchors are measured in window space and refreshed on every scroll, so the
    // target is computed against live positions rather than a stale layout pass.
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
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

    if (showDatePicker) {
        FormDatePickerDialog(
            date = state.date,
            onConfirm = { picked ->
                picked?.let(viewModel::onDateChange)
                showDatePicker = false
                advanceTo(FormSection.Category)
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        FormTopBar(
            title = if (showCalc) "Calculator" else "New transaction",
            onBack = { if (showCalc) showCalc = false else requestBack() }
        )

        if (showCalc) {
            CalculatorView(
                initial = state.amount,
                onUse   = { result -> viewModel.onAmountChange(result); showCalc = false },
                modifier = Modifier.weight(1f)
            )
        } else {
            // ── Scrollable form ──────────────────────────────────────────
            // `imePadding` on the frame (not on the scrolling content) shrinks the viewport
            // when the soft keyboard opens, so the focused note field is scrolled clear of it
            // instead of being covered — the window itself is edge-to-edge and never resizes.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned {
                            viewportTop = it.positionInWindow().y
                            viewportHeight = it.size.height
                        }
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    TypeToggle(
                        selected = state.type,
                        onSelect = { type ->
                            SecureLogger.d(TAG, "User clicked type toggle: $type")
                            viewModel.onTypeChange(type)
                            // Type sits above the amount, so the natural next step is
                            // entering the amount, not the date.
                            focusAmount()
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    AmountHero(
                        amount = state.amount,
                        amountColor = amountColor,
                        hint = if (showNumpad) "Tap keypad below — or use calculator"
                        else "Tap the amount to edit",
                        onTap = { focusAmount() },
                        modifier = Modifier.sectionAnchor(FormSection.Amount, anchors)
                    )

                    Spacer(Modifier.height(20.dp))

                    DateSection(
                        chips = dateChips,
                        selectedIndex = state.dateChipIndex,
                        pickedDate = state.date,
                        onChipClick = { index, label ->
                            SecureLogger.d(TAG, "User selected date: label=$label, index=$index")
                            showNumpad = false
                            // "Pick" advances only once the dialog is confirmed.
                            if (index == 3) showDatePicker = true
                            else {
                                viewModel.onDateChipChange(index)
                                pendingSection = FormSection.Category
                            }
                        },
                        modifier = Modifier.sectionAnchor(FormSection.Date, anchors)
                    )

                    Spacer(Modifier.height(18.dp))

                    if (state.categories.isNotEmpty()) {
                        CategorySection(
                            categories = state.categories,
                            selectedId = state.categoryId,
                            onSelect = { cat ->
                                SecureLogger.d(TAG, "User selected category: name=${cat.name}, id=${cat.id}")
                                viewModel.onCategoryChange(cat.id)
                                advanceTo(FormSection.SubCategory)
                            },
                            modifier = Modifier.sectionAnchor(FormSection.Category, anchors)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    if (state.subCategories.isNotEmpty()) {
                        SubCategorySection(
                            subCategories = state.subCategories,
                            selectedId = state.subCategoryId,
                            onSelect = { id ->
                                SecureLogger.d(TAG, "User selected sub-category: id=$id")
                                viewModel.onSubCategoryChange(id)
                                advanceTo(FormSection.Note)
                            },
                            modifier = Modifier.sectionAnchor(FormSection.SubCategory, anchors)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    NoteSection(
                        note = state.note,
                        onNoteChange = viewModel::onNoteChange,
                        onFocused = { showNumpad = false },
                        onDone = { focusManager.clearFocus() },
                        modifier = Modifier.sectionAnchor(FormSection.Note, anchors)
                    )
                    // Clears the floating Save button, which overlays the bottom of the form.
                    Spacer(Modifier.height(84.dp))
                }

                // ── Floating save ────────────────────────────────────────
                SaveFab(
                    visible = state.isValid,
                    isSaving = state.isLoading,
                    onClick = {
                        SecureLogger.d(TAG, "User clicked floating Save: isValid=${state.isValid}")
                        if (!state.isLoading) viewModel.save()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 18.dp)
                )
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

// ── Form sections ─────────────────────────────────────────────────────────────
//
// Each section is its own composable rather than a block inside TransactionFormScreen.
// That is not only for readability: the Compose compiler emits one JVM method per
// composable, and folding all of this into the screen function produced a method with
// 283 registers, which Android's bytecode verifier rejected outright with a VerifyError
// the moment the screen was opened. Keep new sections out here too.

@Composable
private fun FormTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        // Saving lives on the floating button over the form. This keeps the row balanced
        // around the title without a second, permanently greyed-out Save.
        Spacer(Modifier.width(48.dp))
    }
}

@Composable
private fun TypeToggle(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
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
                    val active = selected == type
                    TextButton(
                        onClick = { onSelect(type) },
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
}

/** The big running total. Tapping anywhere on it reopens the keypad. */
@Composable
private fun AmountHero(
    amount: String,
    amountColor: Color,
    hint: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                LocalCurrencyFormat.current.symbol,
                fontSize = 30.sp,
                fontFamily = FontFamily.Serif,
                color = amountColor.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 8.dp, end = 4.dp)
            )
            Text(
                amount.ifEmpty { "0" },
                fontSize = 60.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                color = if (amount.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else amountColor,
                lineHeight = 60.sp
            )
        }
        Text(
            hint,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun DateSection(
    chips: List<Pair<String, LocalDate>>,
    selectedIndex: Int,
    pickedDate: LocalDate,
    onChipClick: (index: Int, label: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayMonth = remember { DateTimeFormatter.ofPattern("MMM d") }
    Column(modifier) {
        FormLabel("Date")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chips.forEachIndexed { index, (label, date) ->
                val active = selectedIndex == index
                ChipButton(
                    active = active,
                    onClick = { onChipClick(index, label) },
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            // The last chip is "Pick": it shows the chosen date once there
                            // is one, and an ellipsis until then.
                            when {
                                index < 3 -> date.format(dayMonth)
                                selectedIndex == 3 -> pickedDate.format(dayMonth)
                                else -> "···"
                            },
                            fontSize = 10.sp,
                            color = if (active) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    categories: List<Category>,
    selectedId: Long?,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        FormLabel("Category")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            categories.forEach { cat ->
                CategoryChip(
                    cat = cat,
                    catColor = rememberCategoryColor(cat.colorHex),
                    active = cat.id == selectedId,
                    onClick = { onSelect(cat) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubCategorySection(
    subCategories: List<SubCategory>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        FormLabel("Sub-category")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SubChip(label = "None", active = selectedId == null) { onSelect(null) }
            subCategories.forEach { sub ->
                SubChip(label = sub.name, active = sub.id == selectedId) { onSelect(sub.id) }
            }
        }
    }
}

@Composable
private fun NoteSection(
    note: String,
    onNoteChange: (String) -> Unit,
    onFocused: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        FormLabel("Note — optional")
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            placeholder = { Text("e.g. Dinner with friends", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                // The soft keyboard and the numpad must never fight for the bottom of
                // the screen.
                .onFocusChanged { if (it.isFocused) onFocused() },
            maxLines = 2,
            // A note is one short sentence, so the keyboard offers Done rather than a
            // newline key that would leave no way to dismiss it. The explicit action is
            // also what stops Compose flagging the multi-line field as having no enter
            // action at all.
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.onSurface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor   = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDatePickerDialog(
    date: LocalDate,
    /** Null when the picker was confirmed with nothing selected — the date then stays as it was. */
    onConfirm: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    pickerState.selectedDateMillis
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                )
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) { DatePicker(state = pickerState) }
}

/**
 * The form's save affordance, floating over the bottom of the fields the way Home's add
 * button floats over the transaction list. It appears only once the form can actually be
 * saved, so its presence is itself the answer to "is this ready?".
 */
@Composable
private fun SaveFab(
    visible: Boolean,
    isSaving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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

/**
 * Ungrouped, for handing back to the amount field, which parses with toDouble().
 * Deliberately not routed through [com.finnflow.ui.CurrencyFormat] — this is a machine-readable
 * value, not a rendered one, and grouping separators would break the parse.
 */
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
                    Text(
                        "= ${LocalCurrencyFormat.current.amount(result)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
