package com.finnflow.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TransactionFormState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val categoryId: Long? = null,
    val subCategoryId: Long? = null,
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val subCategories: List<SubCategory> = emptyList(),
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

    init {
        loadCategories()
        transactionId?.let { loadTransaction(it) }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepo.getCategoriesByType(_state.value.type).collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }
    }

    private fun loadTransaction(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            transactionRepo.getTransactionById(id)?.let { tx ->
                _state.update { s ->
                    s.copy(
                        type = tx.type,
                        amount = tx.amount.toString(),
                        date = tx.date,
                        categoryId = tx.categoryId,
                        subCategoryId = tx.subCategoryId,
                        note = tx.note,
                        isLoading = false
                    )
                }
                loadSubCategories(tx.categoryId)
            } ?: _state.update { it.copy(isLoading = false, error = "Transaction not found") }
        }
    }

    private fun loadSubCategories(categoryId: Long) {
        viewModelScope.launch {
            categoryRepo.getSubCategories(categoryId).collect { subs ->
                _state.update { it.copy(subCategories = subs) }
            }
        }
    }

    fun onTypeChange(type: TransactionType) {
        _state.update { it.copy(type = type, categoryId = null, subCategoryId = null, subCategories = emptyList()) }
        loadCategories()
    }

    fun onAmountChange(amount: String) = _state.update { it.copy(amount = amount) }
    fun onDateChange(date: LocalDate) = _state.update { it.copy(date = date) }
    fun onNoteChange(note: String) = _state.update { it.copy(note = note) }

    fun onCategoryChange(categoryId: Long) {
        _state.update { it.copy(categoryId = categoryId, subCategoryId = null, subCategories = emptyList()) }
        loadSubCategories(categoryId)
    }

    fun onSubCategoryChange(subCategoryId: Long?) = _state.update { it.copy(subCategoryId = subCategoryId) }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
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
            if (transactionId == null) transactionRepo.addTransaction(transaction)
            else transactionRepo.updateTransaction(transaction)
            _state.update { it.copy(isLoading = false, isSaved = true) }
        }
    }
}
