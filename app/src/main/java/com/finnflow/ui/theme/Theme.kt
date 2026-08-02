package com.finnflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * App-specific colour roles Material 3 has no slot for.
 *
 * Anything a screen needs that isn't a standard Material role lives here, so no screen
 * ever needs a colour literal. Read it through [FinnFlowTheme.colors].
 */
@Immutable
data class FinnFlowColors(
    /** True when the dark scheme is active. Drives render-time colour adaptation. */
    val isDark: Boolean,

    /** Income amounts, positive deltas, the income dot. */
    val income: Color,
    /** Expense amounts, negative deltas, the expense dot. Doubles as the error colour. */
    val expense: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,

    /**
     * De-emphasised text. Clears 3:1 but not 4.5:1, so it is valid **only** for large text
     * (≥18.66sp, or ≥14sp bold), purely decorative labels, and disabled text. Body-size text
     * that carries real information must use `colorScheme.onSurfaceVariant` instead.
     */
    val inkFaint: Color,

    val disabledText: Color,
    val disabledContainer: Color,

    /** Interaction state overlays, drawn over the surface beneath. */
    val hoverOverlay: Color,
    val pressedOverlay: Color,
    /** Background for the currently selected / highlighted list row. */
    val selectedRow: Color,
    /** Pill behind the active bottom-navigation tab. */
    val navPill: Color,

    /** Hero card — a fixed dark surface, identical in both themes. */
    val heroGradient: Brush,
    val heroOnSurface: Color,
    val heroOnSurfaceVariant: Color,
    val heroIncome: Color,
    val heroExpense: Color,

    /** Positional donut/legend colours, already adapted for the active theme. */
    val chartSlices: List<Color>,
    /** Single-hue ramp for breaking one category into sub-category shades. */
    val subCategoryShades: List<Color>,
) {
    /**
     * A text colour that reads on top of [background] — for labels drawn inside a chart slice,
     * where the fill is a data colour rather than a theme surface. Dark-mode slices are lifted
     * to L≥0.72, so a fixed white label would be illegible on them.
     */
    fun onChartSlice(background: Color): Color = com.finnflow.ui.theme.onChartSlice(background)

    /**
     * Adapts a fixed data colour — a row accent, a chart base — to the active theme, using the
     * same OKLCH rule that adapts stored category colours. A no-op in light mode.
     */
    fun adapt(color: Color): Color = if (isDark) adaptForDark(color) else color
}

private val OnLightSlice = Ink
private val OnDarkSlice = Color.White

/**
 * A text colour that reads on top of [background] — for labels drawn inside a chart slice,
 * where the fill is a data colour rather than a theme surface.
 *
 * Picks whichever of the two label colours actually measures better. A fixed luminance
 * crossover gets mid-lightness fills wrong in both directions: amber is light enough to look
 * pale yet still needs dark text, and a dark-adapted red is the reverse.
 */
fun onChartSlice(background: Color): Color =
    if (contrastAgainst(OnLightSlice, background) >= contrastAgainst(OnDarkSlice, background))
        OnLightSlice else OnDarkSlice

/** WCAG contrast ratio between two opaque colours. */
private fun contrastAgainst(foreground: Color, background: Color): Float {
    val a = foreground.luminance()
    val b = background.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

private val HeroBrush = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to HeroGradientStart,
        0.4f to HeroGradientMid,
        1.0f to HeroGradientEnd
    ),
    start = Offset(0f, 0f),
    end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

private val LightColors = FinnFlowColors(
    isDark               = false,
    income               = IncomeGreen,
    expense              = ExpenseClay,
    incomeContainer      = IncomeContainer,
    onIncomeContainer    = OnIncomeContainer,
    expenseContainer     = ExpenseContainer,
    onExpenseContainer   = OnExpenseContainer,
    inkFaint             = InkFaint,
    disabledText         = DisabledText,
    disabledContainer    = DisabledContainer,
    hoverOverlay         = Ink.copy(alpha = 0.08f),
    pressedOverlay       = Ink.copy(alpha = 0.12f),
    selectedRow          = SelectedRow,
    navPill              = NavPill,
    heroGradient         = HeroBrush,
    heroOnSurface        = Color.White,
    heroOnSurfaceVariant = Color.White.copy(alpha = 0.6f),
    heroIncome           = HeroIncomeAccent,
    heroExpense          = HeroExpenseAccent,
    chartSlices          = ChartSlicePalette,
    subCategoryShades    = shadeRamp(ShadeRampBase, SHADE_RAMP_STEPS, isDark = false),
)

