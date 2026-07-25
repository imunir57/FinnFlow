package com.finnflow.data.db.dao

import androidx.room.*
import com.finnflow.data.db.entity.CategoryEntity
import com.finnflow.data.db.entity.SubCategoryEntity
import com.finnflow.data.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // ─── Category ──────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    // ─── SubCategory ───────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubCategory(subCategory: SubCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSubCategories(subCategories: List<SubCategoryEntity>)

    @Update
    suspend fun updateSubCategory(subCategory: SubCategoryEntity)

    @Delete
    suspend fun deleteSubCategory(subCategory: SubCategoryEntity)

    @Query("DELETE FROM sub_categories")
    suspend fun deleteAllSubCategories()

    @Query("SELECT * FROM sub_categories WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getSubCategories(categoryId: Long): Flow<List<SubCategoryEntity>>

    @Query("SELECT * FROM sub_categories WHERE id = :id")
    suspend fun getSubCategoryById(id: Long): SubCategoryEntity?

    @Query("SELECT * FROM sub_categories")
    fun getAllSubCategories(): Flow<List<SubCategoryEntity>>
}
