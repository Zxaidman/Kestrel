package io.github.zxaidman.kestrel.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.zxaidman.kestrel.core.common.Outcome
import io.github.zxaidman.kestrel.core.input.AnalogProfile
import io.github.zxaidman.kestrel.core.layout.Anchor
import io.github.zxaidman.kestrel.core.layout.BuiltInLayouts
import io.github.zxaidman.kestrel.core.layout.Cluster
import io.github.zxaidman.kestrel.core.layout.Clustering
import io.github.zxaidman.kestrel.core.layout.ControlKind
import io.github.zxaidman.kestrel.core.layout.ControlShape
import io.github.zxaidman.kestrel.core.layout.ControllerLayout
import io.github.zxaidman.kestrel.core.layout.LayoutElement
import io.github.zxaidman.kestrel.core.layout.LayoutSurface
import io.github.zxaidman.kestrel.core.layout.PixelRect
import io.github.zxaidman.kestrel.core.layout.Placement
import io.github.zxaidman.kestrel.core.layout.centeredAt
import io.github.zxaidman.kestrel.core.layout.effectiveShapeFor
import io.github.zxaidman.kestrel.core.layout.isWithin
import io.github.zxaidman.kestrel.core.layout.resolve
import io.github.zxaidman.kestrel.core.layout.scaledBy
import io.github.zxaidman.kestrel.core.layout.shapedAs
import io.github.zxaidman.kestrel.core.settings.EditorPreferences
import io.github.zxaidman.kestrel.core.settings.IdlePreferences
import io.github.zxaidman.kestrel.core.settings.KestrelSettings
import io.github.zxaidman.kestrel.platform.display.DeviceSurface
import io.github.zxaidman.kestrel.platform.session.SessionState
import io.github.zxaidman.kestrel.platform.settings.AppSettings
import io.github.zxaidman.kestrel.ui.theme.KButton
import io.github.zxaidman.kestrel.ui.theme.KOutlinedButton
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * What came of a Save.
 *
 * A message on its own was not enough to act on: the editor has to know whether the file actually
 * changed before it moves its idea of what is in it, and reading that back out of the wording
 * would make the wording load-bearing.
 */
public data class SaveOutcome(val written: Boolean, val message: String)

/**
 * Editing a layout by moving it, rather than by typing numbers into a file.
 *
 * The file stays the truth — this writes the same document a text editor would, and everything it
 * can do can still be done by hand. What it adds is the one thing a text editor cannot: **seeing
 * where a control is while deciding where it should be.**
 *
 * Three rules it keeps, because they are the schema's rules rather than this screen's:
 *
 * **A built-in is never edited.** Opening the editor on one duplicates it first, and the copy is
 * what is edited from then on.
 *
 * **Nothing is saved until it is saved.** Dragging changes what is on screen; the file changes when
 * the button is pressed.
 *
 * **The canvas is the phone, not the page.** The arrangement is drawn inside a rectangle with the
 * device's own aspect ratio, scaled to fit whole and never scrolled. The previous version drew into
 * whatever space the screen gave it, which on this device is close to ultrawide — so controls
 * appeared to overlap that did not, and, worse, some that did overlap looked clear. An editor that
 * lies about overlap is worse than a text file, because it invites trust it has not earned.
 */
