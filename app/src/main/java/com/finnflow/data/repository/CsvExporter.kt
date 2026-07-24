package com.finnflow.data.repository

import com.finnflow.data.model.Transaction
import java.time.format.DateTimeFormatter

private val exportDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private val CsvHeader = listOf("date", "type", "category", "subcategory", "amount", "note")

/**
 * Pure, side-effect-free CSV formatter for transactions.
 *
 * Columns: date, type, category, subcategory, amount, note.
 * Free-text fields are quoted per RFC 4180 rules whenever they contain a
 * comma, double quote, or newline; embedded quotes are doubled.
 */
fun List<Transaction>.toCsv(
    categoryNameOf: (Long) -> String,
    subCategoryNameOf: (Long?) -> String?
): String {
    val rows = map { tx ->
        listOf(
            tx.date.format(exportDateFormatter),
            tx.type.name,
            categoryNameOf(tx.categoryId),
            subCategoryNameOf(tx.subCategoryId) ?: "",
            tx.amount.toString(),
            tx.note
        )
    }
    return (listOf(CsvHeader) + rows).joinToString(separator = "\r\n") { row ->
        row.joinToString(separator = ",") { it.csvEscape() }
    }
}

private fun String.csvEscape(): String =
    if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + replace("\"", "\"\"") + "\""
    } else {
        this
    }
