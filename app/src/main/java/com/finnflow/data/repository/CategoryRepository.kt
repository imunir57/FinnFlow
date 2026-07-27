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

interface CategoryRepository {
    suspend fun addCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun getCategoryById(id: Long): Category?
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>

    suspend fun addSubCategory(subCategory: SubCategory): Long
    suspend fun updateSubCategory(subCategory: SubCategory)
    suspend fun deleteSubCategory(subCategory: SubCategory)
    fun getSubCategories(categoryId: Long): Flow<List<SubCategory>>
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

    override suspend fun deleteCategory(category: Category) {
        SecureLogger.d(TAG, "Starting deleteCategory operation for ID: ${category.id}, name: ${category.name}")
        try {
            dao.deleteCategory(CategoryEntity.fromDomain(category))
            SecureLogger.d(TAG, "Category deleted successfully, ID: ${category.id}, name: ${category.name}")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to delete category with ID: ${category.id}", e)
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

    override suspend fun deleteSubCategory(subCategory: SubCategory) {
        SecureLogger.d(TAG, "Starting deleteSubCategory operation for ID: ${subCategory.id}, name: ${subCategory.name}")
        try {
            dao.deleteSubCategory(SubCategoryEntity.fromDomain(subCategory))
            SecureLogger.d(TAG, "SubCategory deleted successfully, ID: ${subCategory.id}, name: ${subCategory.name}")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to delete subcategory with ID: ${subCategory.id}", e)
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

    override fun getAllSubCategories(): Flow<List<SubCategory>> {
        SecureLogger.d(TAG, "Starting getAllSubCategories query")
        return dao.getAllSubCategories().map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} total subcategories from database")
            list.map { it.toDomain() }
        }
    }
}
