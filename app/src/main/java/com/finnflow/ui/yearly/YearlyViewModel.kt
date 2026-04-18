package com.finnflow.ui.yearly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.db.dao.MonthlyTotal
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

data class YearlyUiState(
    val year: Int = LocalDate.now().year,
    val incomeByMonth: List<MonthlyTotal> = emptyList(),
    val expenseByMonth: List<MonthlyTotal> = emptyList(),
    val isLoading: Boolean = true
) {
    val totalIncome get() = incomeByMonth.sumOf { it.total }
    val totalExpense get() = expenseByMonth.sumOf { it.total }
    val netBalance get() = totalIncome - totalExpense
}

@HiltViewModel
class YearlyViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _year = MutableStateFlow(LocalDate.now().year)
    val state: StateFlow<YearlyUiState> = _year.flatMapLatest { year ->
        combine(
            repository.getMonthlyTotalsByYear(year.toString(), TransactionType.INCOME),
            repository.getMonthlyTotalsByYear(year.toString(), TransactionType.EXPENSE)
        ) { income, expense ->
            YearlyUiState(year = year, incomeByMonth = income, expenseByMonth = expense, isLoading = false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YearlyUiState())

    fun previousYear() = _year.update { it - 1 }
    fun nextYear() = _year.update { it + 1 }
}
