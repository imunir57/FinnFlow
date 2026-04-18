package com.finnflow.data.repository

import com.finnflow.data.db.dao.MonthlyTotal
import com.finnflow.data.db.dao.TransactionDao
import com.finnflow.data.db.entity.TransactionEntity
import com.finnflow.data.model.CategorySummary
import com.finnflow.data.model.SubCategorySummary
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    override suspend fun addTransaction(transaction: Transaction) =
        dao.insert(TransactionEntity.fromDomain(transaction))

    override suspend fun updateTransaction(transaction: Transaction) =
        dao.update(TransactionEntity.fromDomain(transaction))

    override suspend fun deleteTransaction(transaction: Transaction) =
        dao.delete(TransactionEntity.fromDomain(transaction))

    override suspend fun getTransactionById(id: Long) =
        dao.getById(id)?.toDomain()

    override fun getTransactionsByMonth(yearMonth: String) =
        dao.getByMonth(yearMonth).map { list -> list.map { it.toDomain() } }

    override fun getTransactionsByDateRange(from: LocalDate, to: LocalDate) =
        dao.getByDateRange(from, to).map { list -> list.map { it.toDomain() } }

    override fun getMonthlyTotalsByYear(year: String, transactionType: TransactionType) =
        dao.getMonthlyTotalsByYear(year, transactionType)

    override fun getCategorySummary(from: LocalDate, to: LocalDate, type: TransactionType) =
        dao.getCategorySummary(from, to, type)

    override fun getSubCategorySummary(
        categoryId: Long, from: LocalDate, to: LocalDate, type: TransactionType
    ) = dao.getSubCategorySummary(categoryId, from, to, type)

    override fun getTransactionsBySubCategory(
        categoryId: Long, subCategoryId: Long?, from: LocalDate, to: LocalDate, type: TransactionType
    ) = dao.getTransactionsBySubCategory(categoryId, subCategoryId, from, to, type)
        .map { list -> list.map { it.toDomain() } }

    override fun getAllTransactions() =
        dao.getAll().map { list -> list.map { it.toDomain() } }
}
