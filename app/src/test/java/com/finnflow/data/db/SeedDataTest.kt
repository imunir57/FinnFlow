package com.finnflow.data.db

import com.finnflow.data.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class SeedDataTest {

    @Test
    fun seedCategories_notEmpty() {
        assertTrue(SeedData.categories.isNotEmpty())
    }

    @Test
    fun allCategoryNames_areUnique() {
        val names = SeedData.categories.map { it.name }
        assertEquals("Duplicate category names found", names.size, names.toSet().size)
    }

    @Test
    fun allCategoryNames_areNotBlank() {
        SeedData.categories.forEach { cat ->
            assertTrue("Category name is blank: $cat", cat.name.isNotBlank())
        }
    }

    @Test
    fun allSubCategoryNames_areNotBlank() {
        SeedData.categories.forEach { cat ->
            cat.subCategories.forEach { sub ->
                assertTrue("SubCategory name is blank in '${cat.name}'", sub.isNotBlank())
            }
        }
    }

    @Test
    fun allColorHex_areValidFormat() {
        val hexPattern = Regex("^#[0-9A-Fa-f]{6}$")
        SeedData.categories.forEach { cat ->
            assertTrue(
                "Invalid colorHex '${cat.colorHex}' in category '${cat.name}'",
                cat.colorHex.matches(hexPattern)
            )
        }
    }

    @Test
    fun incomeCategories_exist() {
        val incomeCount = SeedData.categories.count { it.type == TransactionType.INCOME }
        assertTrue("Expected at least 3 income categories, found $incomeCount", incomeCount >= 3)
    }

    @Test
    fun expenseCategories_exist() {
        val expenseCount = SeedData.categories.count { it.type == TransactionType.EXPENSE }
        assertTrue("Expected at least 5 expense categories, found $expenseCount", expenseCount >= 5)
    }

    @Test
    fun transferCategory_exists() {
        val transferCount = SeedData.categories.count { it.type == TransactionType.TRANSFER }
        assertTrue("Expected at least 1 transfer category", transferCount >= 1)
    }

    @Test
    fun transportationCategory_hasExpectedSubCategories() {
        val transport = SeedData.categories.first { it.name == "Transportation" }
        val subs = transport.subCategories
        assertTrue("Expected Bus in Transportation", subs.contains("Bus"))
        assertTrue("Expected Train in Transportation", subs.contains("Train"))
        assertTrue("Expected Fuel in Transportation", subs.contains("Fuel"))
    }

    @Test
    fun foodCategory_hasExpectedSubCategories() {
        val food = SeedData.categories.first { it.name == "Food & Dining" }
        val subs = food.subCategories
        assertTrue("Expected Restaurant in Food & Dining", subs.contains("Restaurant"))
        assertTrue("Expected Groceries in Food & Dining", subs.contains("Groceries"))
    }

    @Test
    fun totalSubCategoryCount_isReasonable() {
        val total = SeedData.categories.sumOf { it.subCategories.size }
        assertTrue("Expected at least 30 subcategories total, found $total", total >= 30)
    }

    @Test
    fun subCategories_withinSameCategory_areUnique() {
        SeedData.categories.forEach { cat ->
            val names = cat.subCategories
            assertEquals(
                "Duplicate subcategories in '${cat.name}'",
                names.size,
                names.toSet().size
            )
        }
    }
}
