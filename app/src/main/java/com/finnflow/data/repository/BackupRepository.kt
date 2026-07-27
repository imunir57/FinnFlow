package com.finnflow.data.repository

import androidx.room.withTransaction
import com.finnflow.data.db.AppDatabase
import com.finnflow.data.db.dao.CategoryDao
import com.finnflow.data.db.dao.TransactionDao
import com.finnflow.data.db.entity.CategoryEntity
import com.finnflow.data.db.entity.SubCategoryEntity
import com.finnflow.data.db.entity.TransactionEntity
import com.finnflow.data.logger.SecureLogger
import com.finnflow.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BackupRepository"

/**
 * Backup file schema. Kept independent from the Room entities so the on-disk
 * format is stable even if the entities change shape later.
 */
@Serializable
data class BackupCategoryDto(
    val id: Long,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorHex: String
)

@Serializable
data class BackupSubCategoryDto(
    val id: Long,
    val categoryId: Long,
    val name: String
)

@Serializable
data class BackupTransactionDto(
    val id: Long,
    val type: TransactionType,
    val amount: Double,
    val date: String,
    val categoryId: Long,
    val subCategoryId: Long?,
    val note: String,
    val fromAccountId: Long?,
    val toAccountId: Long?
)

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val categories: List<BackupCategoryDto>,
    val subCategories: List<BackupSubCategoryDto>,
    val transactions: List<BackupTransactionDto>
)

interface BackupRepository {
    suspend fun exportBackup(outputStream: OutputStream)
    suspend fun restoreBackup(inputStream: InputStream): Result<Unit>
}

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : BackupRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Indirection around [androidx.room.withTransaction] so tests can swap in a no-op
     * runner and exercise DAO ordering without a real (or Room-shaped) database — the
     * real extension is `inline`, which makes it unmockable directly.
     */
    internal var transactionRunner: suspend (suspend () -> Unit) -> Unit =
        { block -> db.withTransaction { block() } }

    override suspend fun exportBackup(outputStream: OutputStream) {
        SecureLogger.d(TAG, "Starting backup export")
        try {
            val categories = categoryDao.getAllCategories().first()
            val subCategories = categoryDao.getAllSubCategories().first()
            val transactions = transactionDao.getAll().first()

            SecureLogger.d(TAG, "Backup data loaded: categories=${categories.size}, subCategories=${subCategories.size}, transactions=${transactions.size}")

            val payload = BackupPayload(
                categories = categories.map {
                    BackupCategoryDto(it.id, it.name, it.type, it.iconName, it.colorHex)
                },
                subCategories = subCategories.map {
                    BackupSubCategoryDto(it.id, it.categoryId, it.name)
                },
                transactions = transactions.map {
                    BackupTransactionDto(
                        id = it.id,
                        type = it.type,
                        amount = it.amount,
                        date = it.date.toString(),
                        categoryId = it.categoryId,
                        subCategoryId = it.subCategoryId,
                        note = it.note,
                        fromAccountId = it.fromAccountId,
                        toAccountId = it.toAccountId
                    )
                }
            )

            SecureLogger.d(TAG, "Backup payload constructed, serializing to JSON")
            val jsonBytes = json.encodeToString(payload).toByteArray(Charsets.UTF_8)
            SecureLogger.d(TAG, "JSON serialization completed: ${jsonBytes.size} bytes")

            outputStream.use { it.write(jsonBytes) }
            SecureLogger.i(TAG, "Backup export completed successfully: file size=${jsonBytes.size} bytes")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Backup export failed", e)
            throw e
        }
    }

    override suspend fun restoreBackup(inputStream: InputStream): Result<Unit> {
        SecureLogger.d(TAG, "Starting backup restore")
        val entities = try {
            val bytes = inputStream.use { it.readBytes() }
            SecureLogger.d(TAG, "Backup file read: ${bytes.size} bytes")

            val text = bytes.toString(Charsets.UTF_8)
            SecureLogger.d(TAG, "JSON deserialization starting")

            val payload = json.decodeFromString<BackupPayload>(text)
            SecureLogger.d(TAG, "JSON deserialization completed: version=${payload.version}, categories=${payload.categories.size}, subCategories=${payload.subCategories.size}, transactions=${payload.transactions.size}")

            val parsed = parseAndValidate(payload)
            SecureLogger.d(TAG, "Backup validation successful")
            parsed
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Backup restore failed during parsing/validation", e)
            return Result.failure(e)
        }

        return try {
            SecureLogger.d(TAG, "Starting database transaction for restore")
            transactionRunner {
                transactionDao.deleteAll()
                categoryDao.deleteAllSubCategories()
                categoryDao.deleteAllCategories()
                SecureLogger.d(TAG, "Database cleared for restore")

                categoryDao.insertAllCategories(entities.categories)
                categoryDao.insertAllSubCategories(entities.subCategories)
                transactionDao.insertAll(entities.transactions)
                SecureLogger.d(TAG, "Database populated: categories=${entities.categories.size}, subCategories=${entities.subCategories.size}, transactions=${entities.transactions.size}")
            }
            SecureLogger.i(TAG, "Backup restore completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Backup restore failed during database write", e)
            Result.failure(e)
        }
    }

    /** Parses DTOs into entities and validates referential integrity before any DB write happens. */
    private fun parseAndValidate(payload: BackupPayload): ParsedBackup {
        SecureLogger.d(TAG, "Starting backup validation")

        val categoryIds = payload.categories.map { it.id }.toSet()
        require(categoryIds.size == payload.categories.size) { "Backup contains duplicate category ids" }
        SecureLogger.d(TAG, "Category uniqueness validated: ${categoryIds.size} unique categories")

        val subCategoryIds = payload.subCategories.map { it.id }.toSet()
        require(subCategoryIds.size == payload.subCategories.size) { "Backup contains duplicate sub-category ids" }
        SecureLogger.d(TAG, "SubCategory uniqueness validated: ${subCategoryIds.size} unique subcategories")

        require(payload.subCategories.all { it.categoryId in categoryIds }) {
            "Backup contains a sub-category referencing an unknown category"
        }
        SecureLogger.d(TAG, "SubCategory referential integrity validated")

        require(payload.transactions.all { it.categoryId in categoryIds }) {
            "Backup contains a transaction referencing an unknown category"
        }
        SecureLogger.d(TAG, "Transaction category referential integrity validated")

        require(payload.transactions.all { it.subCategoryId == null || it.subCategoryId in subCategoryIds }) {
            "Backup contains a transaction referencing an unknown sub-category"
        }
        SecureLogger.d(TAG, "Transaction subcategory referential integrity validated")

        val categories = payload.categories.map {
            CategoryEntity(it.id, it.name, it.type, it.iconName, it.colorHex)
        }
        val subCategories = payload.subCategories.map {
            SubCategoryEntity(it.id, it.categoryId, it.name)
        }
        val transactions = payload.transactions.map {
            TransactionEntity(
                id = it.id,
                type = it.type,
                amount = it.amount,
                date = LocalDate.parse(it.date),
                categoryId = it.categoryId,
                subCategoryId = it.subCategoryId,
                note = it.note,
                fromAccountId = it.fromAccountId,
                toAccountId = it.toAccountId
            )
        }

        SecureLogger.d(TAG, "Backup validation completed: all entities parsed successfully")
        return ParsedBackup(categories, subCategories, transactions)
    }

    private data class ParsedBackup(
        val categories: List<CategoryEntity>,
        val subCategories: List<SubCategoryEntity>,
        val transactions: List<TransactionEntity>
    )
}
