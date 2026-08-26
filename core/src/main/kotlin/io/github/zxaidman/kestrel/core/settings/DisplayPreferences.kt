package io.github.zxaidman.kestrel.core.settings

/**
 * Which way up Kestrel should be.
 *
 * A handheld is held one way, so the default is landscape — but a phone is not a handheld, and a
 * user who wants to arrange a layout with one hand on the sofa should not have to turn the room.
 * Every option here is a real answer for somebody, which is why this is a setting rather than a
 * decision.
 */
public enum class AppOrientation(public val wireName: String) {

    /** Whatever the phone's own rotation setting says, including its rotation lock. */
    AUTO("auto"),

    /** Landscape, and stay there whichever way the phone is turned. */
    LANDSCAPE("landscape"),

    /** Landscape the other way up, for a phone whose camera or cable is on the wrong side. */
    REVERSE_LANDSCAPE("reverse-landscape"),

    /** Landscape, but flip when the phone is turned over. */
    SENSOR_LANDSCAPE("sensor-landscape"),

    /** Portrait, and stay there. */
    PORTRAIT("portrait"),

    /**
     * Portrait, flipping when the phone is turned over — where the phone allows it.
     *
     * Most do not. Reverse portrait is unsupported on a great many devices, so this often behaves
     * exactly like [PORTRAIT]; that is the platform's answer rather than Kestrel's, and there is no
     * separate reverse-portrait option because it would be an option that does nothing.
     */
    SENSOR_PORTRAIT("sensor-portrait"),
    ;

    public companion object {
        public fun of(wireName: String): AppOrientation? =
            entries.firstOrNull { it.wireName == wireName }
    }
}

/**
 * How Kestrel is painted.
 *
 * Three ways to be dark rather than one, because they are not the same thing on this hardware.
 * [DARK_GREY] is the ordinary dark surface, where an unlit pixel is still a lit grey pixel.
 * [DARK_AMOLED] is true black, so on an OLED panel those pixels are genuinely off — a difference in
 * what the screen draws and what it costs to draw, not a matter of taste.
 */
public enum class AppTheme(public val wireName: String) {

    /** Whatever the phone is set to. The default. */
    SYSTEM("system"),

    LIGHT("light"),

    DARK("dark"),
    ;

    public companion object {

        /**
         * Reads a theme, including the two names an earlier build wrote.
         *
         * `dark-grey` and `dark-amoled` were shipped as separate themes before it was clear they
         * are one theme and a property of it. Settings files on phones say those words, and a file
         * a previous version of Kestrel wrote must not become a file this version refuses.
         */
        public fun of(wireName: String): AppTheme? = when (wireName) {
            "dark-grey", "dark-amoled" -> DARK
            else -> entries.firstOrNull { it.wireName == wireName }
        }

        /** Whether a stored theme name also means true black was chosen. */
        public fun trueBlackFrom(wireName: String): Boolean = wireName == "dark-amoled"
    }
}

/**
 * How much of the screen Kestrel takes, and what it is allowed to draw under.
 *
 * Both default to on, and both are settings rather than decisions.
 *
 * **Full screen** hides the system bars. A pad drawn under a status bar loses the space to it, and
 * a notification sliding in over a control mid-play is worse than not seeing the time.
 *
 * **The cutout** is the notch or hole. Drawing under it is what makes a phone with one the same
 * shape as a phone without: refuse, and the platform letterboxes the whole application to below the
 * notch, which on a wide screen is a visible black band and less room for controls. Some people
 * would rather have the band than have a control near the camera, so it can be turned off.
 */
public data class DisplayPreferences(
    public val fullScreen: Boolean = true,
    public val drawUnderCutout: Boolean = true,
    public val orientation: AppOrientation = AppOrientation.LANDSCAPE,
    public val theme: AppTheme = AppTheme.SYSTEM,

    /**
     * Dark on black rather than dark on grey.
     *
     * A property of being dark, not a third theme — there are two questions here, *light or dark*
     * and *how dark*, and three buttons in a row made them look like one. Ignored while the theme
     * resolves to light.
     *
     * It is not a matter of taste on this hardware: on an OLED panel a black pixel is an unlit one.
     */
    public val trueBlack: Boolean = false,
)

