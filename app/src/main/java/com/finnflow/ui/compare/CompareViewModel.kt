package com.finnflow.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

private const val TAG = "CompareViewModel"

data class CompareUiState(
    val mode: ComparePeriodMode = ComparePeriodMode.MONTH,
    val type: TransactionType = TransactionType.EXPENSE,
    val periods: List<ComparePeriod> = emptyList(),
    val items: List<CompareItem> = emptyList(),
    val series: List<CompareSeries> = emptyList(),
    val isLoading: Boolean = false
) {
    val canAddPeriod: Boolean get() = periods.size < MAX_COMPARE_PERIODS
    val canAddItem: Boolean get() = items.size < MAX_COMPARE_ITEMS

    /** Below two items there is nothing to compare — the screen prompts instead of charting. */
    val hasEnoughToCompare: Boolean get() = items.size >= 2 && periods.isNotEmpty()

    /** True once a chart is drawable but every selected item came back empty. */
    val allSeriesEmpty: Boolean
        get() = hasEnoughToCompare && series.isNotEmpty() && series.none { it.hasAnyData }
}

/** Everything the data query depends on. Changing any of it re-runs the load. */
private data class CompareSelection(
    val mode: ComparePeriodMode,
    val type: TransactionType,
    val periods: List<ComparePeriod>,
    val items: List<CompareItem>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CompareViewModel @Inject constructor(
    private val transactions: TransactionRepository,
    categories: CategoryRepository
) : ViewModel() {

    private val _mode = MutableStateFlow(ComparePeriodMode.MONTH)
    private val _type = MutableStateFlow(TransactionType.EXPENSE)
    private val _periods = MutableStateFlow(
        ComparePeriod.recent(ComparePeriodMode.MONTH, DEFAULT_COMPARE_PERIODS)
    )
    private val _items = MutableStateFlow<List<CompareItem>>(emptyList())

    /** Categories of the active type, for the item picker. */
    val pickerCategories: StateFlow<List<Category>> = _type
        .flatMapLatest { categories.getCategoriesByType(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Every subcategory, grouped by owning category, for drilling in from the item picker.
     *
     * Started eagerly on purpose. With `WhileSubscribed` this only loads while something is
     * collecting it — and the picker reads it synchronously to decide whether a category has
     * a drill-in arrow, so it would read an empty list and silently offer no subcategories
     * at all.
     */
    val subCategoriesByCategory: StateFlow<Map<Long, List<SubCategory>>> =
        categories.getAllSubCategories()
            // This is a picker, so archived sub-categories are filtered out the same way
            // getCategoriesByType drops archived categories.
            .map { subs -> subs.filterNot { it.isArchived }.groupBy { it.categoryId } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val state: StateFlow<CompareUiState> =
        combine(_mode, _type, _periods, _items) { mode, type, periods, items ->
            CompareSelection(mode, type, periods, items)
        }.flatMapLatest { selection ->
            if (selection.items.isEmpty() || selection.periods.isEmpty()) {
                flowOf(selection.toState(series = emptyList(), isLoading = false))
            } else {
                SecureLogger.d(
                    TAG,
                    "Loading comparison: mode=${selection.mode}, type=${selection.type}, " +
                        "periods=${selection.periods.size}, items=${selection.items.size}"
                )
                combine(
                    selection.periods.map { period -> periodTotals(period, selection) }
                ) { perPeriod ->
                    selection.toState(
                        series = selection.items.map { item ->
                            CompareSeries(
                                item = item,
                                amounts = perPeriod.map { totals -> totals[item.key] ?: 0.0 }
                            )
                        },
                        isLoading = false
                    )
                }.onStart {
                    // Up to five periods each fan out to their own queries, so show the
                    // spinner rather than briefly rendering an empty chart.
                    emit(selection.toState(series = emptyList(), isLoading = true))
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CompareUiState(periods = _periods.value)
        )

    /**
     * Totals for one period, keyed by [CompareItem.key].
     *
     * Whole-category items come from one summary query; subcategory items need a separate query
     * per owning category, so those are de-duplicated first — five subcategories of one category
     * cost one query, not five.
     */
    private fun periodTotals(
        period: ComparePeriod,
        selection: CompareSelection
    ): Flow<Map<String, Double>> {
        val wholeCategories = selection.items.filter { it.isWholeCategory }
        val subCategoryParents = selection.items
            .filterNot { it.isWholeCategory }
            .map { it.categoryId }
            .distinct()

        val sources = buildList<Flow<Map<String, Double>>> {
            if (wholeCategories.isNotEmpty()) {
                add(
                    transactions.getCategorySummary(period.from, period.to, selection.type)
                        .map { rows ->
                            wholeCategories.associate { item ->
                                item.key to (
                                    rows.firstOrNull { it.categoryId == item.categoryId }
                                        ?.totalAmount ?: 0.0
                                    )
                            }
                        }
                )
            }
            subCategoryParents.forEach { parentId ->
                val wanted = selection.items.filter {
                    !it.isWholeCategory && it.categoryId == parentId
                }
                add(
                    transactions
                        .getSubCategorySummary(parentId, period.from, period.to, selection.type)
                        .map { rows ->
                            wanted.associate { item ->
                                item.key to (
                                    rows.firstOrNull { it.subCategoryId == item.subCategoryId }
                                        ?.totalAmount ?: 0.0
                                    )
                            }
                        }
                )
            }
        }

        return if (sources.isEmpty()) flowOf(emptyMap())
        else combine(sources) { parts -> parts.fold(emptyMap()) { acc, part -> acc + part } }
    }

    // ── Period selection ──────────────────────────────────────────────────

    /** Switching between months and years resets to that mode's most recent periods. */
    fun setMode(mode: ComparePeriodMode) {
        if (_mode.value == mode) return
        SecureLogger.d(TAG, "User switched compare mode to $mode")
        _mode.value = mode
        _periods.value = ComparePeriod.recent(mode, DEFAULT_COMPARE_PERIODS)
    }

    fun setType(type: TransactionType) {
        if (_type.value == type) return
        SecureLogger.d(TAG, "User switched compare type to $type")
        _type.value = type
        // Categories are type-scoped, so anything already picked belongs to the other type.
        _items.value = emptyList()
    }

    /** Adds a period, keeping the list in chronological order. Silently ignores duplicates. */
    fun addPeriod(period: ComparePeriod) {
        _periods.update { current ->
            if (current.size >= MAX_COMPARE_PERIODS || current.any { it.key == period.key }) current
            else (current + period).sorted()
        }
    }

    fun removePeriod(period: ComparePeriod) {
        _periods.update { current -> current.filterNot { it.key == period.key } }
    }

    // ── Item selection ────────────────────────────────────────────────────

    /**
     * Adds an item, resolving the overlap between a category and its own subcategories.
     *
     * The two are mutually exclusive within one category: picking the whole of Food & Dining
     * drops any of its subcategories already picked, and picking one of its subcategories
     * drops the whole-category entry. Charting a total alongside part of that same total
     * double-counts, and the picker's tri-state checkbox has no way to depict it either.
     *
     * The cap is checked *after* that pruning, so replacing two subcategories with their
     * parent can never be refused for being over the limit.
     */
    fun addItem(item: CompareItem) {
        _items.update { current ->
            if (current.any { it.key == item.key }) return@update current

            val pruned = if (item.isWholeCategory) {
                current.filterNot { it.categoryId == item.categoryId }
            } else {
                current.filterNot { it.categoryId == item.categoryId && it.isWholeCategory }
            }
            if (pruned.size >= MAX_COMPARE_ITEMS) current else pruned + item
        }
    }

    fun removeItem(item: CompareItem) {
        _items.update { current -> current.filterNot { it.key == item.key } }
    }

    fun toggleItem(item: CompareItem) {
        if (isSelected(item)) removeItem(item) else addItem(item)
    }

    fun isSelected(item: CompareItem): Boolean = _items.value.any { it.key == item.key }

    /** True when this category is selected as a whole rather than by its parts. */
    fun isWholeCategorySelected(categoryId: Long): Boolean =
        _items.value.any { it.categoryId == categoryId && it.isWholeCategory }

    /** How many of [categoryId]'s subcategories are selected individually. */
    fun selectedSubCount(categoryId: Long): Int =
        _items.value.count { it.categoryId == categoryId && !it.isWholeCategory }
}

private fun CompareSelection.toState(series: List<CompareSeries>, isLoading: Boolean) =
    CompareUiState(
        mode = mode,
        type = type,
        periods = periods,
        items = items,
        series = series,
        isLoading = isLoading
    )
