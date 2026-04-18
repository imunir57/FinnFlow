package com.finnflow.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finnflow.data.db.entity.CategoryEntity
import com.finnflow.data.db.entity.SubCategoryEntity
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

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            seed(dbProvider.get())
        }
    }

    private suspend fun seed(database: AppDatabase) {
        val categoryDao = database.categoryDao()

        SeedData.categories.forEach { seedCat ->
            val catId = categoryDao.insertCategory(
                CategoryEntity(
                    name = seedCat.name,
                    type = seedCat.type,
                    iconName = seedCat.iconName,
                    colorHex = seedCat.colorHex
                )
            )
            seedCat.subCategories.forEach { subName ->
                categoryDao.insertSubCategory(
                    SubCategoryEntity(categoryId = catId, name = subName)
                )
            }
        }
    }
}
