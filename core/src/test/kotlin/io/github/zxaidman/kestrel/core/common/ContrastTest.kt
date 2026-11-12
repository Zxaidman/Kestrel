package io.github.zxaidman.kestrel.core.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContrastTest {

    private fun hex(s: String): Int = (0xFF000000.toInt()) or s.removePrefix("#").toInt(16)

    // The theme's two inks and three grounds, exactly as KestrelTheme defines them.
    private val inkDark = hex("1A1B21")
    private val inkLight = hex("E4E1E9")
    private val grounds = mapOf(
        "light" to hex("FBFAFE"),
        "grey" to hex("131318"),
        "black" to hex("000000"),
    )

    private fun translucent(rgb: String, alpha: Double): Int =
        ((alpha * 255).toInt() shl 24) or rgb.removePrefix("#").toInt(16)

    @Test
    fun `the reference pairs come out where WCAG puts them`() {
        assertEquals(21.0, Contrast.ratio(hex("000000"), hex("FFFFFF")), 0.01)
        assertEquals(1.0, Contrast.ratio(hex("777777"), hex("777777")), 0.0001)
    }

    /**
     * `BUG-55` in the form it shipped: the theme's light ink on a proven tile, grey dark theme.
     * This is the number the design system measured, and the assertion that it fails is what keeps
     * the fix from being undone by someone "simplifying" back to one ink.
     */
    @Test
    fun `one fixed ink fails on a bright tile in the dark theme`() {
        val tile = Contrast.composite(translucent("5FBF77", 0.85), grounds.getValue("grey"))
        assertTrue(Contrast.ratio(inkLight, tile) < Contrast.TEXT_MINIMUM)
    }

    /**
     * The fix: every tile state, in every theme, gets an ink that passes. Includes the dim partial
     * tile in the dark themes, which is the one a fixed dark ink would fail.
     */
    @Test
    fun `the chosen ink passes on every tile state in every theme`() {
        val fills = listOf(
            "proven" to translucent("5FBF77", 0.85),
            "held" to translucent("8C9EFF", 0.85),
            "partial" to translucent("F2B441", 0.35),
        )
        for ((theme, ground) in grounds) {
            for ((state, fill) in fills) {
                val drawn = Contrast.composite(fill, ground)
                val ink = Contrast.inkFor(drawn, inkDark, inkLight)
                val ratio = Contrast.ratio(ink, drawn)
                assertTrue(ratio >= Contrast.TEXT_MINIMUM, "$state tile, $theme theme: $ratio")
            }
        }
    }

    @Test
    fun `a dim fill takes light ink and a bright one takes dark ink`() {
        val partialOnGrey = Contrast.composite(translucent("F2B441", 0.35), grounds.getValue("grey"))
        assertEquals(inkLight, Contrast.inkFor(partialOnGrey, inkDark, inkLight))
        val provenOnGrey = Contrast.composite(translucent("5FBF77", 0.85), grounds.getValue("grey"))
        assertEquals(inkDark, Contrast.inkFor(provenOnGrey, inkDark, inkLight))
    }

    /** The light-theme signal text values chosen for `BUG-55`, on the plain surface. */
    @Test
    fun `the light-theme signal text values pass on the light surface`() {
        val surface = grounds.getValue("light")
        assertTrue(Contrast.ratio(hex("7D5200"), surface) >= Contrast.TEXT_MINIMUM)
        assertTrue(Contrast.ratio(hex("256B3A"), surface) >= Contrast.TEXT_MINIMUM)
        // And the originals did not, which is the bug.
        assertTrue(Contrast.ratio(hex("F2B441"), surface) < Contrast.TEXT_MINIMUM)
    }
}
