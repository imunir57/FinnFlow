package com.finnflow.ui.yearly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.db.dao.MonthlyCategoryTotal
import com.finnflow.data.db.dao.MonthlyTotal
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "YearlyViewModel"

/** How many categories the hero card and each month row show. */
const val TOP_CATEGORY_COUNT = 3

/** A category's share of a period, ready to render. */
data class CategoryShare(
    val categoryId: Long,
    val name: String,
    val colorHex: String,
    val amount: Double
)

data class YearlyUiState(
    val year: Int = LocalDate.now().year,
    val incomeByMonth: List<MonthlyTotal> = emptyList(),
    val expenseByMonth: List<MonthlyTotal> = emptyList(),
    /** Largest income categories for the whole year, biggest first. */
    val topIncome: List<CategoryShare> = emptyList(),
    /** Largest expense categories for the whole year, biggest first. */
    val topExpense: List<CategoryShare> = emptyList(),
    /** `MM` → that month's largest income categories. Absent when the month has none. */
    val topIncomeByMonth: Map<String, List<CategoryShare>> = emptyMap(),
    /** `MM` → that month's largest expense categories. */
    val topExpenseByMonth: Map<String, List<CategoryShare>> = emptyMap(),
    val isLoading: Boolean = true
) {
    val totalIncome get() = incomeByMonth.sumOf { it.total }
    val totalExpense get() = expenseByMonth.sumOf { it.total }
    val netBalance get() = totalIncome - totalExpense
    val avgMonthlyIncome get() = totalIncome / incomeByMonth.count { it.total > 0 }.coerceAtLeast(1)
    val avgMonthlyExpense get() = totalExpense / expenseByMonth.count { it.total > 0 }.coerceAtLeast(1)

    /** True when [month] (`MM`) has anything to expand into. */
    fun hasBreakdown(month: String): Boolean =
        topIncomeByMonth.containsKey(month) || topExpenseByMonth.containsKey(month)
}

/** The four source flows, named rather than positional — see CLAUDE.md on combining 3+ flows. */
private data class YearlyRaw(
    val income: List<MonthlyTotal>,
    val expense: List<MonthlyTotal>,
    val incomeCategories: List<MonthlyCategoryTotal>,
    val expenseCategories: List<MonthlyCategoryTotal>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class YearlyViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    init {
        SecureLogger.d(TAG, "YearlyViewModel initialized")
    }

    private val _year = MutableStateFlow(LocalDate.now().year)

    /**
     * Explicit expand/collapse choices the user has made, keyed `YYYY-MM`.
     *
     * Absence means "use the default", which is open for the month we are currently in and
     * closed for every other. Storing overrides rather than the resolved set is what lets the
     * user collapse the current month and have it stay collapsed — seeding a plain set at
     * startup would either be undoable or spring back open on every year change.
     *
     * Keyed by year as well as month so that browsing to a past year doesn't inherit this
     * year's expansion, and doesn't open the same month number there.
     */
    private val _expansionOverrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    /** `MM` keys that are open for the year currently on screen. */
    val expandedMonths: StateFlow<Set<String>> =
        combine(_year, _expansionOverrides) { year, overrides -> resolveExpanded(year, overrides) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                resolveExpanded(LocalDate.now().year, emptyMap())
            )

    val state: StateFlow<YearlyUiState> = _year.flatMapLatest { year ->
        SecureLogger.d(TAG, "Loading yearly stats for year: $year")
        combine(
            repository.getMonthlyTotalsByYear(year.toString(), TransactionType.INCOME),
            repository.getMonthlyTotalsByYear(year.toString(), TransactionType.EXPENSE),
            repository.getMonthlyCategoryTotals(year.toString(), TransactionType.INCOME),
            repository.getMonthlyCategoryTotals(year.toString(), TransactionType.EXPENSE)
        ) { income, expense, incomeCategories, expenseCategories ->
            YearlyRaw(income, expense, incomeCategories, expenseCategories)
        }.map { raw ->
            val totalIncome = raw.income.sumOf { it.total }
            val totalExpense = raw.expense.sumOf { it.total }
            SecureLogger.d(
                TAG,
                "Yearly stats calculated: year=$year, total_income=$totalIncome, " +
                    "total_expense=$totalExpense, net_balance=${totalIncome - totalExpense}"
            )
            YearlyUiState(
                year = year,
                incomeByMonth = raw.income,
                expenseByMonth = raw.expense,
                topIncome = raw.incomeCategories.foldToYearTop(),
                topExpense = raw.expenseCategories.foldToYearTop(),
                topIncomeByMonth = raw.incomeCategories.groupToMonthTop(),
                topExpenseByMonth = raw.expenseCategories.groupToMonthTop(),
                isLoading = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YearlyUiState())

    fun previousYear() {
        val newYear = _year.value - 1
        SecureLogger.d(TAG, "User navigated to previous year: $newYear")
        _year.update { it - 1 }
    }

    fun nextYear() {
        val newYear = _year.value + 1
        SecureLogger.d(TAG, "User navigated to next year: $newYear")
        _year.update { it + 1 }
    }

    fun toggleMonth(month: String) {
        val year = _year.value
        val key = expansionKey(year, month)
        val open = _expansionOverrides.value[key] ?: isDefaultExpanded(year, month)
        _expansionOverrides.update { it + (key to !open) }
    }

    /**
     * Expands or collapses the whole year at once. Writes an explicit choice for all twelve
     * months, so collapsing also closes the current month rather than leaving its default open.
     */
    fun setAllExpanded(expanded: Boolean) {
        SecureLogger.d(TAG, "User toggled all months expanded=$expanded")
        val year = _year.value
        _expansionOverrides.update { current ->
            current + MONTH_KEYS.associate { expansionKey(year, it) to expanded }
        }
    }
}

private val MONTH_KEYS: List<String> = (1..12).map { "%02d".format(it) }

private fun expansionKey(year: Int, month: String) = "$year-$month"

/**
 * The month currently in progress starts open — it is the one the user is most likely to be
 * checking, and leaving every row closed makes the breakdown easy to miss entirely.
 */
private fun isDefaultExpanded(year: Int, month: String): Boolean {
    val today = LocalDate.now()
    return year == today.year && month == "%02d".format(today.monthValue)
}

private fun resolveExpanded(year: Int, overrides: Map<String, Boolean>): Set<String> =
    MONTH_KEYS.filterTo(mutableSetOf()) { month ->
        overrides[expansionKey(year, month)] ?: isDefaultExpanded(year, month)
    }

/**
 * Rolls the month-by-category grid up into the year's biggest categories. The query orders
 * within a month, not across the year, so this re-sorts after summing.
 */
private fun List<MonthlyCategoryTotal>.foldToYearTop(): List<CategoryShare> =
    groupBy { it.categoryId }
        .map { (categoryId, rows) ->
            CategoryShare(
                categoryId = categoryId,
                name = rows.first().categoryName,
                colorHex = rows.first().colorHex,
                amount = rows.sumOf { it.totalAmount }
            )
        }
        .sortedByDescending { it.amount }
        .take(TOP_CATEGORY_COUNT)

/** Keeps each month's biggest categories. Already ordered by the query, so `take` is enough. */
private fun List<MonthlyCategoryTotal>.groupToMonthTop(): Map<String, List<CategoryShare>> =
    groupBy { it.month }
        .mapValues { (_, rows) ->
            rows.take(TOP_CATEGORY_COUNT).map {
                CategoryShare(it.categoryId, it.categoryName, it.colorHex, it.totalAmount)
            }
        }
