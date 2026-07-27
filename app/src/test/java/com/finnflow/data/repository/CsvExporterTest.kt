package com.finnflow.data.repository

import com.finnflow.data.model.Transaction
import com.finnflow.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CsvExporterTest {

    private val categoryNames = mapOf(1L to "Food", 2L to "Salary")
    private val subCategoryNames = mapOf(10L to "Groceries")

    private val categoryNameOf: (Long) -> String = { id -> categoryNames[id] ?: "" }
    private val subCategoryNameOf: (Long?) -> String? = { id -> id?.let { subCategoryNames[it] } }

    @Test
    fun toCsv_emptyList_producesHeaderOnly() {
        val csv = emptyList<Transaction>().toCsv(categoryNameOf, subCategoryNameOf)
        assertEquals("date,type,category,subcategory,amount,note", csv)
    }

    @Test
    fun toCsv_singleTransaction_formatsRowCorrectly() {
        val tx = Transaction(
            id = 1L,
            type = TransactionType.EXPENSE,
            amount = 45.5,
            date = LocalDate.of(2024, 4, 1),
            categoryId = 1L,
            subCategoryId = 10L,
            note = "Weekly shop"
        )

        val csv = listOf(tx).toCsv(categoryNameOf, subCategoryNameOf)

        val expected = "date,type,category,subcategory,amount,note\r\n" +
            "2024-04-01,EXPENSE,Food,Groceries,45.5,Weekly shop"
        assertEquals(expected, csv)
    }

    @Test
    fun toCsv_transactionWithNoSubCategory_leavesSubcategoryColumnBlank() {
        val tx = Transaction(
            id = 2L,
            type = TransactionType.INCOME,
            amount = 3000.0,
            date = LocalDate.of(2024, 5, 1),
            categoryId = 2L,
            subCategoryId = null,
            note = ""
        )

        val csv = listOf(tx).toCsv(categoryNameOf, subCategoryNameOf)

        val expected = "date,type,category,subcategory,amount,note\r\n" +
            "2024-05-01,INCOME,Salary,,3000.0,"
        assertEquals(expected, csv)
    }

    @Test
    fun toCsv_noteWithCommaAndQuote_isProperlyEscaped() {
        val tx = Transaction(
            id = 3L,
            type = TransactionType.EXPENSE,
            amount = 12.0,
            date = LocalDate.of(2024, 6, 15),
            categoryId = 1L,
            subCategoryId = null,
            note = "Lunch, with a \"friend\""
        )

        val csv = listOf(tx).toCsv(categoryNameOf, subCategoryNameOf)

        val expected = "date,type,category,subcategory,amount,note\r\n" +
            "2024-06-15,EXPENSE,Food,,12.0,\"Lunch, with a \"\"friend\"\"\""
        assertEquals(expected, csv)
    }

    @Test
    fun toCsv_noteWithNewline_isQuoted() {
        val tx = Transaction(
            id = 4L,
            type = TransactionType.EXPENSE,
            amount = 5.0,
            date = LocalDate.of(2024, 7, 1),
            categoryId = 1L,
            note = "Line one\nLine two"
        )

        val csv = listOf(tx).toCsv(categoryNameOf, subCategoryNameOf)

        val expected = "date,type,category,subcategory,amount,note\r\n" +
            "2024-07-01,EXPENSE,Food,,5.0,\"Line one\nLine two\""
        assertEquals(expected, csv)
    }

    @Test
    fun toCsv_noteStartingWithFormulaCharacter_isNeutralized() {
        val tx = Transaction(
            id = 5L,
            type = TransactionType.EXPENSE,
            amount = 1.0,
            date = LocalDate.of(2024, 8, 1),
            categoryId = 1L,
            note = "=HYPERLINK(\"http://evil.example\",\"click\")"
        )

        val csv = listOf(tx).toCsv(categoryNameOf, subCategoryNameOf)

        // The leading single quote defuses the formula; the cell is still RFC 4180-quoted
        // because it contains commas and double quotes.
        val expected = "date,type,category,subcategory,amount,note\r\n" +
            "2024-08-01,EXPENSE,Food,,1.0," +
            "\"'=HYPERLINK(\"\"http://evil.example\"\",\"\"click\"\")\""
        assertEquals(expected, csv)
    }

    @Test
    fun toCsv_noteStartingWithPlusMinusOrAt_isNeutralized() {
        fun noteOf(note: String): String {
            val tx = Transaction(
                id = 6L, type = TransactionType.EXPENSE, amount = 1.0,
                date = LocalDate.of(2024, 8, 2), categoryId = 1L, note = note
            )
            return listOf(tx).toCsv(categoryNameOf, subCategoryNameOf).split("\r\n")[1]
        }

        assertEquals("2024-08-02,EXPENSE,Food,,1.0,'+1234", noteOf("+1234"))
        assertEquals("2024-08-02,EXPENSE,Food,,1.0,'-cmd", noteOf("-cmd"))
        assertEquals("2024-08-02,EXPENSE,Food,,1.0,'@user", noteOf("@user"))
        // Formula characters mid-string are untouched.
        assertEquals("2024-08-02,EXPENSE,Food,,1.0,a=b", noteOf("a=b"))
    }

    @Test
    fun toCsv_multipleTransactions_producesOneRowEach() {
        val tx1 = Transaction(
            id = 1L, type = TransactionType.EXPENSE, amount = 10.0,
            date = LocalDate.of(2024, 1, 1), categoryId = 1L, note = "A"
        )
        val tx2 = Transaction(
            id = 2L, type = TransactionType.INCOME, amount = 20.0,
            date = LocalDate.of(2024, 1, 2), categoryId = 2L, note = "B"
        )

        val csv = listOf(tx1, tx2).toCsv(categoryNameOf, subCategoryNameOf)
        val lines = csv.split("\r\n")

        assertEquals(3, lines.size)
        assertEquals("date,type,category,subcategory,amount,note", lines[0])
        assertEquals("2024-01-01,EXPENSE,Food,,10.0,A", lines[1])
        assertEquals("2024-01-02,INCOME,Salary,,20.0,B", lines[2])
    }
}
