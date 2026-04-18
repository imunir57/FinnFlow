package com.finnflow.data.repository

import app.cash.turbine.test
import com.finnflow.data.db.dao.CategoryDao
import com.finnflow.data.db.entity.CategoryEntity
import com.finnflow.data.db.entity.SubCategoryEntity
import com.finnflow.data.model.Category
import com.finnflow.data.model.SubCategory
import com.finnflow.data.model.TransactionType
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CategoryRepositoryTest {

    private lateinit var dao: CategoryDao
    private lateinit var repo: CategoryRepositoryImpl

    private val domainCat = Category(id = 1L, name = "Food", type = TransactionType.EXPENSE)
    private val entityCat = CategoryEntity(id = 1L, name = "Food", type = TransactionType.EXPENSE)
    private val domainSub = SubCategory(id = 1L, categoryId = 1L, name = "Restaurant")
    private val entitySub = SubCategoryEntity(id = 1L, categoryId = 1L, name = "Restaurant")

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repo = CategoryRepositoryImpl(dao)
    }

    @Test
    fun addCategory_delegatesToDao() = runTest {
        coEvery { dao.insertCategory(any()) } returns 1L
        val id = repo.addCategory(domainCat)
        assertEquals(1L, id)
        coVerify { dao.insertCategory(CategoryEntity.fromDomain(domainCat)) }
    }

    @Test
    fun updateCategory_delegatesToDao() = runTest {
        repo.updateCategory(domainCat)
        coVerify { dao.updateCategory(CategoryEntity.fromDomain(domainCat)) }
    }

    @Test
    fun deleteCategory_delegatesToDao() = runTest {
        repo.deleteCategory(domainCat)
        coVerify { dao.deleteCategory(CategoryEntity.fromDomain(domainCat)) }
    }

    @Test
    fun getCategoryById_mapsToDomain() = runTest {
        coEvery { dao.getCategoryById(1L) } returns entityCat
        assertEquals(domainCat, repo.getCategoryById(1L))
    }

    @Test
    fun getAllCategories_emitsMappedList() = runTest {
        every { dao.getAllCategories() } returns flowOf(listOf(entityCat))
        repo.getAllCategories().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(domainCat, list[0])
            awaitComplete()
        }
    }

    @Test
    fun getCategoriesByType_filtersCorrectly() = runTest {
        every { dao.getCategoriesByType(TransactionType.EXPENSE) } returns flowOf(listOf(entityCat))
        repo.getCategoriesByType(TransactionType.EXPENSE).test {
            assertEquals(1, awaitItem().size)
            awaitComplete()
        }
    }

    @Test
    fun addSubCategory_delegatesToDao() = runTest {
        coEvery { dao.insertSubCategory(any()) } returns 1L
        val id = repo.addSubCategory(domainSub)
        assertEquals(1L, id)
        coVerify { dao.insertSubCategory(SubCategoryEntity.fromDomain(domainSub)) }
    }

    @Test
    fun getSubCategories_emitsMappedList() = runTest {
        every { dao.getSubCategories(1L) } returns flowOf(listOf(entitySub))
        repo.getSubCategories(1L).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(domainSub, list[0])
            awaitComplete()
        }
    }

    @Test
    fun deleteSubCategory_delegatesToDao() = runTest {
        repo.deleteSubCategory(domainSub)
        coVerify { dao.deleteSubCategory(SubCategoryEntity.fromDomain(domainSub)) }
    }
}
