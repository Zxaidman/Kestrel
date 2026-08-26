package io.github.zxaidman.kestrel.core.diagnostics

import io.github.zxaidman.kestrel.core.input.GamepadControl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PadDirectionTest {

    @Test
    fun `a centred pad is no direction at all`() {
        assertNull(PadDirection.of(0.0, 0.0))
        assertNull(PadDirection.of(0.1, -0.2))
    }

    @Test
    fun `each of the eight is reachable`() {
        val found = listOf(
            0.0 to -1.0, 0.8 to -0.8, 1.0 to 0.0, 0.8 to 0.8,
            0.0 to 1.0, -0.8 to 0.8, -1.0 to 0.0, -0.8 to -0.8,
        ).mapNotNull { (x, y) -> PadDirection.of(x, y) }.toSet()
        assertEquals(PadDirection.entries.toSet(), found, "a direction no push can reach is a dead corner")
    }

    @Test
    fun `a cardinal is wider than a diagonal, as the overlay draws it`() {
        // Straight up with a little sideways drift is still up. A thumb aiming for "up" misses more
        // often than one aiming for a corner it can feel itself reaching for.
        assertEquals(PadDirection.UP, PadDirection.of(0.3, -0.95))
    }
}

class ProofBoardTest {

    @Test
    fun `nothing is proven before anything is pressed`() {
        val board = ProofBoard()
        assertEquals(0, board.provenCount)
        assertEquals(GamepadControl.entries.size, board.total)
    }

    @Test
    fun `a button is proven by arriving once, and stays proven after release`() {
        val board = ProofBoard().button(GamepadControl.A, down = true).button(GamepadControl.A, down = false)
        assertTrue(board.reading(GamepadControl.A).proven)
        assertFalse(board.reading(GamepadControl.A).active, "it is not held any more")
    }

    /**
     * The rule the whole screen exists for. Seven directions is a pad with a dead corner, and a
     * screen that called that proven would be a screen that ships the fault.
     */
    @Test
    fun `a pad is not proven until all eight directions have arrived`() {
        var board = ProofBoard()
        val pushes = listOf(
            0.0 to -1.0, 0.8 to -0.8, 1.0 to 0.0, 0.8 to 0.8,
            0.0 to 1.0, -0.8 to 0.8, -1.0 to 0.0,
        )
        pushes.forEach { (x, y) -> board = board.pad(x, y) }
        assertFalse(board.reading(GamepadControl.DPAD).proven, "seven of eight is not a proven pad")
        assertEquals(7, board.reading(GamepadControl.DPAD).directions.size)

        board = board.pad(-0.8, -0.8)
        assertTrue(board.reading(GamepadControl.DPAD).proven)
    }

    @Test
    fun `a trigger that only ever reaches a third of its range is seen but not proven`() {
        val board = ProofBoard().trigger(GamepadControl.LEFT_TRIGGER, 0.33)
        val reading = board.reading(GamepadControl.LEFT_TRIGGER)
        assertTrue(reading.seen, "it arrived")
        assertFalse(reading.proven, "an axis stuck at a third of its range still arrives")
    }

    @Test
    fun `a trigger remembers the furthest it reached, not where it is now`() {
        val board = ProofBoard()
            .trigger(GamepadControl.RIGHT_TRIGGER, 1.0)
            .trigger(GamepadControl.RIGHT_TRIGGER, 0.0)
        assertTrue(board.reading(GamepadControl.RIGHT_TRIGGER).proven, "asking the reader to catch the moment is asking them to watch rather than sweep")
    }

    @Test
    fun `a stick is proven by reaching its corner, not by twitching`() {
        var board = ProofBoard().stick(GamepadControl.LEFT_STICK, 0.2, 0.1)
        assertFalse(board.reading(GamepadControl.LEFT_STICK).proven)
        board = board.stick(GamepadControl.LEFT_STICK, 0.7, 0.7)
        assertTrue(board.reading(GamepadControl.LEFT_STICK).proven)
    }

    @Test
    fun `what is left to prove is listed, and clearing puts it all back`() {
        val board = ProofBoard().button(GamepadControl.A, down = true)
        assertFalse(GamepadControl.A in board.unproven)
        assertTrue(GamepadControl.B in board.unproven)
        assertEquals(0, board.cleared().provenCount)
    }
}
