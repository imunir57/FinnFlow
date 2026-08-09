package com.finnflow.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.profile.UserProfileRepository
import com.finnflow.data.repository.CategoryRepository
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "HomeViewModel"

data class HomeUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val dailyGroups: Map<LocalDate, List<Transaction>> = emptyMap(),
    val categories: Map<Long, Category> = emptyMap(),
    val subCategories: Map<Long, SubCategory> = emptyMap(),
    val isLoading: Boolean = true,
    val displayName: String = "",
    val initials: String = "?"
) {
    val balance get() = totalIncome - totalExpense
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val profileRepo: UserProfileRepository
) : ViewModel() {

    init {
        SecureLogger.d(TAG, "HomeViewModel initialized")
    }

    private val _selectedMonth = MutableStateFlow(YearMonth.now())

    private data class TxData(val month: YearMonth, val txs: List<Transaction>, val groups: Map<LocalDate, List<Transaction>>)

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedMonth.flatMapLatest { month ->
            val yearMonth = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            SecureLogger.d(TAG, "Loading transactions for month: $yearMonth")
            repository.getTransactionsByMonth(yearMonth).map { txs ->
                SecureLogger.d(TAG, "Loaded ${txs.size} transactions for $yearMonth")
                TxData(month, txs, txs.groupBy { it.date })
            }
        },
        categoryRepo.getAllCategories(),
        categoryRepo.getAllSubCategories(),
        profileRepo.profile
    ) { txData, cats, subCats, profile ->
        val income = txData.txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = txData.txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        SecureLogger.d(TAG, "HomeUiState computed: month=${txData.month}, tx_count=${txData.txs.size}, income_sum=$income, expense_sum=$expense, daily_groups=${txData.groups.size}")
        HomeUiState(
            selectedMonth = txData.month,
            transactions  = txData.txs,
            totalIncome   = income,
            totalExpense  = expense,
            dailyGroups   = txData.groups,
            categories    = cats.associateBy { it.id },
            subCategories = subCats.associateBy { it.id },
            isLoading     = false,
            displayName   = profile.displayName,
            initials      = profile.initials.ifBlank { "?" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun previousMonth() {
        val newMonth = _selectedMonth.value.minusMonths(1)
        SecureLogger.d(TAG, "User navigated to previous month: $newMonth")
        _selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        val newMonth = _selectedMonth.value.plusMonths(1)
        SecureLogger.d(TAG, "User navigated to next month: $newMonth")
        _selectedMonth.update { it.plusMonths(1) }
    }

    fun deleteTransaction(transaction: Transaction) {
        SecureLogger.d(TAG, "Deleting transaction: id=${transaction.id}, category=${transaction.categoryId}")
        viewModelScope.launch {
            try {
                repository.deleteTransaction(transaction)
                SecureLogger.d(TAG, "Transaction deleted successfully: id=${transaction.id}")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to delete transaction: id=${transaction.id}", e)
            }
        }
    }
}
