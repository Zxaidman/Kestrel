package io.github.zxaidman.kestrel.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.zxaidman.kestrel.core.common.Outcome
import io.github.zxaidman.kestrel.core.layout.LayoutRepository
import io.github.zxaidman.kestrel.platform.session.ControllerSessionService
import io.github.zxaidman.kestrel.platform.session.SessionState
import io.github.zxaidman.kestrel.platform.settings.AppSettings
import io.github.zxaidman.kestrel.platform.shizuku.ShizukuCapability
import io.github.zxaidman.kestrel.platform.storage.KestrelStorage
import io.github.zxaidman.kestrel.ui.theme.KButton
import io.github.zxaidman.kestrel.ui.theme.KOutlinedButton
import io.github.zxaidman.kestrel.ui.theme.provenText

/**
 * The first thing a person sees: what Kestrel is, what state it is in, and what to do next.
 *
 * **What it replaced.** Kestrel opened on its diagnostics harness — every developer control, every
 * raw number, Shizuku's version and privilege level, one long scroll. It was right while the
 * question was "does any of this work", and the project owner put it plainly: *"we can't have our dev
 * build homepage as release build"* (`CRIT-2`). The harness is still here, unchanged, behind
 * "Developer tools" at the bottom — a deliberate door rather than the front door.
 *
 * **What it says.** Three facts, each one a person can act on: whether the controller is running,
 * whether the privilege it needs is there, and which layout it will draw. Then the one thing to do
 * about the first, then the three places to go. Nothing here is a number a person has to interpret.
 *
 * **What it cannot do yet.** Launch a game. There is no discovery and no launcher (`FEAT-5`), so the
 * controller is started here and the game is opened from the phone's own home screen. Saying so is
 * better than a Play button that does not exist.
 *
 * [refresh] changes every second while the page is shown, because Shizuku and the session change from
 * outside Kestrel and nothing else would tell this page to look again.
 */
@Composable
public fun HomeScreen(
    refresh: Int,
    onEditLayout: () -> Unit,
    onTestGround: () -> Unit,
    onSettings: () -> Unit,
    onDeveloper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shizuku = remember(refresh) { ShizukuCapability.state() }
    val running = SessionState.open.value
    val layoutId = AppSettings.current.value.layoutId
    val layoutName = remember(layoutId, refresh) {
        when (val loaded = LayoutRepository(KestrelStorage.current(context)).load(layoutId)) {
            is Outcome.Success -> loaded.value.header.name
            is Outcome.Failure -> "$layoutId — could not be read"
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Kestrel", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "A gamepad you arrange by hand, on the phone you already have.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusRow(
                    label = "Controller",
                    value = if (running) "running" else "not running",
                    good = running,
                )
                StatusRow(
                    label = "Shizuku",
                    value = when {
                        shizuku.usable -> "ready"
                        !shizuku.serviceRunning -> "not running"
                        !shizuku.permissionGranted -> "needs permission"
                        else -> "not usable"
                    },
                    good = shizuku.usable,
                )
                StatusRow(label = "Layout", value = layoutName, good = null)
                if (!shizuku.usable) {
                    Text(
                        text = shizuku.advice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (running) {
            KOutlinedButton(
                onClick = { ControllerSessionService.stop(context) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Stop controller") }
            Text(
                text = "The pad is drawn over whatever you open next. Open your game from the " +
                    "phone's home screen — Kestrel cannot launch one yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            KButton(
                onClick = { ControllerSessionService.start(context) },
                enabled = shizuku.usable,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Start controller") }
        }

        Column {
            Destination("Edit layout", "Arrange the pad for $layoutName", onEditLayout)
            HorizontalDivider()
            Destination("Test ground", "Press every control and see it arrive", onTestGround)
            HorizontalDivider()
            Destination("Settings", "Display, theme and where your files are kept", onSettings)
        }

        // The deliberate door. Quiet on purpose: everything behind it is for finding faults, and a
        // person who has not met one should not have to scroll past it to start a game.
        TextButton(
            onClick = onDeveloper,
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            Text(
                "Developer tools",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One fact: a label, its value, and whether that value is the good one. */
@Composable
private fun StatusRow(label: String, value: String, good: Boolean?) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(112.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            // Good is said in green, drawn for the ground it is on (`BUG-55`). Not-yet-good is said
            // in words, in the ordinary colour: an error colour for "not running" would shout about
            // the normal state of a controller nobody has started.
            color = if (good == true) provenText() else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Destination(title: String, detail: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(detail) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
