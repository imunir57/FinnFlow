package com.finnflow.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

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
            combine(
                repository.getCategorySummary(p.from, p.to, TransactionType.INCOME),
                repository.getCategorySummary(p.from, p.to, TransactionType.EXPENSE)
            ) { income, expense ->
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
        _params.update { it.copy(period = period, from = from, to = to) }
    }

    fun onCustomRangeChange(from: LocalDate, to: LocalDate) {
        _params.update { it.copy(period = StatsPeriod.CUSTOM, from = from, to = to) }
    }

    fun onTypeChange(type: TransactionType) {
        _params.update { it.copy(selectedType = type) }
    }

    val currentFrom: LocalDate get() = _params.value.from
    val currentTo: LocalDate get() = _params.value.to
    val currentType: TransactionType get() = _params.value.selectedType
}
