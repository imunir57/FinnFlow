package com.finnflow.ui.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.model.SubCategorySummary
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import com.finnflow.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CategoryDetailUiState(
    val categoryId: Long = 0L,
    val categoryName: String = "",
    val from: LocalDate = LocalDate.now(),
    val to: LocalDate = LocalDate.now(),
    val type: TransactionType = TransactionType.EXPENSE,
    val summaries: List<SubCategorySummary> = emptyList(),
    val totalAmount: Double = 0.0,
    val expandedSubCategoryId: Long? = NONE_EXPANDED,
    // Lazily loaded transactions per subcategoryId key; null key = "Uncategorised"
    val transactionsBySubCategory: Map<Long?, List<Transaction>> = emptyMap(),
    val isLoading: Boolean = true
) {
    companion object {
        // Sentinel: nothing expanded
        const val NONE_EXPANDED = -1L
    }

    val isExpanded: (Long?) -> Boolean = { subCatId ->
        when {
            subCatId == null -> expandedSubCategoryId == null
            else -> expandedSubCategoryId == subCatId
        }
    }

    fun percentOf(amount: Double): Int =
        if (totalAmount > 0) (amount / totalAmount * 100).toInt() else 0
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val categoryId: Long = checkNotNull(savedStateHandle["categoryId"])
    private val from: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle["from"]))
    private val to: LocalDate = LocalDate.parse(checkNotNull(savedStateHandle["to"]))
    private val type: TransactionType = TransactionType.valueOf(
        checkNotNull(savedStateHandle["type"])
    )

    private val _expandedSubCategoryId = MutableStateFlow<Long?>(CategoryDetailUiState.NONE_EXPANDED)
    private val _transactionsBySubCategory = MutableStateFlow<Map<Long?, List<Transaction>>>(emptyMap())

    val state: StateFlow<CategoryDetailUiState> = combine(
        transactionRepo.getSubCategorySummary(categoryId, from, to, type),
        _expandedSubCategoryId,
        _transactionsBySubCategory
    ) { summaries, expandedId, txMap ->
        val total = summaries.sumOf { it.totalAmount }
        CategoryDetailUiState(
            categoryId = categoryId,
            from = from,
            to = to,
            type = type,
            summaries = summaries,
            totalAmount = total,
            expandedSubCategoryId = expandedId,
            transactionsBySubCategory = txMap,
            isLoading = false
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryDetailUiState())

    init {
        // Load category name separately
        viewModelScope.launch {
            categoryRepo.getCategoryById(categoryId)?.let { cat ->
                // Update once; categoryName is not in the combine above to keep it simple
                _categoryName.value = cat.name
            }
        }
    }

    private val _categoryName = MutableStateFlow("")
    val categoryName: StateFlow<String> = _categoryName.asStateFlow()

    fun toggleSubCategory(subCategoryId: Long?) {
        val current = _expandedSubCategoryId.value
        val isCurrentlyExpanded = when {
            subCategoryId == null -> current == null
            else -> current == subCategoryId
        }

        if (isCurrentlyExpanded) {
            // Collapse
            _expandedSubCategoryId.value = CategoryDetailUiState.NONE_EXPANDED
        } else {
            // Expand and load transactions if not yet cached
            _expandedSubCategoryId.value = subCategoryId ?: CategoryDetailUiState.NONE_EXPANDED.also {
                // Use null sentinel separately
                _expandedSubCategoryId.value = null
            }
            loadTransactionsIfNeeded(subCategoryId)
        }
    }

    private fun loadTransactionsIfNeeded(subCategoryId: Long?) {
        if (_transactionsBySubCategory.value.containsKey(subCategoryId)) return
        viewModelScope.launch {
            transactionRepo.getTransactionsBySubCategory(
                categoryId, subCategoryId, from, to, type
            ).collect { txList ->
                _transactionsBySubCategory.update { map ->
                    map + (subCategoryId to txList)
                }
            }
        }
    }
}
