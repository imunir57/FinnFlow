package com.finnflow.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Pins the palette to the WCAG AA thresholds the theme audit was signed off against.
 *
 * Thresholds: 4.5:1 for body text, 3:1 for large text (≥18.66sp, or ≥14sp bold) and non-text
 * UI such as borders, icons and chart strokes. A token that drops below its threshold fails
 * here with the measured ratio, rather than shipping as an unreadable screen.
 */
class ThemeContrastTest {

    private val lightSurfaces = listOf(
        "WarmPaper" to WarmPaper,
        "WarmCard" to WarmCard,
        "WarmSurface" to WarmSurface,
    )

    private val darkSurfaces = listOf(
        "DarkPaper" to DarkPaper,
        "DarkCard" to DarkCard,
        "DarkSurface" to DarkSurface,
    )

    @Test
    fun bodyText_clearsAaOnEverySurface() {
        val bodyTokens = listOf(
            "Ink" to Ink,
            "InkMedium" to InkMedium,
        )
        bodyTokens.forEach { (name, color) ->
            lightSurfaces.forEach { (surfaceName, surface) ->
                assertAtLeast(4.5, color, surface, "$name on $surfaceName")
            }
        }

        val darkBodyTokens = listOf(
            "IvoryInk" to IvoryInk,
            "IvoryInkMedium" to IvoryInkMedium,
        )
        darkBodyTokens.forEach { (name, color) ->
            darkSurfaces.forEach { (surfaceName, surface) ->
                assertAtLeast(4.5, color, surface, "$name on $surfaceName")
            }
        }
    }

    @Test
    fun faintText_clearsLargeTextThresholdOnly() {
        // The audit deliberately corrected these to 3:1, not 4.5:1 — a faint role forced to
        // 4.5:1 lands on top of the medium role and stops being a separate step. Their usage
        // is restricted to large text, decorative labels and disabled state.
        lightSurfaces.forEach { (surfaceName, surface) ->
            assertAtLeast(3.0, InkFaint, surface, "InkFaint on $surfaceName")
        }
        darkSurfaces.forEach { (surfaceName, surface) ->
            assertAtLeast(3.0, IvoryInkFaint, surface, "IvoryInkFaint on $surfaceName")
        }
    }

    @Test
    fun moneyColors_clearAaAsText() {
        assertAtLeast(4.5, IncomeGreen, WarmPaper, "IncomeGreen on WarmPaper")
        assertAtLeast(4.5, IncomeGreen, WarmCard, "IncomeGreen on WarmCard")
        assertAtLeast(4.5, ExpenseClay, WarmPaper, "ExpenseClay on WarmPaper")
        assertAtLeast(4.5, ExpenseClay, WarmCard, "ExpenseClay on WarmCard")

        assertAtLeast(4.5, IncomeGreenDark, DarkPaper, "IncomeGreenDark on DarkPaper")
        assertAtLeast(4.5, IncomeGreenDark, DarkCard, "IncomeGreenDark on DarkCard")
        assertAtLeast(4.5, ExpenseClayDark, DarkPaper, "ExpenseClayDark on DarkPaper")
        assertAtLeast(4.5, ExpenseClayDark, DarkCard, "ExpenseClayDark on DarkCard")
    }

    @Test
    fun outline_clearsNonTextThresholdWhereRuleDoesNot() {
        // This split is the fix for the audit's second finding: one divider token was doing
        // two jobs. Rule stays decorative; Outline is the one borders and focus rings use.
        assertAtLeast(3.0, Outline, WarmPaper, "Outline on WarmPaper")
        assertAtLeast(3.0, Outline, WarmCard, "Outline on WarmCard")
        assertAtLeast(3.0, OutlineDark, DarkPaper, "OutlineDark on DarkPaper")
        assertAtLeast(3.0, OutlineDark, DarkCard, "OutlineDark on DarkCard")

        assertTrue(
            "Rule should stay below 3:1 — if it clears it, the Outline split is redundant",
            contrastRatio(Rule, WarmPaper) < 3.0
        )
    }

    @Test
    fun containerPairs_clearAa() {
        assertAtLeast(4.5, OnIncomeContainer, IncomeContainer, "OnIncomeContainer")
        assertAtLeast(4.5, OnExpenseContainer, ExpenseContainer, "OnExpenseContainer")
        assertAtLeast(4.5, IncomeContainer, IncomeContainerDark, "dark income container pair")
        assertAtLeast(4.5, ExpenseContainer, ExpenseContainerDark, "dark expense container pair")
    }

    @Test
    fun heroCard_readableAgainstItsWorstGradientStop() {
        // The gradient is a fixed dark surface in both themes, so it is checked once against
        // its lightest stop — the worst case for white-ish foregrounds.
        val worstStop = listOf(HeroGradientStart, HeroGradientMid, HeroGradientEnd)
            .maxBy { relativeLuminance(it) }

        assertAtLeast(4.5, Color.White, worstStop, "hero white text")
        assertAtLeast(4.5, flatten(Color.White.copy(alpha = 0.6f), worstStop), worstStop,
            "hero white @60%")
        assertAtLeast(4.5, HeroIncomeAccent, worstStop, "hero income accent")
        assertAtLeast(4.5, HeroExpenseAccent, worstStop, "hero expense accent")
    }

