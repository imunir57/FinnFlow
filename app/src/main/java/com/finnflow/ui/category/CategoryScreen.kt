package com.finnflow.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.TransactionType
import com.finnflow.ui.theme.*

private const val TAG = "CategoryScreen"

// ── Icon catalogue ────────────────────────────────────────────────────────────

private val ICON_KEYS = listOf(
    "utensils", "car", "home", "heart", "book", "bag",
    "film", "phone", "sparkle", "gift", "bank", "wallet",
    "briefcase", "laptop", "trending", "dots"
)

private val ICON_MAP: Map<String, ImageVector> = mapOf(
    "utensils"  to Icons.Default.Restaurant,
    "car"       to Icons.Default.DirectionsCar,
    "home"      to Icons.Default.Home,
    "heart"     to Icons.Default.Favorite,
    "book"      to Icons.Default.MenuBook,
    "bag"       to Icons.Default.ShoppingBag,
    "film"      to Icons.Default.Movie,
    "phone"     to Icons.Default.Phone,
    "sparkle"   to Icons.Default.AutoAwesome,
    "gift"      to Icons.Default.CardGiftcard,
    "bank"      to Icons.Default.AccountBalance,
    "wallet"    to Icons.Default.AccountBalanceWallet,
    "briefcase" to Icons.Default.Work,
    "laptop"    to Icons.Default.Laptop,
    "trending"  to Icons.Default.TrendingUp,
    "dots"      to Icons.Default.MoreHoriz,
)

private val COLOR_CHOICES = listOf(
    "#C44536", "#D18842", "#7A5C3E", "#6E8A4A", "#4A8A5C", "#2E8B94",
    "#3A6EA5", "#3E4A8A", "#7A4FA0", "#B5456E", "#B85A3E", "#556B74", "#8A8A8A"
)

private fun String?.toComposeColor(): Color {
    if (isNullOrEmpty()) return InkMedium
    return try { Color(android.graphics.Color.parseColor(this)) } catch (_: Exception) { InkMedium }
}

private fun iconFor(key: String?): ImageVector =
    if (key.isNullOrEmpty()) Icons.Default.MoreHoriz else ICON_MAP[key] ?: Icons.Default.MoreHoriz

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmPaper)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 18.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkMedium)
                }
                Text(
                    "Categories",
                    fontFamily = FontFamily.Serif,
                    fontSize = 26.sp,
                    color = Ink,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    SecureLogger.d(TAG, "User clicked Add category button (top bar)")
                    viewModel.openEditSheet(null)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add category", tint = InkMedium)
                }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${state.displayItems.size} categories · drag to reorder",
                    fontSize = 10.5.sp,
                    color = InkFaint,
                    letterSpacing = 1.sp
                )
                Text(
                    "Subs",
                    fontSize = 10.5.sp,
                    color = InkFaint,
                    letterSpacing = 1.sp
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                itemsIndexed(
                    state.displayItems,
                    key = { _, item -> item.category.id }
                ) { _, item ->
                    CategoryRow(
                        item = item,
                        onOpenSubs = {
                            SecureLogger.d(TAG, "User navigating to sub-categories: categoryId=${item.category.id}")
                            onNavigateToSubCategories(item.category.id)
                        },
                        onEdit = {
                            SecureLogger.d(TAG, "User clicked Edit: categoryId=${item.category.id}, name=${item.category.name}")
                            viewModel.openEditSheet(item.category)
                        }
                    )
                    HorizontalDivider(color = Rule)
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
                .background(Ink)
                .clickable {
                    SecureLogger.d(TAG, "User clicked Add category FAB")
                    viewModel.openEditSheet(null)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add category", tint = WarmPaper)
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
            onDelete = { cat ->
                SecureLogger.d(TAG, "User deleted category: id=${cat.id}, name=${cat.name}")
                viewModel.deleteCategory(cat)
                viewModel.closeEditSheet()
            }
        )
    }
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
            .background(WarmSurface)
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
                        .background(if (active) Ink else Color.Transparent)
                        .clickable {
                            SecureLogger.d(TAG, "User changed category filter: type=$type")
                            onTypeChange(type)
                        }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        color      = if (active) WarmPaper else InkMedium,
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
    onOpenSubs: () -> Unit,
    onEdit: () -> Unit
) {
    val cat      = item.category
    val catColor = remember(cat.colorHex) { (cat.colorHex as? String).toComposeColor() }
    val icon     = remember(cat.iconName)  { iconFor(cat.iconName as? String) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Drag handle ≡
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.width(14.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(11.dp)
                        .height(1.5.dp)
                        .background(InkFaint)
                )
            }
        }

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

        // Name + sub preview
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
                color      = Ink,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (item.subPreviewNames.isNotEmpty()) {
                val preview = item.subPreviewNames.joinToString(" · ") +
                        if (item.subCount > 3) " …" else ""
                Text(
                    preview,
                    fontSize = 11.5.sp,
                    color    = InkFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Sub-count pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, Rule, RoundedCornerShape(999.dp))
                .clickable(onClick = onOpenSubs)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                "${item.subCount}",
                fontSize   = 11.5.sp,
                color      = InkMedium,
                fontWeight = FontWeight.Medium
            )
        }

        // Edit button
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = InkFaint, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Info box ──────────────────────────────────────────────────────────────────

