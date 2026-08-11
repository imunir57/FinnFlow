package com.finnflow.data.repository

import com.finnflow.data.db.dao.CategoryDao
import com.finnflow.data.db.entity.CategoryEntity
import com.finnflow.data.db.entity.SubCategoryEntity
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Categories are archived, never deleted.
 *
 * Both tables are referenced by `transactions`, so removing a row either fails outright
 * (`categoryId` is ON DELETE RESTRICT) or silently rewrites history (`subCategoryId` is
 * ON DELETE SET NULL). Archiving retires a category from the pickers while every past
 * transaction keeps the label it was filed under, and Stats keeps reporting it wherever it
 * has amounts.
 */
interface CategoryRepository {
    suspend fun addCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun setCategoryArchived(categoryId: Long, archived: Boolean)
    suspend fun getCategoryById(id: Long): Category?

    /** Every category, archived included — for resolving a transaction's category by id. */
    fun getAllCategories(): Flow<List<Category>>

    /** Pickable categories of a type, archived ones excluded. */
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    /**
     * Persists [categoryIdsInOrder] as the list order, positions 0..n-1.
     *
     * Callers pass the complete list of the type being reordered, not just the moved row — see
     * `CategoryDao.setCategoryOrder`.
     */
    suspend fun reorderCategories(categoryIdsInOrder: List<Long>)

    suspend fun addSubCategory(subCategory: SubCategory): Long
    suspend fun updateSubCategory(subCategory: SubCategory)
    suspend fun setSubCategoryArchived(subCategoryId: Long, archived: Boolean)

    /** Every sub-category of a parent, archived included — for the management screen. */
    fun getSubCategories(categoryId: Long): Flow<List<SubCategory>>

    /** Pickable sub-categories of a parent, archived ones excluded. */
    fun getActiveSubCategories(categoryId: Long): Flow<List<SubCategory>>

    /** Persists [subCategoryIdsInOrder] as the list order within their parent, positions 0..n-1. */
    suspend fun reorderSubCategories(subCategoryIdsInOrder: List<Long>)

