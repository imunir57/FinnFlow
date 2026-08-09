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
    /** Active categories of the selected type. */
    val displayItems: List<CategoryDisplayItem> = emptyList(),
    /** Archived categories of the selected type, listed below the active ones. */
    val archivedItems: List<CategoryDisplayItem> = emptyList(),
    val subCategories: List<SubCategory> = emptyList(),
    val archivedSubCategories: List<SubCategory> = emptyList(),
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
                    val (archived, active) = subs.partition { it.isArchived }
                    SecureLogger.d(TAG, "Sub-categories loaded: active=${active.size}, archived=${archived.size}")
                    _state.update {
                        it.copy(
                            subCategories = active,
                            archivedSubCategories = archived,
                            selectedCategoryId = parentCategoryId,
                            isLoading = false
                        )
                    }
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
                    val (archived, active) = filtered.partition { it.isArchived }
                    // The type-toggle counts are what is pickable, so archived ones are left out
                    // of them — they are counted by the Archived section's own header instead.
                    val activeOfType = { type: TransactionType ->
                        raw.categories.count { it.type == type && !it.isArchived }
                    }
                    SecureLogger.d(TAG, "Categories loaded: total=${raw.categories.size}, filtered=${filtered.size}, active=${active.size}, archived=${archived.size}")
                    val toDisplayItem = { cat: Category ->
                        val catSubs = subMap[cat.id] ?: emptyList()
                        CategoryDisplayItem(
                            category = cat,
                            subCount = catSubs.size,
                            subPreviewNames = catSubs.take(3).map { it.name }
                        )
                    }
                    _state.update { s ->
                        s.copy(
                            selectedType = raw.selectedType,
                            expenseCount = activeOfType(TransactionType.EXPENSE),
                            incomeCount = activeOfType(TransactionType.INCOME),
                            displayItems = active.map(toDisplayItem),
                            archivedItems = archived.map(toDisplayItem),
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

    /**
     * Retires [category] from the pickers, keeping it for the transactions already filed under
     * it. This is what the UI's delete action does — `transactions.categoryId` is ON DELETE
     * RESTRICT, so a category in use could never actually be deleted anyway.
     */
    fun archiveCategory(category: Category) {
        SecureLogger.d(TAG, "Archiving category: id=${category.id}, name=${category.name}")
        viewModelScope.launch {
            try {
                repository.setCategoryArchived(category.id, archived = true)
                SecureLogger.i(TAG, "Category archived successfully: id=${category.id}")
                _messages.send("\"${category.name}\" archived")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error archiving category: id=${category.id}", e)
                _messages.send("Couldn't archive \"${category.name}\"")
            }
        }
    }

    fun restoreCategory(category: Category) {
        SecureLogger.d(TAG, "Restoring category: id=${category.id}, name=${category.name}")
        viewModelScope.launch {
            try {
                repository.setCategoryArchived(category.id, archived = false)
                SecureLogger.i(TAG, "Category restored successfully: id=${category.id}")
                _messages.send("\"${category.name}\" restored")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error restoring category: id=${category.id}", e)
                _messages.send("Couldn't restore \"${category.name}\"")
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

    /**
     * Retires [subCategory] from the pickers. Deleting instead would set
     * `transactions.subCategoryId` to NULL and turn every past entry into "Uncategorised".
     */
    fun archiveSubCategory(subCategory: SubCategory) {
        SecureLogger.d(TAG, "Archiving sub-category: id=${subCategory.id}, name=${subCategory.name}")
        viewModelScope.launch {
            try {
                repository.setSubCategoryArchived(subCategory.id, archived = true)
                SecureLogger.i(TAG, "Sub-category archived successfully: id=${subCategory.id}")
                _messages.send("\"${subCategory.name}\" archived")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error archiving sub-category: id=${subCategory.id}", e)
                _messages.send("Couldn't archive \"${subCategory.name}\"")
            }
        }
    }

    fun restoreSubCategory(subCategory: SubCategory) {
        SecureLogger.d(TAG, "Restoring sub-category: id=${subCategory.id}, name=${subCategory.name}")
        viewModelScope.launch {
            try {
                repository.setSubCategoryArchived(subCategory.id, archived = false)
                SecureLogger.i(TAG, "Sub-category restored successfully: id=${subCategory.id}")
                _messages.send("\"${subCategory.name}\" restored")
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Error restoring sub-category: id=${subCategory.id}", e)
                _messages.send("Couldn't restore \"${subCategory.name}\"")
            }
        }
    }
}