/**
 * How the editor was last set up.
 *
 * Working state rather than taste — which is the argument that kept it out of this file for a
 * round. The project owner asked for it to survive a restart, and they are right for a plain
 * reason: somebody who turns edge snapping on is *working that way*, and having to say so again
 * after every restart is the application forgetting something the person has not.
 */
public data class EditorPreferences(
    /** Grid step, as a fraction of the screen's shorter side. */
    public val gridUnit: Double = 0.04,
    public val snapToGrid: Boolean = false,
    public val snapToEdges: Boolean = false,
) {
    public companion object {
        /** Wide enough to hold any step the editor offers, tight enough to catch nonsense. */
        public const val MIN_GRID: Double = 0.005
        public const val MAX_GRID: Double = 0.5
    }
}

/**
 * When the pad gets out of the way on its own.
 *
 * A pad is drawn over somebody else's application, and a hand that is not using it is a hand that
 * would rather see the game. So after a while of no touches the controls fade, and after a while
 * longer they go — and the toggle brings them back.
 *
 * **The toggle itself only ever fades.** It is the way out: a user who cannot make the controls go
 * away has lost their phone until they reboot it, which has happened here once. Fading it is a
 * courtesy; making it need two taps, or letting it disappear, would be the same mistake with a
 * timer attached.
 */
public data class IdlePreferences(
    /** Whether the controls fade and then go. */
    public val controlsEnabled: Boolean = true,

    /** Whether the toggle fades. Separate, because they are separate things to want. */
    public val toggleEnabled: Boolean = true,

    /**
     * Seconds of no touch before the controls fade.
     *
     * Separate from [toggleSeconds] because they are answers to different questions: how long a pad
     * should wait before getting out of the way, and how long a small button in a corner should sit
     * at full strength. One number for both made the second one hostage to the first.
     */
    public val controlsFadeSeconds: Int = 5,

    /**
     * Seconds of no touch before the controls go entirely.
     *
     * Its own number rather than twice the first. Fading and disappearing are different amounts of
     * getting out of the way, and somebody who wants a long faded state before anything vanishes
     * should be able to say so.
     */
    public val controlsHideSeconds: Int = 10,

    /** Seconds of no touch before the toggle dims. It only ever dims — see the note above. */
    public val toggleSeconds: Int = 5,
) {
    public companion object {
        public const val MIN_SECONDS: Int = 2
        public const val MAX_SECONDS: Int = 120
    }
}

/**
 * How a trigger travels, in seconds.
 *
 * **A feel preference, in the settings file, not a property of a layout.** `BUG-8` planned to put
 * this in the layout document, which would have meant a schema version bump and a migration. That
 * was the wrong home: a ramp is the same kind of thing as dead zone, curve and sensitivity, all of
 * which live here — and two people sharing an arrangement should not be sharing each other's trigger
 * feel.
 *
 * The press is two rates, which is the project owner's own proposal. A single 0.5s ramp was chosen
 * for a measured reason — a trigger that jumps straight to full feels broken to the hand — and it
 * made the press slow to *register*, which is a different complaint. So the first half arrives
 * quickly and the second half travels, and both are adjustable because which of the two matters
 * depends on the target.
 *
 * **Do not** turn any of these back into a step per frame. A fixed step gave 0.31s on a 120Hz panel
 * and 0.5s on a 60Hz one; a ramp is measured in time.
 */
public data class TriggerPreferences(
    /** Seconds for the first half of the pull. Short, so the press registers almost at once. */
    public val quickSeconds: Double = 0.10,

    /** Seconds for the second half. Longer, so a full pull still feels like a trigger. */
    public val travelSeconds: Double = 0.35,

    /**
     * Seconds to fall back to nothing.
     *
     * Quicker than the pull on purpose: a control that lingers after the thumb has gone feels
     * broken, while one that takes a moment to reach full feels like a trigger.
     */
    public val releaseSeconds: Double = 0.30,
) {
    public companion object {
        /** Below this a ramp is a step, and a step is what this exists to avoid. */
        public const val MIN_SECONDS: Double = 0.02

        /** Above this a trigger is a slider. */
        public const val MAX_SECONDS: Double = 2.0
    }
}
