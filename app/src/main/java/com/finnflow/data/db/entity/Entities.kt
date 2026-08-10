package com.finnflow.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import java.time.LocalDate

/**
 * Transaction entity for Room persistence.
 *
 * DAOs (TransactionDao) perform bulk operations that are logged at the repository level.
 * For logging strategy, see BackupRepository (backup/restore operations) and
 * TransactionRepository (export operations). Individual DAO queries are not logged
 * to avoid excessive overhead.
 */
@Entity(tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = SubCategoryEntity::class, parentColumns = ["id"], childColumns = ["subCategoryId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("categoryId"), Index("subCategoryId"), Index("date")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val date: LocalDate,
    val categoryId: Long,
    val subCategoryId: Long? = null,
    val note: String = "",
    val fromAccountId: Long? = null,
    val toAccountId: Long? = null
) {
    fun toDomain() = Transaction(id, type, amount, date, categoryId, subCategoryId, note, fromAccountId, toAccountId)

    companion object {
        fun fromDomain(t: Transaction) = TransactionEntity(t.id, t.type, t.amount, t.date, t.categoryId, t.subCategoryId, t.note, t.fromAccountId, t.toAccountId)
    }
}

/**
 * Category entity for Room persistence.
 *
 * DAOs (CategoryDao) perform bulk operations that are logged at the repository level
 * (see BackupRepository for backup/restore logging). Individual DAO queries are not logged.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val iconName: String = "",
    val colorHex: String = "#607D8B",
    /**
     * Retired, but kept so historical transactions keep their name, icon and colour.
     *
     * `transactions.categoryId` is ON DELETE RESTRICT, so a category in use cannot be deleted at
     * all — archiving is what the UI's delete action does instead. Archived categories stay out
     * of the pickers and still appear in Stats wherever they have amounts.
     */
    val isArchived: Boolean = false,
    /**
     * User-chosen position within its type, ascending.
     *
     * Every query orders by `sortOrder` then `name`, so rows that were never reordered — seeded
     * ones, and everything that existed before this column — keep falling back to alphabetical.
     * Reordering rewrites the whole list's positions at once, which is what makes the fallback
     * safe: partial values would interleave unpredictably with the zeros.
     */
    val sortOrder: Int = 0
) {
    fun toDomain() = Category(id, name, type, iconName, colorHex, isArchived, sortOrder)

    companion object {
        fun fromDomain(c: Category) = CategoryEntity(c.id, c.name, c.type, c.iconName, c.colorHex, c.isArchived, c.sortOrder)
    }
}

/**
 * SubCategory entity for Room persistence.
 *
 * DAOs (CategoryDao) perform bulk operations that are logged at the repository level
 * (see BackupRepository for backup/restore logging). Individual DAO queries are not logged.
 */
@Entity(tableName = "sub_categories",
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("categoryId")]
)
data class SubCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    /**
     * Retired, but kept so historical transactions keep this label.
     *
     * `transactions.subCategoryId` is ON DELETE SET NULL, so deleting outright would quietly
     * turn every past entry into "Uncategorised". Archiving is what the UI's delete action does
     * instead. Independent of the parent's flag: archiving a category leaves its sub-categories
     * as they are, so restoring the parent brings back exactly what was there before.
     */
    val isArchived: Boolean = false,
    /** User-chosen position within the parent category, ascending. See [CategoryEntity.sortOrder]. */
    val sortOrder: Int = 0
) {
    fun toDomain() = SubCategory(id, categoryId, name, isArchived, sortOrder)

    companion object {
        fun fromDomain(s: SubCategory) = SubCategoryEntity(s.id, s.categoryId, s.name, s.isArchived, s.sortOrder)
    }
}