@Composable
public fun LayoutEditorScreen(
    layout: ControllerLayout,
    onSave: (ControllerLayout) -> SaveOutcome,
    onClose: () -> Unit,
    onPreviewOrientation: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    // The whole display, or what is left after the system's share, depending on the setting — and
    // whichever it is, it is the same answer the pad itself asks for. The bars are still measured
    // so the band can be drawn: a control under the status bar is allowed and worth seeing.
    val wholeScreen = AppSettings.current.value.display.drawUnderCutout
    // The bars only. The camera cutout is not shaded, because a control in it works whether or not
    // the phone is full screen — it is the system's *windows* that take touches, and the cutout is
    // not one.
    val screen = remember(configuration) { DeviceSurface.screen(context, cutoutCounts = false) }
    val device = remember(configuration, wholeScreen) {
        DeviceSurface.forPad(context, wholeScreen)
    }
    val bars = remember(screen, device, wholeScreen) {
        if (!wholeScreen) {
            null
        } else {
            Rect(
                left = screen.insetLeft.toFloat(),
                top = screen.insetTop.toFloat(),
                right = (screen.widthPx - screen.insetRight).toFloat(),
                bottom = (screen.heightPx - screen.insetBottom).toFloat(),
            )
        }
    }

    var working by remember(layout.header.id) { mutableStateOf(layout) }
    var selectedId by remember(layout.header.id) { mutableStateOf<String?>(null) }
    // What is in the file. Save writes over it; everything else leaves it alone. Keeping it lets
    // the editor answer two questions it could not before: which of the two arrangements has
    // unsaved work, and what the *other* one looked like on disk when this one is written out.
    var savedDoc by remember(layout.header.id) { mutableStateOf(layout) }
    var message by remember { mutableStateOf("") }
    // Remembered for the session, not written to settings.json. Somebody who turns edge snapping
    // on wants it on for the arranging they are doing — but it is working state rather than a
    // preference, and every field in that file is one more thing to version and migrate.
    val editorPreferences = AppSettings.current.value.editor
    var gridUnit by remember { mutableStateOf(editorPreferences.gridUnit) }
    var snapToGrid by remember { mutableStateOf(editorPreferences.snapToGrid) }
    var snapToEdges by remember { mutableStateOf(editorPreferences.snapToEdges) }

    fun rememberEditorSetup(update: (EditorPreferences) -> EditorPreferences) {
        AppSettings.update { it.copy(editor = update(it.editor)) }
        AppSettings.persist(context)
    }
    var typingNumbers by remember { mutableStateOf(false) }
    var toolsOpen by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    var menuFor by remember(layout.header.id) { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf<ControlStyle?>(null) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    // The buttons and their captions are one block that can be dragged anywhere and put out of the
    // way. It was pinned to the middle, which is the one place a pad never is — right up until a
    // control is dragged there, and then it is on top of the thing being edited with no way to move
    // either of them.
    var panelLandscape by remember { mutableStateOf(Offset.Zero) }
    var panelPortrait by remember { mutableStateOf(Offset.Zero) }
    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    var panelHidden by remember { mutableStateOf(false) }
    var panelMenu by remember { mutableStateOf(false) }

    val selected = working.element(selectedId ?: "")
    val landscape = device.widthPx >= device.heightPx

    // The size setting the pad is showing right now. The document is the pad at full size and the
    // setting is applied on top of it — so the canvas has to apply it too, or it draws a pad 17%
    // larger than the one on the phone. Two rounds of "it does not match" had this underneath the
    // cause that was found first.
    // Which arrangement is being edited, and which size setting applies. Both belong to the
    // orientation the phone is in — the editor turns the phone to work on the other one rather than
    // drawing a small picture of it, so "the orientation on screen" is always the honest answer.
    val portrait = device.heightPx > device.widthPx

    // Unsaved work, counted per orientation rather than once for the document. Save used to write
    // whatever was in memory, so arranging landscape and pressing Save committed a half-finished
    // portrait arrangement along with it. Derived from the file rather than set by a flag: every
    // edit goes through `working`, and a flag would have to be set at each of the dozen places
    // that touch it — one missed call site and the editor lies about what is saved.
    val dirtyLandscape = remember(working, savedDoc) { working.differsFrom(savedDoc, portrait = false) }
    val dirtyPortrait = remember(working, savedDoc) { working.differsFrom(savedDoc, portrait = true) }
    val dirtyHere = if (portrait) dirtyPortrait else dirtyLandscape
    val dirtyOther = if (portrait) dirtyLandscape else dirtyPortrait

    // Where the block was put, per orientation. One position for both meant that moving it out of
    // the way in landscape put it in the way upright — the pad is in a different place in each,
    // which is the whole reason a layout has two arrangements.
    val panel = if (portrait) panelPortrait else panelLandscape

    // Read through these inside the gesture. `panel` is derived from two states and the gesture is
    // keyed on neither, so what the drag captured was the position at the moment the gesture
    // started — every frame added its delta to the same stale number and the block did not move at
    // all. The same trap as the canvas's own drag, which is why that one reads through
    // `rememberUpdatedState` too.
    val livePanel by rememberUpdatedState(panel)
    val livePanelPortrait by rememberUpdatedState(portrait)
    val controlScale = AppSettings.current.value.let {
        if (portrait) it.controlScalePortrait else it.controlScale
    }.toFloat()

    // Controls that have ended up somewhere they cannot be used. Kept even though dragging can no
    // longer put one off the screen: a hand-written file, a layout from somewhere else, or a bug
    // nobody has met yet can all still do it, and the project owner asked for the way back to stay.
    val strays = working.elements.filter { element ->
        !element.placementFor(portrait).scaledBy(controlScale.toDouble()).resolve(device)
            .shapedAs(element.effectiveShapeFor(portrait))
            .isWithin(device)
    }.map { it.id }

    // Controls sitting on top of another control. Above the size the shipped layout is guaranteed
    // clean at, this is allowed and the editor's job is to say where it happens — a range whose top
    // is unmarked is a range whose top is a surprise.
    val overlapping = remember(working, device, controlScale, portrait) {
        val placed = working.elements.map { element ->
            element.id to element.placementFor(portrait).scaledBy(controlScale.toDouble())
                .resolve(device).shapedAs(element.effectiveShapeFor(portrait))
        }
        val met = mutableSetOf<String>()
        for (i in placed.indices) {
            for (j in i + 1 until placed.size) {
                if (Clustering.gapBetween(placed[i].second, placed[j].second) < 0) {
                    met += placed[i].first
                    met += placed[j].first
                }
            }
        }
        met
    }

    // Controls in the strip the system bars take. They work while a game is full screen and not
    // while the bars are showing, which is worth counting rather than explaining twice.
    val underBars = bars?.let { band ->
        working.elements.filter { element ->
            val rect = element.placementFor(portrait).scaledBy(controlScale.toDouble())
                .resolve(device).shapedAs(element.effectiveShapeFor(portrait))
            rect.top < band.top || rect.bottom > band.bottom ||
                rect.left < band.left || rect.right > band.right
        }.map { it.id }
    }.orEmpty()

    Box(modifier = Modifier.fillMaxSize().onSizeChanged { rootSize = it }) {
        EditorCanvas(
            modifier = Modifier.fillMaxSize(),
            device = device,
            bars = bars,
            controlScale = controlScale,
            portrait = portrait,
            layout = working,
            selectedId = selectedId,
            marked = overlapping,
            dimExcept = menuFor,
            gridUnit = gridUnit,
            snapToGrid = snapToGrid,
            snapToEdges = snapToEdges,
            onSelect = {
                selectedId = it
                menuFor = null
                // Touching a control brings the panel back. A hidden panel that could only be
                // recovered from a menu inside itself would be a way to lose Save and Exit.
                if (it != null) panelHidden = false
            },
            onLongPress = { id, _ ->
                selectedId = id
                menuFor = id
            },
            onPlace = { updated ->
                working = working.replacing(updated)
            },
        )

        // The middle of the screen, which is the one place a pad never is: controls belong to the
        // corners and edges a thumb reaches, and the centre is what a game is played through.
        // Anywhere else and these would sit on top of the thing being arranged.
        if (panelHidden) {
            // Minimised: one button, draggable like the block was, and a tap brings the block back
            // to the middle at full size. Fading it to a fifth and leaving it in place solved the
            // wrong problem — it was still there to be caught by a thumb, and it was still on top
            // of whatever it had been covering.
            FloatingActionButton(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(panel.x.roundToInt(), panel.y.roundToInt()) }
                    .onSizeChanged { panelSize = it }
                    .pointerInput(rootSize, panelSize) {
                        detectDragGestures { change, dragged ->
                            change.consume()
                            val limitX = max(0f, (rootSize.width - panelSize.width) / 2f)
                            val limitY = max(0f, (rootSize.height - panelSize.height) / 2f)
                            val moved = Offset(
                                (livePanel.x + dragged.x).coerceIn(-limitX, limitX),
                                (livePanel.y + dragged.y).coerceIn(-limitY, limitY),
                            )
                            if (livePanelPortrait) panelPortrait = moved else panelLandscape = moved
                        }
                    },
                onClick = {
                    panelHidden = false
                    if (portrait) panelPortrait = Offset.Zero else panelLandscape = Offset.Zero
                },
            ) { Icon(Icons.Filled.Settings, contentDescription = "Show the editor buttons") }
        } else {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(panel.x.roundToInt(), panel.y.roundToInt()) }
                    .onSizeChanged { panelSize = it }
                    .pointerInput(rootSize, panelSize) {
                        detectDragGestures { change, dragged ->
                            change.consume()
                            val limitX = max(0f, (rootSize.width - panelSize.width) / 2f)
                            val limitY = max(0f, (rootSize.height - panelSize.height) / 2f)
                            val moved = Offset(
                                (livePanel.x + dragged.x).coerceIn(-limitX, limitX),
                                (livePanel.y + dragged.y).coerceIn(-limitY, limitY),
                            )
                            if (livePanelPortrait) panelPortrait = moved else panelLandscape = moved
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { panelMenu = true })
                    },
                // Opaque, and a window rather than a wash. Translucent, it was still catching
                // thumbs meant for the pad behind it, which is a control you cannot see being
                // pressed by mistake.
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FloatingActionButton(onClick = { toolsOpen = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                        FloatingActionButton(
                            onClick = { onPreviewOrientation(!landscape) },
                        ) { Icon(Icons.Filled.Refresh, contentDescription = "Turn the phone") }
                        if (strays.isNotEmpty()) {
                            FloatingActionButton(
                                containerColor = Color(0xFFE0603A),
                                onClick = {
                                    working = working.withStraysBroughtBack(strays, portrait)
                                    message = "${strays.size} put back."
                                },
                            ) { Icon(Icons.Filled.Home, contentDescription = "Bring strays back") }
                        }
                        FloatingActionButton(
                            containerColor = if (dirtyHere) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            onClick = {
                                message = if (dirtyHere) {
                                    // Only this orientation. `working` still holds whatever was
                                    // arranged in the other one; it stays in memory, unsaved,
                                    // until the phone is turned to it and Save is pressed there.
                                    val write = working.mergedOver(savedDoc, portrait)
                                    val said = onSave(write)
                                    // Only on a write that happened. Moving the baseline after a
                                    // failed save would report the work as filed when the file
                                    // still holds what it held before.
                                    if (said.written) savedDoc = write
                                    said.message
                                } else {
                                    "Nothing has changed here."
                                }
                            },
                        ) { Text("Save") }
                        FloatingActionButton(
                            onClick = {
                                if (dirtyHere || dirtyOther) leaving = true else onClose()
                            },
                        ) { Text("Exit") }
                    }

                    // Four lines, each answering one question, in a small face so they cost little
                    // room. One line holding all four was smaller and read as noise: the warning
                    // and the name of the thing you are holding are not the same kind of fact.

                    // 1. Warnings, and only when there are any.
                    val warnings = buildList {
                        if (strays.isNotEmpty()) add("${strays.size} off screen")
                        if (overlapping.isNotEmpty()) add("${overlapping.size} overlapping")
                        if (underBars.isNotEmpty()) add("${underBars.size} under the bars")
                        if (message.isNotBlank()) add(message)
                    }
                    if (warnings.isNotEmpty()) {
                        Text(
                            text = warnings.joinToString("   "),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFF2B441),
                            textAlign = TextAlign.Center,
                        )
                    }

                    // 2. Which layout, which orientation, and whether it is saved.
                    Text(
                        text = working.header.name +
                            (if (portrait) "  ·  portrait" else "  ·  landscape") +
                            if (dirtyHere) "  •  unsaved" else "",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                    )

                    // 3. What is selected.
                    Text(
                        text = selected?.let { "${it.id}  ·  ${it.kind.wireName}" }
                            ?: "nothing selected",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                    )

                    // 4. Where it is and how big, in both units.
                    Text(
                        text = selected?.summary(device, portrait) ?: " ",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (panelMenu) {
            AlertDialog(
                onDismissRequest = { panelMenu = false },
                title = { Text("These buttons") },
                text = {
                    Text(
                        "Drag them anywhere. Minimised, they become one button that also drags — " +
                            "tap it to bring them back to the middle."
                    )
                },
                confirmButton = {
                    KButton(onClick = {
                        panelHidden = true
                        panelMenu = false
                    }) { Text("Minimise") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        if (portrait) panelPortrait = Offset.Zero else panelLandscape = Offset.Zero
                        panelMenu = false
                    }) { Text("Back to the middle") }
                },
            )
        }

        val menuElement = working.element(menuFor ?: "")
        if (menuElement != null && !toolsOpen) {
            ControlMenu(
                element = menuElement,
                device = device,
                landscape = !portrait,
                copied = copied,
                onSize = {
                    menuFor = null
                    typingNumbers = true
                },
                onShape = { shape ->
                    working = working.replacing(menuElement.withShapeFor(portrait, shape))
                },
                onStep = { updated ->
                    working = working.replacing(updated)
                },
                onGroup = { step ->
                    val options = working.windowOptions()
                    working = working.replacing(
                        if (step == 0) {
                            menuElement.copy(group = null)
                        } else {
                            menuElement.withGroupStep(options, step)
                        }
                    )
                },
                portrait = portrait,
                onCopy = {
                    copied = ControlStyle.of(menuElement, portrait)
                    menuFor = null
                },
                onPaste = {
                    copied?.let { style ->
                        working = working.replacing(style.appliedTo(menuElement, portrait))
                    }
                    menuFor = null
                },
                onDismiss = { menuFor = null },
            )
        }

        if (toolsOpen) {
            ToolsSheet(
                landscape = landscape,
                onDismiss = { toolsOpen = false },
            ) {
                WindowSummary(
                    layout = working,
                    device = device,
                    controlScale = controlScale,
                    portrait = portrait,
                    element = selected,
                )

                OrientationTools(
                    portrait = portrait,
                    layout = working,
                    onChange = { updated ->
                        working = updated
                    },
                )

                PadTools(
                    portrait = portrait,
                    onPersist = { AppSettings.persist(context) },
                )

                GridTools(
                    gridUnit = gridUnit,
                    onGrid = {
                        gridUnit = it
                        rememberEditorSetup { prefs -> prefs.copy(gridUnit = it) }
                    },
                    snapToGrid = snapToGrid,
                    onSnapToGrid = {
                        snapToGrid = it
                        rememberEditorSetup { prefs -> prefs.copy(snapToGrid = it) }
                    },
                    snapToEdges = snapToEdges,
                    onSnapToEdges = {
                        snapToEdges = it
                        rememberEditorSetup { prefs -> prefs.copy(snapToEdges = it) }
                    },
                    device = device,
                    wholeScreen = wholeScreen,
                )
            }
        }
    }

    if (typingNumbers && selected != null) {
        NumbersDialog(
            element = selected,
            device = device,
            portrait = portrait,
            onDismiss = { typingNumbers = false },
            onApply = { updated ->
                working = working.replacing(updated)
                typingNumbers = false
            },
        )
    }

    if (leaving) {
        // Save writes one orientation, so leaving can lose work that is not the work on screen.
        // The dialog names which arrangement is unsaved rather than saying "the arrangement",
        // because the one that is about to be lost may be the one the phone is not showing.
        val here = if (portrait) "portrait" else "landscape"
        val other = if (portrait) "landscape" else "portrait"
        AlertDialog(
            onDismissRequest = { leaving = false },
            title = {
                Text(
                    if (dirtyOther && !dirtyHere) "Editing still pending" else "Leave without saving?"
                )
            },
            text = {
                Text(
                    when {
                        dirtyHere && dirtyOther ->
                            "Neither arrangement has been written to the file. Save writes the " +
                                "one on screen, so $other needs the phone turned to it and a " +
                                "Save of its own."
                        dirtyOther ->
                            "The $here arrangement is saved, but $other still has changes that " +
                                "are not in the file. Turn the phone to $other and save there, " +
                                "or leave and lose them."
                        else ->
                            "The $here arrangement on screen has not been written to the file."
                    }
                )
            },
            confirmButton = {
                KButton(onClick = {
                    leaving = false
                    onClose()
                }) { Text("Leave") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (dirtyOther) {
                        TextButton(onClick = {
                            leaving = false
                            onPreviewOrientation(portrait)
                        }) { Text("Go to $other") }
                    }
                    TextButton(onClick = { leaving = false }) { Text("Stay") }
                }
            },
        )
    }
}


/**
 * Grid steps, in the unit the document itself uses: a fraction of the screen's shorter side.
 *
 * It was pixels, and the project owner named the fault: a control is `0.12` and a grid line was
 * `32px`, so comparing them meant doing arithmetic while arranging a pad. Moving the grid rather
 * than the control also buys something the pixel grid could not promise — `0.01` is exactly the
 * precision the file is rounded to, so a snapped control lands on a number the file can hold.
 *
 * The extremes are gone after a round of use: `0.01` was too fine to see and `0.25` too coarse to
 * place anything with. What is left spans a fifth of a button to most of one.
 */

