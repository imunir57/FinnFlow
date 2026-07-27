package com.finnflow.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "TransactionVM"

data class TransactionFormState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val dateChipIndex: Int = 0,
    val categoryId: Long? = null,
    val subCategoryId: Long? = null,
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val subCategories: List<SubCategory> = emptyList(),
    val isLoadingSubCategories: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val amountError get() = when {
        amount.isEmpty() -> null
        amount.toDoubleOrNull() == null -> "Invalid amount"
        amount.toDouble() <= 0 -> "Amount must be positive"
        else -> null
    }
    val isValid get() = amountError == null && amount.isNotEmpty() && categoryId != null
}

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Long? = savedStateHandle.get<Long>("transactionId")
    private val _state = MutableStateFlow(TransactionFormState())
    val state: StateFlow<TransactionFormState> = _state.asStateFlow()

    // Each load replaces the previous collector. Without cancelling, switching type or
    // category leaves the old Flow collecting and a late emission overwrites the list
    // with the previously selected category's data.
    private var categoriesJob: Job? = null
    private var subCategoriesJob: Job? = null

    init {
        SecureLogger.d(TAG, "Initializing TransactionViewModel for transactionId=$transactionId")
        loadCategories()
        transactionId?.let { loadTransaction(it) }
    }

    private fun loadCategories() {
        SecureLogger.d(TAG, "Loading categories for type=${_state.value.type}")
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            try {
                categoryRepo.getCategoriesByType(_state.value.type).collect { cats ->
                    SecureLogger.d(TAG, "Categories loaded: count=${cats.size}")
                    _state.update { it.copy(categories = cats) }
                }
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error loading categories", e)
            }
        }
    }

    private fun loadTransaction(id: Long) {
        SecureLogger.d(TAG, "Loading transaction with id=$id")
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                transactionRepo.getTransactionById(id)?.let { tx ->
                    SecureLogger.d(TAG, "Transaction loaded: type=${tx.type}, categoryId=${tx.categoryId}")
                    _state.update { s ->
                        s.copy(
                            type = tx.type,
                            amount = tx.amount.toString(),
                            date = tx.date,
                            categoryId = tx.categoryId,
                            subCategoryId = tx.subCategoryId,
                            note = tx.note,
                            isLoading = false,
                            isLoadingSubCategories = true
                        )
                    }
                    loadSubCategories(tx.categoryId)
                } ?: run {
                    val errorMsg = "Transaction not found"
                    SecureLogger.w(TAG, errorMsg)
                    _state.update { it.copy(isLoading = false, error = errorMsg) }
                }
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error loading transaction id=$id", e)
                _state.update { it.copy(isLoading = false, error = "Failed to load transaction") }
            }
        }
    }

    private fun loadSubCategories(categoryId: Long) {
        SecureLogger.d(TAG, "Loading sub-categories for categoryId=$categoryId")
        subCategoriesJob?.cancel()
        subCategoriesJob = viewModelScope.launch {
            try {
                categoryRepo.getSubCategories(categoryId).collect { subs ->
                    SecureLogger.d(TAG, "Sub-categories loaded: count=${subs.size}")
                    _state.update { it.copy(subCategories = subs, isLoadingSubCategories = false) }
                }
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error loading sub-categories for categoryId=$categoryId", e)
            }
        }
    }

    fun onTypeChange(type: TransactionType) {
        SecureLogger.d(TAG, "Transaction type changed: type=$type")
        subCategoriesJob?.cancel()
        _state.update {
            it.copy(
                type = type,
                categoryId = null,
                subCategoryId = null,
                subCategories = emptyList(),
                isLoadingSubCategories = false
            )
        }
        loadCategories()
    }

    fun onAmountChange(amount: String) = _state.update { it.copy(amount = amount) }

    fun onAmountDigit(digit: String) = _state.update { s ->
        val current = s.amount
        val next = when {
            current.isEmpty() || current == "0" -> digit
            else -> current + digit
        }
        s.copy(amount = next)
    }

    fun onAmountBackspace() = _state.update { s ->
        s.copy(amount = s.amount.dropLast(1))
    }

    /** No-op once the amount already has a decimal point; seeds a leading zero if empty. */
    fun onAmountDecimal() = _state.update { s ->
        when {
            s.amount.contains('.') -> s
            s.amount.isEmpty()     -> s.copy(amount = "0.")
            else                   -> s.copy(amount = s.amount + ".")
        }
    }

    fun onAmountClear() = _state.update { it.copy(amount = "") }

    fun onDateChipChange(index: Int) {
        val today = LocalDate.now()
        val newDate = when (index) {
            0    -> today
            1    -> today.minusDays(1)
            2    -> today.minusDays(2)
            else -> _state.value.date
        }
        _state.update { it.copy(dateChipIndex = index, date = newDate) }
    }

    fun onDateChange(date: LocalDate) = _state.update { it.copy(date = date, dateChipIndex = 3) }
    fun onNoteChange(note: String) = _state.update { it.copy(note = note) }

    fun onCategoryChange(categoryId: Long) {
        SecureLogger.d(TAG, "Category selected: categoryId=$categoryId")
        _state.update {
            it.copy(
                categoryId = categoryId,
                subCategoryId = null,
                subCategories = emptyList(),
                isLoadingSubCategories = true
            )
        }
        loadSubCategories(categoryId)
    }

    fun onSubCategoryChange(subCategoryId: Long?) {
        SecureLogger.d(TAG, "Sub-category selected: subCategoryId=$subCategoryId")
        _state.update { it.copy(subCategoryId = subCategoryId) }
    }

    fun save() {
        val s = _state.value
        if (!s.isValid) {
            SecureLogger.w(TAG, "Save attempt with invalid form state: amountError=${s.amountError}")
            return
        }
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val transaction = Transaction(
                    id = transactionId ?: 0L,
                    type = s.type,
                    amount = s.amount.toDouble(),
                    date = s.date,
                    categoryId = s.categoryId!!,
                    subCategoryId = s.subCategoryId,
                    note = s.note
                )
                if (transactionId == null) {
                    SecureLogger.d(TAG, "Creating new transaction: type=${s.type}, categoryId=${s.categoryId}")
                    transactionRepo.addTransaction(transaction)
                    SecureLogger.i(TAG, "New transaction saved successfully")
                } else {
                    SecureLogger.d(TAG, "Updating transaction id=$transactionId: type=${s.type}")
                    transactionRepo.updateTransaction(transaction)
                    SecureLogger.i(TAG, "Transaction id=$transactionId updated successfully")
                }
                _state.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error saving transaction", e)
                _state.update { it.copy(isLoading = false, error = "Failed to save transaction") }
            }
        }
    }
}
