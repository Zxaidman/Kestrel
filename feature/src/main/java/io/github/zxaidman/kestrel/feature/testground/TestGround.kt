package io.github.zxaidman.kestrel.feature.testground

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.zxaidman.kestrel.core.diagnostics.ProofBoard
import io.github.zxaidman.kestrel.core.input.GamepadControl

/**
 * What the platform has actually delivered back, per control.
 *
 * **The loop this closes is the one that found every input fault in this project**: press something,
 * read a number. Until now that loop ran inside a target application — put the pad up, launch an
 * emulator, press a button, watch a character. Every claim about behaviour cost a full session, and
 * a claim that turned out to be wrong cost another.
 *
 * Here it is one screen. Kestrel injects, the platform delivers the event back to this window like
 * any other, and the board says what arrived. **Nothing here creates input**: it observes, exactly
 * as `MainActivity` does, and the events it sees are the same events a game would see.
 *
 * The decision of what counts as *proven* is in `:core` and unit tested; this class is the Android
 * half — key codes and axis constants, which are read here and never leave.
 */
public class TestGroundState {

    public var board: ProofBoard by mutableStateOf(ProofBoard())
        private set

    /** Which device the last event came from, so an unplugged pad is visible as such. */
    public var source: String by mutableStateOf("—")
        private set

    public var events: Int by mutableStateOf(0)
        private set

    public fun clear() {
        board = board.cleared()
        events = 0
    }

    /**
     * A key event, mapped to the control it stands for.
     *
     * The pad arrives as key codes on many devices and as a hat axis on others, and both are
     * handled — a d-pad proven on one phone and dead on another is exactly the kind of difference
     * this screen exists to show.
     */
    public fun record(event: KeyEvent) {
        val down = event.action == KeyEvent.ACTION_DOWN
        events += 1
        source = event.device?.name ?: "device ${event.deviceId}"

        padDirectionFor(event.keyCode)?.let { (x, y) ->
            board = board.pad(if (down) x else 0.0, if (down) y else 0.0)
            return
        }
        controlFor(event.keyCode)?.let { control ->
            board = when (control) {
                GamepadControl.LEFT_TRIGGER, GamepadControl.RIGHT_TRIGGER ->
                    // Some devices report a trigger as a button rather than an axis. A full pull is
                    // the only honest reading available when that is all the device says.
                    board.trigger(control, if (down) 1.0 else 0.0)
                else -> board.button(control, down)
            }
        }
    }

    /** A motion event: both sticks, both trigger axes, and the pad's hat. */
    public fun record(event: MotionEvent) {
        events += 1
        source = event.device?.name ?: "device ${event.deviceId}"

        board = board
            .stick(
                GamepadControl.LEFT_STICK,
                event.getAxisValue(MotionEvent.AXIS_X).toDouble(),
                event.getAxisValue(MotionEvent.AXIS_Y).toDouble(),
            )
            .stick(
                GamepadControl.RIGHT_STICK,
                event.getAxisValue(MotionEvent.AXIS_Z).toDouble(),
                event.getAxisValue(MotionEvent.AXIS_RZ).toDouble(),
            )
            .trigger(
                GamepadControl.LEFT_TRIGGER,
                event.getAxisValue(MotionEvent.AXIS_BRAKE).toDouble(),
            )
            .trigger(
                GamepadControl.RIGHT_TRIGGER,
                event.getAxisValue(MotionEvent.AXIS_GAS).toDouble(),
            )
            .pad(
                event.getAxisValue(MotionEvent.AXIS_HAT_X).toDouble(),
                event.getAxisValue(MotionEvent.AXIS_HAT_Y).toDouble(),
            )
    }

    private fun controlFor(keyCode: Int): GamepadControl? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> GamepadControl.A
        KeyEvent.KEYCODE_BUTTON_B -> GamepadControl.B
        KeyEvent.KEYCODE_BUTTON_X -> GamepadControl.X
        KeyEvent.KEYCODE_BUTTON_Y -> GamepadControl.Y
        KeyEvent.KEYCODE_BUTTON_L1 -> GamepadControl.LEFT_BUMPER
        KeyEvent.KEYCODE_BUTTON_R1 -> GamepadControl.RIGHT_BUMPER
        KeyEvent.KEYCODE_BUTTON_L2 -> GamepadControl.LEFT_TRIGGER
        KeyEvent.KEYCODE_BUTTON_R2 -> GamepadControl.RIGHT_TRIGGER
        KeyEvent.KEYCODE_BUTTON_THUMBL -> GamepadControl.LEFT_STICK_PRESS
        KeyEvent.KEYCODE_BUTTON_THUMBR -> GamepadControl.RIGHT_STICK_PRESS
        KeyEvent.KEYCODE_BUTTON_START -> GamepadControl.START
        KeyEvent.KEYCODE_BUTTON_SELECT -> GamepadControl.SELECT
        else -> null
    }

    private fun padDirectionFor(keyCode: Int): Pair<Double, Double>? = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP -> 0.0 to -1.0
        KeyEvent.KEYCODE_DPAD_DOWN -> 0.0 to 1.0
        KeyEvent.KEYCODE_DPAD_LEFT -> -1.0 to 0.0
        KeyEvent.KEYCODE_DPAD_RIGHT -> 1.0 to 0.0
        KeyEvent.KEYCODE_DPAD_UP_LEFT -> -0.8 to -0.8
        KeyEvent.KEYCODE_DPAD_UP_RIGHT -> 0.8 to -0.8
        KeyEvent.KEYCODE_DPAD_DOWN_LEFT -> -0.8 to 0.8
        KeyEvent.KEYCODE_DPAD_DOWN_RIGHT -> 0.8 to 0.8
        else -> null
    }
}
