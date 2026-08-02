package com.finnflow.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.finnflow.data.model.Currency
import kotlin.math.floor

/** How many decimals a rendered amount carries. */
enum class AmountStyle {
    /** Two decimals only when the value actually has a fractional part. */
    Auto,

    /** Always rounded to whole units — for dense charts, legends and stat strips. */
    Whole
}

/**
 * The single definition of how a money value is rendered in this app.
 *
 * No screen should format an amount or write a currency symbol itself. Read the active
 * instance from [LocalCurrencyFormat], which [MainNavHost] seeds from the user's profile,
 * so changing the currency in Settings re-renders every visible screen with no restart.
 *
 * Grouping and decimal separators follow the device locale, matching the behaviour the
 * per-screen helpers this replaced already had.
 */
@Immutable
data class CurrencyFormat(val currency: Currency = Currency.BDT) {

    /** The bare symbol, for the places that lay the glyph out separately from the digits. */
    val symbol: String get() = currency.symbol

    /** Grouped digits only, no symbol — for signed deltas and axis labels. */
    fun amount(value: Double, style: AmountStyle = AmountStyle.Auto): String = when (style) {
        AmountStyle.Whole -> "%,.0f".format(value)
        AmountStyle.Auto ->
            if (value == floor(value)) "%,.0f".format(value) else "%,.2f".format(value)
    }

    /**
     * Symbol followed by the grouped digits. [spaced] controls the thin gap between the
     * two; dense cards set it to `false` so a long figure still fits on one line.
     */
    fun format(
        value: Double,
        style: AmountStyle = AmountStyle.Auto,
        spaced: Boolean = true
    ): String = if (spaced) "$symbol ${amount(value, style)}" else symbol + amount(value, style)
}

/**
 * Ambient currency for the composition. Defaults to the same currency [com.finnflow.data.profile.UserProfile]
 * does, so previews and any screen rendered before DataStore has been read — Onboarding, most
 * of all — show the default rather than a blank.
 */
val LocalCurrencyFormat = staticCompositionLocalOf { CurrencyFormat() }
