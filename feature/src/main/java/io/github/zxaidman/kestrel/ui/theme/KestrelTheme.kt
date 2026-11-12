package io.github.zxaidman.kestrel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.zxaidman.kestrel.core.common.Contrast
import io.github.zxaidman.kestrel.core.settings.AppTheme

/**
 * One palette, three surfaces, and a rule about which is used when.
 *
 * The application is built from Material 3 already, so a colour scheme is the whole of the change:
 * every screen, dialog, sheet, chip and button follows it at once, and nothing has to be repainted
 * by hand. What this is *not* is a redesign — the home page is still a developer's diagnostics
 * screen, and painting it does not make it a product. That is `CRIT-2`.
 *
 * **The accent is the same in all three.** A slate blue, chosen because it stays legible on white,
 * on grey and on black without becoming three different products; the schemes differ in what they
 * are painted on, not in what they are.
 *
 * **Three ways to be dark, because they are not the same thing on this hardware.** Grey dark is the
 * ordinary Material dark surface, where an unlit pixel is still a lit grey pixel. AMOLED dark is
 * true black, so on an OLED panel those pixels are actually off — a difference in what the screen
 * draws and what it costs to draw it, which on a handheld being held for hours is worth offering.
 *
 * **The overlay keeps its own palette, deliberately.** A pad is drawn over somebody else's
 * application and has to be legible on a white page and a black one both, so its colours answer to
 * that rather than to this. A pad that followed the application's theme would be invisible half the
 * time — see the note on the palette in `ControllerOverlay`.
 */
private val ACCENT = Color(0xFF5B6CC4)
private val ACCENT_DARK = Color(0xFF9AA6EE)
private val WARNING = Color(0xFFB3261E)
private val WARNING_DARK = Color(0xFFE0603A)

private val LIGHT: ColorScheme = lightColorScheme(
    primary = ACCENT,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E3F8),
    onPrimaryContainer = Color(0xFF141A44),
    secondary = Color(0xFF585E72),
    onSecondary = Color.White,
    background = Color(0xFFFBFAFE),
    onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFBFAFE),
    onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
    error = WARNING,
    onError = Color.White,
)

private val DARK_GREY: ColorScheme = darkColorScheme(
    primary = ACCENT_DARK,
    onPrimary = Color(0xFF1D2456),
    primaryContainer = Color(0xFF343B6E),
    onPrimaryContainer = Color(0xFFE0E3F8),
    secondary = Color(0xFFC1C5DD),
    onSecondary = Color(0xFF2A3042),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A),
    error = WARNING_DARK,
    onError = Color(0xFF3B1006),
)

/**
 * True black, and it has to be true black everywhere it shows.
 *
 * Material draws elevation as a tint over the surface, so a dialog on a black background comes out
 * dark grey unless the container colours are set as well — which would make this "black background,
 * grey everything" rather than an AMOLED scheme. The containers are dark rather than black, because
 * a sheet that is exactly the colour of the page behind it has no edge at all.
 */
private val DARK_BLACK: ColorScheme = DARK_GREY.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF1C1C21),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0E),
    surfaceContainer = Color(0xFF121216),
    surfaceContainerHigh = Color(0xFF17171C),
    surfaceContainerHighest = Color(0xFF1E1E24),
    outline = Color(0xFF7C7C86),
)

/**
 * The signal colours: what a state *means*, whatever it is drawn on.
 *
 * **They were chosen for the editor canvas, which is always dark, and then reused as text on the
 * application's own surface, which follows the theme.** In the light theme that made them vanish —
 * amber warning text measured 1.78 : 1, the orange refusal 2.98 : 1, against a floor of 4.5
 * (`BUG-55`). Nobody reported it; it was measured while the design system was being written down.
 *
 * So a signal keeps its meaning and changes its value with the ground. The bright values are still
 * right on the canvas and on the dark themes' surfaces (6.8 to 11.4 : 1); a darker one is drawn on a
 * light surface. Use [warningText], [provenText] and [offscreenText] for words, never the raw values.
 * The raw values are for fills and marks on dark grounds.
 */
