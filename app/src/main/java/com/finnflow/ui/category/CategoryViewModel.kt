package com.finnflow.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.TransactionType
import com.finnflow.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CategoryVM"

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

    /** One-shot user-facing messages, surfaced by the screen as a snackbar. */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    init {
        SecureLogger.d(TAG, "Initializing CategoryViewModel with parentCategoryId=$parentCategoryId")
        if (parentCategoryId != null) {
            SecureLogger.d(TAG, "Loading sub-categories for parent: $parentCategoryId")
            repository.getSubCategories(parentCategoryId)
                .onEach { subs ->
                    SecureLogger.d(TAG, "Sub-categories loaded: count=${subs.size}")
                    _state.update { it.copy(subCategories = subs, selectedCategoryId = parentCategoryId, isLoading = false) }
                }
                .launchIn(viewModelScope)
        } else {
            SecureLogger.d(TAG, "Loading all categories and sub-categories")
            combine(
                repository.getAllCategories(),
                repository.getAllSubCategories(),
                _selectedType
            ) { cats, subs, type -> CatRaw(cats, subs, type) }
                .onEach { raw ->
                    val subMap = raw.subCategories.groupBy { it.categoryId }
                    val filtered = raw.categories.filter { it.type == raw.selectedType }
                    SecureLogger.d(TAG, "Categories loaded: total=${raw.categories.size}, expense=${raw.categories.count { it.type == TransactionType.EXPENSE }}, income=${raw.categories.count { it.type == TransactionType.INCOME }}, filtered=${filtered.size}")
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
        SecureLogger.d(TAG, "Category type filter changed: type=$type")
        _selectedType.value = type
    }

    fun openEditSheet(category: Category? = null) {
        val action = if (category == null) "create new" else "edit (id=${category.id})"
        SecureLogger.d(TAG, "Opening edit sheet: action=$action")
        _state.update { it.copy(isEditSheetOpen = true, editingCategory = category, isNewCategory = category == null) }
    }

    fun closeEditSheet() {
        SecureLogger.d(TAG, "Closing edit sheet")
        _state.update { it.copy(isEditSheetOpen = false, editingCategory = null, isNewCategory = false) }
    }

    fun moveItem(fromIndex: Int, toIndex: Int) {
        val current = _state.value.displayItems.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) {
            SecureLogger.w(TAG, "Invalid move indices: fromIndex=$fromIndex, toIndex=$toIndex")
            return
        }
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        SecureLogger.d(TAG, "Category reordered: item=${item.category.name}, from=$fromIndex, to=$toIndex")
        _state.update { it.copy(displayItems = current) }
    }

    fun addCategory(name: String, type: TransactionType, iconName: String = "dots", colorHex: String = "#607D8B") {
        SecureLogger.d(TAG, "Creating new category: name=$name, type=$type, icon=$iconName")
        viewModelScope.launch {
            try {
                repository.addCategory(Category(name = name, type = type, iconName = iconName, colorHex = colorHex))
                SecureLogger.i(TAG, "Category created successfully: name=$name")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error creating category: name=$name", e)
            }
        }
    }

    fun updateCategory(category: Category) {
        SecureLogger.d(TAG, "Updating category: id=${category.id}, name=${category.name}, type=${category.type}")
        viewModelScope.launch {
            try {
                repository.updateCategory(category)
                SecureLogger.i(TAG, "Category updated successfully: id=${category.id}")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error updating category: id=${category.id}", e)
            }
        }
    }

    fun deleteCategory(category: Category) {
        SecureLogger.d(TAG, "Deleting category: id=${category.id}, name=${category.name}")
        viewModelScope.launch {
            try {
                repository.deleteCategory(category)
                SecureLogger.i(TAG, "Category deleted successfully: id=${category.id}")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error deleting category: id=${category.id}", e)
                // `transactions.categoryId` is ON DELETE RESTRICT, so a category still in use
                // by any transaction refuses to delete. Without this the user confirms the
                // dialog, the sheet closes, and nothing visibly happens.
                _messages.send(
                    "\"${category.name}\" is still used by existing transactions, so it can't " +
                        "be deleted. Move those transactions to another category first."
                )
            }
        }
    }

    fun addSubCategory(name: String) {
        val catId = parentCategoryId ?: run {
            SecureLogger.w(TAG, "Cannot add sub-category: parentCategoryId is null")
            return
        }
        SecureLogger.d(TAG, "Creating sub-category: name=$name, categoryId=$catId")
        viewModelScope.launch {
            try {
                repository.addSubCategory(SubCategory(categoryId = catId, name = name))
                SecureLogger.i(TAG, "Sub-category created successfully: name=$name, categoryId=$catId")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error creating sub-category: name=$name, categoryId=$catId", e)
            }
        }
    }

    fun updateSubCategory(subCategory: SubCategory) {
        SecureLogger.d(TAG, "Updating sub-category: id=${subCategory.id}, name=${subCategory.name}")
        viewModelScope.launch {
            try {
                repository.updateSubCategory(subCategory)
                SecureLogger.i(TAG, "Sub-category updated successfully: id=${subCategory.id}")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error updating sub-category: id=${subCategory.id}", e)
            }
        }
    }

    fun deleteSubCategory(subCategory: SubCategory) {
        SecureLogger.d(TAG, "Deleting sub-category: id=${subCategory.id}, name=${subCategory.name}")
        viewModelScope.launch {
            try {
                repository.deleteSubCategory(subCategory)
                SecureLogger.i(TAG, "Sub-category deleted successfully: id=${subCategory.id}")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error deleting sub-category: id=${subCategory.id}", e)
            }
        }
    }
}
