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
 * comma, double quote, or newline; embedded quotes are doubled. Free-text
 * fields are also formula-escaped so a note like "=HYPERLINK(...)" stays
 * inert text when the export is opened in Excel or Google Sheets.
 */
fun List<Transaction>.toCsv(
    categoryNameOf: (Long) -> String,
    subCategoryNameOf: (Long?) -> String?
): String {
    val rows = map { tx ->
        listOf(
            tx.date.format(exportDateFormatter),
            tx.type.name,
            categoryNameOf(tx.categoryId).formulaEscape(),
            subCategoryNameOf(tx.subCategoryId)?.formulaEscape() ?: "",
            tx.amount.toString(),
            tx.note.formulaEscape()
        )
    }
    return (listOf(CsvHeader) + rows).joinToString(separator = "\r\n") { row ->
        row.joinToString(separator = ",") { it.csvEscape() }
    }
}

// Spreadsheet apps evaluate cells starting with these as formulas, even in CSV files —
// the standard injection vector (OWASP "CSV injection"). A leading single quote forces
// the cell to plain text. Only applied to user-controlled free-text columns.
private val formulaTriggers = charArrayOf('=', '+', '-', '@', '\t')

private fun String.formulaEscape(): String =
    if (isNotEmpty() && first() in formulaTriggers) "'$this" else this

private fun String.csvEscape(): String =
    if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + replace("\"", "\"\"") + "\""
    } else {
        this
    }