public object Signal {
    /** Amber: look at this. Overlap, the snap guide, a warning line, a proof not yet complete. */
    public val WARNING: Color = Color(0xFFF2B441)

    /** Orange: too far. A control off the screen, a window over a quarter of the screen. */
    public val OFFSCREEN: Color = Color(0xFFE0603A)

    /** Green: proven on the test ground. */
    public val PROVEN: Color = Color(0xFF5FBF77)

    /** Periwinkle: held right now, on the test ground. */
    public val HELD: Color = Color(0xFF8C9EFF)

    // Measured on the light surface and on the most tinted light ground they land on (a window at
    // 8dp of tonal elevation): 5.76, 5.47 and 5.71 : 1 at worst.
    internal val WARNING_ON_LIGHT: Color = Color(0xFF7D5200)
    internal val PROVEN_ON_LIGHT: Color = Color(0xFF256B3A)
    internal val OFFSCREEN_ON_LIGHT: Color = Color(0xFF9C3B17)
}

/** Whether the theme in force paints on a light ground. Read from the surface, so it follows any theme. */
@Composable
@ReadOnlyComposable
private fun onLightGround(): Boolean = MaterialTheme.colorScheme.surface.luminance() > 0.5f

/** Amber, drawn to be read on the surface it is on. For words; fills use [Signal.WARNING]. */
@Composable
@ReadOnlyComposable
public fun warningText(): Color = if (onLightGround()) Signal.WARNING_ON_LIGHT else Signal.WARNING

/** Green, drawn to be read on the surface it is on. */
@Composable
@ReadOnlyComposable
public fun provenText(): Color = if (onLightGround()) Signal.PROVEN_ON_LIGHT else Signal.PROVEN

/** Orange, drawn to be read on the surface it is on. */
@Composable
@ReadOnlyComposable
public fun offscreenText(): Color = if (onLightGround()) Signal.OFFSCREEN_ON_LIGHT else Signal.OFFSCREEN

/**
 * The ink to write on [fill], which may be translucent: whichever of the theme's two text colours
 * reads better on what is actually drawn.
 *
 * Not one fixed ink, and that is the point. A proven tile is a bright fill that needs dark ink in
 * every theme; a tile at 35% comes out a dim brown in the dark themes and needs light ink. The
 * theme's ordinary text colour was right on the page and 1.9 to 2.6 : 1 on those fills (`BUG-55`).
 */
@Composable
@ReadOnlyComposable
public fun inkOn(fill: Color): Color {
    val drawn = Contrast.composite(fill.toArgb(), MaterialTheme.colorScheme.surface.toArgb())
    return Color(Contrast.inkFor(drawn, INK_DARK.toArgb(), INK_LIGHT.toArgb()))
}

// The two text colours the schemes already use, so a tile's ink is never a third grey.
private val INK_DARK = Color(0xFF1A1B21)
private val INK_LIGHT = Color(0xFFE4E1E9)

/** Whether the chosen theme paints on a dark ground, which the system bars need to know. */
public fun isDark(theme: AppTheme, systemIsDark: Boolean): Boolean = when (theme) {
    AppTheme.SYSTEM -> systemIsDark
    AppTheme.LIGHT -> false
    AppTheme.DARK -> true
}

/**
 * The corner a button is drawn with.
 *
 * Material 3 draws a filled button as a capsule and the theme cannot say otherwise — the token maps
 * to a full corner whatever `Shapes` holds. So the buttons are wrapped instead, and this is the one
 * number that decides the corner for all of them.
 */
public val BUTTON_CORNER: Dp = 12.dp

@Composable
public fun KButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(BUTTON_CORNER),
        colors = colors,
        content = content,
    )
}

@Composable
public fun KOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(BUTTON_CORNER),
        content = content,
    )
}

@Composable
public fun KestrelTheme(theme: AppTheme, trueBlack: Boolean, content: @Composable () -> Unit) {
    val systemIsDark = isSystemInDarkTheme()
    val dark = isDark(theme, systemIsDark)
    val scheme = when {
        !dark -> LIGHT
        trueBlack -> DARK_BLACK
        else -> DARK_GREY
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
