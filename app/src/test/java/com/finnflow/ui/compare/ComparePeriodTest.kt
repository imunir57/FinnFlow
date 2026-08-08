package com.finnflow.ui.compare

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ComparePeriodTest {

    @Test
    fun monthLabelIsAbbreviatedWithATwoDigitYear() {
        // "Mar '26" — short enough for the chart's axis gutter, unambiguous across years.
        assertEquals("Mar '26", ComparePeriod(2026, 3).label)
        assertEquals("Dec '09", ComparePeriod(2009, 12).label)
    }

    @Test
    fun yearLabelIsJustTheYear() {
        assertEquals("2026", ComparePeriod.year(2026).label)
    }

    @Test
    fun monthRangeCoversTheWholeMonthIncludingLeapDay() {
        val feb = ComparePeriod(2024, 2)
        assertEquals(LocalDate.of(2024, 2, 1), feb.from)
        assertEquals(LocalDate.of(2024, 2, 29), feb.to)
    }

    @Test
    fun yearRangeCoversTheWholeYear() {
        val year = ComparePeriod.year(2026)
        assertEquals(LocalDate.of(2026, 1, 1), year.from)
        assertEquals(LocalDate.of(2026, 12, 31), year.to)
    }

    @Test
    fun recentMonthsAreOldestFirstAndCrossTheYearBoundary() {
        val recent = ComparePeriod.recent(
            ComparePeriodMode.MONTH,
            count = 3,
            today = LocalDate.of(2026, 1, 15)
        )
        assertEquals(
            listOf(ComparePeriod(2025, 11), ComparePeriod(2025, 12), ComparePeriod(2026, 1)),
            recent
        )
    }

    @Test
    fun recentYearsAreOldestFirst() {
        val recent = ComparePeriod.recent(
            ComparePeriodMode.YEAR,
            count = 2,
            today = LocalDate.of(2026, 8, 2)
        )
        assertEquals(listOf(ComparePeriod.year(2025), ComparePeriod.year(2026)), recent)
    }

    @Test
    fun keysDistinguishAMonthFromAYear() {
        // Both are "2026" to a careless key scheme; they must never collide in a selection set.
        assertNotEquals(ComparePeriod.year(2026).key, ComparePeriod(2026, 1).key)
    }

    @Test
    fun sortingIsChronologicalAcrossYears() {
        val unsorted = listOf(
            ComparePeriod(2026, 1),
            ComparePeriod(2025, 12),
            ComparePeriod(2026, 3)
        )
        assertEquals(
            listOf(ComparePeriod(2025, 12), ComparePeriod(2026, 1), ComparePeriod(2026, 3)),
            unsorted.sorted()
        )
    }

    @Test
    fun modeIsDerivedFromWhetherAMonthIsPresent() {
        assertEquals(ComparePeriodMode.MONTH, ComparePeriod.month(YearMonth.of(2026, 5)).mode)
        assertEquals(ComparePeriodMode.YEAR, ComparePeriod.year(2026).mode)
    }

    // ── Change percentage ─────────────────────────────────────────────────

    private fun series(vararg amounts: Double) =
        CompareSeries(CompareItem(1L, null, "X", "#F44336"), amounts.toList())

    @Test
    fun changeIsFirstToLast() {
        assertEquals(100, series(100.0, 200.0).endpointChangePercent)
        assertEquals(-50, series(200.0, 100.0).endpointChangePercent)
    }

    @Test
    fun changeIgnoresTheMiddleWhichIsWhyItMustBeLabelled() {
        // Up then down then up still reports the endpoints. This is exactly the ambiguity the
        // UI compensates for by naming the first and last period whenever there are 3+.
        assertEquals(0, series(100.0, 900.0, 100.0).endpointChangePercent)
    }

    @Test
    fun changeIsNullWhenItCannotBeExpressed() {
        assertNull("single period has nothing to compare to", series(100.0).endpointChangePercent)
        assertNull("growth from zero is not a percentage", series(0.0, 500.0).endpointChangePercent)
    }
}
