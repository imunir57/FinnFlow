package com.finnflow.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.ui.components.categoryIconFor
import com.finnflow.ui.theme.rememberCategoryColor
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Picks a month **and** its year in one dialog.
 *
 * Material's `DatePicker` would make the user choose a day they don't care about, and a
 * separate year step would mean two dialogs for one decision. Here the year sits in the title
 * row with arrows either side, and the grid below re-renders in place.
 */
@Composable
fun MonthYearPickerDialog(
    initial: ComparePeriod,
    disabledKeys: Set<String>,
    onConfirm: (ComparePeriod) -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { LocalDate.now() }
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.month ?: today.monthValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add month") },
        text = {
            Column {
                YearStepper(year = year, onChange = { year = it })
                Spacer(Modifier.height(14.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(184.dp)
                ) {
                    items((1..12).toList()) { m ->
                        val candidate = ComparePeriod(year, m)
                        // A month that is already being compared, or hasn't happened yet, can't
                        // be added — greying it out explains why better than a silent no-op.
                        val isFuture = LocalDate.of(year, m, 1).isAfter(today.withDayOfMonth(1))
                        val enabled = candidate.key !in disabledKeys && !isFuture
                        MonthCell(
                            label = Month.of(m)
                                .getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            selected = m == month,
                            enabled = enabled,
                            onClick = { month = m }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(ComparePeriod(year, month)) },
                enabled = ComparePeriod(year, month).key !in disabledKeys
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Year equivalent — no month grid, just the stepper and a confirm. */
@Composable
fun YearPickerDialog(
    initial: Int,
    disabledKeys: Set<String>,
    onConfirm: (ComparePeriod) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableIntStateOf(initial) }
    val alreadyUsed = ComparePeriod.year(year).key in disabledKeys

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add year") },
        text = {
            Column {
                YearStepper(year = year, onChange = { year = it })
                if (alreadyUsed) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "$year is already being compared.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(ComparePeriod.year(year)) },
                enabled = !alreadyUsed
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun YearStepper(year: Int, onChange: (Int) -> Unit) {
    val thisYear = remember { LocalDate.now().year }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = { onChange(year - 1) }) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                "Previous year",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            year.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = 64.dp),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = { onChange(year + 1) }, enabled = year < thisYear) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                "Next year",
                tint = if (year < thisYear) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun MonthCell(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        selected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) background else MaterialTheme.colorScheme.surfaceContainer)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, color = content, maxLines = 1)
    }
}

// ── Item picker ───────────────────────────────────────────────────────────────

// ── Item picker ───────────────────────────────────────────────────────────────

/**
 * Categories and their subcategories in one expandable list.
 *
 * The parent checkbox selects the category as a whole; expanding it reveals its
 * subcategories, each individually selectable. The two are mutually exclusive within a
 * category — see `CompareViewModel.addItem` — which is what the parent's indeterminate state
 * depicts: partially covered by its own parts rather than selected outright.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemPickerSheet(
    categories: List<Category>,
    subCategoriesByCategory: Map<Long, List<SubCategory>>,
    isSelected: (CompareItem) -> Boolean,
    isWholeSelected: (Long) -> Boolean,
    selectedSubCount: (Long) -> Int,
    remainingSlots: Int,
    onToggle: (CompareItem) -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf<Set<Long>>(emptySet()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(bottom = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CATEGORIES & SUBCATEGORIES",
                    fontSize = 10.5.sp,
                    letterSpacing = 0.9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (remainingSlots > 0) "$remainingSlots left" else "Limit reached",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(modifier = Modifier.heightIn(max = 460.dp)) {
                categories.forEach { category ->
                    val subs = subCategoriesByCategory[category.id].orEmpty()
                    val wholeSelected = isWholeSelected(category.id)
                    val subSelected = selectedSubCount(category.id)
                    val isExpanded = category.id in expanded

                    item(key = "cat-${category.id}") {
                        CategoryPickerRow(
                            category = category,
                            subCount = subs.size,
                            selectedSubCount = subSelected,
                            wholeSelected = wholeSelected,
                            expanded = isExpanded,
                            // A category already partly covered by its own subcategories can
                            // always be switched to "whole", since that frees slots rather
                            // than consuming one.
                            enabled = wholeSelected || subSelected > 0 || remainingSlots > 0,
                            onToggle = {
                                onToggle(
                                    CompareItem(
                                        categoryId = category.id,
                                        subCategoryId = null,
                                        name = category.name,
                                        colorHex = category.colorHex,
                                        iconName = category.iconName
                                    )
                                )
                            },
                            onExpandToggle = {
                                expanded = if (isExpanded) expanded - category.id
                                           else expanded + category.id
                            }
                        )
                    }

                    if (isExpanded) {
                        items(subs, key = { "sub-${it.id}" }) { sub ->
                            val item = CompareItem(
                                categoryId = category.id,
                                subCategoryId = sub.id,
                                name = sub.name,
                                colorHex = category.colorHex,
                                iconName = category.iconName,
                                parentName = category.name
                            )
                            val selected = isSelected(item)
                            SubCategoryPickerRow(
                                name = sub.name,
                                selected = selected,
                                // Selecting a subcategory of a whole-selected category swaps
                                // one entry for another, so it never needs a spare slot.
                                enabled = selected || wholeSelected || remainingSlots > 0,
                                onToggle = { onToggle(item) }
                            )
                        }
                    }

                    item(key = "div-${category.id}") {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerRow(
    category: Category,
    subCount: Int,
    selectedSubCount: Int,
    wholeSelected: Boolean,
    expanded: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onExpandToggle: () -> Unit
) {
    val swatch = rememberCategoryColor(category.colorHex)
    val state = when {
        wholeSelected -> ToggleableState.On
        selectedSubCount > 0 -> ToggleableState.Indeterminate
        else -> ToggleableState.Off
    }
    val subtitle = when {
        wholeSelected -> "Whole category"
        selectedSubCount > 0 -> "$selectedSubCount of $subCount subcategories"
        subCount > 0 -> "$subCount subcategories"
        else -> "Whole category"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TriStateCheckbox(
            state = state,
            onClick = onToggle,
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(swatch.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                categoryIconFor(category.iconName),
                contentDescription = null,
                tint = swatch,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                category.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = if (wholeSelected || selectedSubCount > 0) swatch
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (subCount > 0) {
            IconButton(onClick = onExpandToggle) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Hide subcategories of ${category.name}"
                                         else "Show subcategories of ${category.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun SubCategoryPickerRow(
    name: String,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 46.dp, end = 20.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(Modifier.width(6.dp))
        Text(
            name,
            fontSize = 13.5.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Removable chip used for both selected periods and selected items. */
@Composable
fun RemovableChip(
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onRemove)
            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "×",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
