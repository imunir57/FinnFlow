package com.finnflow.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw palette values — the single place in the app where a colour literal may appear.
 *
 * Screens must never import these directly. Read colours from `MaterialTheme.colorScheme`
 * for standard Material roles, or from `FinnFlowTheme.colors` for the app-specific roles
 * Material has no slot for. See [FinnFlowColors] for the full list.
 *
 * Values are contrast-audited against WCAG AA — 4.5:1 for body text, 3:1 for large text
 * (≥18.66sp, or ≥14sp bold) and non-text UI such as borders, icons and chart strokes.
 * Ratios quoted in the comments are measured against Paper / Card / Surface in that order.
 */

// ── Light — warm paper ────────────────────────────────────────────────────────
val WarmPaper           = Color(0xFFFAF9F6)
val WarmCard            = Color(0xFFF2EFE9)
val WarmSurface         = Color(0xFFEDE9E2)
val WarmSurfaceLow      = Color(0xFFF6F4F0)
val Ink                 = Color(0xFF28221E) // 14.91 / 13.68 / 12.97
val InkMedium           = Color(0xFF6B6056) // 5.81 / 5.33 / 5.06
val InkFaint            = Color(0xFF948375) // 3.46 / 3.18 / 3.01 — large text & UI only
val Rule                = Color(0xFFDDD8D0) // decorative dividers only, not a border
val Outline             = Color(0xFF988870) // 3.27 / 3.00 — borders, focus rings
val IncomeGreen         = Color(0xFF2D6B41) // 6.06 / 5.56
val ExpenseClay         = Color(0xFFB5452B) // 5.18 / 4.75
val IncomeContainer     = Color(0xFFCCE8D6)
val OnIncomeContainer   = Color(0xFF0D3D21) // 9.42 on IncomeContainer
val ExpenseContainer    = Color(0xFFF5D5CC)
val OnExpenseContainer  = Color(0xFF4A1508) // 10.89 on ExpenseContainer
val DisabledText        = Color(0xFFA5A19C) // Ink flattened at 38% onto Card
val DisabledContainer   = Color(0xFFDAD6D1) // Ink flattened at 12% onto Card
val SelectedRow         = Color(0xFFEDECE9) // Ink at 6% onto Paper
val NavPill             = Color(0xFFE9E8E5) // Ink at 8% onto Paper

// ── Dark — warm near-black ────────────────────────────────────────────────────
val DarkPaper           = Color(0xFF1B1815)
val DarkCard            = Color(0xFF262220)
val DarkSurface         = Color(0xFF2E2926)
val DarkSurfaceLow      = Color(0xFF201C19)
val IvoryInk            = Color(0xFFF3EFE9) // 15.43 / 13.76 / 12.55
val IvoryInkMedium      = Color(0xFFC7BFB5) // 9.72 / 8.67 / 7.90
val IvoryInkFaint       = Color(0xFF8C8377) // 4.74 / 4.22 / 3.85 — large text & UI only
val DarkRule            = Color(0xFF3A342F) // decorative dividers only, not a border
val OutlineDark         = Color(0xFF766A5F) // 3.36 / 3.00 — borders, focus rings
val IncomeGreenDark     = Color(0xFF6FCB8C) // 8.92 / 7.96
val ExpenseClayDark     = Color(0xFFE08469) // 6.46 / 5.76
val IncomeContainerDark = Color(0xFF14361F)
val ExpenseContainerDark = Color(0xFF4A1508)
val DisabledTextDark    = Color(0xFF74706C) // 3.21 on DarkCard
val DisabledContainerDark = Color(0xFF3F3B38)
val SelectedRowDark     = Color(0xFF312E2A) // IvoryInk at 10% onto DarkPaper
val NavPillDark         = Color(0xFF4A4541) // IvoryInk at 14% onto DarkPaper

// ── Hero card — fixed dark surface, identical in both themes ──────────────────
// Verified against the worst-case gradient stop (#1A2820): white 15.35,
// white @60% 6.42, income accent 7.68, expense accent 6.04. All pass.
val HeroGradientStart   = Color(0xFF1A2820)
val HeroGradientMid     = Color(0xFF1E1916)
val HeroGradientEnd     = Color(0xFF241410)
val HeroIncomeAccent    = Color(0xFF78C898)
val HeroExpenseAccent   = Color(0xFFDC9070)

// ── Chart slice palette ───────────────────────────────────────────────────────
// Positional colours for donut slices and legends, used where a category has no colour of
// its own. Dark-mode variants are derived from these by the same OKLCH rule that adapts
// stored category colours, so the two palettes stay consistent — see CategoryColor.kt.
val ChartSlicePalette = listOf(
    Color(0xFFE24B4A), Color(0xFF378ADD), Color(0xFF1D9E75),
    // Was #EF9F27 — 2.07:1 on WarmPaper, invisible as a legend swatch. Deepened along its own
    // hue to 3.34:1, the least that clears the non-text threshold.
    Color(0xFFBC7C1C), Color(0xFF7F77DD), Color(0xFFD4537E),
    Color(0xFF639922), Color(0xFF888780)
)

/** Hue the Category Detail donut's shade ramp is built from. See `shadeRamp`. */
val ShadeRampBase = Color(0xFFE24B4A)
const val SHADE_RAMP_STEPS = 6

// ── Row accents ───────────────────────────────────────────────────────────────
// Per-row icon tints on Settings and Profile. Muted on purpose — they identify a row, they
// don't signal state. Pass through `FinnFlowTheme.colors.adapt()` at the call site so they
// lift for dark mode like every other data colour.
val AccentEmail      = Color(0xFF3A6EA5)
val AccentCloud      = Color(0xFF2E8B94)
val AccentPrivacy    = Color(0xFF7A5C3E)
val AccentCalendar   = Color(0xFF7A4FA0)
val AccentCurrency   = Color(0xFF3E4A8A)
val AccentCategories = Color(0xFF7A5C3E)
val AccentNotify     = Color(0xFFB5456E)
val AccentBackup     = Color(0xFF3A6EA5)
val AccentRestore    = Color(0xFF2E8B94)
val AccentExport     = Color(0xFF7A4FA0)
// Was #D18842 — 2.75:1 on WarmPaper. Deepened along its own hue to 3.19:1.
val AccentAppearance = Color(0xFFC17E3C)
val AccentAppLock    = Color(0xFF556B74)
val AccentAbout      = Color(0xFF6E8A4A)

/** Pale end of the day-of-week spending heatmap in light mode. */
val HeatmapLow = Color(0xFFF5E8E4)

// ── Theme-independent ─────────────────────────────────────────────────────────
val ScrimBlack          = Color(0xFF000000)
