package io.github.zxaidman.kestrel.platform.input

import io.github.zxaidman.kestrel.core.input.AnalogProfile
import io.github.zxaidman.kestrel.platform.shizuku.IPrivilegedShell
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The exact text the input path writes to the virtual device, pinned before the backend seam was
 * cut and required to be identical after it.
 *
 * This is the highest-risk code in the project and the one with no device in the loop when it is
 * refactored. These strings are what the platform's uinput helper turns into controller events; one
 * changed number is a button that presses the wrong control, and nothing on a laptop would show it.
 * So the test does not check that the refactor "works" — it checks that not one byte changed.
 *
 * Written against the engine as it stood at `4a2d780`, run green there, and kept unchanged across
 * the refactor.
 */
class WrittenBytesTest {

    /** A shell that records what it was asked to write, and never touches Android. */
    private class RecordingShell : IPrivilegedShell {
        val written = mutableListOf<String>()
        var openedPath: String? = null
        var closed = false
        override fun destroy() {}
        override fun exec(command: String?, timeoutMs: Int): String = ""
        override fun openDeviceStream(path: String?): Boolean { openedPath = path; return true }
        override fun writeDeviceStream(data: String?): Boolean { written += data.orEmpty(); return true }
        override fun closeDeviceStream() { closed = true }
        override fun asBinder(): android.os.IBinder? = null
    }

    private fun line(vararg events: Int): String =
        """{"id": 1, "command": "inject", "events": [${(events.toList() + listOf(0, 0, 0)).joinToString(", ")}]}""" + "\n"

    @Test
    fun `every kind of control writes exactly the bytes it wrote before the seam`() {
        val shell = RecordingShell()
        val engine = Fixture.engine(shell)
        val profile = AnalogProfile.DEFAULT_STICK

        assertEquals(true, Fixture.start(engine))
        Fixture.press(engine, "a", true)        // BTN_SOUTH down
        Fixture.press(engine, "a", false)       // BTN_SOUTH up
        Fixture.press(engine, "right-bumper", true)
        Fixture.press(engine, "start", true)
        Fixture.press(engine, "right-stick-press", true)
        engine.hat(-1, 1)
        engine.hat(0, 0)
        engine.trigger(1.0, profile, right = false)
        engine.trigger(1.0, profile, right = true)
        engine.trigger(0.0, profile, right = true)
        engine.stop()                            // releases both sticks, then closes

        assertEquals(
            listOf(
                line(1, 304, 1),
                line(1, 304, 0),
                line(1, 311, 1),
                line(1, 315, 1),
                line(1, 318, 1),
                line(3, 16, -1, 3, 17, 1),
                line(3, 16, 0, 3, 17, 0),
                line(3, 10, 255),
                line(3, 9, 255),
                line(3, 9, 0),
                line(3, 0, 0, 3, 1, 0),
                line(3, 2, 0, 3, 5, 0),
            ),
            shell.written,
        )
        assertEquals(true, shell.closed)
        assertEquals(12L, engine.delivered)
    }

    @Test
    fun `the stream opened is the one the virtual device reads`() {
        val shell = RecordingShell()
        Fixture.start(Fixture.engine(shell))
        assertEquals("/data/local/tmp/kestrel-stream.json", shell.openedPath)
    }
}
