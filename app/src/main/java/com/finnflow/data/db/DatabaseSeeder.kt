package com.finnflow.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finnflow.data.db.dao.CategoryDao
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
                seedDefaultCategories(dbProvider.get().categoryDao())
            } catch (e: Exception) {
                SecureLogger.e(TAG, "Failed to seed database", e)
            }
        }
    }
}

/**
 * Inserts [SeedData]'s default categories and their subcategories.
 *
 * Lives outside [DatabaseSeeder] because the callback is not the only caller: erasing all data
 * empties the category tables without dropping the database file, so Room's `onCreate` never
 * fires again and the app would be left with nothing to file a transaction against.
 */
internal suspend fun seedDefaultCategories(categoryDao: CategoryDao) {
    SecureLogger.d(SEED_TAG, "Starting database seeding with ${SeedData.categories.size} categories")
    var totalSubCategoriesInserted = 0

    try {
        SeedData.categories.forEach { seedCat ->
            SecureLogger.d(SEED_TAG, "Inserting category: ${seedCat.name} (type: ${seedCat.type})")
            val catId = categoryDao.insertCategory(
                CategoryEntity(
                    name = seedCat.name,
                    type = seedCat.type,
                    iconName = seedCat.iconName,
                    colorHex = seedCat.colorHex
                )
            )
            SecureLogger.d(SEED_TAG, "Category inserted with ID: $catId, name: ${seedCat.name}")

            seedCat.subCategories.forEach { subName ->
                categoryDao.insertSubCategory(
                    SubCategoryEntity(categoryId = catId, name = subName)
                )
                totalSubCategoriesInserted++
            }
            SecureLogger.d(SEED_TAG, "Inserted ${seedCat.subCategories.size} subcategories for category: ${seedCat.name}")
        }
        SecureLogger.d(SEED_TAG, "Database seeding completed successfully. Total categories: ${SeedData.categories.size}, total subcategories: $totalSubCategoriesInserted")
    } catch (e: Exception) {
        SecureLogger.e(SEED_TAG, "Error during database seeding after inserting $totalSubCategoriesInserted subcategories", e)
        throw e
    }
}

private const val SEED_TAG = "DatabaseSeeder"
