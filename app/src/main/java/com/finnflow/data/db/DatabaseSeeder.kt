package com.finnflow.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finnflow.data.db.entity.CategoryEntity
import com.finnflow.data.db.entity.SubCategoryEntity
import com.finnflow.data.logger.SecureLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider

/**
 * Room [RoomDatabase.Callback] that seeds default categories and subcategories
 * the very first time the database is created (fresh install or after data clear).
 *
 * Uses a [Provider<AppDatabase>] to avoid a circular Hilt dependency
 * (AppDatabase → Callback → AppDatabase).
 */
class DatabaseSeeder(
    private val dbProvider: Provider<AppDatabase>
) : RoomDatabase.Callback() {
    companion object {
        private const val TAG = "DatabaseSeeder"
    }

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        SecureLogger.d(TAG, "Database onCreate callback triggered, starting seed operation")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                seed(dbProvider.get())
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to seed database", e)
            }
        }
    }

    private suspend fun seed(database: AppDatabase) {
        SecureLogger.d(TAG, "Starting database seeding with ${SeedData.categories.size} categories")
        val categoryDao = database.categoryDao()
        var totalSubCategoriesInserted = 0

        try {
            SeedData.categories.forEach { seedCat ->
                SecureLogger.d(TAG, "Inserting category: ${seedCat.name} (type: ${seedCat.type})")
                val catId = categoryDao.insertCategory(
                    CategoryEntity(
                        name = seedCat.name,
                        type = seedCat.type,
                        iconName = seedCat.iconName,
                        colorHex = seedCat.colorHex
                    )
                )
                SecureLogger.d(TAG, "Category inserted with ID: $catId, name: ${seedCat.name}")

                seedCat.subCategories.forEach { subName ->
                    categoryDao.insertSubCategory(
                        SubCategoryEntity(categoryId = catId, name = subName)
                    )
                    totalSubCategoriesInserted++
                }
                SecureLogger.d(TAG, "Inserted ${seedCat.subCategories.size} subcategories for category: ${seedCat.name}")
            }
            SecureLogger.d(TAG, "Database seeding completed successfully. Total categories: ${SeedData.categories.size}, total subcategories: $totalSubCategoriesInserted")
        } catch (e: Exception) {
            SecureLogger.e(TAG, "Error during database seeding after inserting $totalSubCategoriesInserted subcategories", e)
            throw e
        }
    }
}
