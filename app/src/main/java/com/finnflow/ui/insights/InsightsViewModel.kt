package com.finnflow.ui.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "InsightsViewModel"

data class InsightsUiState(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val dailyTotals: List<Double> = emptyList(),
    val daysInMonth: Int = 30,
    val todayDayIndex: Int = 0,
    val weekdayTotals: List<Double> = List(7) { 0.0 },
    val biggestExpenseAmount: Double = 0.0,
    val biggestExpenseCategoryName: String = "",
    val biggestExpenseDate: LocalDate? = null,
    val mostFrequentCategoryName: String = "",
    val mostFrequentCategoryCount: Int = 0,
    val biggestMoMCategoryName: String = "",
    val biggestMoMDelta: Double = 0.0,
    val monthLabel: String = "",
    val isLoading: Boolean = true
) {
    val net: Double get() = income - expense
    val savingsRate: Double get() = if (income > 0) (net / income) * 100.0 else 0.0

    val peakDayIndex: Int
        get() = dailyTotals.indices.maxByOrNull { dailyTotals[it] } ?: 0

    val quietestDayIndex: Int
        get() {
            val visible = dailyTotals.take(todayDayIndex + 1)
            val minNonZero = visible.filter { it > 0 }.minOrNull() ?: return -1
            return visible.indexOf(minNonZero)
        }

    val daysRecorded: Int
        get() = dailyTotals.take(todayDayIndex + 1).count { it > 0 }

    val avgDailySpend: Double
        get() {
            val daysWithData = daysRecorded
            return if (daysWithData > 0) dailyTotals.take(todayDayIndex + 1).sum() / daysWithData else 0.0
        }
}

private data class InsightsRaw(
    val transactions: List<Transaction>,
    val currentExpense: List<CategorySummary>,
    val prevExpense: List<CategorySummary>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    init {
        SecureLogger.d(TAG, "InsightsViewModel initialized")
    }

    /**
     * The month Stats handed over, falling back to the current one. A malformed argument is
     * not worth crashing the screen for — the current month is always a sensible answer.
     */
    private val _month = MutableStateFlow(
        savedStateHandle.get<String>("month")
            ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            ?: YearMonth.now()
    )

    val uiState: StateFlow<InsightsUiState> = _month.flatMapLatest { ym ->
        val prevYm = ym.minusMonths(1)
        SecureLogger.d(TAG, "Loading insights data for month: $ym")
        combine(
            repository.getTransactionsByMonth(ym.toString()),
            repository.getCategorySummary(ym.atDay(1), ym.atEndOfMonth(), TransactionType.EXPENSE),
            repository.getCategorySummary(prevYm.atDay(1), prevYm.atEndOfMonth(), TransactionType.EXPENSE)
        ) { txns, curExpense, prevExpense ->
            buildState(txns, curExpense, prevExpense, ym)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsUiState())

    fun previousMonth() {
        val newMonth = _month.value.minusMonths(1)
        SecureLogger.d(TAG, "User navigated to previous month: $newMonth")
        _month.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        val newMonth = _month.value.plusMonths(1)
        SecureLogger.d(TAG, "User navigated to next month: $newMonth")
        _month.update { it.plusMonths(1) }
    }

    private fun buildState(
        txns: List<Transaction>,
        curExpense: List<CategorySummary>,
        prevExpense: List<CategorySummary>,
        ym: YearMonth
    ): InsightsUiState {
        val today = LocalDate.now()
        val daysInMonth = ym.lengthOfMonth()
        val todayDayIndex = if (today.year == ym.year && today.monthValue == ym.monthValue)
            today.dayOfMonth - 1
        else
            daysInMonth - 1

        var income = 0.0
        var expense = 0.0
        val dailyArr = DoubleArray(daysInMonth)
        val weekdayArr = DoubleArray(7)
        var biggestExpense: Transaction? = null
        val freqMap = mutableMapOf<Long, Int>()

        for (t in txns) {
            when (t.type) {
                TransactionType.INCOME -> income += t.amount
                TransactionType.EXPENSE -> {
                    expense += t.amount
                    val dayIdx = t.date.dayOfMonth - 1
                    if (dayIdx in 0 until daysInMonth) dailyArr[dayIdx] += t.amount
                    // Sun=7 → 0, Mon=1 → 1 ... Sat=6 → 6
                    val dow = t.date.dayOfWeek.value % 7
                    weekdayArr[dow] += t.amount
                    if (biggestExpense == null || t.amount > biggestExpense.amount) biggestExpense = t
                    freqMap[t.categoryId] = (freqMap[t.categoryId] ?: 0) + 1
                }
                else -> Unit
            }
        }

        val categoryNameMap = curExpense.associate { it.categoryId to it.categoryName }
        val topCatEntry = freqMap.maxByOrNull { it.value }

        val prevMap = prevExpense.associate { it.categoryId to it.totalAmount }
        val biggestMoM = curExpense.maxByOrNull { it.totalAmount - (prevMap[it.categoryId] ?: 0.0) }
        val biggestMoMDelta = biggestMoM?.let { it.totalAmount - (prevMap[it.categoryId] ?: 0.0) } ?: 0.0

        SecureLogger.d(TAG, "InsightsUiState calculated: month=$ym, income=$income, expense=$expense, days_in_month=$daysInMonth, biggest_expense=${biggestExpense?.amount}, top_category=${topCatEntry?.key}, mom_leader=${biggestMoM?.categoryName}")

        return InsightsUiState(
            income = income,
            expense = expense,
            dailyTotals = dailyArr.toList(),
            daysInMonth = daysInMonth,
            todayDayIndex = todayDayIndex,
            weekdayTotals = weekdayArr.toList(),
            biggestExpenseAmount = biggestExpense?.amount ?: 0.0,
            biggestExpenseCategoryName = biggestExpense?.let { categoryNameMap[it.categoryId] } ?: "",
            biggestExpenseDate = biggestExpense?.date,
            mostFrequentCategoryName = topCatEntry?.key?.let { categoryNameMap[it] } ?: "",
            mostFrequentCategoryCount = topCatEntry?.value ?: 0,
            biggestMoMCategoryName = biggestMoM?.categoryName ?: "",
            biggestMoMDelta = biggestMoMDelta,
            monthLabel = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            isLoading = false
        )
    }
}
