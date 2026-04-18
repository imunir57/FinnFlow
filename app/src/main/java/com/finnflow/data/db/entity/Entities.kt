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

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TransactionType,
    val iconName: String = "",
    val colorHex: String = "#607D8B"
) {
    fun toDomain() = Category(id, name, type, iconName, colorHex)

    companion object {
        fun fromDomain(c: Category) = CategoryEntity(c.id, c.name, c.type, c.iconName, c.colorHex)
    }
}

@Entity(tableName = "sub_categories",
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("categoryId")]
)
data class SubCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String
) {
    fun toDomain() = SubCategory(id, categoryId, name)

    companion object {
        fun fromDomain(s: SubCategory) = SubCategoryEntity(s.id, s.categoryId, s.name)
    }
}
