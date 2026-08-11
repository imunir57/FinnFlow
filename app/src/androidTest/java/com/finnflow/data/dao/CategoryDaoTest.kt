package com.finnflow.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnflow.data.db.AppDatabase
import com.finnflow.data.db.dao.CategoryDao
import com.finnflow.data.db.entity.CategoryEntity
import com.finnflow.data.db.entity.SubCategoryEntity
import com.finnflow.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CategoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.categoryDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun insertCategory_andGetAll() = runTest {
        dao.insertCategory(CategoryEntity(name = "Food", type = TransactionType.EXPENSE))
        dao.insertCategory(CategoryEntity(name = "Salary", type = TransactionType.INCOME))

        val all = dao.getAllCategories().first()
        assertEquals(2, all.size)
    }

    @Test
    fun getCategoriesByType_filtersCorrectly() = runTest {
        dao.insertCategory(CategoryEntity(name = "Food", type = TransactionType.EXPENSE))
        dao.insertCategory(CategoryEntity(name = "Transport", type = TransactionType.EXPENSE))
        dao.insertCategory(CategoryEntity(name = "Salary", type = TransactionType.INCOME))

        val expense = dao.getCategoriesByType(TransactionType.EXPENSE).first()
        assertEquals(2, expense.size)
    }

    @Test
    fun deleteCategory_cascadesToSubCategories() = runTest {
        val catId = dao.insertCategory(CategoryEntity(name = "Transport", type = TransactionType.EXPENSE))
        dao.insertSubCategory(SubCategoryEntity(categoryId = catId, name = "Bus"))
        dao.insertSubCategory(SubCategoryEntity(categoryId = catId, name = "Train"))

        val cat = dao.getCategoryById(catId)!!
        dao.deleteCategory(cat)

        val subs = dao.getSubCategories(catId).first()
        assertTrue(subs.isEmpty())
    }

    @Test
    fun updateCategory_persistsChange() = runTest {
        val id = dao.insertCategory(CategoryEntity(name = "Misc", type = TransactionType.EXPENSE))
        val original = dao.getCategoryById(id)!!
        dao.updateCategory(original.copy(name = "Entertainment"))
        assertEquals("Entertainment", dao.getCategoryById(id)!!.name)
    }

    @Test
    fun insertSubCategory_retrievableByParent() = runTest {
        val catId = dao.insertCategory(CategoryEntity(name = "Food", type = TransactionType.EXPENSE))
        dao.insertSubCategory(SubCategoryEntity(categoryId = catId, name = "Restaurant"))
        dao.insertSubCategory(SubCategoryEntity(categoryId = catId, name = "Groceries"))

        val subs = dao.getSubCategories(catId).first()
        assertEquals(2, subs.size)
        assertTrue(subs.any { it.name == "Restaurant" })
    }

    // ── Ordering ──────────────────────────────────────────────────────────

    @Test
    fun getCategoriesByType_fallsBackToNameWhenSortOrderIsUnset() = runTest {
        dao.insertCategory(CategoryEntity(name = "Transport", type = TransactionType.EXPENSE))
        dao.insertCategory(CategoryEntity(name = "Food", type = TransactionType.EXPENSE))

        val expense = dao.getCategoriesByType(TransactionType.EXPENSE).first()

        assertEquals(listOf("Food", "Transport"), expense.map { it.name })
    }

    @Test
    fun setCategoryOrder_writesPositionsAndDrivesQueryOrder() = runTest {
        val food = dao.insertCategory(CategoryEntity(name = "Food", type = TransactionType.EXPENSE))
        val rent = dao.insertCategory(CategoryEntity(name = "Rent", type = TransactionType.EXPENSE))
        val transport = dao.insertCategory(CategoryEntity(name = "Transport", type = TransactionType.EXPENSE))

        dao.setCategoryOrder(listOf(transport, food, rent))

        val expense = dao.getCategoriesByType(TransactionType.EXPENSE).first()
        assertEquals(listOf("Transport", "Food", "Rent"), expense.map { it.name })
        assertEquals(listOf(0, 1, 2), expense.map { it.sortOrder })
    }

    @Test
    fun setCategoryOrder_survivesAnUpdateToTheRow() = runTest {
        val food = dao.insertCategory(CategoryEntity(name = "Food", type = TransactionType.EXPENSE))
        val rent = dao.insertCategory(CategoryEntity(name = "Rent", type = TransactionType.EXPENSE))
        dao.setCategoryOrder(listOf(rent, food))

        // Renaming goes through @Update with the whole entity, so the position has to make the
        // round trip with it or an edit would silently reset the list order.
        val stored = dao.getCategoryById(rent)!!
        dao.updateCategory(stored.copy(name = "Housing"))

        val expense = dao.getCategoriesByType(TransactionType.EXPENSE).first()
        assertEquals(listOf("Housing", "Food"), expense.map { it.name })
    }

    @Test
    fun setSubCategoryOrder_writesPositionsAndDrivesQueryOrder() = runTest {
        val catId = dao.insertCategory(CategoryEntity(name = "Food", type = TransactionType.EXPENSE))
        val groceries = dao.insertSubCategory(SubCategoryEntity(categoryId = catId, name = "Groceries"))
        val coffee = dao.insertSubCategory(SubCategoryEntity(categoryId = catId, name = "Coffee"))

        dao.setSubCategoryOrder(listOf(groceries, coffee))

        val subs = dao.getActiveSubCategories(catId).first()
        assertEquals(listOf("Groceries", "Coffee"), subs.map { it.name })
    }

    @Test
    fun setSubCategoryOrder_leavesOtherParentsAlone() = runTest {
        val food = dao.insertCategory(CategoryEntity(name = "Food", type = TransactionType.EXPENSE))
        val travel = dao.insertCategory(CategoryEntity(name = "Travel", type = TransactionType.EXPENSE))
        val groceries = dao.insertSubCategory(SubCategoryEntity(categoryId = food, name = "Groceries"))
        val coffee = dao.insertSubCategory(SubCategoryEntity(categoryId = food, name = "Coffee"))
        dao.insertSubCategory(SubCategoryEntity(categoryId = travel, name = "Flights"))
        dao.insertSubCategory(SubCategoryEntity(categoryId = travel, name = "Hotels"))

        dao.setSubCategoryOrder(listOf(groceries, coffee))

        val travelSubs = dao.getSubCategories(travel).first()
        assertEquals(listOf("Flights", "Hotels"), travelSubs.map { it.name })
        assertEquals(listOf(0, 0), travelSubs.map { it.sortOrder })
    }
}