    fun getAllSubCategories(): Flow<List<SubCategory>>
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {
    companion object {
        private const val TAG = "CategoryRepository"
    }

    override suspend fun addCategory(category: Category): Long {
        SecureLogger.d(TAG, "Starting addCategory operation for category: ${category.name}, type: ${category.type}")
        return try {
            val categoryId = dao.insertCategory(CategoryEntity.fromDomain(category))
            SecureLogger.d(TAG, "Category inserted successfully with ID: $categoryId, name: ${category.name}")
            categoryId
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to add category: ${category.name}", e)
            throw e
        }
    }

    override suspend fun updateCategory(category: Category) {
        SecureLogger.d(TAG, "Starting updateCategory operation for ID: ${category.id}, name: ${category.name}")
        try {
            dao.updateCategory(CategoryEntity.fromDomain(category))
            SecureLogger.d(TAG, "Category updated successfully, ID: ${category.id}, name: ${category.name}")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to update category with ID: ${category.id}", e)
            throw e
        }
    }

    override suspend fun setCategoryArchived(categoryId: Long, archived: Boolean) {
        val action = if (archived) "archive" else "restore"
        SecureLogger.d(TAG, "Starting $action operation for category ID: $categoryId")
        try {
            dao.setCategoryArchived(categoryId, archived)
            SecureLogger.i(TAG, "Category ${action}d successfully, ID: $categoryId")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to $action category with ID: $categoryId", e)
            throw e
        }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        SecureLogger.d(TAG, "Starting getCategoryById for ID: $id")
        return try {
            val category = dao.getCategoryById(id)?.toDomain()
            if (category != null) {
                SecureLogger.d(TAG, "Category found with ID: $id, name: ${category.name}")
            } else {
                SecureLogger.d(TAG, "No category found with ID: $id")
            }
            category
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to get category by ID: $id", e)
            throw e
        }
    }

    override fun getAllCategories(): Flow<List<Category>> {
        SecureLogger.d(TAG, "Starting getAllCategories query")
        return dao.getAllCategories().map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} categories from database")
            list.map { it.toDomain() }
        }
    }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        SecureLogger.d(TAG, "Starting getCategoriesByType query for type: $type")
        return dao.getCategoriesByType(type).map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} categories for type: $type")
            list.map { it.toDomain() }
        }
    }

    override suspend fun reorderCategories(categoryIdsInOrder: List<Long>) {
        SecureLogger.d(TAG, "Starting reorderCategories for ${categoryIdsInOrder.size} categories")
        try {
            dao.setCategoryOrder(categoryIdsInOrder)
            SecureLogger.i(TAG, "Category order saved for ${categoryIdsInOrder.size} categories")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to save category order", e)
            throw e
        }
    }

    override suspend fun addSubCategory(subCategory: SubCategory): Long {
        SecureLogger.d(TAG, "Starting addSubCategory operation for: ${subCategory.name}, categoryId: ${subCategory.categoryId}")
        return try {
            val subCategoryId = dao.insertSubCategory(SubCategoryEntity.fromDomain(subCategory))
            SecureLogger.d(TAG, "SubCategory inserted successfully with ID: $subCategoryId, name: ${subCategory.name}, categoryId: ${subCategory.categoryId}")
            subCategoryId
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to add subcategory: ${subCategory.name}", e)
            throw e
        }
    }

    override suspend fun updateSubCategory(subCategory: SubCategory) {
        SecureLogger.d(TAG, "Starting updateSubCategory operation for ID: ${subCategory.id}, name: ${subCategory.name}")
        try {
            dao.updateSubCategory(SubCategoryEntity.fromDomain(subCategory))
            SecureLogger.d(TAG, "SubCategory updated successfully, ID: ${subCategory.id}, name: ${subCategory.name}")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to update subcategory with ID: ${subCategory.id}", e)
            throw e
        }
    }

    override suspend fun setSubCategoryArchived(subCategoryId: Long, archived: Boolean) {
        val action = if (archived) "archive" else "restore"
        SecureLogger.d(TAG, "Starting $action operation for sub-category ID: $subCategoryId")
        try {
            dao.setSubCategoryArchived(subCategoryId, archived)
            SecureLogger.i(TAG, "SubCategory ${action}d successfully, ID: $subCategoryId")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to $action subcategory with ID: $subCategoryId", e)
            throw e
        }
    }

    override fun getSubCategories(categoryId: Long): Flow<List<SubCategory>> {
        SecureLogger.d(TAG, "Starting getSubCategories query for categoryId: $categoryId")
        return dao.getSubCategories(categoryId).map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} subcategories for categoryId: $categoryId")
            list.map { it.toDomain() }
        }
    }

    override fun getActiveSubCategories(categoryId: Long): Flow<List<SubCategory>> {
        SecureLogger.d(TAG, "Starting getActiveSubCategories query for categoryId: $categoryId")
        return dao.getActiveSubCategories(categoryId).map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} active subcategories for categoryId: $categoryId")
            list.map { it.toDomain() }
        }
    }

    override suspend fun reorderSubCategories(subCategoryIdsInOrder: List<Long>) {
        SecureLogger.d(TAG, "Starting reorderSubCategories for ${subCategoryIdsInOrder.size} sub-categories")
        try {
            dao.setSubCategoryOrder(subCategoryIdsInOrder)
            SecureLogger.i(TAG, "Sub-category order saved for ${subCategoryIdsInOrder.size} sub-categories")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to save sub-category order", e)
            throw e
        }
    }

    override fun getAllSubCategories(): Flow<List<SubCategory>> {
        SecureLogger.d(TAG, "Starting getAllSubCategories query")
        return dao.getAllSubCategories().map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} total subcategories from database")
            list.map { it.toDomain() }
        }
    }
}
