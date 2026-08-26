package io.github.zxaidman.kestrel.feature.testground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.zxaidman.kestrel.core.diagnostics.ControlReading
import io.github.zxaidman.kestrel.core.diagnostics.PadDirection
import io.github.zxaidman.kestrel.core.diagnostics.ProofBoard
import io.github.zxaidman.kestrel.core.input.ControlForm
import io.github.zxaidman.kestrel.core.input.GamepadControl
import io.github.zxaidman.kestrel.ui.theme.KButton
import io.github.zxaidman.kestrel.ui.theme.KOutlinedButton

/** Proven. */
private val LIT = Color(0xFF5FBF77)

/** Arrived, but not as far as it should go — a trigger at a third, a pad missing a corner. */
private val PARTIAL = Color(0xFFF2B441)

/** Held right now. */
private val HELD = Color(0xFF8C9EFF)

/**
 * Every control, and what the platform has delivered for each.
 *
 * **This is the loop that found every input fault in this project, moved into the product.** Press
 * something, read a number. Until now that loop ran inside a target application: put the pad up,
 * launch an emulator, press a button, watch a character — so every claim about behaviour cost a full
 * session, and a claim that turned out to be wrong cost another one.
 *
 * Three things it says that a game cannot:
 *
 * - **What has been proven and what has not.** A control stays marked once it has arrived, so a
 *   sweep can be done in any order without remembering what has been tried.
 * - **How far an axis actually went.** A trigger stuck at a third of its range still arrives; a
 *   stick that reads 0.7 at the corner is a stick with a problem. Both look like "it works" in a
 *   game.
 * - **Which of a pad's eight directions are dead.** Seven working is a fault that a game shows only
 *   when a player happens to need the eighth.
 *
 * **Nothing here creates input.** It observes the events the platform delivers to this window, the
 * same ones a game would receive.
 */
@Composable
public fun TestGroundScreen(state: TestGroundState, onClose: () -> Unit) {
    val board = state.board

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Test ground", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "Press every control. Each one lights when the platform delivers it back here — " +
                "so this proves the whole path, not just that a control was drawn. Sticks and " +
                "triggers have to reach their limit and the pad has to visit all eight " +
                "directions, because an axis stuck at a third of its range still arrives.",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "${board.provenCount} / ${board.total}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = if (board.provenCount == board.total) LIT else MaterialTheme.colorScheme.onSurface,
            )
            LinearProgressIndicator(
                progress = { board.provenCount.toFloat() / board.total },
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = "from  ${state.source}\n${state.events} events",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Section("Buttons")
        Grid(GamepadControl.entries.filter { it.form == ControlForm.BUTTON }) { Tile(board.reading(it)) }

        Section("Triggers")
        GamepadControl.entries.filter { it.form == ControlForm.TRIGGER }.forEach {
            AxisBar(board.reading(it))
        }

        Section("Sticks")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GamepadControl.entries.filter { it.form == ControlForm.STICK }.forEach {
                StickBox(board.reading(it), Modifier.weight(1f))
            }
        }

        Section("D-pad")
        PadWheel(board.reading(GamepadControl.DPAD))

        if (board.unproven.isNotEmpty()) {
            Text(
                text = "left to prove: " + board.unproven.joinToString(", ") { it.defaultLabel },
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = PARTIAL,
            )
        } else {
            Text(
                text = "Every control proven on this device, this session.",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = LIT,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KOutlinedButton(onClick = state::clear, modifier = Modifier.weight(1f)) { Text("Start again") }
            KButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Done") }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

/** Wrapped by hand rather than with a lazy grid: seventeen tiles do not need windowing. */
@Composable
private fun <T> Grid(items: List<T>, perRow: Int = 4, cell: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { Box(Modifier.weight(1f)) { cell(it) } }
                repeat(perRow - row.size) { Box(Modifier.weight(1f)) {} }
            }
        }
    }
}

@Composable
private fun Tile(reading: ControlReading) {
    val colour = when {
        reading.active -> HELD
        reading.proven -> LIT
        reading.seen -> PARTIAL
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(
        modifier = Modifier.fillMaxWidth().height(46.dp),
        color = colour.copy(alpha = if (reading.proven || reading.active) 0.85f else 0.35f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = reading.control.defaultLabel,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

/** A trigger: how far it is now, and a mark at the furthest it has ever been. */
@Composable
private fun AxisBar(reading: ControlReading) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "%-6s  now %.2f   best %.2f%s".format(
                reading.control.defaultLabel,
                if (reading.active) reading.magnitude else 0.0,
                reading.magnitude,
                if (reading.proven) "  ✓" else "",
            ),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
        )
        LinearProgressIndicator(
            progress = { reading.magnitude.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (reading.proven) LIT else PARTIAL,
        )
    }
}

/**
 * A stick as a square with a dot in it, because two numbers do not show a corner that will not
 * reach. The ring is the distance that counts as proven.
 */
@Composable
private fun StickBox(reading: ControlReading, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .border(
                    1.dp,
                    if (reading.proven) LIT else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (reading.active) HELD else LIT.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                    .padding(0.dp)
            )
            Text(
                text = "x %+.2f\ny %+.2f\nreach %.2f".format(reading.x, reading.y, reading.magnitude),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(4.dp),
            )
        }
        Text(
            text = reading.control.defaultLabel + if (reading.proven) "  ✓" else "",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/** The eight, each lit once it has arrived — so a dead corner is visible rather than inferred. */
@Composable
private fun PadWheel(reading: ControlReading) {
    val order = listOf(
        listOf(PadDirection.UP_LEFT, PadDirection.UP, PadDirection.UP_RIGHT),
        listOf(PadDirection.LEFT, null, PadDirection.RIGHT),
        listOf(PadDirection.DOWN_LEFT, PadDirection.DOWN, PadDirection.DOWN_RIGHT),
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        order.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { direction ->
                    Surface(
                        modifier = Modifier.size(46.dp),
                        color = when {
                            direction == null -> Color.Transparent
                            direction in reading.directions -> LIT.copy(alpha = 0.85f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(direction?.label ?: "", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                Column(Modifier.width(120.dp).padding(start = 8.dp)) {}
            }
        }
        Text(
            text = "${reading.directions.size} / 8 directions" + if (reading.proven) "  ✓" else "",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = if (reading.proven) LIT else PARTIAL,
        )
    }
}