private val GRID_SIZES = listOf(0.02, 0.04, 0.06, 0.10)
private const val DEFAULT_GRID = 0.04


/** A step small enough to place a control with and large enough to feel like a press. */
private const val STEP = 0.02

/**
 * What the editor will let a control become, at full size.
 *
 * `Placement`'s own bounds are 0.01 and 2.0 and they exist to catch a corrupt file — not to keep a
 * control usable. A control at 0.01 of the shorter side is about half a millimetre on this phone,
 * and one at 2.0 is twice the screen. Neither is a mistake worth being able to make with a button.
 *
 * These are the editor's limits, deliberately, and the file's stay where they are: a layout written
 * by hand or arriving from somewhere else is still read, because refusing to open a file over a
 * matter of taste is worse than showing what it says.
 */
private const val EDITOR_MIN_SIZE = 0.05
private const val EDITOR_MAX_SIZE = 0.50

// --- the furniture that floats on the canvas -----------------------------------------------------

/** A line of text that has to stay readable over whatever the canvas is drawing behind it. */
@Composable
private fun Caption(
    text: String,
    colour: Color = Color(0xFFE8EBEF),
    modifier: Modifier = Modifier.widthIn(max = 320.dp),
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF0B0D11).copy(alpha = 0.82f),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = colour,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * Everything that used to be a side panel, opened over the canvas and closed again.
 *
 * The canvas wants the whole screen because it is a picture of the whole screen; a panel that
 * permanently takes a quarter of it is a quarter of the picture missing. So the tools come when
 * they are asked for. Down one edge in landscape and up from the bottom in portrait, which is
 * where a hand already is in each.
 */
@Composable
private fun ToolsSheet(
    landscape: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onDismiss),
    ) {
        Surface(
            modifier = Modifier
                .align(if (landscape) Alignment.CenterEnd else Alignment.BottomCenter)
                .then(
                    if (landscape) {
                        // Smaller than it was: it used to hold two editors and now holds
                        // settings, and a sheet is a hole in the picture of the pad while it is up.
                        Modifier.fillMaxHeight().fillMaxWidth(0.46f)
                    } else {
                        Modifier.fillMaxWidth().fillMaxHeight(0.52f)
                    }
                )
                // The sheet swallows its own touches; only the dimmed area outside it dismisses.
                .clickable(enabled = false, onClick = {}),
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    KButton(onClick = onDismiss) { Text("Done") }
                }
                content()
            }
        }
    }
}

/**
 * A shape drawn as itself.
 *
 * Three words that all mean "look at the picture you are already looking at" become the picture.
 * Deliberately drawn rather than a glyph from a font: a font has whatever squares and circles it
 * happens to have, at whatever weight, and these have to read at button size on a dark sheet.
 *
 * Where this rule stops: `own window`, `snap to the grid` and the anchor names have no picture that
 * is faster to read than the words, and a project with no icon vocabulary should not invent one a
 * control at a time.
 */
@Composable
private fun ShapeMark(shape: ControlShape, tint: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = Stroke(width = 3.5f * density)
        when (shape) {
            ControlShape.CIRCLE ->
                drawCircle(tint, radius = size.minDimension / 2 - stroke.width, style = stroke)

            ControlShape.SQUARE -> {
                val side = size.minDimension - stroke.width * 2
                drawRoundRect(
                    color = tint,
                    topLeft = Offset((size.width - side) / 2, (size.height - side) / 2),
                    size = Size(side, side),
                    cornerRadius = CornerRadius(side * 0.16f),
                    style = stroke,
                )
            }

            ControlShape.RECTANGLE -> {
                val wide = size.width - stroke.width * 2
                val tall = wide * 0.56f
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(stroke.width, (size.height - tall) / 2),
                    size = Size(wide, tall),
                    cornerRadius = CornerRadius(tall * 0.24f),
                    style = stroke,
                )
            }
        }
    }
}

/** The three shapes as buttons, the current one filled. Used by the tools and by the menu alike. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShapeChoice(current: ControlShape?, enabled: Boolean, onShape: (ControlShape) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ControlShape.entries.forEach { shape ->
            if (shape == current) {
                KButton(enabled = enabled, onClick = { onShape(shape) }) {
                    ShapeMark(shape, MaterialTheme.colorScheme.onPrimary)
                }
            } else {
                KOutlinedButton(enabled = enabled, onClick = { onShape(shape) }) {
                    ShapeMark(shape, MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/**
 * What can be taken from one control and given to another: its size and its outline, and nothing
 * else.
 *
 * **Position is deliberately not copied.** Two controls in the same place are two controls, one of
 * which cannot be pressed — a paste that did that would be a way to lose a button silently.
 */
private data class ControlStyle(
    val width: Double,
    val height: Double,
    val shape: ControlShape,
    val family: ControlFamily,
) {
    fun appliedTo(element: LayoutElement, portrait: Boolean): LayoutElement =
        element.withShapeFor(portrait, shape).let {
            it.withPlacementFor(
                portrait,
                it.placementFor(portrait).copy(width = width, height = height),
            )
        }

    companion object {
        fun of(element: LayoutElement, portrait: Boolean) = ControlStyle(
            width = element.placementFor(portrait).width,
            height = element.placementFor(portrait).height,
            shape = element.shapeFor(portrait),
            family = element.kind.family(),
        )
    }
}

/**
 * Which controls a size means anything between.
 *
 * The project owner's rule, and it is the right one: a face button's size means nothing on a stick.
 * Offering a paste that produces nonsense and then refusing it is worse than not offering it.
 */
private enum class ControlFamily(val label: String) {
    /** The sticks and the pad — the same kind of object, sized against the same thumb. */
    DIRECTIONAL("directional"),

    /** Face, shoulders and menu: everything pressed rather than pushed around. */
    BUTTONS("buttons"),

    /**
     * Triggers, on their own.
     *
     * Decided in round `0.0.29-dev`, and it is right: a trigger is a long rectangle with a fill
     * running up it, and nothing else on a pad is shaped like one. A face button's size on a
     * trigger is a trigger nobody can read.
     */
    TRIGGERS("triggers"),

    /** Anything that sends nothing. It keeps to itself. */
    OTHER("other"),
}

/** Whether saying a shape for this kind of control changes anything on screen. */
private fun ControlKind.shapeMatters(): Boolean =
    this != ControlKind.STICK && this != ControlKind.DPAD

private fun ControlKind.family(): ControlFamily = when (this) {
    ControlKind.STICK, ControlKind.DPAD -> ControlFamily.DIRECTIONAL
    ControlKind.ANALOG_TRIGGER, ControlKind.DIGITAL_TRIGGER -> ControlFamily.TRIGGERS
    ControlKind.BUTTON -> ControlFamily.BUTTONS
    ControlKind.DECORATION -> ControlFamily.OTHER
}

/**
 * The things done to one control, at the control.
 *
 * Everything here is about the control under the finger, so it opens under the finger rather than
 * in a sheet somewhere else on the screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlMenu(
    element: LayoutElement,
    device: LayoutSurface,
    landscape: Boolean,
    copied: ControlStyle?,
    onSize: () -> Unit,
    onShape: (ControlShape) -> Unit,
    onStep: (LayoutElement) -> Unit,
    onGroup: (Int) -> Unit,
    portrait: Boolean,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onDismiss: () -> Unit,
) {
    MenuAt(landscape = landscape, onDismiss = onDismiss) {
        MenuHeader(
            title = element.id,
            detail = element.kind.family().label,
            onDismiss = onDismiss,
        )

        // Two to a row, not one long button each. The menu was taller than a portrait screen and
        // `copy` was simply off the bottom of it — a button nobody could reach, in a menu whose
        // whole point is being reachable.
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KButton(onClick = onSize, modifier = Modifier.weight(1f)) { Text("values") }
            KOutlinedButton(
                onClick = { onStep(element.withNextAnchor(portrait, device)) },
                modifier = Modifier.weight(1f),
            ) { Text(element.placementFor(portrait).anchor.wireName) }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KButton(onClick = { onStep(element.resizedBy(-STEP, portrait)) }) {
                Text("−", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            KButton(onClick = { onStep(element.resizedBy(STEP, portrait)) }) {
                Text("+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            KButton(onClick = { onStep(element.taller(STEP, portrait)) }) { Text("taller") }
            KButton(onClick = { onStep(element.taller(-STEP, portrait)) }) { Text("shorter") }
        }

        // A stick and a pad are drawn round whatever the document says, for reasons recorded in
        // `ControlShape`. Offering the choice anyway was offering a control that does nothing, and
        // the shipped layout has a `"shape": "square"` on a stick to prove somebody tried it.
        if (element.kind.shapeMatters()) {
            ShapeChoice(
                current = element.effectiveShapeFor(portrait),
                enabled = true,
                onShape = onShape,
            )
        }

        // Which window this control is in, at the control. It used to be a mode of its own, which
        // meant the setting with the largest effect on how much of the screen the pad takes away
        // was two taps and a mode away from the control it belongs to.
        Text("window: " + (element.group ?: "its own"), style = MaterialTheme.typography.labelMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KButton(onClick = { onGroup(-1) }) { Text("◀") }
            KButton(onClick = { onGroup(1) }) { Text("▶") }
            KButton(onClick = { onGroup(0) }) { Text("its own") }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KButton(onClick = onCopy) { Text("copy") }
            // Shown only when what is on the clipboard means something here. There is no greyed-out
            // paste, because "why is this disabled" is a worse question than "where is paste" has
            // an answer for.
            if (copied != null && copied.family == element.kind.family()) {
                KButton(onClick = onPaste) { Text("paste") }
            }
        }
        if (copied != null && copied.family != element.kind.family()) {
            Text(
                text = "copied a ${copied.family.label} size — it means nothing here",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * The menu, in the middle, with everything behind it out of the way.
 *
 * It used to open at the finger, which took two bug fixes — one to stop it running off the edge and
 * one to stop it flashing there first — and neither was needed once it stopped following the
 * finger. A menu that is always in the same place is also a menu somebody stops having to look for.
 *
 * **Vertical in landscape, wide in portrait.** A tall panel in the middle of a landscape screen
 * leaves the sides showing; a wide one in portrait leaves the top and bottom. Either way the pad
 * stays visible around it, which matters because the pad is what is being edited.
 *
 * The dimming behind it is drawn by the canvas rather than here, so the selected control can stay
 * lit while everything else goes dark — see `drawDim`.
 */
