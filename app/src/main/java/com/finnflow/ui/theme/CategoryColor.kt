package com.finnflow.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Category colours are chosen by the user and persisted as a hex string, so their stored
 * values can't be swapped out for a dark-theme palette without a migration. Instead they are
 * adapted at render time.
 *
 * The rule, in OKLCH:
 *  1. convert the stored sRGB hex to OKLCH
 *  2. `L' = max(L, 0.72)` — lift lightness enough to read on near-black, never darken
 *  3. `C' = min(C * 0.9, 0.14)` — trim and cap chroma so lifted colours don't vibrate
 *  4. leave H untouched — this is what keeps a category recognisably the same colour
 *  5. convert back to sRGB
 *
 * Every adapted colour clears 5.9:1 against `DarkCard` and 6.6:1 against `DarkPaper`, so they
 * are safe as text as well as swatches.
 */

/** Fallback used when a category has no colour, or its stored value doesn't parse. */
const val DEFAULT_CATEGORY_HEX = "#607D8B"

/**
 * Parses a `#RRGGBB` / `#AARRGGBB` string, falling back to [DEFAULT_CATEGORY_HEX] when the
 * value is missing or malformed. Pure Kotlin — no Android framework dependency, so it is
 * usable from unit tests.
 */
fun parseHexColor(hex: String?, fallback: Color = Color(0xFF607D8B)): Color {
    val raw = hex?.trim()?.removePrefix("#") ?: return fallback
    return when (raw.length) {
        6 -> raw.toLongOrNull(16)?.let { Color(it or 0xFF000000L) } ?: fallback
        8 -> raw.toLongOrNull(16)?.let { Color(it) } ?: fallback
        else -> fallback
    }
}

/**
 * Applies the dark-mode adaptation rule to [color]. Returns [color] unchanged when it already
 * sits above the lightness floor and within the chroma cap.
 */
fun adaptForDark(color: Color): Color {
    val (l, a, b) = srgbToOklab(color)
    val chroma = sqrt(a * a + b * b)
    val hue = atan2(b, a)

    val newL = maxOf(l, LIGHTNESS_FLOOR)
    val newC = min(chroma * CHROMA_SCALE, CHROMA_CAP)
    if (newL == l && newC == chroma) return color

    return oklabToSrgb(newL, newC * cos(hue), newC * sin(hue), color.alpha)
}

/**
 * The colour to draw a category in, for the active theme: the stored hex in light mode, the
 * adapted equivalent in dark mode.
 */
@Composable
@ReadOnlyComposable
fun categoryColor(hex: String?): Color {
    val isDark = FinnFlowTheme.colors.isDark
    val parsed = parseHexColor(hex)
    return if (isDark) adaptForDark(parsed) else parsed
}

/** [categoryColor] with the conversion memoised — prefer this inside list rows. */
@Composable
fun rememberCategoryColor(hex: String?): Color {
    val isDark = FinnFlowTheme.colors.isDark
    return remember(hex, isDark) {
        val parsed = parseHexColor(hex)
        if (isDark) adaptForDark(parsed) else parsed
    }
}

/**
 * Builds a [count]-step ramp in [base]'s hue, for charts that break a single category into
 * shades of itself.
 *
 * The ramp runs *away* from the background — darker on paper, lighter on near-black — so every
 * step keeps at least 3:1 against it. A single fixed ramp can't do that: shades light enough to
 * read on a dark background are invisible on a light one.
 */
fun shadeRamp(base: Color, count: Int, isDark: Boolean): List<Color> {
    if (count <= 0) return emptyList()
    val (_, a, b) = srgbToOklab(base)
    val baseChroma = sqrt(a * a + b * b)
    val hue = atan2(b, a)

    return List(count) { index ->
        val t = if (count == 1) 0f else index.toFloat() / (count - 1)
        val lightness: Float
        val chroma: Float
        if (isDark) {
            lightness = RAMP_DARK_START + (RAMP_DARK_END - RAMP_DARK_START) * t
            // Chroma has to fall as lightness rises or the pale end turns neon.
            chroma = min(baseChroma * CHROMA_SCALE, CHROMA_CAP) * (1f - 0.6f * t)
        } else {
            lightness = RAMP_LIGHT_START + (RAMP_LIGHT_END - RAMP_LIGHT_START) * t
            chroma = min(baseChroma, RAMP_LIGHT_CHROMA_CAP)
        }
        oklabToSrgb(lightness, chroma * cos(hue), chroma * sin(hue), base.alpha)
    }
}

// ── OKLab conversion (Björn Ottosson) ─────────────────────────────────────────

private const val LIGHTNESS_FLOOR = 0.72f
private const val CHROMA_SCALE = 0.9f
private const val CHROMA_CAP = 0.14f

// Shade-ramp lightness bands, chosen so both ends clear 3:1 against their own background.
private const val RAMP_LIGHT_START = 0.55f
private const val RAMP_LIGHT_END = 0.32f
private const val RAMP_LIGHT_CHROMA_CAP = 0.16f
private const val RAMP_DARK_START = 0.72f
private const val RAMP_DARK_END = 0.93f

private data class Oklab(val l: Float, val a: Float, val b: Float)

private fun srgbToLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

private fun linearToSrgb(c: Float): Float =
    if (c <= 0.0031308f) c * 12.92f else 1.055f * c.toDouble().pow(1.0 / 2.4).toFloat() - 0.055f

private fun srgbToOklab(color: Color): Oklab {
    val r = srgbToLinear(color.red)
    val g = srgbToLinear(color.green)
    val b = srgbToLinear(color.blue)

    val lCone = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b
    val mCone = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b
    val sCone = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b

    val l = cbrt(lCone)
    val m = cbrt(mCone)
    val s = cbrt(sCone)

    return Oklab(
        l = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
        a = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
        b = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
    )
}

private fun oklabToSrgb(l: Float, a: Float, b: Float, alpha: Float): Color {
    val lCone = l + 0.3963377774f * a + 0.2158037573f * b
    val mCone = l - 0.1055613458f * a - 0.0638541728f * b
    val sCone = l - 0.0894841775f * a - 1.2914855480f * b

    val l3 = lCone * lCone * lCone
    val m3 = mCone * mCone * mCone
    val s3 = sCone * sCone * sCone

    val r = 4.0767416621f * l3 - 3.3077115913f * m3 + 0.2309699292f * s3
    val g = -1.2684380046f * l3 + 2.6097574011f * m3 - 0.3413193965f * s3
    val bl = -0.0041960863f * l3 - 0.7034186147f * m3 + 1.7076147010f * s3

    return Color(
        red = quantise(linearToSrgb(r)),
        green = quantise(linearToSrgb(g)),
        blue = quantise(linearToSrgb(bl)),
        alpha = alpha
    )
}

/**
 * Rounds to the nearest 8-bit channel value. Without this the result drifts by a fraction of
 * a step from the audited hex values, which makes the design doc's table impossible to verify.
 */
private fun quantise(channel: Float): Float =
    (channel.coerceIn(0f, 1f) * 255f).roundToInt() / 255f
