package com.finnflow.ui.compare

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Caps on how much can be compared at once.
 *
 * Five is a legibility limit, not a technical one — beyond it the bar charts stop being
 * comparable at a glance. Everything downstream reads these constants rather than hard-coding
 * a count, so raising them is a one-line change.
 */
const val MAX_COMPARE_PERIODS = 5
const val MAX_COMPARE_ITEMS = 5

/** How many periods are pre-selected when the screen opens or the mode changes. */
const val DEFAULT_COMPARE_PERIODS = 2

/** Whether periods are whole months or whole years. */
enum class ComparePeriodMode { MONTH, YEAR }

/**
 * One period on the x-axis. [month] is null in [ComparePeriodMode.YEAR], which is what
 * distinguishes the two shapes rather than carrying a separate flag.
 */
data class ComparePeriod(val year: Int, val month: Int? = null) : Comparable<ComparePeriod> {

    val mode: ComparePeriodMode get() = if (month == null) ComparePeriodMode.YEAR else ComparePeriodMode.MONTH

    val from: LocalDate get() =
        if (month == null) LocalDate.of(year, 1, 1) else YearMonth.of(year, month).atDay(1)

    val to: LocalDate get() =
        if (month == null) LocalDate.of(year, 12, 31) else YearMonth.of(year, month).atEndOfMonth()

    /** `Mar '26` for a month, `2026` for a year — short enough to sit in a chart's axis gutter. */
    val label: String
        get() = if (month == null) {
            year.toString()
        } else {
            val name = java.time.Month.of(month)
                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
            "$name '${"%02d".format(year % 100)}"
        }

    /** Stable identity for list keys and selection sets. */
    val key: String get() = if (month == null) "y$year" else "m$year-${"%02d".format(month)}"

    override fun compareTo(other: ComparePeriod): Int =
        compareValuesBy(this, other, { it.year }, { it.month ?: 0 })

    companion object {
        fun month(ym: YearMonth) = ComparePeriod(ym.year, ym.monthValue)
        fun year(year: Int) = ComparePeriod(year, null)

        /** The [count] most recent periods of [mode], oldest first. */
        fun recent(mode: ComparePeriodMode, count: Int, today: LocalDate = LocalDate.now()): List<ComparePeriod> =
            when (mode) {
                ComparePeriodMode.MONTH -> {
                    val current = YearMonth.from(today)
                    (0 until count).map { month(current.minusMonths(it.toLong())) }.reversed()
                }
                ComparePeriodMode.YEAR ->
                    (0 until count).map { year(today.year - it) }.reversed()
            }
    }
}

/**
 * One thing being compared: either a whole category, or a single subcategory within one.
 * [subCategoryId] null means the whole category.
 */
data class CompareItem(
    val categoryId: Long,
    val subCategoryId: Long?,
    val name: String,
    val colorHex: String,
    /** Owning category's icon — a subcategory inherits its parent's. */
    val iconName: String = "",
    /** Owning category's name, shown when this item is a subcategory. */
    val parentName: String? = null
) {
    val key: String get() = "$categoryId:${subCategoryId ?: "all"}"
    val isWholeCategory: Boolean get() = subCategoryId == null

    /** Second line of the chart header: what this row actually covers. */
    val breadcrumb: String get() = parentName ?: "Whole category"
}

/** One item's totals across the selected periods, index-aligned with them. */
data class CompareSeries(
    val item: CompareItem,
    val amounts: List<Double>
) {
    val max: Double get() = amounts.maxOrNull() ?: 0.0
    val hasAnyData: Boolean get() = amounts.any { it > 0.0 }

    /**
     * Change from the first period to the last, as a percentage, or null when it can't be
     * expressed — fewer than two periods, or a first period of zero (any increase from nothing
     * is an infinite rise, not a percentage).
     *
     * With more than two periods this is deliberately *only* the endpoints: the caller is
     * expected to label it with the two period names, because a single figure across three or
     * more points hides everything in between.
     */
    val endpointChangePercent: Int?
        get() {
            if (amounts.size < 2) return null
            val first = amounts.first()
            val last = amounts.last()
            if (first <= 0.0) return null
            return (((last - first) / first) * 100).toInt()
        }
}
