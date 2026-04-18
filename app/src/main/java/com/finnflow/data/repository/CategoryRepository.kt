package com.finnflow.data.repository

import com.finnflow.data.db.dao.CategoryDao
import com.finnflow.data.db.entity.CategoryEntity
import com.finnflow.data.db.entity.SubCategoryEntity
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
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override suspend fun addCategory(category: Category) =
        dao.insertCategory(CategoryEntity.fromDomain(category))

    override suspend fun updateCategory(category: Category) =
        dao.updateCategory(CategoryEntity.fromDomain(category))

    override suspend fun deleteCategory(category: Category) =
        dao.deleteCategory(CategoryEntity.fromDomain(category))

    override suspend fun getCategoryById(id: Long) =
        dao.getCategoryById(id)?.toDomain()

    override fun getAllCategories() =
        dao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override fun getCategoriesByType(type: TransactionType) =
        dao.getCategoriesByType(type).map { list -> list.map { it.toDomain() } }

    override suspend fun addSubCategory(subCategory: SubCategory) =
        dao.insertSubCategory(SubCategoryEntity.fromDomain(subCategory))

    override suspend fun updateSubCategory(subCategory: SubCategory) =
        dao.updateSubCategory(SubCategoryEntity.fromDomain(subCategory))

    override suspend fun deleteSubCategory(subCategory: SubCategory) =
        dao.deleteSubCategory(SubCategoryEntity.fromDomain(subCategory))

    override fun getSubCategories(categoryId: Long) =
        dao.getSubCategories(categoryId).map { list -> list.map { it.toDomain() } }
}