    @Test
    fun navPillAndSelectedRow_keepTheirTextReadable() {
        assertAtLeast(4.5, Ink, NavPill, "Ink on NavPill")
        assertAtLeast(4.5, IvoryInk, NavPillDark, "IvoryInk on NavPillDark")
        assertAtLeast(4.5, Ink, SelectedRow, "Ink on SelectedRow")
        assertAtLeast(4.5, IvoryInk, SelectedRowDark, "IvoryInk on SelectedRowDark")
    }

    @Test
    fun chartSlices_stayVisibleInBothThemes() {
        ChartSlicePalette.forEachIndexed { index, slice ->
            assertAtLeast(3.0, slice, WarmPaper, "slice $index on WarmPaper")
            assertAtLeast(3.0, adaptForDark(slice), DarkPaper, "adapted slice $index on DarkPaper")
            assertAtLeast(3.0, adaptForDark(slice), DarkCard, "adapted slice $index on DarkCard")
        }
    }

    @Test
    fun chartSliceLabels_readOnTheirOwnSlice() {
        // Slice labels are drawn inside the arc, so their contrast is against the data colour,
        // not a theme surface.
        val cases = ChartSlicePalette.map { false to it } +
            ChartSlicePalette.map { true to adaptForDark(it) } +
            shadeRamp(ShadeRampBase, SHADE_RAMP_STEPS, isDark = false).map { false to it } +
            shadeRamp(ShadeRampBase, SHADE_RAMP_STEPS, isDark = true).map { true to it }

        cases.forEach { (isDark, slice) ->
            assertAtLeast(3.0, onChartSlice(slice), slice, "label on slice $slice (isDark=$isDark)")
        }
    }

    @Test
    fun shadeRamps_stayVisibleAgainstTheirOwnBackground() {
        // The single fixed ramp this replaced had steps invisible on paper. Both generated
        // ramps must clear 3:1 end to end.
        shadeRamp(ShadeRampBase, SHADE_RAMP_STEPS, isDark = false).forEachIndexed { i, shade ->
            assertAtLeast(3.0, shade, WarmPaper, "light ramp step $i on WarmPaper")
            assertAtLeast(3.0, shade, WarmCard, "light ramp step $i on WarmCard")
        }
        shadeRamp(ShadeRampBase, SHADE_RAMP_STEPS, isDark = true).forEachIndexed { i, shade ->
            assertAtLeast(3.0, shade, DarkPaper, "dark ramp step $i on DarkPaper")
            assertAtLeast(3.0, shade, DarkCard, "dark ramp step $i on DarkCard")
        }
    }

    @Test
    fun rowAccents_stayVisibleInBothThemes() {
        val accents = mapOf(
            "AccentEmail" to AccentEmail,
            "AccentCloud" to AccentCloud,
            "AccentPrivacy" to AccentPrivacy,
            "AccentCalendar" to AccentCalendar,
            "AccentCurrency" to AccentCurrency,
            "AccentCategories" to AccentCategories,
            "AccentNotify" to AccentNotify,
            "AccentBackup" to AccentBackup,
            "AccentRestore" to AccentRestore,
            "AccentExport" to AccentExport,
            "AccentAppearance" to AccentAppearance,
            "AccentAppLock" to AccentAppLock,
            "AccentAbout" to AccentAbout,
        )
        accents.forEach { (name, base) ->
            assertAtLeast(3.0, base, WarmPaper, "$name on WarmPaper")
            assertAtLeast(3.0, adaptForDark(base), DarkPaper, "$name adapted on DarkPaper")
            assertAtLeast(3.0, adaptForDark(base), DarkCard, "$name adapted on DarkCard")
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun assertAtLeast(threshold: Double, fg: Color, bg: Color, label: String) {
        val ratio = contrastRatio(fg, bg)
        // Labels can contain '%' (e.g. "white @60%"), so they are concatenated, not formatted.
        val measured = "%.2f".format(ratio)
        val needed = "%.1f".format(threshold)
        assertTrue("$label: $measured:1, needs $needed:1", ratio >= threshold)
    }

    /** Composites a translucent foreground onto an opaque background. */
    private fun flatten(fg: Color, bg: Color): Color = Color(
        red = fg.red * fg.alpha + bg.red * (1 - fg.alpha),
        green = fg.green * fg.alpha + bg.green * (1 - fg.alpha),
        blue = fg.blue * fg.alpha + bg.blue * (1 - fg.alpha)
    )

    private fun relativeLuminance(color: Color): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun Color.luminance(): Float = relativeLuminance(this).toFloat()
}