private val DarkColors = FinnFlowColors(
    isDark               = true,
    income               = IncomeGreenDark,
    expense              = ExpenseClayDark,
    incomeContainer      = IncomeContainerDark,
    onIncomeContainer    = IncomeContainer,
    expenseContainer     = ExpenseContainerDark,
    onExpenseContainer   = ExpenseContainer,
    inkFaint             = IvoryInkFaint,
    disabledText         = DisabledTextDark,
    disabledContainer    = DisabledContainerDark,
    hoverOverlay         = IvoryInk.copy(alpha = 0.10f),
    pressedOverlay       = IvoryInk.copy(alpha = 0.14f),
    selectedRow          = SelectedRowDark,
    navPill              = NavPillDark,
    heroGradient         = HeroBrush,
    heroOnSurface        = Color.White,
    heroOnSurfaceVariant = Color.White.copy(alpha = 0.6f),
    heroIncome           = HeroIncomeAccent,
    heroExpense          = HeroExpenseAccent,
    chartSlices          = ChartSlicePalette.map(::adaptForDark),
    subCategoryShades    = shadeRamp(ShadeRampBase, SHADE_RAMP_STEPS, isDark = true),
)

private val WarmScheme = lightColorScheme(
    primary               = Ink,
    onPrimary             = WarmPaper,
    primaryContainer      = WarmCard,
    onPrimaryContainer    = Ink,
    inversePrimary        = IvoryInk,
    secondary             = IncomeGreen,
    onSecondary           = WarmPaper,
    secondaryContainer    = IncomeContainer,
    onSecondaryContainer  = OnIncomeContainer,
    tertiary              = ExpenseClay,
    onTertiary            = WarmPaper,
    tertiaryContainer     = ExpenseContainer,
    onTertiaryContainer   = OnExpenseContainer,
    background            = WarmPaper,
    onBackground          = Ink,
    surface               = WarmPaper,
    onSurface             = Ink,
    surfaceVariant        = WarmCard,
    onSurfaceVariant      = InkMedium,
    surfaceContainerLowest = WarmPaper,
    surfaceContainerLow   = WarmSurfaceLow,
    surfaceContainer      = WarmCard,
    surfaceContainerHigh  = WarmSurface,
    surfaceContainerHighest = WarmSurface,
    inverseSurface        = Ink,
    inverseOnSurface      = WarmPaper,
    outline               = Outline,
    outlineVariant        = Rule,
    error                 = ExpenseClay,
    onError               = WarmPaper,
    errorContainer        = ExpenseContainer,
    onErrorContainer      = OnExpenseContainer,
    scrim                 = ScrimBlack,
)

private val DarkScheme = darkColorScheme(
    primary               = IvoryInk,
    onPrimary             = DarkPaper,
    primaryContainer      = DarkCard,
    onPrimaryContainer    = IvoryInk,
    inversePrimary        = Ink,
    secondary             = IncomeGreenDark,
    onSecondary           = DarkPaper,
    secondaryContainer    = IncomeContainerDark,
    onSecondaryContainer  = IncomeContainer,
    tertiary              = ExpenseClayDark,
    onTertiary            = DarkPaper,
    tertiaryContainer     = ExpenseContainerDark,
    onTertiaryContainer   = ExpenseContainer,
    background            = DarkPaper,
    onBackground          = IvoryInk,
    surface               = DarkPaper,
    onSurface             = IvoryInk,
    surfaceVariant        = DarkCard,
    onSurfaceVariant      = IvoryInkMedium,
    surfaceContainerLowest = DarkPaper,
    surfaceContainerLow   = DarkSurfaceLow,
    surfaceContainer      = DarkCard,
    surfaceContainerHigh  = DarkSurface,
    surfaceContainerHighest = DarkSurface,
    inverseSurface        = WarmPaper,
    inverseOnSurface      = Ink,
    outline               = OutlineDark,
    outlineVariant        = DarkRule,
    error                 = ExpenseClayDark,
    onError               = DarkPaper,
    errorContainer        = ExpenseContainerDark,
    onErrorContainer      = ExpenseContainer,
    scrim                 = ScrimBlack,
)

private val LocalFinnFlowColors = staticCompositionLocalOf { LightColors }

/** Accessor for the app-specific colour roles. Use alongside `MaterialTheme.colorScheme`. */
object FinnFlowTheme {
    val colors: FinnFlowColors
        @Composable @ReadOnlyComposable get() = LocalFinnFlowColors.current
}

/**
 * @param darkTheme resolved dark/light flag. Defaults to the system setting so
 * existing call sites keep working; callers that support the in-app theme
 * picker (system/light/dark) should resolve [darkTheme] themselves and pass
 * it explicitly.
 */
@Composable
fun FinnFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalFinnFlowColors provides if (darkTheme) DarkColors else LightColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else WarmScheme,
            content = content
        )
    }
}
