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

    @Query("UPDATE categories SET isArchived = :archived WHERE id = :id")
    suspend fun setCategoryArchived(id: Long, archived: Boolean)

    /**
     * Every category, archived included.
     *
     * This is the lookup query: Home, CSV export and backup resolve a transaction's category id
     * through it, so filtering archived ones out here would leave historical entries nameless.
     */
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    /** Pickable categories of a type — archived ones are deliberately absent. */
    @Query("SELECT * FROM categories WHERE type = :type AND isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("UPDATE categories SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setCategorySortOrder(id: Long, sortOrder: Int)

    /**
     * Writes [idsInOrder] as positions 0..n-1 in one transaction.
     *
     * The whole list is rewritten rather than the moved row alone: with untouched rows still at
     * the default 0, a single write would leave the list ordered by the name fallback in places
     * and by position in others.
     */
    @Transaction
    suspend fun setCategoryOrder(idsInOrder: List<Long>) {
        idsInOrder.forEachIndexed { index, id -> setCategorySortOrder(id, index) }
    }

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

    @Query("UPDATE sub_categories SET isArchived = :archived WHERE id = :id")
    suspend fun setSubCategoryArchived(id: Long, archived: Boolean)

    /** Every sub-category of a parent, archived included — for the management screen. */
    @Query("SELECT * FROM sub_categories WHERE categoryId = :categoryId ORDER BY sortOrder ASC, name ASC")
    fun getSubCategories(categoryId: Long): Flow<List<SubCategoryEntity>>

    /** Pickable sub-categories of a parent — archived ones are deliberately absent. */
    @Query("SELECT * FROM sub_categories WHERE categoryId = :categoryId AND isArchived = 0 ORDER BY sortOrder ASC, name ASC")
    fun getActiveSubCategories(categoryId: Long): Flow<List<SubCategoryEntity>>

    @Query("SELECT * FROM sub_categories WHERE id = :id")
    suspend fun getSubCategoryById(id: Long): SubCategoryEntity?

    @Query("SELECT * FROM sub_categories ORDER BY sortOrder ASC, name ASC")
    fun getAllSubCategories(): Flow<List<SubCategoryEntity>>

    @Query("UPDATE sub_categories SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSubCategorySortOrder(id: Long, sortOrder: Int)

    /** Writes [idsInOrder] as positions 0..n-1 in one transaction. See [setCategoryOrder]. */
    @Transaction
    suspend fun setSubCategoryOrder(idsInOrder: List<Long>) {
        idsInOrder.forEachIndexed { index, id -> setSubCategorySortOrder(id, index) }
    }
}
