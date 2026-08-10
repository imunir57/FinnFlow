package com.finnflow.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.TransactionType
import com.finnflow.ui.components.CATEGORY_ICON_KEYS
import com.finnflow.ui.components.CATEGORY_ICON_MAP
import com.finnflow.ui.components.ConfirmationDialog
import com.finnflow.ui.components.DraggableItem
import com.finnflow.ui.components.categoryIconFor
import com.finnflow.ui.components.dragHandle
import com.finnflow.ui.components.rememberDragDropState
import com.finnflow.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

private const val TAG = "CategoryScreen"

// ── Icon catalogue ────────────────────────────────────────────────────────────



private val COLOR_CHOICES = listOf(
    "#C44536", "#D18842", "#7A5C3E", "#6E8A4A", "#4A8A5C", "#2E8B94",
    "#3A6EA5", "#3E4A8A", "#7A4FA0", "#B5456E", "#B85A3E", "#556B74", "#8A8A8A"
)


private fun Modifier.dashedBorder(width: Dp, color: Color, cornerRadius: Dp): Modifier =
    drawWithContent {
        drawContent()
        drawRoundRect(
            color = color,
            style = Stroke(
                width = width.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx())
        )
    }

// ── Category screen ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSubCategories: (Long) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Holds the category awaiting confirmation rather than a bare flag, so the dialog
    // survives recomposition and always names the row the user actually tapped.
    var pendingArchive by remember { mutableStateOf<Category?>(null) }

    // Archiving moves a row from one section of the same screen to another, which is easy to
    // miss — the snackbar confirms what happened, and reports it when it fails.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    val listState = rememberLazyListState()
    // Only the active rows are draggable, and they come first in the list — the archived
    // section and the info box below them are neither draggable nor drop targets.
    val dragDropState = rememberDragDropState(
        lazyListState = listState,
        draggableItemCount = state.displayItems.size,
        onMove = viewModel::moveCategory,
        onDragFinished = viewModel::commitCategoryOrder
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────
            // Back arrow + title: adding a category is the FAB's job, and a second "+" here was
            // redundant. Padding matches the Profile / About title rows. The reorder toggle
            // earns its place because reordering changes what every other row does.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 18.dp, top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "Categories",
                    fontFamily = FontFamily.Serif,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // ── Type toggle ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CategoryTypeToggle(
                    selectedType = state.selectedType,
                    expenseCount = state.expenseCount,
                    incomeCount  = state.incomeCount,
                    onTypeChange = viewModel::setSelectedType
                )
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // ── Section header ────────────────────────────────────────────
            // "Subs" is padded off the end by the width of the edit button plus its gap, so it
            // sits over the sub-count pill it labels rather than over the pencil icon.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${state.displayItems.size} categories",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    "Subs",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(end = SUB_COUNT_HEADER_END_PADDING)
                )
            }

            if (state.displayItems.isEmpty() && state.archivedItems.isEmpty()) {
                EmptyListMessage(
                    icon = Icons.Default.Category,
                    title = "No ${if (state.selectedType == TransactionType.EXPENSE) "expense" else "income"} categories yet",
                    body = "Tap + to create one. Categories are what every transaction gets filed under.",
                    modifier = Modifier.weight(1f)
                )
                return@Column
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                itemsIndexed(
                    state.displayItems,
                    key = { _, item -> item.category.id }
                ) { index, item ->
                    DraggableItem(dragDropState, index) { isDragging ->
                        CategoryRow(
                            item = item,
                            isDragging = isDragging,
                            dragHandleModifier = Modifier.dragHandle(dragDropState, index),
                            onOpenSubs = {
                                SecureLogger.d(TAG, "User navigating to sub-categories: categoryId=${item.category.id}")
                                onNavigateToSubCategories(item.category.id)
                            },
                            onEdit = {
                                SecureLogger.d(TAG, "User clicked Edit: categoryId=${item.category.id}, name=${item.category.name}")
                                viewModel.openEditSheet(item.category)
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                if (state.archivedItems.isNotEmpty()) {
                    item(key = "archived-header") {
                        Text(
                            "ARCHIVED · ${state.archivedItems.size}",
                            fontSize = 10.5.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 6.dp)
                        )
                    }
                    itemsIndexed(
                        state.archivedItems,
                        key = { _, item -> "archived-${item.category.id}" }
                    ) { _, item ->
                        ArchivedCategoryRow(
                            item = item,
                            onRestore = {
                                SecureLogger.d(TAG, "User restoring category: id=${item.category.id}")
                                viewModel.restoreCategory(item.category)
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                item {
                    CategoryInfoBox(
                        modifier = Modifier.padding(
                            start = 22.dp, end = 22.dp, top = 20.dp, bottom = 8.dp
                        )
                    )
                }
            }
        }

        // ── FAB ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable {
                    SecureLogger.d(TAG, "User clicked Add category FAB")
                    viewModel.openEditSheet(null)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add category", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }

    // ── Edit / New bottom sheet ───────────────────────────────────────────
    if (state.isEditSheetOpen) {
        CategoryEditSheet(
            category    = if (state.isNewCategory) null else state.editingCategory,
            defaultType = state.selectedType,
            onClose     = viewModel::closeEditSheet,
            onSave      = { name, type, iconName, colorHex ->
                if (state.isNewCategory) {
                    SecureLogger.d(TAG, "Saving new category: name=$name, type=$type")
                    viewModel.addCategory(name, type, iconName, colorHex)
                } else {
                    state.editingCategory?.let { existing ->
                        SecureLogger.d(TAG, "Saving category changes: id=${existing.id}, oldName=${existing.name}, newName=$name")
                        viewModel.updateCategory(
                            existing.copy(name = name, type = type, iconName = iconName, colorHex = colorHex)
                        )
                    }
                }
                viewModel.closeEditSheet()
            },
            onArchive = { cat ->
                SecureLogger.d(TAG, "User requested archive of category: id=${cat.id}, name=${cat.name}")
                viewModel.closeEditSheet()
                pendingArchive = cat
            }
        )
    }

    // ── Archive confirmation ──────────────────────────────────────────────
    // Says archived rather than deleted, because that is what happens: the row is kept so the
    // transactions already filed under it keep their name, icon and colour.
    pendingArchive?.let { cat ->
        val subCount = state.displayItems.firstOrNull { it.category.id == cat.id }?.subCount ?: 0
        val subClause = when (subCount) {
            0 -> ""
            1 -> " and its 1 sub-category"
            else -> " and its $subCount sub-categories"
        }
        ConfirmationDialog(
            title = "Archive \"${cat.name}\"?",
            message = "\"${cat.name}\"$subClause will stop appearing when you add a " +
                    "transaction. It isn't deleted — older records keep it, and Stats still " +
                    "counts it for any period where it has amounts.\n\nYou can restore it from " +
                    "the Archived list at any time.",
            confirmLabel = "Archive",
            onConfirm = {
                SecureLogger.d(TAG, "User confirmed archive of category: id=${cat.id}")
                viewModel.archiveCategory(cat)
                pendingArchive = null
            },
            onDismiss = {
                SecureLogger.d(TAG, "User cancelled archive of category: id=${cat.id}")
                pendingArchive = null
            }
        )
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.fillMaxSize().wrapContentHeight(Alignment.Bottom)
    )
}

// ── Type toggle ───────────────────────────────────────────────────────────────

@Composable
private fun CategoryTypeToggle(
    selectedType: TransactionType,
    expenseCount: Int,
    incomeCount: Int,
    onTypeChange: (TransactionType) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(3.dp)
    ) {
        Row {
            listOf(
                TransactionType.EXPENSE to "Expense · $expenseCount",
                TransactionType.INCOME  to "Income · $incomeCount"
            ).forEach { (type, label) ->
                val active = selectedType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable {
                            SecureLogger.d(TAG, "User changed category filter: type=$type")
                            onTypeChange(type)
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        color      = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ── Category row ──────────────────────────────────────────────────────────────

@Composable
private fun CategoryRow(
    item: CategoryDisplayItem,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onOpenSubs: () -> Unit,
    onEdit: () -> Unit
) {
    val cat      = item.category
    val catColor = rememberCategoryColor(cat.colorHex as? String)
    val icon     = remember(cat.iconName)  { categoryIconFor(cat.iconName as? String) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .padding(start = 8.dp, end = 22.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_ITEM_SPACING)
    ) {
        DragHandle(dragHandleModifier)

        // Icon swatch
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(catColor.copy(alpha = 0.11f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = catColor, modifier = Modifier.size(18.dp))
        }

        // Name + sub preview. Opening the subs screen is off while reordering — a mis-tap
        // there would navigate away mid-rearrange.
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenSubs),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                cat.name,
                fontSize   = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (item.subPreviewNames.isNotEmpty()) {
                val preview = item.subPreviewNames.joinToString(" · ") +
                        if (item.subCount > 3) " …" else ""
                Text(
                    preview,
                    fontSize = 11.5.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Sub-count pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                .clickable(onClick = onOpenSubs)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                "${item.subCount}",
                fontSize   = 11.5.sp,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }

        // Edit button
        IconButton(onClick = onEdit, modifier = Modifier.size(EDIT_BUTTON_SIZE)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Reorder handle ────────────────────────────────────────────────────────────

/** Width of the trailing edit button, and the gap before it — see [SUB_COUNT_HEADER_END_PADDING]. */
private val EDIT_BUTTON_SIZE = 32.dp
private val ROW_ITEM_SPACING = 12.dp

/** Lines the "Subs" column header up with the pill instead of the edit button beside it. */
private val SUB_COUNT_HEADER_END_PADDING = EDIT_BUTTON_SIZE + ROW_ITEM_SPACING

/** Touch target of the drag handle — also the indent archived rows leave in its place. */
private val DRAG_HANDLE_SIZE = 32.dp

/**
 * Grab area for reordering, on every row rather than behind a mode.
 *
 * [modifier] carries the gesture (`Modifier.dragHandle`), so the drag starts on touch here and
 * nowhere else: a swipe across the rest of the row still scrolls the list.
 */
@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(DRAG_HANDLE_SIZE),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

/** Centred in whatever space it is given, so an empty list explains itself instead of going blank. */
@Composable
private fun EmptyListMessage(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            fontFamily = FontFamily.Serif,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Archived category row ─────────────────────────────────────────────────────

/**
 * Dimmed, non-editable, and offering only Restore — an archived category exists to keep old
 * transactions readable, so editing it would rewrite how history reads.
 */
@Composable
private fun ArchivedCategoryRow(
    item: CategoryDisplayItem,
    onRestore: () -> Unit
) {
    val cat  = item.category
    val icon = remember(cat.iconName) { categoryIconFor(cat.iconName) }
    val dim  = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 22.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_ITEM_SPACING)
    ) {
        // No handle: archived rows are not reorderable. The gap keeps them aligned with the
        // active rows above rather than jutting out to the left.
        Spacer(Modifier.size(DRAG_HANDLE_SIZE))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = dim, modifier = Modifier.size(18.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                cat.name,
                fontSize   = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color      = dim,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                "Hidden from new transactions · still counted in Stats",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                .clickable(onClick = onRestore)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                "Restore",
                fontSize   = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Info box ──────────────────────────────────────────────────────────────────

@Composable
private fun CategoryInfoBox(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(1.dp, MaterialTheme.colorScheme.outlineVariant, 14.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Text(
                "About categories",
                fontSize   = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "Categories are archived, never deleted. An archived category stops appearing when " +
                    "you add a transaction, while older records keep its name, icon and colour, " +
                    "and Stats keeps counting it wherever it has amounts. Restore it from the " +
                    "Archived list at any time.",
            fontSize    = 12.5.sp,
            color       = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight  = 18.sp
        )
    }
}

// ── Bottom sheet: add / edit category ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditSheet(
    category: Category?,
    defaultType: TransactionType,
    onClose: () -> Unit,
    onSave: (name: String, type: TransactionType, iconName: String, colorHex: String) -> Unit,
    onArchive: (Category) -> Unit
) {
    var name     by remember { mutableStateOf(category?.name ?: "") }
    var iconKey  by remember { mutableStateOf(category?.iconName?.takeIf { it.isNotEmpty() } ?: "dots") }
    var colorHex by remember {
        mutableStateOf(
            category?.colorHex?.takeIf { it.isNotEmpty() && it != "#607D8B" } ?: COLOR_CHOICES[0]
        )
    }
    var type by remember { mutableStateOf(if (category != null) category.type else defaultType) }

    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val previewColor = rememberCategoryColor(colorHex as? String)
    val previewIcon  = remember(iconKey)  { categoryIconFor(iconKey) }

    ModalBottomSheet(
        onDismissRequest  = onClose,
        sheetState        = sheetState,
        containerColor    = MaterialTheme.colorScheme.background,
        shape             = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {

            // Header: preview swatch + title + close
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(previewColor.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(previewIcon, contentDescription = null, tint = previewColor, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (category != null) "Edit category" else "New category",
                        fontFamily = FontFamily.Serif,
                        fontSize   = 20.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text("Pick an icon and a colour", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Name
            SheetFieldLabel("Name")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value          = name,
                onValueChange  = { name = it },
                placeholder    = { Text("e.g. Subscriptions", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine     = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (name.isNotBlank()) onSave(name.trim(), type, iconKey, colorHex)
                }),
                shape  = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor    = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor  = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor      = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor    = MaterialTheme.colorScheme.onSurface,
                    cursorColor           = MaterialTheme.colorScheme.onSurface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Type — only for new categories (changing type on edit would orphan transactions)
            if (category == null) {
                Spacer(Modifier.height(16.dp))
                SheetFieldLabel("Type")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (t, label) ->
                        val active = type == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
                                .border(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(999.dp))
                                .clickable { type = t }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                label,
                                fontSize   = 13.sp,
                                color      = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Icon picker — 2 rows of 8
            Spacer(Modifier.height(16.dp))
            SheetFieldLabel("Icon")
            Spacer(Modifier.height(8.dp))
            CATEGORY_ICON_KEYS.chunked(8).forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowKeys.forEach { key ->
                        val active = key == iconKey
                        val ic     = CATEGORY_ICON_MAP[key] ?: Icons.Default.MoreHoriz
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) previewColor.copy(alpha = 0.11f) else MaterialTheme.colorScheme.background)
                                .border(
                                    width  = if (active) 1.5.dp else 1.dp,
                                    color  = if (active) previewColor else MaterialTheme.colorScheme.outlineVariant,
                                    shape  = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    SecureLogger.d(TAG, "User selected icon: key=$key")
                                    iconKey = key
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                ic, contentDescription = null,
                                tint     = if (active) previewColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // Color picker — chunked into rows of 7 (fits any screen width)
            Spacer(Modifier.height(10.dp))
            SheetFieldLabel("Colour")
            Spacer(Modifier.height(8.dp))
            COLOR_CHOICES.chunked(7).forEach { rowColors ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowColors.forEach { hex ->
                        val active = hex == colorHex
                        val c      = rememberCategoryColor(hex)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (active) Modifier.border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    else Modifier
                                )
                                .clickable {
                                    SecureLogger.d(TAG, "User selected color: hex=$hex")
                                    colorHex = hex
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint     = FinnFlowTheme.colors.onChartSlice(c),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Archive — edit only
            if (category != null) {
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick  = { onArchive(category) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, FinnFlowTheme.colors.expense),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = FinnFlowTheme.colors.expense)
                ) {
                    Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Archive category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Cancel / Save
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick  = onClose,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick  = { if (name.isNotBlank()) onSave(name.trim(), type, iconKey, colorHex) },
                    enabled  = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = MaterialTheme.colorScheme.onSurface,
                        contentColor           = MaterialTheme.colorScheme.background,
                        disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                        disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        if (category != null) "Save changes" else "Create category",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetFieldLabel(text: String) {
    Text(
        text,
        fontSize   = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp
    )
}

// ─── SubCategoryScreen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubCategoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Pending sub-category, hoisted out of the list item so the confirmation is not tied
    // to a row that may be recomposed or recycled underneath it.
    var pendingArchive by remember { mutableStateOf<SubCategory?>(null) }

    // The ViewModel has been reporting archive and restore results all along; this screen had
    // nowhere to show them.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collectLatest { snackbarHostState.showSnackbar(it) }
    }

    val listState = rememberLazyListState()
    // Active sub-categories come first in the list; the archived section below them is not a
    // drop target.
    val dragDropState = rememberDragDropState(
        lazyListState = listState,
        draggableItemCount = state.subCategories.size,
        onMove = viewModel::moveSubCategory,
        onDragFinished = viewModel::commitSubCategoryOrder
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sub-categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                SecureLogger.d(TAG, "User clicked Add sub-category FAB")
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, "Add sub-category")
            }
        }
    ) { padding ->
        if (state.subCategories.isEmpty() && state.archivedSubCategories.isEmpty()) {
            EmptyListMessage(
                icon = Icons.Default.Sell,
                title = "No sub-categories yet",
                body = "Tap + to add one. Sub-categories are optional — they split a category " +
                        "into the finer detail you want Stats to break down.",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding)
        ) {
            itemsIndexed(state.subCategories, key = { _, sub -> sub.id }) { index, sub ->
                DraggableItem(dragDropState, index) { isDragging ->
                    SubCategoryItem(
                        subCategory = sub,
                        isDragging = isDragging,
                        dragHandleModifier = Modifier.dragHandle(dragDropState, index),
                        onEdit      = {
                            SecureLogger.d(TAG, "User saved sub-category: id=${it.id}, name=${it.name}")
                            viewModel.updateSubCategory(it)
                        },
                        onArchive   = {
                            SecureLogger.d(TAG, "User requested archive of sub-category: id=${it.id}, name=${it.name}")
                            pendingArchive = it
                        }
                    )
                    HorizontalDivider()
                }
            }

            if (state.archivedSubCategories.isNotEmpty()) {
                item(key = "archived-subs-header") {
                    Text(
                        "ARCHIVED · ${state.archivedSubCategories.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 4.dp)
                    )
                }
                itemsIndexed(
                    state.archivedSubCategories,
                    key = { _, sub -> "archived-${sub.id}" }
                ) { _, sub ->
                    ListItem(
                        // Not reorderable, but indented to match the rows above it.
                        leadingContent = { Spacer(Modifier.size(DRAG_HANDLE_SIZE)) },
                        headlineContent = {
                            Text(sub.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        supportingContent = {
                            Text(
                                "Hidden from new transactions · kept on older ones",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            TextButton(onClick = {
                                SecureLogger.d(TAG, "User restoring sub-category: id=${sub.id}")
                                viewModel.restoreSubCategory(sub)
                            }) { Text("Restore") }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Sub-category") },
            text  = {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Name") },
                    singleLine    = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick  = {
                        if (name.isNotBlank()) {
                            SecureLogger.d(TAG, "User creating sub-category: name=$name")
                            viewModel.addSubCategory(name.trim())
                            showAddDialog = false
                        }
                    },
                    enabled  = name.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }

    // Archived rather than deleted: transactions.subCategoryId is ON DELETE SET NULL, so
    // removing the row would quietly turn every past entry into "Uncategorised".
    pendingArchive?.let { sub ->
        ConfirmationDialog(
            title = "Archive \"${sub.name}\"?",
            message = "\"${sub.name}\" will stop appearing when you add a transaction. It " +
                    "isn't deleted — transactions already filed under it keep the label.\n\n" +
                    "You can restore it from the Archived list at any time.",
            confirmLabel = "Archive",
            onConfirm = {
                SecureLogger.d(TAG, "User confirmed archive of sub-category: id=${sub.id}")
                viewModel.archiveSubCategory(sub)
                pendingArchive = null
            },
            onDismiss = {
                SecureLogger.d(TAG, "User cancelled archive of sub-category: id=${sub.id}")
                pendingArchive = null
            }
        )
    }
}

@Composable
fun SubCategoryItem(
    subCategory: SubCategory,
    onEdit: (SubCategory) -> Unit,
    onArchive: (SubCategory) -> Unit,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier
) {
    var showEdit by remember { mutableStateOf(false) }

    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh
            else MaterialTheme.colorScheme.surface
        ),
        leadingContent = { DragHandle(dragHandleModifier) },
        headlineContent = { Text(subCategory.name) },
        trailingContent = {
            Row {
                IconButton(onClick = {
                    SecureLogger.d(TAG, "User clicked Edit sub-category: id=${subCategory.id}")
                    showEdit = true
                }) { Icon(Icons.Default.Edit, "Edit") }
                IconButton(onClick = {
                    SecureLogger.d(TAG, "User clicked Archive sub-category: id=${subCategory.id}")
                    onArchive(subCategory)
                }) { Icon(Icons.Default.Archive, "Archive") }
            }
        }
    )

    if (showEdit) {
        var name by remember { mutableStateOf(subCategory.name) }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit Sub-category") },
            text  = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        SecureLogger.d(TAG, "User saving sub-category edit: id=${subCategory.id}, oldName=${subCategory.name}, newName=$name")
                        onEdit(subCategory.copy(name = name.trim()))
                        showEdit = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
        )
    }
}
