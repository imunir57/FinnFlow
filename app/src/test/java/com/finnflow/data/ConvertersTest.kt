package com.finnflow.data

import com.finnflow.data.db.Converters
import com.finnflow.data.model.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun dateRoundTrip() {
        val date = LocalDate.of(2024, 6, 15)
        val str = converters.fromDate(date)
        assertEquals(date, converters.toDate(str))
    }

    @Test
    fun transactionTypeRoundTrip() {
        TransactionType.entries.forEach { type ->
            val str = converters.fromType(type)
            assertEquals(type, converters.toType(str))
        }
    }

    @Test
    fun fromDate_producesIsoString() {
        assertEquals("2024-01-01", converters.fromDate(LocalDate.of(2024, 1, 1)))
    }
}
