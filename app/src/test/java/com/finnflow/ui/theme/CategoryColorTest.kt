package com.finnflow.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Locks the OKLCH category-colour adaptation to the audited values from the theme design.
 * If [adaptForDark] drifts, these fail with the exact hex that moved.
 */
class CategoryColorTest {

    /** Stored seed hex → the dark-mode value the contrast audit was run against. */
    private val auditedPairs = listOf(
        "#F44336" to "#EF8071", // Food & Dining
        "#2196F3" to "#56AAF8", // Transportation
        "#795548" to "#C09B8E", // Housing
        "#E91E63" to "#ED7C91", // Health
        "#9C27B0" to "#CC86D9", // Education
        "#FF9800" to "#F29F49", // Shopping
        "#00BCD4" to "#35BAD0", // Entertainment
        "#607D8B" to "#8EA9B7", // Communication
        "#FF5722" to "#EE8264", // Personal Care
        "#8BC34A" to "#90C15D", // Family & Gifts
        "#3F51B5" to "#859EFC", // Finance
        "#9E9E9E" to "#A4A4A4", // Other Expense
        "#4CAF50" to "#69BB6A", // Salary
        "#009688" to "#53B8AB", // Business
    )

    @Test
    fun adaptForDark_matchesAuditedValues() {
        auditedPairs.forEach { (stored, expected) ->
            val actual = adaptForDark(parseHexColor(stored)).toHex()
            assertEquals("stored $stored", expected, actual)
        }
    }

    @Test
    fun adaptedColors_clearAaLargeTextOnDarkSurfaces() {
        // The audit's promise: every adapted colour is legible as text, not just as a swatch.
        auditedPairs.forEach { (stored, _) ->
            val adapted = adaptForDark(parseHexColor(stored))
            assertTrue(
                "$stored on DarkPaper: ${contrastRatio(adapted, DarkPaper)}",
                contrastRatio(adapted, DarkPaper) >= 4.5
            )
            assertTrue(
                "$stored on DarkCard: ${contrastRatio(adapted, DarkCard)}",
                contrastRatio(adapted, DarkCard) >= 4.5
            )
        }
    }

    @Test
    fun adaptForDark_preservesHue() {
        // Hue is what makes a category recognisably "the same colour" across themes.
        auditedPairs.forEach { (stored, _) ->
            val source = parseHexColor(stored)
            val adapted = adaptForDark(source)
            val delta = hueDegrees(source) - hueDegrees(adapted)
            val wrapped = abs(((delta + 180f).mod(360f)) - 180f)
            // Greys have no meaningful hue; 8-bit rounding moves them arbitrarily.
            if (chroma(source) > 0.02f) {
                assertTrue("$stored hue moved $wrapped°", wrapped < 1.5f)
            }
        }
    }

    @Test
    fun adaptForDark_neverDarkens() {
        // Step 2 of the rule is a floor, not a set: a category colour that is already light
        // enough must come through untouched rather than being pulled down to the floor.
        auditedPairs.forEach { (stored, _) ->
            val source = parseHexColor(stored)
            val adapted = adaptForDark(source)
            assertTrue(
                "$stored got darker: ${lightness(source)} -> ${lightness(adapted)}",
                lightness(adapted) >= lightness(source) - 0.005f
            )
        }
    }

    @Test
    fun adaptForDark_leavesLightnessAloneAboveTheFloor() {
        // Step 2 is `max`, not `set`. A colour already lighter than the floor keeps its own
        // lightness; only chroma is trimmed. (Chroma is scaled on every call, which is why the
        // rule must run on stored values only and never on its own output.)
        val pale = parseHexColor("#C7BFB5")
        val adapted = adaptForDark(pale)
        assertEquals(lightness(pale).toDouble(), lightness(adapted).toDouble(), 0.005)
        assertTrue("chroma should not grow", chroma(adapted) <= chroma(pale) + 0.001f)
    }

    @Test
    fun parseHexColor_handlesMalformedInput() {
        val fallback = parseHexColor(DEFAULT_CATEGORY_HEX)
        assertEquals(fallback, parseHexColor(null))
        assertEquals(fallback, parseHexColor(""))
        assertEquals(fallback, parseHexColor("#12"))
        assertEquals(fallback, parseHexColor("not-a-color"))
        assertEquals(fallback, parseHexColor("#GGGGGG"))
    }

    @Test
    fun parseHexColor_acceptsBothLengthsAndOptionalHash() {
        assertEquals(Color(0xFFF44336), parseHexColor("#F44336"))
        assertEquals(Color(0xFFF44336), parseHexColor("F44336"))
        assertEquals(Color(0xFFF44336), parseHexColor("  #f44336  "))
        assertEquals(Color(0x80F44336), parseHexColor("#80F44336"))
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun Color.toHex(): String = "#%02X%02X%02X".format(
        (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
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

    private fun chroma(color: Color): Float {
        val (a, b) = labAb(color)
        return kotlin.math.sqrt(a * a + b * b)
    }

    private fun lightness(color: Color): Float {
        fun lin(c: Float) =
            if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

        val r = lin(color.red)
        val g = lin(color.green)
        val b = lin(color.blue)

        val l = kotlin.math.cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
        val m = kotlin.math.cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
        val s = kotlin.math.cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)

        return 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s
    }

    private fun hueDegrees(color: Color): Float {
        val (a, b) = labAb(color)
        return Math.toDegrees(kotlin.math.atan2(b, a).toDouble()).toFloat()
    }

    /** Mirrors the production sRGB→OKLab transform so the test can assert on hue independently. */
    private fun labAb(color: Color): Pair<Float, Float> {
        fun lin(c: Float) =
            if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

        val r = lin(color.red)
        val g = lin(color.green)
        val bch = lin(color.blue)

        val l = kotlin.math.cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * bch)
        val m = kotlin.math.cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * bch)
        val s = kotlin.math.cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * bch)

        return Pair(
            1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
            0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
        )
    }
}
