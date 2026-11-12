package io.github.zxaidman.kestrel.platform.input

import io.github.zxaidman.kestrel.core.diagnostics.InputTrail
import io.github.zxaidman.kestrel.core.diagnostics.changedEnough
import io.github.zxaidman.kestrel.core.input.AnalogProfile
import io.github.zxaidman.kestrel.core.input.ControlForm
import io.github.zxaidman.kestrel.core.input.GamepadControl
import io.github.zxaidman.kestrel.core.input.InputBackend
import io.github.zxaidman.kestrel.core.input.applyStick
import io.github.zxaidman.kestrel.core.input.applyTrigger

/**
 * Carries a control the user touched to the controller the platform sees.
 *
 * This is the piece the product had been missing, and its absence was invisible in exactly the way
 * that matters: the on-screen stick moved, the numbers moved, the controller existed and was
 * recognised by five emulators — and nothing connected the two, so an emulator saw a controller
 * that never moved. Everything looked right and nothing arrived.
 *
 * `UI → InputEngine → InputBackend → platform` is the path `CLAUDE.md` §4 requires, and this is the
 * middle of it. The UI hands over controller semantics; the shaping in `core/input/` is applied
 * here, once, for every source; and only the [InputBackend] below turns anything into a kernel
 * event code. The engine no longer knows what a uinput stream is (`ADR-002`), which is what lets a
 * second backend exist without anything above this noticing.
 */
public class InputEngine(private val backend: InputBackend) {

    /**
     * The latest position, and a thread that writes it.
     *
     * A thumb produces far more positions than a device needs, and each one written separately
     * would queue behind the last. Only the newest matters — an old stick position is not partial
     * information, it is wrong information — so the writer takes the newest and discards the rest.
     */
    @Volatile private var pendingX: Double = 0.0
    @Volatile private var pendingY: Double = 0.0
    @Volatile private var dirty: Boolean = false
    @Volatile private var pendingRightX: Double = 0.0
    @Volatile private var pendingRightY: Double = 0.0
    @Volatile private var rightDirty: Boolean = false
    @Volatile private var running: Boolean = false

    /** Reports that arrived, as the backend counts arriving. */
    @Volatile public var delivered: Long = 0L
        private set

    /** Why the last write did not arrive, if one did not. */
    public val lastError: String
        get() = backend.lastError

    /** Whether the backend's way to the device went away. Once true, nothing sent can arrive. */
    public val lost: Boolean
        get() = backend.lost

    /**
     * What was sent, in order.
     *
     * The other half of a diagnostic report. Knowing what the platform delivered back answers "did
     * anything arrive"; knowing what Kestrel sent in the same file answers the question that
     * actually gets asked when something is wrong — **whether the fault is above the device or
     * below it.** A press with no matching release, or a release Kestrel never sent, is visible
     * here and nowhere else.
     *
     * Recorded in controller terms: a button is `a down`, not the kernel code it becomes, because
     * the engine no longer knows the code — and a report a person reads is better for it.
     */
    public val trail: InputTrail = InputTrail()

    private var markedX = 0.0
    private var markedY = 0.0
    private var markedRightX = 0.0
    private var markedRightY = 0.0

    /** Opens the backend and starts writing. `false` if the backend could not open. */
    public fun start(): Boolean {
        if (!backend.open()) return false

        running = true
        Thread {
            while (running) {
                if (dirty) {
                    dirty = false
                    writeStick(pendingX, pendingY)
                }
                if (rightDirty) {
                    rightDirty = false
                    writeRightStick(pendingRightX, pendingRightY)
                }
                // About sixty times a second. Faster gains nothing a screen can show; slower is
                // visible as lag on a fast flick.
                Thread.sleep(16)
            }
        }.also { it.isDaemon = true }.start()
        return true
    }

    public fun stop() {
        running = false
        // Never leave a control held. A stick abandoned at full deflection keeps the platform
        // emitting directional keys indefinitely — measured at over 360 repeats in Phase 0.
        writeStick(0.0, 0.0)
        writeRightStick(0.0, 0.0)
        backend.close()
    }

    /** Accepts a stick position in controller terms and shapes it before it goes anywhere. */
    public fun stick(rawX: Double, rawY: Double, profile: AnalogProfile) {
        val shaped = applyStick(rawX, rawY, profile)
        pendingX = shaped.x
        pendingY = shaped.y
        dirty = true
    }

    /**
     * The right stick.
     *
     * Coalesced separately from the left, because the two are independent controls and a player
     * aiming while moving would otherwise have one overwrite the other.
     */
    public fun rightStick(rawX: Double, rawY: Double, profile: AnalogProfile) {
        val shaped = applyStick(rawX, rawY, profile)
        pendingRightX = shaped.x
        pendingRightY = shaped.y
        rightDirty = true
    }

    /**
     * A button, written immediately: a press is a moment, not a position to be coalesced.
     *
     * Anything that is not a button — a trigger, a stick, the pad — is ignored here, exactly as the
     * overlay used to ignore it by finding it had no button code. The kernel table and the control
     * forms in `:core` agree on which controls those are.
     */
    public fun button(control: GamepadControl, pressed: Boolean) {
        if (control.form != ControlForm.BUTTON) return
        mark("button", "${control.wireName} ${if (pressed) "down" else "up"}")
        count(backend.button(control, pressed))
    }

    /** The d-pad, as the hat axes a controller reports rather than as four buttons. */
    public fun hat(x: Int, y: Int) {
        mark("hat", "x=$x y=$y")
        count(backend.hat(x, y))
    }

    /** A trigger, shaped like any other analog control. */
    public fun trigger(raw: Double, profile: AnalogProfile, right: Boolean) {
        val shaped = applyTrigger(raw, profile)
        mark(if (right) "R2" else "L2", "%.3f".format(shaped))
        count(backend.trigger(right, shaped))
    }

    private fun mark(kind: String, detail: String) {
        trail.add(System.currentTimeMillis(), kind, detail)
    }

    private fun writeRightStick(x: Double, y: Double) {
        // Coalesced before it is recorded: a thumb held still writes sixty identical positions a
        // second, and sixty copies of one value would crowd every press out of the trail.
        if (changedEnough(markedRightX, x) || changedEnough(markedRightY, y)) {
            markedRightX = x
            markedRightY = y
            mark("rightStick", "%+.3f %+.3f".format(x, y))
        }
        count(backend.stick(right = true, x, y))
    }

    private fun writeStick(x: Double, y: Double) {
        if (changedEnough(markedX, x) || changedEnough(markedY, y)) {
            markedX = x
            markedY = y
            mark("leftStick", "%+.3f %+.3f".format(x, y))
        }
        count(backend.stick(right = false, x, y))
    }

    private fun count(arrived: Boolean) {
        if (arrived) delivered += 1
        // A backend that has lost its way to the device stops the writer too: nothing it sends can
        // arrive, and a thread retrying sixty times a second would only bury that fact.
        if (backend.lost) running = false
    }
}
