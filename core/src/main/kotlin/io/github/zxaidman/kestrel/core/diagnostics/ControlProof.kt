package io.github.zxaidman.kestrel.core.diagnostics

import io.github.zxaidman.kestrel.core.input.ControlForm
import io.github.zxaidman.kestrel.core.input.GamepadControl

/**
 * The eight ways a pad can be pushed.
 *
 * Named here rather than left as a pair of axes because a d-pad is **proven one direction at a
 * time**: seven working and one dead is the failure that matters, and a pair of numbers cannot say
 * which of the eight has never arrived.
 */
public enum class PadDirection(public val label: String) {
    UP("↑"),
    UP_RIGHT("↗"),
    RIGHT("→"),
    DOWN_RIGHT("↘"),
    DOWN("↓"),
    DOWN_LEFT("↙"),
    LEFT("←"),
    UP_LEFT("↖"),
    ;

    public companion object {
        /**
         * Which direction a pad reading is, or null when it is centred.
         *
         * The same sector rule the overlay draws with: a cardinal is wider than a diagonal, because
         * a thumb aiming for "up" misses more often than one aiming for a corner it can feel.
         */
        public fun of(x: Double, y: Double, centre: Double = 0.3): PadDirection? {
            if (kotlin.math.abs(x) < centre && kotlin.math.abs(y) < centre) return null
            val nearX = kotlin.math.abs(x) < SECTOR
            val nearY = kotlin.math.abs(y) < SECTOR
            return when {
                nearX && y < 0 -> UP
                nearX && y > 0 -> DOWN
                nearY && x > 0 -> RIGHT
                nearY && x < 0 -> LEFT
                x > 0 && y < 0 -> UP_RIGHT
                x > 0 -> DOWN_RIGHT
                y < 0 -> UP_LEFT
                else -> DOWN_LEFT
            }
        }

        private const val SECTOR = 0.42
    }
}

/**
 * What one control has been observed to do.
 *
 * [seen] is the point of the whole screen. A control that lights while a thumb is on it and goes
 * dark afterwards proves nothing an hour later; a control that is *marked* proves it was pressed
 * once and the platform delivered it, which is what somebody sweeping a pad needs to know without
 * remembering what they have already tried.
 */
public data class ControlReading(
    public val control: GamepadControl,
    /** Delivered at least once since the sweep began. */
    public val seen: Boolean = false,
    /** Delivered right now — a button held, a stick pushed, a trigger pulled. */
    public val active: Boolean = false,
    /** For a trigger, how far. For a stick, how far from centre. Zero for a button. */
    public val magnitude: Double = 0.0,
    public val x: Double = 0.0,
    public val y: Double = 0.0,
    /** For the pad only: which of the eight have arrived. Empty for everything else. */
    public val directions: Set<PadDirection> = emptySet(),
) {
    /**
     * Whether this control has been proven as far as it can be.
     *
     * A button is proven by arriving. **A pad is not**: seven of eight directions is a pad with a
     * dead corner, and calling that proven is how a fault ships. A stick and a trigger are proven by
     * reaching somewhere near their limit, because an axis stuck at a third of its range still
     * "arrives".
     */
    public val proven: Boolean
        get() = when (control.form) {
            ControlForm.BUTTON -> seen
            ControlForm.DPAD -> directions.size == PadDirection.entries.size
            ControlForm.TRIGGER, ControlForm.STICK -> seen && magnitude >= NEAR_LIMIT
        }

    public companion object {
        /**
         * How far an axis must have travelled to count as proven.
         *
         * Not 1.0. A stick's corner reads a little under full on hardware, and a shaped stick's
         * outer limit is a setting — demanding the exact end would leave a working stick unproven
         * for a reason that is not a fault.
         */
        public const val NEAR_LIMIT: Double = 0.85
    }
}

/**
 * Every control, and what each has been seen to do — the whole point of the test ground.
 *
 * **Pure, and here rather than on the screen, because this is the part worth testing.** Every input
 * fault this project has found was found by a person pressing something and reading a number; the
 * arithmetic that turns readings into "proven" is that judgement written down, and it should not
 * live somewhere only a phone can run it.
 *
 * Immutable: each observation returns a new board, so a screen holds one value and a test can assert
 * on a sequence.
 */
public data class ProofBoard(
    public val readings: Map<GamepadControl, ControlReading> =
        GamepadControl.entries.associateWith { ControlReading(it) },
) {
    public fun reading(control: GamepadControl): ControlReading =
        readings[control] ?: ControlReading(control)

    /** Controls proven so far, and how many there are to prove. */
    public val provenCount: Int get() = readings.values.count { it.proven }
    public val total: Int get() = readings.size

    /** What is left, in the order a person would sweep them. */
    public val unproven: List<GamepadControl>
        get() = GamepadControl.entries.filter { !reading(it).proven }

    /** A button, a bumper, a stick press: down or up. */
    public fun button(control: GamepadControl, down: Boolean): ProofBoard =
        with(control) {
            copy(
                readings = readings + (
                    this to reading(this).copy(seen = reading(this).seen || down, active = down)
                    )
            )
        }

    /** A trigger, at however far it has been pulled. */
    public fun trigger(control: GamepadControl, value: Double): ProofBoard {
        val was = reading(control)
        return copy(
            readings = readings + (
                control to was.copy(
                    seen = was.seen || value > 0.0,
                    active = value > 0.0,
                    // The furthest it has reached, not where it is. A trigger that was pulled to
                    // the stop and released has been proven; asking the reader to catch the moment
                    // is asking them to watch rather than to sweep.
                    magnitude = maxOf(was.magnitude, value),
                )
            )
        )
    }

    /** A stick, at wherever it is being held. */
    public fun stick(control: GamepadControl, x: Double, y: Double): ProofBoard {
        val was = reading(control)
        val reach = kotlin.math.sqrt(x * x + y * y)
        return copy(
            readings = readings + (
                control to was.copy(
                    seen = was.seen || reach > 0.0,
                    active = reach > 0.0,
                    magnitude = maxOf(was.magnitude, reach),
                    x = x,
                    y = y,
                )
            )
        )
    }

    /** The pad, at wherever it is being pushed. Remembers every direction it has been. */
    public fun pad(x: Double, y: Double): ProofBoard {
        val was = reading(GamepadControl.DPAD)
        val now = PadDirection.of(x, y)
        return copy(
            readings = readings + (
                GamepadControl.DPAD to was.copy(
                    seen = was.seen || now != null,
                    active = now != null,
                    x = x,
                    y = y,
                    directions = if (now == null) was.directions else was.directions + now,
                )
            )
        )
    }

    /** Back to nothing proven, for a second sweep or a different controller. */
    public fun cleared(): ProofBoard = ProofBoard()
}
