package com.finnflow.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryDisplayItem(
    val category: Category,
    val subCount: Int,
    val subPreviewNames: List<String>
)

data class CategoryUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val expenseCount: Int = 0,
    val incomeCount: Int = 0,
    val displayItems: List<CategoryDisplayItem> = emptyList(),
    val subCategories: List<SubCategory> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isEditSheetOpen: Boolean = false,
    val editingCategory: Category? = null,
    val isNewCategory: Boolean = false,
    val isLoading: Boolean = true
)

private data class CatRaw(
    val categories: List<Category>,
    val subCategories: List<SubCategory>,
    val selectedType: TransactionType
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val parentCategoryId: Long? = savedStateHandle.get<Long>("categoryId")

    private val _selectedType = MutableStateFlow(TransactionType.EXPENSE)
    private val _state = MutableStateFlow(CategoryUiState())
    val state: StateFlow<CategoryUiState> = _state.asStateFlow()

    init {
        if (parentCategoryId != null) {
            repository.getSubCategories(parentCategoryId)
                .onEach { subs ->
                    _state.update { it.copy(subCategories = subs, selectedCategoryId = parentCategoryId, isLoading = false) }
                }
                .launchIn(viewModelScope)
        } else {
            combine(
                repository.getAllCategories(),
                repository.getAllSubCategories(),
                _selectedType
            ) { cats, subs, type -> CatRaw(cats, subs, type) }
                .onEach { raw ->
                    val subMap = raw.subCategories.groupBy { it.categoryId }
                    val filtered = raw.categories.filter { it.type == raw.selectedType }
                    _state.update { s ->
                        s.copy(
                            selectedType = raw.selectedType,
                            expenseCount = raw.categories.count { it.type == TransactionType.EXPENSE },
                            incomeCount = raw.categories.count { it.type == TransactionType.INCOME },
                            displayItems = filtered.map { cat ->
                                val catSubs = subMap[cat.id] ?: emptyList()
                                CategoryDisplayItem(
                                    category = cat,
                                    subCount = catSubs.size,
                                    subPreviewNames = catSubs.take(3).map { it.name }
                                )
                            },
                            isLoading = false
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun setSelectedType(type: TransactionType) {
        _selectedType.value = type
    }

    fun openEditSheet(category: Category? = null) {
        _state.update { it.copy(isEditSheetOpen = true, editingCategory = category, isNewCategory = category == null) }
    }

    fun closeEditSheet() {
        _state.update { it.copy(isEditSheetOpen = false, editingCategory = null, isNewCategory = false) }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val current = _state.value.displayItems.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _state.update { it.copy(displayItems = current) }
    }

    fun addCategory(name: String, type: TransactionType, iconName: String = "dots", colorHex: String = "#607D8B") {
        viewModelScope.launch {
            repository.addCategory(Category(name = name, type = type, iconName = iconName, colorHex = colorHex))
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch { repository.updateCategory(category) }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }

    fun addSubCategory(name: String) {
        val catId = parentCategoryId ?: return
        viewModelScope.launch {
            repository.addSubCategory(SubCategory(categoryId = catId, name = name))
        }
    }

    fun updateSubCategory(subCategory: SubCategory) {
        viewModelScope.launch { repository.updateSubCategory(subCategory) }
    }

    fun deleteSubCategory(subCategory: SubCategory) {
        viewModelScope.launch { repository.deleteSubCategory(subCategory) }
    }
}
