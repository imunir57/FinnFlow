package com.finnflow.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val dailyGroups: Map<LocalDate, List<Transaction>> = emptyMap(),
    val isLoading: Boolean = true
) {
    val balance get() = totalIncome - totalExpense
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<HomeUiState> = _selectedMonth
        .flatMapLatest { month ->
            val yearMonth = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            repository.getTransactionsByMonth(yearMonth)
                .map { transactions ->
                    HomeUiState(
                        selectedMonth = month,
                        transactions = transactions,
                        totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                        totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                        dailyGroups = transactions.groupBy { it.date },
                        isLoading = false
                    )
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun previousMonth() {
        _selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        _selectedMonth.update { it.plusMonths(1) }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