@Composable
private fun MenuAt(
    landscape: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Draggable, like the floating block and for the same reason: it opens in the middle, which is
    // the one place a pad never is — until a control is dragged there, and then the menu is on top
    // of the control it is about.
    var moved by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var within by remember { mutableStateOf(IntSize.Zero) }
    val liveMoved by rememberUpdatedState(moved)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { within = it }
            // Touching outside the menu closes it, which is what people try first.
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(liveMoved.x.roundToInt(), liveMoved.y.roundToInt()) }
                .onSizeChanged { size = it }
                .then(
                    if (landscape) Modifier.width(MENU_WIDTH.dp) else Modifier.fillMaxWidth(0.94f)
                )
                .pointerInput(within, size) {
                    detectDragGestures { change, dragged ->
                        change.consume()
                        val limitX = max(0f, (within.width - size.width) / 2f)
                        val limitY = max(0f, (within.height - size.height) / 2f)
                        moved = Offset(
                            (liveMoved.x + dragged.x).coerceIn(-limitX, limitX),
                            (liveMoved.y + dragged.y).coerceIn(-limitY, limitY),
                        )
                    }
                }
                .clickable(enabled = false, onClick = {}),
            tonalElevation = 8.dp,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}


/**
 * Whose menu this is, said loudly enough to be the answer to that question.
 *
 * It was the smallest line in the panel, which is backwards: the identity of the thing being edited
 * is what somebody checks first and the only thing that says whether the right control was caught.
 */
@Composable
private fun MenuHeader(title: String, detail: String, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = detail, style = MaterialTheme.typography.labelMedium)
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close",
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

private const val MENU_WIDTH = 380

/**
 * What one orientation of a document actually is: the header, the shared per-control fields, and
 * the placement and shape that orientation draws with. Whether the control has a portrait
 * arrangement of its own is part of it — giving portrait one leaves what it draws unchanged on the
 * first frame, and without this the editor would call that "nothing has changed" and refuse to
 * write it.
 */
private fun ControllerLayout.viewOf(portrait: Boolean): Any =
    header to elements.map { element ->
        listOf(
            element.id,
            element.kind,
            element.binds,
            element.label,
            element.group,
            element.placementFor(portrait),
            element.shapeFor(portrait),
            element.portraitPlacement != null,
        )
    }

/** True when this document would draw [portrait] differently from [other]. */
private fun ControllerLayout.differsFrom(other: ControllerLayout, portrait: Boolean): Boolean =
    viewOf(portrait) != other.viewOf(portrait)

/**
 * The document to write when Save is pressed in one orientation: this orientation as it is on
 * screen, and the other one as it is in the file.
 *
 * Shared fields — the header, the bindings, the window a control belongs to — are not per
 * orientation and are written whichever way round the phone is. Two cases are not held back
 * either. A control with no portrait arrangement of its own has exactly one arrangement, so
 * editing it upright *is* editing landscape and there is nothing to keep separate. And giving or
 * dropping a portrait arrangement is a change to the shape of the document rather than to one
 * view of it, so that control is written whole.
 */
private fun ControllerLayout.mergedOver(
    saved: ControllerLayout,
    portrait: Boolean,
): ControllerLayout = copy(
    elements = elements.map { element ->
        val was = saved.element(element.id) ?: return@map element
        val separateNow = element.portraitPlacement != null
        if (!separateNow || separateNow != (was.portraitPlacement != null)) {
            element
        } else if (portrait) {
            element.copy(placement = was.placement, shape = was.shape)
        } else {
            element.copy(
                portraitPlacement = was.portraitPlacement,
                portraitShape = was.portraitShape,
            )
        }
    }
)

// --- the tools -----------------------------------------------------------------------------------


/** The selected control in both units at once, which is the only way the two read on one scale. */
private fun LayoutElement.summary(device: LayoutSurface, portrait: Boolean): String {
    val unit = device.shortSide
    val p = placementFor(portrait)
    return "$id  x %.2f  y %.2f  w %.2f  h %.2f   (%d × %d px)".format(
        p.offsetX, p.offsetY, p.width, p.height,
        (p.width * unit).roundToInt(), (p.height * unit).roundToInt(),
    )
}



/**
 * What the windows currently are, without offering to change any of them.
 *
 * Changing one happens at the control, by long press. This is the view that cannot be had there:
 * every window at once, and what share of the screen each is taking.
 */
@Composable
private fun WindowSummary(
    layout: ControllerLayout,
    device: LayoutSurface,
    controlScale: Float,
    portrait: Boolean,
    element: LayoutElement?,
) {
    Spacer(modifier = Modifier.height(2.dp))
    Text("Windows", style = MaterialTheme.typography.labelLarge)

    val clusters = layout.clustersOn(device, controlScale, portrait)
    val screen = device.widthPx * device.heightPx
    clusters.forEach { cluster ->
        val share = if (screen <= 0) 0.0 else cluster.bounds.width * cluster.bounds.height / screen
        val mine = element != null && element.id in cluster.elementIds
        Text(
            text = (if (mine) "▸ " else "  ") +
                "%3d%%  %s".format((share * 100).roundToInt(), cluster.elementIds.joinToString(" ")),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (share > CROWDED) Color(0xFFE0603A) else MaterialTheme.colorScheme.onSurface,
        )
    }
    Text(
        text = "Percentages are of the whole screen. Past a quarter, a window is taking away more " +
            "than a pad should. The boxes are drawn on the canvas while you work; long press a " +
            "control to change which one it is in.",
        style = MaterialTheme.typography.bodySmall,
    )
}

/**
 * A grid step said in both units at once, which is the point of the change.
 *
 * `0.04 · 37 px` — the number the file holds, and what it is on this phone. Neither alone was
 * enough: the fraction is what gets written and the pixels are what the eye is measuring.
 */
private fun gridLabel(unit: Double, device: LayoutSurface): String =
    "%.2f · %d px".format(unit, (unit * device.shortSide).roundToInt())

/**
 * The settings that shape what the pad does, where the pad is being arranged.
 *
 * Size, dead zone, curve, sensitivity and inversion were on the diagnostics screen, which is the
 * one place they could not be judged: nothing is being played there and the pad is not on screen.
 * Here they are a slider away from the thing they change.
 *
 * **Size is per orientation** and the others are not, deliberately. A pad's size is a matter of
 * where the thumbs are, which the orientation decides; a dead zone is a matter of the hardware and
 * the hand, which it does not.
 */
