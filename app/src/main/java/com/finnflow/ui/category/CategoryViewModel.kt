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

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val subCategories: List<SubCategory> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // When navigated to sub-category management, categoryId is passed
    val parentCategoryId: Long? = savedStateHandle.get<Long>("categoryId")

    private val _state = MutableStateFlow(CategoryUiState())
    val state: StateFlow<CategoryUiState> = _state.asStateFlow()

    init {
        if (parentCategoryId != null) loadSubCategories(parentCategoryId)
        else loadCategories()
    }

    private fun loadCategories() {
        repository.getAllCategories()
            .onEach { cats -> _state.update { it.copy(categories = cats, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    private fun loadSubCategories(categoryId: Long) {
        repository.getSubCategories(categoryId)
            .onEach { subs -> _state.update { it.copy(subCategories = subs, selectedCategoryId = categoryId, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun addCategory(name: String, type: TransactionType, colorHex: String = "#607D8B") {
        viewModelScope.launch {
            repository.addCategory(Category(name = name, type = type, colorHex = colorHex))
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
