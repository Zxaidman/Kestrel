package io.github.zxaidman.kestrel.platform.input.virtual

import io.github.zxaidman.kestrel.core.input.GamepadControl
import io.github.zxaidman.kestrel.core.input.InputBackend
import io.github.zxaidman.kestrel.platform.input.GamepadCodes
import io.github.zxaidman.kestrel.platform.shizuku.IPrivilegedShell

/**
 * The [InputBackend] `ADR-INPUT-001` accepted: reports written as text to the stream the platform's
 * uinput helper reads, through the shell-privileged service Shizuku runs.
 *
 * **Lifted out of `InputEngine` without changing a byte.** The encoding below is the engine's own
 * code, moved; `WrittenBytesTest` pins the exact text written for every kind of control and was run
 * green against the engine before the move and again after. This is the highest-risk code in the
 * project and the refactor was done with no device in the loop, so "it compiles" was not the bar —
 * "not one byte changed" was.
 *
 * [VirtualControllerBackend] creates and holds the device itself; this only writes to it.
 */
public class VirtualControllerInput(
    private val shell: IPrivilegedShell,
    private val streamPath: String = VirtualControllerBackend.STREAM,
) : InputBackend {

    @Volatile override var lost: Boolean = false
        private set

    @Volatile override var lastError: String = ""
        private set

    override fun open(): Boolean {
        val opened = try {
            shell.openDeviceStream(streamPath)
        } catch (e: Throwable) {
            lastError = "Could not open the stream: ${e.javaClass.simpleName}"
            return false
        }
        if (!opened) lastError = "The privileged service refused the stream. Is a session open?"
        return opened
    }

    override fun close() {
        runCatching { shell.closeDeviceStream() }
    }

    override fun button(control: GamepadControl, pressed: Boolean): Boolean {
        val code = GamepadCodes.buttonCode(control) ?: return false
        return write(report(listOf(EV_KEY, code, if (pressed) 1 else 0)))
    }

    override fun hat(x: Int, y: Int): Boolean =
        write(report(listOf(EV_ABS, ABS_HAT0X, x, EV_ABS, ABS_HAT0Y, y)))

    override fun trigger(right: Boolean, value: Double): Boolean =
        write(report(listOf(EV_ABS, if (right) ABS_GAS else ABS_BRAKE, (value * TRIGGER_MAX).toInt())))

    override fun stick(right: Boolean, x: Double, y: Double): Boolean =
        write(
            report(
                listOf(
                    EV_ABS, if (right) ABS_Z else ABS_X, (x * STICK_MAX).toInt(),
                    EV_ABS, if (right) ABS_RZ else ABS_Y, (y * STICK_MAX).toInt(),
                )
            )
        )

    /**
     * One report, ending in the synchronisation marker.
     *
     * Every report must end with it: without the marker the kernel holds the values and the device
     * appears not to move, which looks identical to nothing being written at all.
     */
    private fun report(events: List<Int>): String {
        val all = events + listOf(SYN, SYN_REPORT, 0)
        return """{"id": 1, "command": "inject", "events": [${all.joinToString(", ")}]}""" + "\n"
    }

    private fun write(json: String): Boolean = try {
        if (shell.writeDeviceStream(json)) {
            true
        } else {
            lastError = "The stream is not open. Start a session first."
            false
        }
    } catch (e: android.os.DeadObjectException) {
        // The privileged service's process is gone — Shizuku stopped, or the platform killed it.
        // Nothing that follows can succeed, and reporting each failed write separately would bury
        // the one fact that matters under hundreds of copies of itself.
        lost = true
        lastError = "The privileged service stopped, so the controller is no longer being " +
            "driven. Start Shizuku and open the session again."
        false
    } catch (e: Throwable) {
        lastError = "Write failed: ${e.javaClass.simpleName}"
        false
    }

    private companion object {
        // Linux input-event constants. Stable kernel ABI, not values invented here.
        const val EV_KEY = 1
        const val EV_ABS = 3
        const val SYN = 0
        const val SYN_REPORT = 0
        const val ABS_X = 0
        const val ABS_Y = 1
        const val ABS_BRAKE = 10
        const val ABS_GAS = 9
        const val ABS_Z = 2
        const val ABS_RZ = 5
        const val ABS_HAT0X = 16
        const val ABS_HAT0Y = 17

        // The ranges the descriptor declares. The platform normalises these back to -1…+1 on the
        // way out, which Phase 0 measured rather than assumed.
        const val STICK_MAX = 32767.0
        const val TRIGGER_MAX = 255.0
    }
}