@Composable
private fun CategoryInfoBox(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(1.dp, Rule, 14.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = InkMedium, modifier = Modifier.size(14.dp))
            Text(
                "About categories",
                fontSize   = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color      = InkMedium
            )
        }
        Text(
            "Categories with existing transactions can't be deleted — archive them instead so old records keep their label.",
            fontSize    = 12.5.sp,
            color       = InkFaint,
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
    onDelete: (Category) -> Unit
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
    val previewColor = remember(colorHex) { (colorHex as? String).toComposeColor() }
    val previewIcon  = remember(iconKey)  { iconFor(iconKey) }

    ModalBottomSheet(
        onDismissRequest  = onClose,
        sheetState        = sheetState,
        containerColor    = WarmPaper,
        shape             = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Rule)
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
                        color      = Ink
                    )
                    Text("Pick an icon and a colour", fontSize = 11.5.sp, color = InkFaint)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = InkMedium)
                }
            }

            // Name
            SheetFieldLabel("Name")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value          = name,
                onValueChange  = { name = it },
                placeholder    = { Text("e.g. Subscriptions", color = InkFaint) },
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
                    focusedBorderColor    = Ink,
                    unfocusedBorderColor  = Rule,
                    focusedTextColor      = Ink,
                    unfocusedTextColor    = Ink,
                    cursorColor           = Ink,
                    unfocusedContainerColor = WarmCard,
                    focusedContainerColor   = WarmCard
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
                                .background(if (active) Ink else WarmCard)
                                .border(1.dp, if (active) Ink else Rule, RoundedCornerShape(999.dp))
                                .clickable { type = t }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                label,
                                fontSize   = 13.sp,
                                color      = if (active) WarmPaper else InkMedium,
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
            ICON_KEYS.chunked(8).forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowKeys.forEach { key ->
                        val active = key == iconKey
                        val ic     = ICON_MAP[key] ?: Icons.Default.MoreHoriz
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) previewColor.copy(alpha = 0.11f) else WarmPaper)
                                .border(
                                    width  = if (active) 1.5.dp else 1.dp,
                                    color  = if (active) previewColor else Rule,
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
                                tint     = if (active) previewColor else InkMedium,
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
                        val c      = remember(hex) { hex.toComposeColor() }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (active) Modifier.border(3.dp, WarmPaper, CircleShape)
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
                                    tint     = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Delete — edit only
            if (category != null) {
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick  = { onDelete(category) },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, ExpenseClay),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseClay)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Cancel / Save
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Rule)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick  = onClose,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Rule),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = InkMedium)
                ) {
                    Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick  = { if (name.isNotBlank()) onSave(name.trim(), type, iconKey, colorHex) },
                    enabled  = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Ink,
                        contentColor           = WarmPaper,
                        disabledContainerColor = Rule,
                        disabledContentColor   = InkFaint
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
        color      = InkFaint,
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
        floatingActionButton = {
            FloatingActionButton(onClick = {
                SecureLogger.d(TAG, "User clicked Add sub-category FAB")
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, "Add sub-category")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            itemsIndexed(state.subCategories, key = { _, sub -> sub.id }) { _, sub ->
                SubCategoryItem(
                    subCategory = sub,
                    onEdit      = {
                        SecureLogger.d(TAG, "User saved sub-category: id=${it.id}, name=${it.name}")
                        viewModel.updateSubCategory(it)
                    },
                    onDelete    = {
                        SecureLogger.d(TAG, "User deleted sub-category: id=${it.id}, name=${it.name}")
                        viewModel.deleteSubCategory(it)
                    }
                )
                HorizontalDivider()
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
}

@Composable
fun SubCategoryItem(
    subCategory: SubCategory,
    onEdit: (SubCategory) -> Unit,
    onDelete: (SubCategory) -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(subCategory.name) },
        trailingContent = {
            Row {
                IconButton(onClick = {
                    SecureLogger.d(TAG, "User clicked Edit sub-category: id=${subCategory.id}")
                    showEdit = true
                }) { Icon(Icons.Default.Edit, "Edit") }
                IconButton(onClick = {
                    SecureLogger.d(TAG, "User clicked Delete sub-category: id=${subCategory.id}")
                    onDelete(subCategory)
                }) { Icon(Icons.Default.Delete, "Delete") }
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
