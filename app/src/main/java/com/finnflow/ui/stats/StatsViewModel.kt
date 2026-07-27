package com.finnflow.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

private const val TAG = "StatsViewModel"

enum class StatsPeriod { MONTHLY, ANNUALLY, CUSTOM }

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.MONTHLY,
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val from: LocalDate = YearMonth.now().atDay(1),
    val to: LocalDate = YearMonth.now().atEndOfMonth(),
    val incomeSummary: List<CategorySummary> = emptyList(),
    val expenseSummary: List<CategorySummary> = emptyList(),
    val isLoading: Boolean = true
) {
    val activeSummary: List<CategorySummary>
        get() = if (selectedType == TransactionType.INCOME) incomeSummary else expenseSummary

    val totalAmount: Double
        get() = activeSummary.sumOf { it.totalAmount }

    fun percentOf(amount: Double): Int =
        if (totalAmount > 0) (amount / totalAmount * 100).toInt() else 0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    init {
        SecureLogger.d(TAG, "StatsViewModel initialized")
    }

    private data class QueryParams(
        val period: StatsPeriod,
        val from: LocalDate,
        val to: LocalDate,
        val selectedType: TransactionType
    )

    private val _params = MutableStateFlow(
        QueryParams(
            period = StatsPeriod.MONTHLY,
            from = YearMonth.now().atDay(1),
            to = YearMonth.now().atEndOfMonth(),
            selectedType = TransactionType.EXPENSE
        )
    )

    val state: StateFlow<StatsUiState> = _params
        .flatMapLatest { p ->
            SecureLogger.d(TAG, "Loading stats for period=${p.period}, type=${p.selectedType}, range=${p.from} to ${p.to}")
            combine(
                repository.getCategorySummary(p.from, p.to, TransactionType.INCOME),
                repository.getCategorySummary(p.from, p.to, TransactionType.EXPENSE)
            ) { income, expense ->
                SecureLogger.d(TAG, "Stats computed: period=${p.period}, income_categories=${income.size}, expense_categories=${expense.size}")
                StatsUiState(
                    period = p.period,
                    selectedType = p.selectedType,
                    from = p.from,
                    to = p.to,
                    incomeSummary = income,
                    expenseSummary = expense,
                    isLoading = false
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun onPeriodChange(period: StatsPeriod) {
        val today = LocalDate.now()
        val (from, to) = when (period) {
            StatsPeriod.MONTHLY  -> YearMonth.now().atDay(1) to YearMonth.now().atEndOfMonth()
            StatsPeriod.ANNUALLY -> today.withDayOfYear(1) to today.withDayOfYear(today.lengthOfYear())
            StatsPeriod.CUSTOM   -> _params.value.from to _params.value.to
        }
        SecureLogger.d(TAG, "Period changed to $period, range: $from to $to")
        _params.update { it.copy(period = period, from = from, to = to) }
    }

    fun onCustomRangeChange(from: LocalDate, to: LocalDate) {
        SecureLogger.d(TAG, "Custom range changed: $from to $to")
        _params.update { it.copy(period = StatsPeriod.CUSTOM, from = from, to = to) }
    }

    fun onTypeChange(type: TransactionType) {
        SecureLogger.d(TAG, "Transaction type filter changed to: $type")
        _params.update { it.copy(selectedType = type) }
    }

    fun previousPeriod() {
        SecureLogger.d(TAG, "User navigated to previous period")
        _params.update { p ->
            when (p.period) {
                StatsPeriod.MONTHLY -> {
                    val ym = YearMonth.from(p.from).minusMonths(1)
                    SecureLogger.d(TAG, "Previous month: $ym")
                    p.copy(from = ym.atDay(1), to = ym.atEndOfMonth())
                }
                StatsPeriod.ANNUALLY -> {
                    val y = p.from.year - 1
                    SecureLogger.d(TAG, "Previous year: $y")
                    p.copy(from = LocalDate.of(y, 1, 1), to = LocalDate.of(y, 12, 31))
                }
                StatsPeriod.CUSTOM -> p
            }
        }
    }

    fun nextPeriod() {
        SecureLogger.d(TAG, "User navigated to next period")
        _params.update { p ->
            when (p.period) {
                StatsPeriod.MONTHLY -> {
                    val ym = YearMonth.from(p.from).plusMonths(1)
                    SecureLogger.d(TAG, "Next month: $ym")
                    p.copy(from = ym.atDay(1), to = ym.atEndOfMonth())
                }
                StatsPeriod.ANNUALLY -> {
                    val y = p.from.year + 1
                    SecureLogger.d(TAG, "Next year: $y")
                    p.copy(from = LocalDate.of(y, 1, 1), to = LocalDate.of(y, 12, 31))
                }
                StatsPeriod.CUSTOM -> p
            }
        }
    }

    fun onPickedDate(date: LocalDate) {
        SecureLogger.d(TAG, "User picked date: $date")
        _params.update { p ->
            when (p.period) {
                StatsPeriod.MONTHLY -> {
                    val ym = YearMonth.from(date)
                    p.copy(from = ym.atDay(1), to = ym.atEndOfMonth())
                }
                StatsPeriod.ANNUALLY -> {
                    val y = date.year
                    p.copy(from = LocalDate.of(y, 1, 1), to = LocalDate.of(y, 12, 31))
                }
                StatsPeriod.CUSTOM -> p
            }
        }
    }

    val currentFrom: LocalDate get() = _params.value.from
    val currentTo: LocalDate get() = _params.value.to
    val currentType: TransactionType get() = _params.value.selectedType
}
