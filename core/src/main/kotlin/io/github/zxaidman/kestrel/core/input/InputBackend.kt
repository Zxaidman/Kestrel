package io.github.zxaidman.kestrel.core.input

/**
 * Something that can make a controller's input arrive on the phone. `ADR-002`, in code.
 *
 * Everything above this line speaks controller semantics — a button is [GamepadControl.A], a stick
 * is a position from -1 to +1, a trigger 0 to 1 — and everything below it is one backend's business:
 * kernel event codes, a uinput stream, a binder to a privileged process. The engine that shapes and
 * coalesces input talks only to this, so a second backend can be written without the overlay, the
 * session or the engine noticing.
 *
 * **Values arrive already shaped.** Dead zone, curve and sensitivity are applied once, above this
 * interface, for every source (`CLAUDE.md` §5). A backend that shaped them again would apply the
 * user's curve twice.
 *
 * **Every write says whether it arrived.** A backend returns `false` for input that went nowhere,
 * so the engine can count what was delivered rather than what was attempted — the difference
 * between those two numbers is how a broken write path was found the first time.
 *
 * There is one implementation today, the virtual controller `ADR-INPUT-001` accepted, scoped to the
 * reference device. This interface is not evidence that any other backend works.
 */
public interface InputBackend {

    /** Opens the device for writing. `false`, with [lastError] saying why, when it could not. */
    public fun open(): Boolean

    /**
     * Stops writing. Does not release anything held: the engine releases every control first,
     * because a stick abandoned at full deflection keeps the platform repeating directional keys.
     */
    public fun close()

    /** A button. Controls whose form is not [ControlForm.BUTTON] are refused with `false`. */
    public fun button(control: GamepadControl, pressed: Boolean): Boolean

    /** A stick at a shaped position, each axis -1 to +1. */
    public fun stick(right: Boolean, x: Double, y: Double): Boolean

    /** A trigger at a shaped value, 0 to 1. */
    public fun trigger(right: Boolean, value: Double): Boolean

    /** The d-pad as the hat a controller reports: each axis -1, 0 or +1. */
    public fun hat(x: Int, y: Int): Boolean

    /**
     * Whatever the backend writes through is gone for good — a privileged process that died, a
     * device that was removed. Nothing written after this can arrive.
     */
    public val lost: Boolean

    /** The most recent reason a write did not arrive, in words a person can act on. Empty if none. */
    public val lastError: String
}
