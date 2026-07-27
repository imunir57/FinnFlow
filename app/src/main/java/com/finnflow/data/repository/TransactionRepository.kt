package com.finnflow.data.repository

import com.finnflow.data.db.dao.MonthlyTotal
import com.finnflow.data.db.dao.TransactionDao
import com.finnflow.data.db.entity.TransactionEntity
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.SubCategorySummary
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.OutputStream
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionRepository {
    suspend fun addTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getTransactionById(id: Long): Transaction?
    fun getTransactionsByMonth(yearMonth: String): Flow<List<Transaction>>
    fun getTransactionsByDateRange(from: LocalDate, to: LocalDate): Flow<List<Transaction>>
    fun getMonthlyTotalsByYear(year: String, transactionType: TransactionType): Flow<List<MonthlyTotal>>
    fun getCategorySummary(from: LocalDate, to: LocalDate, type: TransactionType): Flow<List<CategorySummary>>
    fun getSubCategorySummary(
        categoryId: Long, from: LocalDate, to: LocalDate, type: TransactionType
    ): Flow<List<SubCategorySummary>>
    fun getTransactionsBySubCategory(
        categoryId: Long, subCategoryId: Long?, from: LocalDate, to: LocalDate, type: TransactionType
    ): Flow<List<Transaction>>

    fun getAllTransactions(): Flow<List<Transaction>>

    /** Writes all transactions as CSV (date, type, category, subcategory, amount, note) to [outputStream]. */
    suspend fun exportTransactionsCsv(outputStream: OutputStream)
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val categoryRepository: CategoryRepository
) : TransactionRepository {
    companion object {
        private const val TAG = "TransactionRepository"
    }

    override suspend fun addTransaction(transaction: Transaction): Long {
        SecureLogger.d(TAG, "Starting addTransaction operation")
        return try {
            val rowId = dao.insert(TransactionEntity.fromDomain(transaction))
            SecureLogger.d(TAG, "Transaction inserted successfully with ID: $rowId, type: ${transaction.type}, categoryId: ${transaction.categoryId}")
            rowId
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to add transaction", e)
            throw e
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        SecureLogger.d(TAG, "Starting updateTransaction operation for ID: ${transaction.id}")
        try {
            dao.update(TransactionEntity.fromDomain(transaction))
            SecureLogger.d(TAG, "Transaction updated successfully, ID: ${transaction.id}, type: ${transaction.type}")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to update transaction with ID: ${transaction.id}", e)
            throw e
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        SecureLogger.d(TAG, "Starting deleteTransaction operation for ID: ${transaction.id}")
        try {
            dao.delete(TransactionEntity.fromDomain(transaction))
            SecureLogger.d(TAG, "Transaction deleted successfully, ID: ${transaction.id}")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to delete transaction with ID: ${transaction.id}", e)
            throw e
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        SecureLogger.d(TAG, "Starting getTransactionById for ID: $id")
        return try {
            val transaction = dao.getById(id)?.toDomain()
            if (transaction != null) {
                SecureLogger.d(TAG, "Transaction found with ID: $id, type: ${transaction.type}")
            } else {
                SecureLogger.d(TAG, "No transaction found with ID: $id")
            }
            transaction
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to get transaction by ID: $id", e)
            throw e
        }
    }

    override fun getTransactionsByMonth(yearMonth: String): Flow<List<Transaction>> {
        SecureLogger.d(TAG, "Starting getTransactionsByMonth query for: $yearMonth")
        return dao.getByMonth(yearMonth).map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} transactions for month: $yearMonth")
            list.map { it.toDomain() }
        }
    }

    override fun getTransactionsByDateRange(from: LocalDate, to: LocalDate): Flow<List<Transaction>> {
        SecureLogger.d(TAG, "Starting getTransactionsByDateRange from: $from to: $to")
        return dao.getByDateRange(from, to).map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} transactions in date range: $from to $to")
            list.map { it.toDomain() }
        }
    }

    override fun getMonthlyTotalsByYear(year: String, transactionType: TransactionType): Flow<List<MonthlyTotal>> {
        SecureLogger.d(TAG, "Starting getMonthlyTotalsByYear for year: $year, type: $transactionType")
        return dao.getMonthlyTotalsByYear(year, transactionType).map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} monthly totals for year: $year, type: $transactionType")
            list
        }
    }

    override fun getCategorySummary(from: LocalDate, to: LocalDate, type: TransactionType): Flow<List<CategorySummary>> {
        SecureLogger.d(TAG, "Starting getCategorySummary from: $from to: $to, type: $type")
        return dao.getCategorySummary(from, to, type).map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} category summaries for range: $from to $to, type: $type")
            list
        }
    }

    override fun getSubCategorySummary(
        categoryId: Long, from: LocalDate, to: LocalDate, type: TransactionType
    ): Flow<List<SubCategorySummary>> {
        SecureLogger.d(TAG, "Starting getSubCategorySummary for categoryId: $categoryId from: $from to: $to, type: $type")
        return dao.getSubCategorySummary(categoryId, from, to, type).map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} subcategory summaries for categoryId: $categoryId, range: $from to $to")
            list
        }
    }

    override fun getTransactionsBySubCategory(
        categoryId: Long, subCategoryId: Long?, from: LocalDate, to: LocalDate, type: TransactionType
    ): Flow<List<Transaction>> {
        SecureLogger.d(TAG, "Starting getTransactionsBySubCategory for categoryId: $categoryId, subCategoryId: $subCategoryId, from: $from to: $to, type: $type")
        return dao.getTransactionsBySubCategory(categoryId, subCategoryId, from, to, type).map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} transactions for subcategoryId: $subCategoryId, categoryId: $categoryId")
            list.map { it.toDomain() }
        }
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        SecureLogger.d(TAG, "Starting getAllTransactions query")
        return dao.getAll().map { list ->
            SecureLogger.d(TAG, "Retrieved ${list.size} total transactions from database")
            list.map { it.toDomain() }
        }
    }

    override suspend fun exportTransactionsCsv(outputStream: OutputStream) {
        SecureLogger.d(TAG, "Starting exportTransactionsCsv operation")
        try {
            val transactions = dao.getAll().first().map { it.toDomain() }
            SecureLogger.d(TAG, "Exporting ${transactions.size} transactions to CSV")

            val categoryNames = categoryRepository.getAllCategories().first().associate { it.id to it.name }
            val subCategoryNames = categoryRepository.getAllSubCategories().first().associate { it.id to it.name }
            SecureLogger.d(TAG, "Loaded ${categoryNames.size} categories and ${subCategoryNames.size} subcategories for export")

            val csv = transactions.toCsv(
                categoryNameOf = { id -> categoryNames[id] ?: "" },
                subCategoryNameOf = { id -> id?.let { subCategoryNames[it] } }
            )
            outputStream.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
            SecureLogger.d(TAG, "CSV export completed successfully, wrote ${csv.length} bytes")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Failed to export transactions to CSV", e)
            throw e
        }
    }
}