@Composable
private fun PadTools(portrait: Boolean, onPersist: () -> Unit) {
    val settings = AppSettings.current.value
    val stick = settings.stickProfile

    fun shape(update: (AnalogProfile) -> AnalogProfile) {
        AppSettings.update { it.copy(stickProfile = update(it.stickProfile)) }
        // The pad on screen is handed the new shaping straight away. Tuning a dead zone and then
        // having to put the controls up again to feel it is tuning by memory.
        val updated = AppSettings.current.value.stickProfile
        SessionState.profile = updated
        // The overlay was handed a profile when it was built and never heard about a change, so
        // every slider moved a number in a file and nothing in the hand. `update` exists precisely
        // for this and was not being called.
        SessionState.overlay?.update(updated)
    }

    Spacer(modifier = Modifier.height(2.dp))
    Text("Pad", style = MaterialTheme.typography.labelLarge)

    val size = (if (portrait) settings.controlScalePortrait else settings.controlScale).toFloat()
    Text(
        text = "size — %s  %.0f%%".format(if (portrait) "portrait" else "landscape", size * 100),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
    )
    Slider(
        value = size,
        onValueChange = { raw ->
            val snapped = Math.round(raw * 100f) / 100f
            AppSettings.update {
                if (portrait) it.copy(controlScalePortrait = snapped.toDouble())
                else it.copy(controlScale = snapped.toDouble())
            }
            SessionState.controlScale.value = snapped
            SessionState.overlay?.resize(snapped)
        },
        onValueChangeFinished = onPersist,
        valueRange = KestrelSettings.MIN_CONTROL_SCALE.toFloat()
            ..KestrelSettings.MAX_CONTROL_SCALE.toFloat(),
    )

    Spacer(modifier = Modifier.height(2.dp))
    Text("Getting out of the way", style = MaterialTheme.typography.labelLarge)
    val idle = settings.idle

    fun setIdle(update: (IdlePreferences) -> IdlePreferences) {
        AppSettings.update { it.copy(idle = update(it.idle)) }
        onPersist()
    }

    IdleSwitch("Fade the controls", idle.controlsEnabled) { on ->
        setIdle { it.copy(controlsEnabled = on) }
    }
    IdleSeconds("controls fade", idle.controlsFadeSeconds, idle.controlsEnabled, onPersist) { v ->
        AppSettings.update { it.copy(idle = it.idle.copy(controlsFadeSeconds = v)) }
    }
    IdleSeconds("then hide after", idle.controlsHideSeconds, idle.controlsEnabled, onPersist) { v ->
        AppSettings.update { it.copy(idle = it.idle.copy(controlsHideSeconds = v)) }
    }

    IdleSwitch("Fade the K button", idle.toggleEnabled) { on ->
        setIdle { it.copy(toggleEnabled = on) }
    }
    IdleSeconds("K button fade", idle.toggleSeconds, idle.toggleEnabled, onPersist) { v ->
        AppSettings.update { it.copy(idle = it.idle.copy(toggleSeconds = v)) }
    }

    Text(
        text = "The controls fade after the first interval and go that long again after fading — " +
            "so fading is always a warning that hiding is coming. The K button has its own " +
            "interval and only ever fades: it is the way out, and a way out that hides itself is " +
            "not one.",
        style = MaterialTheme.typography.bodySmall,
    )

    Spacer(modifier = Modifier.height(2.dp))
    Text("Sticks", style = MaterialTheme.typography.labelLarge)
    ShapedSlider("dead zone", stick.deadzone.toFloat(), 0f..0.5f, onPersist) { v ->
        shape { it.copy(deadzone = v.toDouble()) }
    }
    ShapedSlider("curve", stick.curve.toFloat(), 0.4f..3f, onPersist) { v ->
        shape { it.copy(curve = v.toDouble()) }
    }
    ShapedSlider("sensitivity", stick.sensitivity.toFloat(), 0.5f..2.5f, onPersist) { v ->
        shape { it.copy(sensitivity = v.toDouble()) }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(
            checked = stick.invertX,
            onCheckedChange = { on ->
                shape { it.copy(invertX = on) }
                onPersist()
            },
        )
        Text("invert X", style = MaterialTheme.typography.bodyMedium)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(
            checked = stick.invertY,
            onCheckedChange = { on ->
                shape { it.copy(invertY = on) }
                onPersist()
            },
        )
        Text("invert Y", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun IdleSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun IdleSeconds(
    label: String,
    seconds: Int,
    enabled: Boolean,
    onPersist: () -> Unit,
    onValue: (Int) -> Unit,
) {
    Text(
        text = "%s  %d s".format(label, seconds),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
    )
    Slider(
        enabled = enabled,
        value = seconds.toFloat(),
        onValueChange = { onValue(it.roundToInt()) },
        onValueChangeFinished = onPersist,
        valueRange = IdlePreferences.MIN_SECONDS.toFloat()..IdlePreferences.MAX_SECONDS.toFloat(),
    )
}

@Composable
private fun ShapedSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onPersist: () -> Unit,
    onValue: (Float) -> Unit,
) {
    Text(
        text = "%s  %.2f".format(label, value),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
    )
    Slider(
        // Written as the finger moves and persisted when it lifts: a slider produces a value per
        // frame, and sixty writes for one decision is sixty writes to the user's storage.
        value = value,
        onValueChange = { onValue(Math.round(it * 100f) / 100f) },
        onValueChangeFinished = onPersist,
        valueRange = range,
    )
}

/**
 * Which arrangement is on screen, and how to start the other one.
 *
 * A layout holds two: landscape and portrait. This says which is being edited, whether the other
 * one exists yet, and offers the two things that are hard to do by dragging — start this
 * orientation from a copy of the other, and give it up so it follows the other again.
 *
 * The button turns the phone rather than switching a mode, because the editor's canvas is a picture
 * of the screen it is on and there is no honest way to draw a picture of a screen the phone is not
 * currently showing.
 */
@Composable
private fun OrientationTools(
    portrait: Boolean,
    layout: ControllerLayout,
    onChange: (ControllerLayout) -> Unit,
) {
    val here = if (portrait) "portrait" else "landscape"
    val other = if (portrait) "landscape" else "portrait"
    val separate = layout.elements.any { it.portraitPlacement != null }

    Spacer(modifier = Modifier.height(2.dp))
    Text("Arrangement", style = MaterialTheme.typography.labelLarge)
    Text(
        text = if (separate) {
            "Editing the $here arrangement. The two are kept separately."
        } else {
            "One arrangement for both. Editing it here changes it upright as well, until portrait " +
                "is given one of its own."
        },
        style = MaterialTheme.typography.bodySmall,
    )

    if (portrait && !separate) {
        KButton(
            onClick = {
                onChange(
                    layout.copy(
                        elements = layout.elements.map { it.copy(portraitPlacement = it.placement) }
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Give portrait its own arrangement") }
    }
    if (separate) {
        KOutlinedButton(
            onClick = {
                onChange(
                    layout.copy(elements = layout.elements.map { it.copy(portraitPlacement = null) })
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Drop the portrait arrangement") }
        Text(
            text = "Dropping it makes portrait follow landscape again. What was arranged upright " +
                "is lost when this is saved.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    Text(
        text = "Turn the phone to edit $other. The canvas is a picture of the screen it is on, and " +
            "there is no honest way to draw a screen the phone is not showing.",
        style = MaterialTheme.typography.bodySmall,
    )
}

/** Where a window stops being a pad and starts being a lid. Matches the shipped layout's tests. */
private const val CROWDED = 0.25

@Composable
private fun GridTools(
    gridUnit: Double,
    onGrid: (Double) -> Unit,
    snapToGrid: Boolean,
    onSnapToGrid: (Boolean) -> Unit,
    snapToEdges: Boolean,
    onSnapToEdges: (Boolean) -> Unit,
    device: LayoutSurface,
    wholeScreen: Boolean,
) {
    var open by remember { mutableStateOf(false) }
    Spacer(modifier = Modifier.height(2.dp))
    Text("Grid and snapping", style = MaterialTheme.typography.labelLarge)

    Box {
        KOutlinedButton(onClick = { open = true }) { Text("Grid  " + gridLabel(gridUnit, device)) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            GRID_SIZES.forEach { size ->
                DropdownMenuItem(
                    text = { Text(gridLabel(size, device)) },
                    onClick = {
                        onGrid(size)
                        open = false
                    },
                )
            }
        }
    }

    // Switches, not checkboxes. A checkbox is a form control — a thing ticked as part of an answer
    // being composed. These are on or off and take effect at once, which is a switch.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(checked = snapToGrid, onCheckedChange = onSnapToGrid)
        Text("Snap to the grid", style = MaterialTheme.typography.bodyMedium)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Switch(checked = snapToEdges, onCheckedChange = onSnapToEdges)
        Text("Snap to gamepad edges", style = MaterialTheme.typography.bodyMedium)
    }
    Text(
        text = "Edge snapping lines a control up with the other controls and with the edges of the " +
            "screen, and wins over the grid when both could apply. While a control is being " +
            "dragged, a yellow line shows what it has caught — that is the guide.",
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = "The grid is measured in the same unit as the controls — a fraction of the screen's " +
            "shorter side — so a 0.12 button against a 0.04 grid means what it looks like. A step " +
            "of 0.02 is exactly what the file stores.",
        style = MaterialTheme.typography.bodySmall,
    )

    Spacer(modifier = Modifier.height(2.dp))
    Text(
        text = if (wholeScreen) {
            ("Canvas is %d × %d px — the whole screen, which is what the pad uses. The shaded band " +
                "is where the system bars and the cutout are: a control there is allowed and will " +
                "share that space with the system.")
                .format(device.widthPx.roundToInt(), device.heightPx.roundToInt())
        } else {
            ("Canvas is %d × %d px — the screen less the system bars and the cutout, because " +
                "\"use the notch area\" is off in settings.")
                .format(device.widthPx.roundToInt(), device.heightPx.roundToInt())
        },
        style = MaterialTheme.typography.bodySmall,
    )
}

/**
 * The four numbers, typed rather than dragged.
 *
 * Dragging is for arranging; typing is for the moment somebody already knows the number they want.
 * The units are stated here because they were reported as confusing, and they are not obvious:
 * an offset runs from the anchor to the control's **centre**, and every one of the four is a
 * fraction of the screen's **shorter side**.
 */
@Composable
private fun NumbersDialog(
    element: LayoutElement,
    device: LayoutSurface,
    portrait: Boolean,
    onDismiss: () -> Unit,
    onApply: (LayoutElement) -> Unit,
) {
    // The arrangement for the orientation on screen. Reading and writing `placement` regardless was
    // a fault with the worst possible shape: dragging edited the right one and typing edited the
    // other, so the same screen did two different things depending on how you asked.
    val current = element.placementFor(portrait)
    var offsetX by remember { mutableStateOf("%.2f".format(current.offsetX)) }
    var offsetY by remember { mutableStateOf("%.2f".format(current.offsetY)) }
    var width by remember { mutableStateOf("%.2f".format(current.width)) }
    var height by remember { mutableStateOf("%.2f".format(current.height)) }
    var problem by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(element.id) },
        text = {
            // Two to a row and the body scrolls. Four fields stacked in a dialog on a landscape
            // phone put width and height below the fold with no way to reach them, which is a
            // feature that works and cannot be used.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Offsets run from the ${current.anchor.wireName} anchor to " +
                        "the control's centre, inwards. All four are fractions of the screen's " +
                        "shorter side.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField("offsetX", offsetX, Modifier.weight(1f)) { offsetX = it }
                    NumberField("offsetY", offsetY, Modifier.weight(1f)) { offsetY = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField("width", width, Modifier.weight(1f)) { width = it }
                    NumberField("height", height, Modifier.weight(1f)) { height = it }
                }
                // A numeric keyboard does not always offer a minus sign, and an offset is allowed
                // to be negative. Reported on the reference device as "the keyboard only shows
                // numbers", with pasting as the only way round it.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KOutlinedButton(onClick = { offsetX = negated(offsetX) }) { Text("± offsetX") }
                    KOutlinedButton(onClick = { offsetY = negated(offsetY) }) { Text("± offsetY") }
                }
                if (problem.isNotBlank()) {
                    Text(
                        text = problem,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE0603A),
                    )
                }
            }
        },
        confirmButton = {
            KButton(onClick = {
                val numbers = listOf(offsetX, offsetY, width, height).map { it.trim().toDoubleOrNull() }
                if (numbers.any { it == null }) {
                    problem = "Every field has to be a number."
                    return@KButton
                }
                // The same limits dragging obeys. A dialog that accepted what a drag refuses is two
                // rules for one thing, and the one nobody sees wins — which is how a control 0.9 of
                // the screen wide and half of it off the edge could be typed in.
                if (numbers.take(2).any { it!! < -Placement.MAX_OFFSET || it > Placement.MAX_OFFSET }) {
                    problem = "An offset has to be between -${Placement.MAX_OFFSET} and " +
                        "${Placement.MAX_OFFSET}."
                    return@KButton
                }
                if (numbers.drop(2).any { it!! < EDITOR_MIN_SIZE || it > EDITOR_MAX_SIZE }) {
                    problem = "A size has to be between $EDITOR_MIN_SIZE and $EDITOR_MAX_SIZE " +
                        "of the screen's shorter side."
                    return@KButton
                }
                val wanted = Placement(
                    anchor = current.anchor,
                    offsetX = round(numbers[0]!!),
                    offsetY = round(numbers[1]!!),
                    width = round(numbers[2]!!),
                    height = round(numbers[3]!!),
                    rotationDegrees = current.rotationDegrees,
                )
                val shape = element.effectiveShapeFor(portrait)
                if (!wanted.resolve(device).shapedAs(shape).isWithin(device)) {
                    // The size check names the two numbers it will accept. This one used to say
                    // only that the control was off the screen, which tells the user they are
                    // wrong without telling them what right looks like — the project owner asked
                    // for the same treatment here.
                    problem = "That puts the control off the screen. At this size, from the " +
                        "${wanted.anchor.wireName} anchor, " +
                        offsetLimit(wanted, device, shape, horizontal = true) + " and " +
                        offsetLimit(wanted, device, shape, horizontal = false) + "."
                    return@KButton
                }
                val candidate = Placement.of(
                    anchor = wanted.anchor,
                    offsetX = wanted.offsetX,
                    offsetY = wanted.offsetY,
                    width = wanted.width,
                    height = wanted.height,
                    rotationDegrees = wanted.rotationDegrees,
                )
                when (candidate) {
                    is Outcome.Failure -> problem = candidate.error.message
                    is Outcome.Success ->
                        onApply(element.withPlacementFor(portrait, candidate.value))
                }
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValue: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

/** Flips the sign of what is typed, including the half-typed and the nonsense. */
private fun negated(text: String): String {
    val trimmed = text.trim()
    return when {
        trimmed.startsWith("-") -> trimmed.removePrefix("-")
        trimmed.isEmpty() -> "-"
        else -> "-$trimmed"
    }
}

// --- editing a control ---------------------------------------------------------------------------

private fun LayoutElement.resizedBy(delta: Double, portrait: Boolean): LayoutElement {
    val current = placementFor(portrait)
    val next = (current.width + delta).coerceIn(EDITOR_MIN_SIZE, EDITOR_MAX_SIZE)
    val ratio = if (current.width == 0.0) 1.0 else next / current.width
    return withPlacementFor(
        portrait,
        current.copy(
            width = round(next),
            height = round((current.height * ratio).coerceIn(EDITOR_MIN_SIZE, EDITOR_MAX_SIZE)),
        ),
    )
}

private fun LayoutElement.taller(delta: Double, portrait: Boolean): LayoutElement =
    placementFor(portrait).let { current ->
        withPlacementFor(
            portrait,
            current.copy(
                height = round(
                    (current.height + delta).coerceIn(EDITOR_MIN_SIZE, EDITOR_MAX_SIZE)
                ),
            ),
        )
    }


/**
 * Moves to the next anchor **without moving the control**.
 *
 * Asked for by name in the project owner's proposal: *"If a user changes a button's anchor … the
 * button stays in the exact same spot on the screen without jumping."* It did jump — the anchor
 * changed and the offsets were kept, so a control pinned bottom-left with 0.2 and 0.2 became a
 * control pinned top-right with 0.2 and 0.2, which is the opposite corner.
 *
 * The position is preserved **in the document's own terms**, at full size, rather than at whatever
 * the size setting happens to be. An offset scales and an anchor does not, so "the same spot" is a
 * different pair of numbers at every size; the document is the thing being edited, and at the
 * default size — which is now full size — the two answers are the same one.
 */
private fun LayoutElement.withNextAnchor(portrait: Boolean, surface: LayoutSurface): LayoutElement {
    // Only the corners and edges a control is ever pinned to. The centre is excluded on purpose:
    // a control anchored to the middle of the screen is one no thumb can reach while holding a
    // phone, and offering it here would be offering a mistake.
    val order = listOf(
        Anchor.BOTTOM_LEFT, Anchor.BOTTOM_RIGHT, Anchor.TOP_LEFT, Anchor.TOP_RIGHT,
        Anchor.BOTTOM_CENTER, Anchor.TOP_CENTER, Anchor.CENTER_LEFT, Anchor.CENTER_RIGHT,
    )
    val current = placementFor(portrait)
    val next = order[(order.indexOf(current.anchor).coerceAtLeast(0) + 1) % order.size]
    val here = current.resolve(surface)
    return withPlacementFor(
        portrait,
        current.copy(anchor = next).centeredAt(surface, here.centerX, here.centerY).rounded(),
    )
}

/**
 * Every window a control could be moved into: its own, one that already exists, or a new one.
 *
 * A new name is offered rather than typed. Group names follow the same rules as element ids, and a
 * keyboard is a way to break that rule on a screen where the only thing the name has to do is be
 * different from the others.
 */
private fun ControllerLayout.windowOptions(): List<String?> {
    val existing = elements.mapNotNull { it.group }.distinct().sorted()
    var n = 1
    while ("group-$n" in existing) n += 1
    return listOf(null) + existing + listOf("group-$n")
}

private fun LayoutElement.withGroupStep(options: List<String?>, step: Int): LayoutElement {
    if (options.isEmpty()) return this
    val at = options.indexOf(group).let { if (it < 0) 0 else it }
    val next = ((at + step) % options.size + options.size) % options.size
    return copy(group = options[next])
}

/**
 * Puts the named controls back where the shipped layout has them, and changes nothing else.
 *
 * **Position only** — not size, not shape, not which window it is in. A control that has been
 * dragged off the screen needs rescuing, and rescuing it by resetting everything about it would
 * throw away work that was never the problem.
 *
 * The built-in is the source of the answer because it is the only arrangement Kestrel can be sure
 * is on the screen: `BuiltInLayoutsTest` checks exactly that, at every size and in both
 * orientations. A control the built-in does not have keeps its anchor and comes in to a sensible
 * distance from it, which is the best that can be said without inventing a position.
 */
private fun ControllerLayout.withStraysBroughtBack(
    ids: List<String>,
    portrait: Boolean,
): ControllerLayout {
    val shipped = when (val outcome = BuiltInLayouts.load(BuiltInLayouts.XBOX_DEFAULT)) {
        is Outcome.Failure -> null
        is Outcome.Success -> outcome.value
    }
    return copy(
        elements = elements.map { element ->
            if (element.id !in ids) return@map element
            val home = shipped?.element(element.id)?.placement
            val current = element.placementFor(portrait)
            val rescued = if (home != null) {
                current.copy(anchor = home.anchor, offsetX = home.offsetX, offsetY = home.offsetY)
            } else {
                current.copy(
                    offsetX = current.width.coerceAtLeast(RESCUE_OFFSET),
                    offsetY = current.height.coerceAtLeast(RESCUE_OFFSET),
                )
            }
            element.withPlacementFor(portrait, rescued)
        }
    )
}

/** Far enough from an edge that a whole control fits, for a control the built-in never had. */
private const val RESCUE_OFFSET = 0.20

private fun ControllerLayout.replacing(element: LayoutElement): ControllerLayout =
    copy(elements = elements.map { if (it.id == element.id) element else it })

private fun ControllerLayout.clustersOn(
    surface: LayoutSurface,
    scale: Float,
    portrait: Boolean,
): List<Cluster> = Clustering.group(
    this,
    elements.map {
        it.id to it.placementFor(portrait).scaledBy(scale.toDouble()).resolve(surface)
            .shapedAs(it.effectiveShapeFor(portrait))
    },
)

/** Two decimals, the same as the file gets, so what is on screen is what will be written. */
private fun round(value: Double): Double = Math.round(value * 100.0) / 100.0

/**
 * What one offset is allowed to be, in words, if the control is to stay on the screen at the size
 * that was typed.
 *
 * Scanned rather than solved. The offset-to-pixels relation depends on the anchor and on the
 * shape's own squaring-up, and a formula that has to agree with `resolve` and `shapedAs` is a
 * second copy of both that will drift from them. It runs only after a value has been refused, so
 * eight hundred cheap probes cost nothing anybody can feel.
 *
 * The two axes are independent: `isWithin` checks left/right against one pair of edges and
 * top/bottom against the other, so a bad offsetY cannot make the offsetX range come out wrong.
 */
private fun offsetLimit(
    wanted: Placement,
    device: LayoutSurface,
    shape: ControlShape,
    horizontal: Boolean,
): String {
    val name = if (horizontal) "offsetX" else "offsetY"
    var low: Double? = null
    var high: Double? = null
    var steps = 0
    val total = (2 * Placement.MAX_OFFSET / 0.01).toInt()
    while (steps <= total) {
        val value = round(-Placement.MAX_OFFSET + steps * 0.01)
        val rect = (
            if (horizontal) wanted.copy(offsetX = value) else wanted.copy(offsetY = value)
            ).resolve(device).shapedAs(shape)
        val fits = if (horizontal) {
            rect.left >= device.insetLeft && rect.right <= device.insetLeft + device.usableWidth
        } else {
            rect.top >= device.insetTop && rect.bottom <= device.insetTop + device.usableHeight
        }
        if (fits) {
            if (low == null) low = value
            high = value
        }
        steps++
    }
    return if (low == null || high == null) {
        "$name has no value that fits — the control is too big for the screen this way"
    } else {
        "$name has to be between $low and $high"
    }
}

// --- the canvas ----------------------------------------------------------------------------------

/**
 * Where the device rectangle sits inside the dock, and how far it is scaled down to get there.
 *
 * [surface] is the **whole screen** at canvas scale, insets included, so a control resolves into the
 * usable part of it exactly as it does on the phone. [usable] is that inner rectangle, in canvas
 * coordinates, which is what the grid is drawn over and what a control is checked against.
 */
private data class Fit(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val scale: Float,
    val surface: LayoutSurface,
    /** Where the system bars and the cutout are, in canvas coordinates, or null when there is no
     *  band to draw. Drawn and never subtracted: a control there is allowed, and shares that space
     *  with the system rather than being pushed out of it. */
    val bars: Rect?,
)

/** A line a control snapped to, drawn only while it is holding. */
private data class Guide(val vertical: Boolean, val at: Float)

/**
 * The layout, drawn inside a rectangle shaped like the phone.
 *
 * Deliberately not the overlay's own renderer. The overlay draws into windows it owns, positioned
 * by the window manager; this draws into a rectangle inside an ordinary screen. They agree on the
 * part that has to match — the same `Placement`, the same `resolve`, the same shape rules — and
 * differ on everything a preview does not need.
 */
@Composable
private fun EditorCanvas(
    modifier: Modifier,
    device: LayoutSurface,
    bars: Rect?,
    controlScale: Float,
    portrait: Boolean,
    layout: ControllerLayout,
    selectedId: String?,
    /** Controls sitting on top of another, drawn so that is visible rather than discovered. */
    marked: Set<String>,
    /** While a menu is open, everything but this control is darkened. */
    dimExcept: String?,
    gridUnit: Double,
    snapToGrid: Boolean,
    snapToEdges: Boolean,
    onSelect: (String?) -> Unit,
    onLongPress: (String, Offset) -> Unit,
    onPlace: (LayoutElement) -> Unit,
) {
    // Read through these inside the gesture handlers rather than capturing them. A pointerInput
    // keyed on anything that changes during a drag restarts mid-gesture, which cancels the drag —
    // so the keys stay still and the values are looked up fresh.
    val liveLayout by rememberUpdatedState(layout)
    val liveGrid by rememberUpdatedState(gridUnit)
    val liveSnapGrid by rememberUpdatedState(snapToGrid)
    val liveSnapEdges by rememberUpdatedState(snapToEdges)
    val liveScale by rememberUpdatedState(controlScale)
    val livePortrait by rememberUpdatedState(portrait)
    val liveSelect by rememberUpdatedState(onSelect)
    val liveLongPress by rememberUpdatedState(onLongPress)
    val livePlace by rememberUpdatedState(onPlace)

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var grab by remember { mutableStateOf(Offset.Zero) }
    var guides by remember { mutableStateOf<List<Guide>>(emptyList()) }

    fun fit(): Fit {
        val bounds = canvasSize
        val empty = LayoutSurface(0.0, 0.0)
        if (bounds.width <= 0f || bounds.height <= 0f || device.widthPx <= 0 || device.heightPx <= 0) {
            return Fit(0f, 0f, 0f, 0f, 1f, empty, null)
        }
        // No margin. The canvas has the screen to itself and the same shape as the screen, so a
        // margin only makes the picture smaller than the thing it is a picture of. Previewing the
        // orientation the phone is in, this comes out at exactly 1 : 1.
        val scale = min(
            bounds.width / device.widthPx.toFloat(),
            bounds.height / device.heightPx.toFloat(),
        )
        val width = device.widthPx.toFloat() * scale
        val height = device.heightPx.toFloat() * scale
        return Fit(
            left = (bounds.width - width) / 2,
            top = (bounds.height - height) / 2,
            width = width,
            height = height,
            scale = scale,
            surface = LayoutSurface(width.toDouble(), height.toDouble()),
            bars = bars?.let {
                Rect(it.left * scale, it.top * scale, it.right * scale, it.bottom * scale)
            },
        )
    }

    fun rectOf(fit: Fit, element: LayoutElement): PixelRect =
        element.placementFor(livePortrait).scaledBy(liveScale.toDouble()).resolve(fit.surface)
            .shapedAs(element.effectiveShapeFor(livePortrait))

    fun hit(fit: Fit, at: Offset): String? {
        val x = (at.x - fit.left).toDouble()
        val y = (at.y - fit.top).toDouble()
        // Last first, so the control drawn on top is the one selected.
        return liveLayout.elements.reversed().firstOrNull { element ->
            val rect = rectOf(fit, element)
            when (element.effectiveShapeFor(livePortrait)) {
                ControlShape.CIRCLE ->
                    hypot(x - rect.centerX, y - rect.centerY) <= min(rect.width, rect.height) / 2
                else -> abs(x - rect.centerX) <= rect.width / 2 &&
                    abs(y - rect.centerY) <= rect.height / 2
            }
        }?.id
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(layout.header.id, device, bars) {
                detectTapGestures(
                    onTap = { at -> liveSelect(hit(fit(), at)) },
                    onLongPress = { at ->
                        hit(fit(), at)?.let { id -> liveLongPress(id, at) }
                    },
                )
            }
            .pointerInput(layout.header.id, device, bars) {
                detectDragGestures(
                    onDragStart = { at ->
                        val fitted = fit()
                        val id = hit(fitted, at)
                        dragging = id
                        liveSelect(id)
                        val element = id?.let { liveLayout.element(it) }
                        grab = if (element == null) {
                            Offset.Zero
                        } else {
                            val rect = rectOf(fitted, element)
                            Offset(
                                at.x - fitted.left - rect.centerX.toFloat(),
                                at.y - fitted.top - rect.centerY.toFloat(),
                            )
                        }
                    },
                    onDragEnd = {
                        dragging = null
                        guides = emptyList()
                    },
                    onDragCancel = {
                        dragging = null
                        guides = emptyList()
                    },
                ) { change, _ ->
                    change.consume()
                    val id = dragging ?: return@detectDragGestures
                    val element = liveLayout.element(id) ?: return@detectDragGestures
                    val fitted = fit()
                    if (fitted.width <= 0f) return@detectDragGestures

                    val wanted = Offset(
                        change.position.x - fitted.left - grab.x,
                        change.position.y - fitted.top - grab.y,
                    )
                    val bounds = element.placementFor(livePortrait)
                        .scaledBy(liveScale.toDouble())
                        .resolve(fitted.surface)
                        .shapedAs(element.effectiveShapeFor(livePortrait))
                    val snapped = snap(
                        layout = liveLayout,
                        fit = fitted,
                        scale = liveScale,
                        portrait = livePortrait,
                        element = element,
                        wanted = wanted,
                        gridUnit = liveGrid,
                        toGrid = liveSnapGrid,
                        toEdges = liveSnapEdges,
                    )
                    guides = snapped.guides
                    // Kept on the screen. It used to be allowed off, with a warning and a button to
                    // bring it back — which is a fault offered, reported and then undone rather
                    // than not made. The project owner asked for the simpler contract: a control
                    // cannot be dragged off the screen.
                    val halfW = bounds.width / 2
                    val halfH = bounds.height / 2
                    val onScreen = Snapped(
                        snapped.x.coerceIn(halfW, fitted.surface.widthPx - halfW),
                        snapped.y.coerceIn(halfH, fitted.surface.heightPx - halfH),
                        snapped.guides,
                    )
                    // Placed as the pad shows it, written as the document holds it. The setting is
                    // applied on top of the file and editing must not fold it into the file.
                    val scale = liveScale.toDouble()
                    val current = element.placementFor(livePortrait)
                    val shown = current.scaledBy(scale)
                        .centeredAt(fitted.surface, onScreen.x, onScreen.y)
                    livePlace(
                        element.withPlacementFor(
                            livePortrait,
                            current.copy(
                                offsetX = shown.offsetX / scale,
                                offsetY = shown.offsetY / scale,
                            ).rounded(),
                        )
                    )
                }
            },
    ) {
        val fitted = fit()
        if (fitted.width <= 0f) return@Canvas
        drawScreen(fitted)
        drawGrid(fitted, gridUnit)
        drawThirds(fitted, gridUnit)
        // The region the selected control's anchor belongs to, lit under everything. A dot at a
        // corner is a dot on the part of the glass most phones round off — this says the same thing
        // with a shape big enough that no corner radius can hide it.
        layout.element(selectedId ?: "")?.let { chosen ->
            drawAnchorRegion(fitted, chosen.placementFor(livePortrait).anchor, gridUnit)
        }

        val placed = layout.elements.map { it.id to rectOf(fitted, it) }
        @Suppress("UNUSED_EXPRESSION") controlScale
        // Always, and behind everything. A window was a mode you had to be in to see, so the way to
        // find out that dragging a control across the screen had turned its window into a lid was
        // to go looking. Drawn faintly under the pad, it is simply visible while it happens.
        drawWindows(fitted, Clustering.group(layout, placed), selectedId)
        layout.elements.forEachIndexed { index, element ->
            drawControl(
                fitted, element, livePortrait, placed[index].second,
                selected = element.id == selectedId,
                marked = element.id in marked,
            )
        }
        // The subject of the menu is the only thing left lit. Drawn here rather than as a scrim
        // behind the menu, because only the canvas knows where the control is.
        if (dimExcept != null) {
            drawRect(
                color = Color(0xFF05070A).copy(alpha = 0.82f),
                topLeft = Offset(fitted.left, fitted.top),
                size = Size(fitted.width, fitted.height),
            )
            val index = layout.elements.indexOfFirst { it.id == dimExcept }
            if (index >= 0) {
                drawControl(
                    fitted, layout.elements[index], livePortrait, placed[index].second,
                    selected = true, marked = false,
                )
            }
        }
        // The anchor a selected control is measured from. An offset is a distance from a point, and
        // until now that point was named in words and shown nowhere.
        layout.element(selectedId ?: "")?.let { chosen ->
            drawAnchor(fitted, chosen.placementFor(livePortrait).anchor)
        }
        guides.forEach { guide -> drawGuide(fitted, guide) }
    }
}

/** The rounding the file gets, applied to a placement rather than to one number at a time. */
private fun Placement.rounded(): Placement = copy(
    offsetX = round(offsetX),
    offsetY = round(offsetY),
    width = round(width),
    height = round(height),
)

// --- snapping ------------------------------------------------------------------------------------

private data class Snapped(val x: Double, val y: Double, val guides: List<Guide>)

/**
 * Where a dragged control actually lands.
 *
 * Two aids, and a rule for when they disagree: **edge snapping wins over the grid**. Lining a
 * control up with the one next to it is a statement about this layout; landing on a grid line is a
 * statement about the screen, and the first is what somebody dragging a control is usually after.
 * Applied per axis, so a control can line up with a neighbour horizontally and sit on the grid
 * vertically.
 *
 * Snapping is done in the canvas's own pixels, which are the phone's pixels scaled by the same
 * factor the whole drawing is — so a 64px grid is 64 phone pixels, not 64 of the editor's.
 */
private fun snap(
    layout: ControllerLayout,
    fit: Fit,
    scale: Float,
    portrait: Boolean,
    element: LayoutElement,
    wanted: Offset,
    gridUnit: Double,
    toGrid: Boolean,
    toEdges: Boolean,
): Snapped {
    val rect = element.placementFor(portrait).scaledBy(scale.toDouble()).resolve(fit.surface)
        .shapedAs(element.effectiveShapeFor(portrait))
    val threshold = max(6.0, min(fit.width, fit.height) * 0.02)
    val step = gridUnit * fit.surface.shortSide

    val others = layout.elements
        .filter { it.id != element.id }
        .map {
            it.placementFor(portrait).scaledBy(scale.toDouble()).resolve(fit.surface)
                .shapedAs(it.effectiveShapeFor(portrait))
        }

    val verticalLines = buildList {
        add(0.0)
        add(fit.width.toDouble() / 2)
        add(fit.width.toDouble())
        others.forEach {
            add(it.left)
            add(it.centerX)
            add(it.right)
        }
    }
    val horizontalLines = buildList {
        add(0.0)
        add(fit.height.toDouble() / 2)
        add(fit.height.toDouble())
        others.forEach {
            add(it.top)
            add(it.centerY)
            add(it.bottom)
        }
    }

    val guides = mutableListOf<Guide>()
    val x = snapAxis(
        wanted.x.toDouble(), rect.width / 2, verticalLines, threshold, toEdges,
        if (toGrid) step else 0.0,
    ) { at -> guides += Guide(vertical = true, at = at.toFloat()) }
    val y = snapAxis(
        wanted.y.toDouble(), rect.height / 2, horizontalLines, threshold, toEdges,
        if (toGrid) step else 0.0,
    ) { at -> guides += Guide(vertical = false, at = at.toFloat()) }

    return Snapped(x, y, guides)
}

private inline fun snapAxis(
    wanted: Double,
    half: Double,
    lines: List<Double>,
    threshold: Double,
    toEdges: Boolean,
    gridStep: Double,
    onGuide: (Double) -> Unit,
): Double {
    if (toEdges) {
        // Any of the control's own three lines may be what lines up: its two edges or its middle.
        var bestShift = 0.0
        var bestDistance = Double.MAX_VALUE
        var bestLine = 0.0
        listOf(-half, 0.0, half).forEach { own ->
            lines.forEach { line ->
                val shift = line - (wanted + own)
                val distance = abs(shift)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestShift = shift
                    bestLine = line
                }
            }
        }
        if (bestDistance <= threshold) {
            onGuide(bestLine)
            return wanted + bestShift
        }
    }
    if (gridStep > 0.0) {
        return Math.round(wanted / gridStep) * gridStep
    }
    return wanted
}

// --- drawing -------------------------------------------------------------------------------------

/**
 * The phone: the whole screen, with what the system takes drawn as a band rather than cut off.
 *
 * Subtracting the bands instead was the previous version's fault. It gave a rectangle 2289 × 927 on
 * a 2400 × 1080 phone — a different shape, presented as the phone — and a control placed against the
 * top of the usable area appeared to hang over the edge of the world when in fact it was sitting
 * against the status bar.
 */
private fun DrawScope.drawScreen(fit: Fit) {
    drawRect(
        color = Color(0xFF0B0D11),
        topLeft = Offset(fit.left, fit.top),
        size = Size(fit.width, fit.height),
    )
    // The band is drawn and not subtracted. The pad uses the whole display, so a control here is
    // where the layout says it is — it simply shares that strip with the status bar or the gesture
    // bar, and somebody arranging a pad should be able to see that while deciding.
    fit.bars?.let { inner ->
        val band = Color(0xFF1B2028)
        drawRect(band, Offset(fit.left, fit.top), Size(fit.width, inner.top))
        drawRect(
            band,
            Offset(fit.left, fit.top + inner.bottom),
            Size(fit.width, fit.height - inner.bottom),
        )
        drawRect(band, Offset(fit.left, fit.top + inner.top), Size(inner.left, inner.height))
        drawRect(
            band,
            Offset(fit.left + inner.right, fit.top + inner.top),
            Size(fit.width - inner.right, inner.height),
        )
        drawRect(
            color = Color(0xFF3C444F),
            topLeft = Offset(fit.left + inner.left, fit.top + inner.top),
            size = Size(inner.width, inner.height),
            style = Stroke(width = 1.5f),
        )
    }
    // No border. It existed to say where the picture of the phone ended, and the picture is the
    // whole screen at 1 : 1 now — the only thing left for it to mark is the edge of the screen,
    // which the screen marks by being the edge of the screen.
}

private fun DrawScope.drawGrid(fit: Fit, gridUnit: Double) {
    val step = (gridUnit * fit.surface.shortSide).toFloat()
    if (step < 6f) return
    val colour = Color(0xFF2A3038)
    var x = step
    while (x < fit.width) {
        drawLine(colour, Offset(fit.left + x, fit.top), Offset(fit.left + x, fit.top + fit.height), 1f)
        x += step
    }
    var y = step
    while (y < fit.height) {
        drawLine(colour, Offset(fit.left, fit.top + y), Offset(fit.left + fit.width, fit.top + y), 1f)
        y += step
    }
}

/**
 * The screen in nine, drawn brighter than the grid.
 *
 * A layout is talked about in these terms — top-left, bottom-middle — and the fine grid is for
 * placing a control rather than for saying where on the screen it is. Two lines each way is the
 * whole of it: enough to read a position at a glance, few enough not to become another grid.
 */
private fun DrawScope.drawThirds(fit: Fit, gridUnit: Double) {
    val colour = Color(0xFF5A6472)
    val step = (gridUnit * fit.surface.shortSide).toFloat()

    // Snapped to the nearest grid line rather than drawn wherever a third falls. Two sets of lines
    // that nearly line up read as a mistake — and a third of a screen is not a measurement anybody
    // needs to the pixel, while "is this control on a line" is a question asked constantly.
    fun snap(value: Float): Float =
        if (step <= 1f) value else Math.round(value / step) * step

    listOf(1f / 3f, 2f / 3f).forEach { at ->
        val x = fit.left + snap(fit.width * at)
        drawLine(colour, Offset(x, fit.top), Offset(x, fit.top + fit.height), 2f)
        val y = fit.top + snap(fit.height * at)
        drawLine(colour, Offset(fit.left, y), Offset(fit.left + fit.width, y), 2f)
    }
}

/**
 * The ninth of the screen the selected control is anchored into.
 *
 * Very light, and drawn **before** the controls so it is under them rather than over them — this
 * says where a control is measured from, and a hint that obscures the thing it is about is worse
 * than no hint.
 */
private fun DrawScope.drawAnchorRegion(fit: Fit, anchor: Anchor, gridUnit: Double) {
    val third = 1f / 3f
    val step = (gridUnit * fit.surface.shortSide).toFloat()

    // Snapped to the same grid lines the dividers are, so the lit region ends exactly where the
    // line that marks it is drawn. Two things describing the same ninth of the screen and
    // disagreeing by a few pixels is worse than either alone.
    fun snapX(v: Float) = if (step <= 1f) v else Math.round(v / step) * step
    fun snapY(v: Float) = if (step <= 1f) v else Math.round(v / step) * step
    val column = when (anchor.originX) {
        0.0 -> 0
        1.0 -> 2
        else -> 1
    }
    val row = when (anchor.originY) {
        0.0 -> 0
        1.0 -> 2
        else -> 1
    }
    val x0 = snapX(fit.width * third * column)
    val x1 = if (column == 2) fit.width else snapX(fit.width * third * (column + 1))
    val y0 = snapY(fit.height * third * row)
    val y1 = if (row == 2) fit.height else snapY(fit.height * third * (row + 1))
    drawRect(
        color = Color(0xFF60BAFF).copy(alpha = 0.16f),
        topLeft = Offset(fit.left + x0, fit.top + y0),
        size = Size(x1 - x0, y1 - y0),
    )
}

/** Where the selected control's offsets are measured from, as a dot you can see. */
private fun DrawScope.drawAnchor(fit: Fit, anchor: Anchor) {
    // Pulled in from the edge by its own size. A corner anchor sits exactly at the corner of the
    // display, and almost every phone rounds that corner off — so four of the nine dots were being
    // drawn on glass that is not there. Inset, the dot is still unmistakably at its corner and is
    // always visible.
    // Far enough in that a rounded corner cannot swallow it. 16px was not: on this phone that is
    // about six of the density-independent pixels a corner radius is measured in.
    val inset = 34f
    val x = (fit.left + fit.width * anchor.originX.toFloat())
        .coerceIn(fit.left + inset, fit.left + fit.width - inset)
    val y = (fit.top + fit.height * anchor.originY.toFloat())
        .coerceIn(fit.top + inset, fit.top + fit.height - inset)
    val red = Color(0xFFE03A3A)
    drawCircle(red, radius = 9f, center = Offset(x, y))
    drawCircle(Color(0xFF0B0D11), radius = 9f, center = Offset(x, y), style = Stroke(width = 2f))
}

private fun DrawScope.drawGuide(fit: Fit, guide: Guide) {
    val colour = Color(0xFFF2B441)
    if (guide.vertical) {
        val x = fit.left + guide.at
        drawLine(colour, Offset(x, fit.top), Offset(x, fit.top + fit.height), 2f)
    } else {
        val y = fit.top + guide.at
        drawLine(colour, Offset(fit.left, y), Offset(fit.left + fit.width, y), 2f)
    }
}

private fun DrawScope.drawWindows(fit: Fit, clusters: List<Cluster>, selectedId: String?) {
    clusters.forEach { cluster ->
        val mine = selectedId != null && selectedId in cluster.elementIds
        val bounds = cluster.bounds
        val crowded = bounds.width * bounds.height >
            fit.width.toDouble() * fit.height.toDouble() * CROWDED
        val colour = when {
            mine -> Color(0xFF60BAFF)
            crowded -> Color(0xFFE0603A)
            else -> Color(0xFF8A93A0)
        }
        drawRect(
            color = colour.copy(alpha = if (mine) 0.16f else 0.08f),
            topLeft = Offset(fit.left + bounds.left.toFloat(), fit.top + bounds.top.toFloat()),
            size = Size(bounds.width.toFloat(), bounds.height.toFloat()),
        )
        drawRect(
            color = colour,
            topLeft = Offset(fit.left + bounds.left.toFloat(), fit.top + bounds.top.toFloat()),
            size = Size(bounds.width.toFloat(), bounds.height.toFloat()),
            style = Stroke(width = if (mine) 4f else 2f),
        )
    }
}

private fun DrawScope.drawControl(
    fit: Fit,
    element: LayoutElement,
    portrait: Boolean,
    rect: PixelRect,
    selected: Boolean,
    marked: Boolean,
) {
    // A control that has left the screen is marked rather than moved. It is a real design to
    // run a shoulder button off an edge, and `ADR-007`'s spirit applies: say what is true, do not
    // overrule the person. What is *not* acceptable is letting it look fine here and then arrive
    // somewhere else on the phone, which is what the window manager will do with it.
    // Measured against the surface the pad itself uses, so what is flagged is genuinely off the
    // display rather than merely under a status bar.
    val outside = !rect.isWithin(fit.surface)
    val fill = Color(0xFF5C626C).copy(alpha = 0.80f)
    val edge = when {
        selected -> Color(0xFF60BAFF)
        outside -> Color(0xFFE0603A)
        marked -> Color(0xFFF2B441)
        else -> Color(0xFF0C0E12).copy(alpha = 0.60f)
    }
    val stroke = if (selected || outside || marked) 6f else 3f
    val centre = Offset(fit.left + rect.centerX.toFloat(), fit.top + rect.centerY.toFloat())

    when (element.effectiveShapeFor(portrait)) {
        ControlShape.CIRCLE -> {
            val radius = (min(rect.width, rect.height) / 2).toFloat()
            drawCircle(fill, radius, centre)
            drawCircle(edge, radius, centre, style = Stroke(width = stroke))
        }

        else -> {
            val size = Size(rect.width.toFloat(), rect.height.toFloat())
            val corner = CornerRadius(min(size.width, size.height) * 0.18f)
            val topLeft = Offset(centre.x - size.width / 2, centre.y - size.height / 2)
            drawRoundRect(fill, topLeft, size, corner)
            drawRoundRect(edge, topLeft, size, corner, style = Stroke(width = stroke))
        }
    }

    // A cross for the pad, so its shape is recognisable at a glance rather than another circle.
    if (element.kind == ControlKind.DPAD) {
        val arm = (min(rect.width, rect.height) / 2 * 0.9).toFloat()
        val half = arm * 0.33f
        drawRoundRect(
            color = Color(0xFF9AA1AC),
            topLeft = Offset(centre.x - half, centre.y - arm),
            size = Size(half * 2, arm * 2),
            cornerRadius = CornerRadius(half * 0.4f),
        )
        drawRoundRect(
            color = Color(0xFF9AA1AC),
            topLeft = Offset(centre.x - arm, centre.y - half),
            size = Size(arm * 2, half * 2),
            cornerRadius = CornerRadius(half * 0.4f),
        )
    }
}

/**
 * Opens a layout for editing, duplicating it first when it is one Kestrel ships.
 *
 * The duplication is done rather than demanded. A user who presses Edit on the built-in wants to
 * change how their pad looks, not to learn why they cannot — and the rule that a built-in is
 * immutable is kept exactly as strictly either way.
 */
public fun openForEditing(
    layout: ControllerLayout,
    duplicate: (ControllerLayout) -> Outcome<ControllerLayout>,
): Outcome<ControllerLayout> =
    if (layout.header.id.isBuiltIn) duplicate(layout) else Outcome.Success(layout)
