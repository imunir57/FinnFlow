package com.finnflow.data.model

import java.time.LocalDate

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
    val colorHex: String = "#607D8B"
)

data class SubCategory(
    val id: Long = 0,
    val categoryId: Long,
    val name: String
)

data class CategoryWithSubCategories(
    val category: Category,
    val subCategories: List<SubCategory>
)

/** Aggregated result used by StatsRepository */
data class CategorySummary(
    val categoryId: Long,
    val categoryName: String,
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
