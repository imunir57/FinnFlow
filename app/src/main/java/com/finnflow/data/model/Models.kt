package com.finnflow.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class TransactionType { INCOME, EXPENSE, TRANSFER }

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val date: LocalDate,
    val categoryId: Long,
    val subCategoryId: Long? = null,
    val note: String = "",
    val fromAccountId: Long? = null, // for TRANSFER
    val toAccountId: Long? = null    // for TRANSFER
)

data class Category(
    val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val iconName: String = "",
    val colorHex: String = "#607D8B",
    /** Retired from the pickers, but kept so past transactions keep their label. */
    val isArchived: Boolean = false
)

data class SubCategory(
    val id: Long = 0,
    val categoryId: Long,
    val name: String,
    /** Retired from the pickers, but kept so past transactions keep their label. */
    val isArchived: Boolean = false
)

data class CategoryWithSubCategories(
    val category: Category,
    val subCategories: List<SubCategory>
)

/**
 * Aggregated result used by StatsRepository.
 *
 * [colorHex] is the owning category's stored colour, carried through so the donut, legend and
 * category table can draw a category in the colour the user picked for it — the same colour the
 * Categories screen shows. Charts must not colour categories positionally: a positional palette
 * wraps and repeats once there are more categories than palette entries.
 *
 * This is a query projection, not a Room entity — it maps a `SELECT` shape, so adding a field
 * here needs no schema version bump or migration.
 */
data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
    val colorHex: String,
    val totalAmount: Double,
    val transactionCount: Int
)

/** Per-subcategory aggregation used by CategoryDetailScreen */
data class SubCategorySummary(
    val subCategoryId: Long?,
    val subCategoryName: String,
    val totalAmount: Double,
    val transactionCount: Int
)
