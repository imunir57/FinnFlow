package com.finnflow.data.db.dao

import androidx.room.*
import com.finnflow.data.db.entity.TransactionEntity
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.SubCategorySummary
import com.finnflow.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("""
        SELECT * FROM transactions 
        WHERE strftime('%Y-%m', date) = :yearMonth 
        ORDER BY date DESC, id DESC
    """)
    fun getByMonth(yearMonth: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :from AND date <= :to 
        ORDER BY date DESC, id DESC
    """)
    fun getByDateRange(from: LocalDate, to: LocalDate): Flow<List<TransactionEntity>>

    @Query("""
        SELECT strftime('%m', date) as month, SUM(amount) as total
        FROM transactions 
        WHERE strftime('%Y', date) = :year AND type = :type
        GROUP BY month
    """)
    fun getMonthlyTotalsByYear(year: String, type: TransactionType): Flow<List<MonthlyTotal>>

    @Query("""
        SELECT t.categoryId, c.name as categoryName, SUM(t.amount) as totalAmount, COUNT(*) as transactionCount
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.date >= :from AND t.date <= :to AND t.type = :type
        GROUP BY t.categoryId
        ORDER BY totalAmount DESC
    """)
    fun getCategorySummary(from: LocalDate, to: LocalDate, type: TransactionType): Flow<List<CategorySummary>>

    /**
     * Subcategory breakdown for a specific category in a date range.
     * LEFT JOIN so transactions without a subcategory appear as "Uncategorised".
     * subCategoryId is nullable — NULL means the user chose no subcategory.
     */
    @Query("""
        SELECT 
            t.subCategoryId,
            COALESCE(s.name, 'Uncategorised') as subCategoryName,
            SUM(t.amount) as totalAmount,
            COUNT(*) as transactionCount
        FROM transactions t
        LEFT JOIN sub_categories s ON t.subCategoryId = s.id
        WHERE t.categoryId = :categoryId
          AND t.date >= :from
          AND t.date <= :to
          AND t.type = :type
        GROUP BY t.subCategoryId
        ORDER BY totalAmount DESC
    """)
    fun getSubCategorySummary(
        categoryId: Long,
        from: LocalDate,
        to: LocalDate,
        type: TransactionType
    ): Flow<List<SubCategorySummary>>

    /**
     * Individual transactions for one subcategory, used by the inline
     * expand panel in CategoryDetailScreen.
     * Pass subCategoryId = null to get transactions with no subcategory.
     */
    @Query("""
        SELECT * FROM transactions
        WHERE categoryId = :categoryId
          AND ((:subCategoryId IS NULL AND subCategoryId IS NULL) OR subCategoryId = :subCategoryId)
          AND date >= :from
          AND date <= :to
          AND type = :type
        ORDER BY date DESC, id DESC
    """)
    fun getTransactionsBySubCategory(
        categoryId: Long,
        subCategoryId: Long?,
        from: LocalDate,
        to: LocalDate,
        type: TransactionType
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAll(): Flow<List<TransactionEntity>>
}

data class MonthlyTotal(val month: String, val total: Double)
