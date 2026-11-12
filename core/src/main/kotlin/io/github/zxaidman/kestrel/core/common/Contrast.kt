package io.github.zxaidman.kestrel.core.common

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Contrast between two colours, by the WCAG 2 definition, on plain ARGB integers.
 *
 * **Here rather than in a screen, because the screen got it wrong.** The test ground drew its tile
 * labels in the theme's ordinary text colour. That colour is right on the page and wrong on a bright
 * fill: light text on a green tile measured 2.33 : 1 in the grey dark theme, where 4.5 is the floor
 * (`BUG-55`). Nobody reported it; it was measured while the design system was being written down.
 *
 * No single ink is right for every tile, which is the part that makes this worth a function: a
 * proven tile is a bright fill that needs dark ink, but an arrived-not-proven tile is drawn at 35%
 * and comes out a dim brown in the dark themes, which needs light ink. So the ink is chosen from the
 * fill actually drawn — composited over the ground behind it — rather than from the theme.
 *
 * Pure arithmetic on ARGB integers, so it is unit tested without a device and has no opinion about
 * Compose.
 */
public object Contrast {

    /** WCAG 2 relative luminance of an opaque colour, 0 (black) to 1 (white). Alpha is ignored. */
    public fun luminance(argb: Int): Double {
        fun channel(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel((argb shr 16) and 0xFF) +
            0.7152 * channel((argb shr 8) and 0xFF) +
            0.0722 * channel(argb and 0xFF)
    }

    /** The contrast ratio between two opaque colours, from 1 (identical) to 21 (black on white). */
    public fun ratio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /**
     * [over] with [top] painted on it at [top]'s own alpha: what a translucent fill actually looks
     * like on the ground behind it. The result is opaque.
     */
    public fun composite(top: Int, over: Int): Int {
        val a = ((top ushr 24) and 0xFF) / 255.0
        fun mix(shift: Int): Int {
            val t = (top shr shift) and 0xFF
            val b = (over shr shift) and 0xFF
            return (a * t + (1 - a) * b).roundToInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    /** Whichever of the two inks reads better on [fill]. A fill with alpha must be composited first. */
    public fun inkFor(fill: Int, dark: Int, light: Int): Int =
        if (ratio(dark, fill) >= ratio(light, fill)) dark else light

    /** The WCAG 2 floor for body text. Large text and marks need only 3. */
    public const val TEXT_MINIMUM: Double = 4.5
}
