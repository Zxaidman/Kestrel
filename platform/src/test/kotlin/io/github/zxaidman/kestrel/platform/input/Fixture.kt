package io.github.zxaidman.kestrel.platform.input

import io.github.zxaidman.kestrel.core.input.GamepadControl
import io.github.zxaidman.kestrel.platform.input.virtual.VirtualControllerInput
import io.github.zxaidman.kestrel.platform.shizuku.IPrivilegedShell

/**
 * How the test builds and drives an engine — the one place that changes when the engine's
 * constructor does. The expected bytes in [WrittenBytesTest] do not change.
 *
 * Before the backend seam this built `InputEngine(shell)`, started it on the stream path and pressed
 * buttons by kernel code. After it, the engine is built on [VirtualControllerInput] and pressed by
 * control. The bytes the test expects were not edited between the two.
 */
internal object Fixture {
    fun engine(shell: IPrivilegedShell): InputEngine = InputEngine(VirtualControllerInput(shell))

    fun start(engine: InputEngine): Boolean = engine.start()

    fun press(engine: InputEngine, wireName: String, down: Boolean) {
        val control = GamepadControl.of(wireName) ?: error("no control $wireName")
        engine.button(control, down)
    }
}
