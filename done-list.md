# Kestrel — Done List

**Document:** `done-list.md`  
**Status:** Active — the record of work that is finished, and what finished means for each item  
**Companion to:** `todo-list.md`, which holds everything not yet finished  

---

## How to read this

`todo-list.md` is the queue. This is the receipt.

An item moves here when it reaches the phase `done` — which means **confirmed on the reference
device by the project owner**, not "written and it compiles". Until then it stays in the queue at
`pending`, `building` or `testing`.

Each entry says four things, in this order:

1. **What was asked for**, in the words it was asked in where they exist.
2. **What was actually built** — enough that somebody reading it a year later knows what to look for
   in the code.
3. **How it is known to work** — Measured, Reported, Reasoned or Unverified, the same vocabulary
   `todo-list.md` uses.
4. **What it cost**, where it cost something: a limit, an assumption, a thing deliberately not done.

**Nothing is written here on the strength of a build succeeding.** A compiler proves that the code
is well-formed. It proves nothing about a phone.

**Reference device for every "Measured" claim:** Redmi Note 13 5G, HyperOS 3.0.3, Android 15,
Shizuku shell (uid 2000), no root. One device, one firmware, one person testing.

---

## Closed items

### `BUG-9` — A square drew as a rectangle in the editor — **closed `0.0.26-dev`**

**Reported.** `width 0.24`, `height 0.12`, shape `square`: a rectangle in the editor, a correct
square once saved and running.

**Built.** The rule — *a square is sized by the shorter of its two sides, a circle by its inscribed
radius* — existed in two places and the two disagreed. It is now one function in the domain,
`PixelRect.shapedAs(shape)`, with `LayoutElement.effectiveShape()` beside it for the related rule
that a stick and a pad are round whatever the document says. The overlay's private copy was deleted
and it calls these; the editor's preview and its hit-testing call them too, so a control is
*selected* by the same outline it is *drawn* with.

**How it is known.** **Measured** on the reference device — the project owner set those exact
numbers and the editor drew a square. Unit tests cover both rules.

**Cost.** None. This removed code.

---

### `FEAT-10` — The window editor — **closed `0.0.26-dev`**

**Asked for.** *"Whatever the case i also want the controller window editor also. on same screen as
layout editor with one toggle to greyout the buttons editor to window editor."*

**Built.** A **Controls / Windows** toggle at the top of the tool panel. The tools of the inactive
mode are **greyed out rather than hidden**, so it stays visible that the other mode exists. In
Windows mode the canvas draws each window as a translucent box around its group, the selected
control's window highlighted, and any window past a quarter of the screen drawn in orange; the panel
lists every window with its share of the screen as a percentage. `◀` and `▶` step a control through
*own window*, every group that exists, and a fresh `group-N` — chosen rather than typed, because
group names follow the same rules as element ids.

**Why it needed a screen at all.** A window is the enclosing rectangle of everything sharing a
group. A finger can slide between controls that share one — that is what makes rolling across face
buttons work, and what lets a thumb hold `L3` and then move the stick — but **every pixel of that
rectangle that is not a control is dead**. A touch there is refused, and, measured on the reference
device, a refused touch is *not* passed to the application underneath. Two grouped controls in
opposite corners make one screen-covering window and the game stops receiving touches. It was
editable only by hand and there was no way to see it.

**How it is known.** **Measured** — the project owner ran tests 9, 10 and 11: greying out, changing
a control's window, and the screen-covering warning all behaved. The platform behaviour underneath
is measured and recorded in `Clustering.kt`.

**Cost.** The editor does not stop a user making a window that covers the screen; it shows the
percentage and turns it orange. `ADR-007`'s spirit: say what is true, do not overrule the person.

---

### `BUG-11` — `Edit layout` reachable in portrait — **closed `0.0.27-dev`**

Three buttons in one non-wrapping row put the third off the edge in portrait, with nothing to scroll
sideways: the editor could not be opened with the phone upright. Split into two rows, `Edit layout`
first. **Measured** — the project owner confirmed it on the device.

**What it did not fix, and cost a round to learn:** the same fault existed in the editor's own tool
row, where `⋮ values` was the seventh button in a panel a quarter of a landscape screen wide. One
non-wrapping row was fixed and the next was left standing. `BUG-16` wraps them properly.

---

### `BUG-12` — Turning the phone keeps you in the editor — **closed `0.0.27-dev`**

`MainActivity` declares `configChanges` for orientation and the sizes that come with it, so a
rotation re-lays-out instead of rebuilding the activity. What this really saves is not the
navigation — it is **every unsaved edit**, which was being thrown away by turning the phone.

**Measured** on the device. It is also what made `FEAT-17` possible: an editor that survives a
rotation can ask for one.

**The standing obligation it creates:** handling a configuration change means Kestrel is now
responsible for anything that should change with it. Compose re-reads `LocalConfiguration` and the
editor re-measures from it, which covers what exists today; a future screen that depends on
configuration-specific resources has to be checked rather than assumed.

---

### `BUG-13` — The numbers dialog fits and scrolls — **closed `0.0.27-dev`**

Two fields to a row and a scrolling body. Four stacked fields in a dialog on a landscape phone put
width and height below the fold with no way to reach them — a feature that worked and could not be
used. **Measured** on the device.

---

### `BUG-14` — A minus sign without a minus key — **closed `0.0.27-dev`**

`± offsetX` and `± offsetY` flip the sign of whatever is in the field, including a half-typed one.
It does not depend on which keyboard someone has, which was the part that could not be relied on.
**Measured** on the device.

The underlying question — whether that keyboard offers a decimal point but no minus — is still not
answered, and no longer needs to be.

---

### `FEAT-13` — Three parts canvas to one part tools — **superseded `0.0.28-dev`**

Built in `0.0.27-dev`, confirmed working on the device, and replaced one round later by `FEAT-16`.

It is recorded rather than deleted because the lesson is worth keeping: three quarters of the screen
was better than half and still an answer to the wrong question. The canvas does not want *most* of
the screen — it is a picture of the screen, so it wants the screen. A permanent panel of any width
is that much of the picture missing.

What survived from it: `⋮ values` as a filled button among the others rather than a text button
nobody could see.

---

---

### `BUG-1` + `BUG-2` — The pad uses the notch, and the setting reaches it — **closed `0.0.28-dev`**

Two entries, one fault, and it took the whole-screen decision to close them. The "use the notch
area" setting existed since `0.0.24-dev` and never reached the overlay: Kestrel's own screen obeyed
it and the pad — the only thing on screen while playing — did not.

Overlay windows now carry `FLAG_LAYOUT_IN_SCREEN` and `FLAG_LAYOUT_NO_LIMITS`, and a cutout mode of
`ALWAYS` on API 30 and above, `SHORT_EDGES` on 29. Without the first two the window manager keeps
every window inside the area it hands out, so a control the layout puts against the top of the
screen quietly arrives below the status bar.

**Measured** — the project owner confirmed the pad's top row sits in the notch strip.

**The cost, and it stands:** a control under the status bar shares that strip with the shade. A
swipe from the top edge will sometimes open the shade instead of pressing the control. The band
stays drawn on the editor's canvas so that is visible while arranging rather than discovered while
playing.

---

### `BUG-16` — Tools wrap instead of running off the edge — **closed `0.0.28-dev`**

The control and window tools are `FlowRow`s. Seven buttons in a fixed row lost the last one off a
narrow panel, which is how `⋮ values` came to exist in portrait and not in landscape. **Measured.**

---

### `FEAT-11` — A grid, and snapping — **closed `0.0.28-dev`**

Grid steps in the layout's own unit — 0.02, 0.04, 0.06, 0.10 of the shorter side, labelled with the
pixels they come to on this phone. Two snapping modes, both off until asked for: to the grid, and to
the other controls' edges and centres and the screen's own. Edge snapping wins over the grid per
axis, because lining up with the control next door is a statement about this layout and landing on a
grid line is a statement about the screen.

A yellow **guide** appears while a control is being dragged, showing what it has caught, and goes
when the finger lifts. It had to be explained before it could be tested — which is a note about
this project's documentation, not about the feature.

**Measured** across three rounds. It shipped first in pixels, which was the wrong unit and was
caught by the project owner: a control is `0.12` and a grid line was `32px`.

---

### `FEAT-12` — Typing the numbers — **closed `0.0.28-dev`**

A `⋮ values` button opens the four numbers — offsetX, offsetY, width, height — as fields on a
decimal keyboard, two to a row, in a body that scrolls. Apply validates through the same
`Placement.of` the file reader uses, so a bad number names the field and the range rather than being
silently clamped. `±` buttons flip the sign of an offset. The dialog states the units, which were
reported as confusing and are not obvious.

**Measured.** It took three rounds: the feature worked from the first, and was unusable in landscape
and unreachable in the tools until `BUG-13` and `BUG-16`.

---

### `FEAT-14` — The grid in the layout's own unit — **closed `0.0.28-dev`**

Covered above with `FEAT-11`. Recorded separately because it was a separate correction: moving the
grid to fractions also meant a snapped control lands on a number the file can hold, which the pixel
grid could not promise on any screen.

---

### `FEAT-16` — The editor is the canvas — **closed `0.0.28-dev`**

The canvas is the entire screen: no title above it, no margin around it, nothing beside it. Tools,
Save and Exit float in the middle — the one region a pad never occupies, because controls belong to
the corners a thumb reaches and the centre is what a game is played through. The tools open as a
sheet and close again; under the buttons, on a dark plate, the layout's name, whether anything is
unsaved, the selected control in both units, and any warning.

Leaving with unsaved changes asks first — added unasked, because "nothing is saved until it is
saved" makes an accidental exit expensive and the exit button had become very easy to press.

**Measured** — *"Yes, absolutely"* and *"working as expected"*.

---

### `FEAT-17` — The preview turns the phone — **closed `0.0.28-dev`**

The orientation buttons ask the activity to turn; the editor then measures the orientation it is
really in; leaving the editor puts the orientation back to the display setting.

What it removed is better than what it added: the old toggle drew a small picture of the phone
turned — a strip too narrow to work in — with system bars that were an estimate, since only the
orientation the phone is in can be measured. That estimate is gone from the code along with the
feature that needed it.

**Measured.** One round later the project owner asked for it to be a floating button rather than a
tool, which is `FEAT-18`.

---

---

### `CRIT-5` + `BUG-10` + `BUG-15` + `BUG-17` — The pad matches the editor — **closed `0.0.29-dev`**

*"now both are perfectly aligned"*, *"actually same size"*.

One thread, four entries, four rounds, and **three real causes stacked underneath one symptom**. It
is the most instructive thing in this file, so it is recorded as one item:

1. **The canvas was the wrong shape** (`CRIT-5`). It took the shape of whatever space the screen
   gave it — near-ultrawide — so controls appeared to overlap that did not.
2. **The canvas and the pad were on different surfaces** (`BUG-10`, then `BUG-15`). The canvas drew
   the display and the pad was laid out in the usable area. Settled by the project owner: the pad
   uses the whole screen, which also closed `BUG-1` and `BUG-2`.
3. **They were drawing at different sizes** (`BUG-17`). The overlay resolves
   `placement.scaledBy(controlScale)` — 0.85 by default — and the editor resolved `placement` alone.
   Every control on the canvas was about 17% larger than the pad draws it, and four controls that
   fit at 85% were reported as leaving the screen at 100%.

**The project owner found the third one, from a screenshot**, after this side had twice declared the
symptom fixed. Each fix was real and each report of success was wrong, because "it looks right now"
was accepted in place of "the two renderers agree".

**Do:** when two renderers must agree, diff the code paths rather than the pictures. `resolve`,
`shapedAs` and `scaledBy` are now the same three calls in both, and `DeviceSurface.forPad` is the
one answer to what they draw on.

**Do not:** report a match as fixed on the strength of one screenshot looking better.

Dragging writes the **unscaled** number to the file — the setting is applied on top of the document
and folding it in would shrink the layout a little further with every drag. There is a unit test for
neither of those, which is the next thing this thread needs.

---

### `FEAT-18` — Rotation is a fourth floating button — **closed `0.0.29-dev`**

`⟳` beside Tools, and the orientation section gone from the sheet. Turning the phone is done *while*
arranging, not configured beforehand. **Measured.**

---

---

### `BUG-18` + `BUG-20` — The canvas is the screen, with nothing drawn round it — **closed `0.0.30-dev`**

The margin went first and the border a round later. Previewing the orientation the phone is in, the
canvas is now exactly 1 : 1 with the display and has nothing marking its edge, because the edge of
the screen is the edge of the screen. **Measured.**

---

### `BUG-19` + `BUG-21` — The home page scrolls to the edges — **closed `0.0.30-dev`**

The title moved into the scroll, then the vertical padding went. Both were bands of a small screen
held permanently by things that had no reason to be fixed. Horizontal padding stays and does a real
job. **Measured.**

---

### `FEAT-19` — Long press a control — **closed `0.0.30-dev`**

A menu at the control, with size, shape, copy and paste. **Copy takes size and outline only** —
position is never copied, because two controls in the same place are two controls one of which
cannot be pressed. Paste is offered **only within a family**: directional (the sticks and the pad),
buttons (face, shoulders, menu) and triggers, which the project owner separated out on the grounds
that a trigger is a long rectangle with a fill in it and nothing else on a pad is shaped like one.
When the clipboard holds the wrong family the menu says so, rather than showing a paste that refuses.

**Measured**, across two rounds — the first version opened partly off screen, which is `BUG-22`.

---

### `FEAT-20` — A shape is drawn as itself — **closed `0.0.30-dev`**

`circle`, `square` and `rectangle` were three words that all meant "look at the picture you are
already looking at". They are the shapes now, drawn rather than taken from a font, from one place
used by both the tools and the menu. **Measured.**

**Where the rule stops:** `own window`, `snap to the grid` and the anchor names keep their words. A
label is not worse than an icon that has to be learned, and a project with no icon vocabulary should
not invent one a control at a time.

---

### `FEAT-21` — The same menu in window mode — **closed `0.0.30-dev`**

Window options at the control, no copy and no paste. A group is a name shared between controls, so
copying one is joining it — which is what stepping through the list already does. **Measured.**

---

### `FEAT-22` — Material, and dark done properly — **closed `0.0.30-dev`**

Every screen, dialog, sheet and button follows one Material 3 colour scheme, and the pad keeps its
own palette because it is drawn over other applications and has to be legible on a white page and a
black one both. **Measured**, including that last part: *"theme doesn't change the gamepad and
canvas good."*

The version that shipped had the *shape* of the setting wrong — three themes in a row, where there
are really two questions: light or dark, and then how dark. `FEAT-23` corrects it. The colours
themselves were right first time.

**What it is not:** a redesign. The home page is still a developer's diagnostics screen, and that is
`CRIT-2`.

---

---

### `BUG-22` + `BUG-23` — The menu opens where it should, first time — **closed `0.0.31-dev`**

Measured rather than guessed, and laid out before being shown. **Measured** on the device — and one
round later the whole problem was removed rather than fixed: `FEAT-28` moves the menu to the centre,
where neither bug can happen. Both fixes were correct and both stopped mattering, which is what
happens when a design question is answered after the bugs it causes.

---

### `FEAT-23` — Two questions, not three answers — **closed `0.0.31-dev`**

**system, light, dark**, plus a **true black** switch live only when dark. The names an earlier build
wrote still read and `dark-amoled` still means true black, so upgrading does not throw away a
choice — unit-tested. Every binary setting is a switch. **Measured**, including the migration.

---

### `FEAT-24` — The sheet is settings — **closed `0.0.31-dev`**

Gear icon, smaller footprint, and only what is genuinely settings inside it: mode, grid, snapping,
the canvas, and a read-out of every window with its share of the screen. **Measured.**

**What made it possible:** the long-press menu became the complete per-control editor first. Taking
the tools out before the menu could replace them would have lost half the editor quietly.

---

### `FEAT-26` — Buttons are rounded rectangles — **closed `0.0.31-dev`**

Material 3 draws a filled button as a capsule and the theme cannot say otherwise — the token maps to
a full corner whatever `Shapes` holds — so the buttons are wrapped once and the application uses the
wrappers. One number decides the corner. Switches stay capsules. **Measured.**

---

---

### `FEAT-15` — One layout, two arrangements — **closed `0.0.32-dev`**

Each element keeps its six placement fields as the landscape arrangement and gains an optional
`portrait` object with the same six. Absent means "the same as landscape", which is what every layout
written before it means. **Measured**: portrait was given its own arrangement, edited, saved, and
landscape was untouched — in the file and in the pad.

**The schema version was not bumped, on purpose.** A build that does not know the field keeps it in
`unknownFields` and writes it back, so an older Kestrel preserves a portrait arrangement it cannot
use. Bumping would have made that file unreadable instead.

**Where the line was drawn wrongly, and corrected a round later.** Shape was put with identity —
kind, binding, group — on the grounds that what a control *is* does not change when the phone turns.
A shape is presentation, and presentation is exactly what an orientation is allowed to differ in.
`FEAT-31` moves it.

**And one half was missed entirely.** Dragging edited the orientation on screen; the values dialog
kept editing landscape. That is `BUG-29`, and it is the second time in this project a rule was
updated in one place and not its copy.

---

### `BUG-25` + `BUG-26` — The toggle clears the camera, and keeps clearing it — **closed `0.0.32-dev`**

Down by its own height in portrait, and repositioned whenever the phone turns rather than decided
once when the window was created. **Measured** in both orientations.

---

### `BUG-28` — The band caption is above the buttons — **closed `0.0.32-dev`**

Above them, and capped at 320dp so it is not a bar of text across the pad it describes. **Measured.**

---

### `FEAT-25` — Snapping survives a restart — **closed `0.0.32-dev`**

Grid size and both snapping switches are in `settings.json`. Built session-only first, on the
argument that working state does not belong in a preference file; the project owner asked for the
stronger thing and that settles it. **Measured** across a force-stop.

---

### `FEAT-28` — The menu opens in the middle — **closed `0.0.32-dev`**

Centred, with everything behind darkened except the control being edited, which stays lit — drawn by
the canvas rather than as a scrim, because only the canvas knows where the control is. Vertical in
landscape, wide in portrait. **Measured.**

**Half of it did not ship**, and the changelog said it had: the larger header and the icon close
button reached the window menu and never reached the control menu. That is `BUG-31`.

---

### `FEAT-29` — Real icons — **closed `0.0.32-dev`**

Google's Material icons through `material-icons-core` — Apache-2.0, from the Compose BOM already in
the build, `core` rather than `extended`. **Measured**, except the close button, which had not
actually been changed.

---

---

## Built, awaiting confirmation — `0.0.33-dev`

### `BUG-29` — Typing edits the orientation on screen

The values dialog read and wrote `placement` while everything else had moved to
`placementFor(portrait)`, so dragging in portrait moved the portrait arrangement and typing moved
the landscape one — from the same screen. It now reads, validates and writes through the same
orientation-aware pair as every other edit. Unverified on the device.

**Second time this shape of fault has happened here.** The first was the editor and the overlay
drawing at different sizes. Both were one copy of a rule not being updated with the other, and both
were found by the project owner rather than by a test.

---

### `BUG-31` — The header and close button are actually applied now

`FEAT-28`'s header reached the window menu and not the control menu: the edit that was supposed to
replace it matched nothing, changed nothing, reported nothing, and the old small text and small `×`
stayed while the changelog said otherwise.

**Do:** when an edit replaces existing text, check the old text is gone afterwards. A
search-and-replace that finds nothing is not a no-op — it is a silent failure to do the work.

Unverified on the device.

---

### `FEAT-34` — The menu fits, and one button does one thing

`size` and `⋮ values` opened the same dialog: two buttons for one action, and a third of the menu's
height spent saying so. With everything else on a full-width row the menu was taller than a portrait
screen and `copy` was off the bottom of it. Now: one `values` button sharing its row with the anchor,
the steppers on one wrapping row, and nothing full-width that does not need to be. Unverified.

---

### `FEAT-31` — A shape belongs to an orientation

An optional `shape` inside the `portrait` block, `null` meaning "the same as landscape". Kind,
binding and group still cannot differ between orientations — those are the fields that would make a
control a different control.

Three unit tests, including the one that caught a real fault while it was being written: the writer
emits every field including the unset ones, so an explicit `"shape": null` had to be read as *absent*
rather than as a missing required value. Unverified on the device.

---

### `FEAT-32` — A way back for a stray control

A fifth floating button, present only while something is off the screen, that puts those controls
back where the shipped layout has them. **Position only** — not size, not shape, not the window they
are in. The built-in is the source because it is the only arrangement Kestrel can be sure fits: its
tests check that at every size in both orientations. Unverified.

---

### `FEAT-35` — The pad's settings live where the pad is

Size, dead zone, curve, sensitivity and both inversions moved into the editor's settings sheet. They
were on the diagnostics screen, which is the one place they cannot be judged — nothing is being
played there and the pad is not on screen. Size is per orientation; the shaping is not, because a
dead zone is a matter of the hardware and the hand rather than of which way the phone is held.
Unverified.

---

### `BUG-30` — The band says what it costs

A control under the system bars gets no touches while those bars are showing, and **this cannot be
fixed**: the status bar and the gesture bar are the system's own windows, they sit above every
application overlay, and a touch that lands on one is never offered to Kestrel.

What is true and useful is that a game is nearly always full screen, and then the bars are not there
and the control works normally. So the editor counts the controls in the band and says what happens
to them when the bars appear. A caption that only said "this is where the system bars are" was a fact
with no consequence attached.

Unverified on the device — the count and the wording, not the platform behaviour, which is not in
doubt.

---

### `BUG-24` + `BUG-27` — carried

The close button was rebuilt at 56dp with a real icon and then did not ship, which is `BUG-31`. These
close when that is seen working.

---

## Before this list existed

The work below closed before `todo-list.md` and this file were created. It has no IDs because none
existed, and it is recorded descriptively so that nothing is rebuilt by accident.

### Phase 0 — input feasibility, and what it cost to find out

**Established, and it is the foundation everything else stands on:** a virtual input device created
through Shizuku's shell privilege delivers real controller input to ordinary target applications on
the reference device. `ADR-INPUT-001` is Accepted with that scope written into it. Evidence is in
`docs/phase0/results/`.

Two results are **binding on any implementation**, because they were measured rather than reasoned:

- **Persistence must be governed, not prevented.** A session is held by a lease that a privileged
  watchdog enforces, so force-stop and uninstall end it without Kestrel running any code. A backend
  that holds a device without one can strand a controller until the phone is rebooted — which
  happened once.
- **Identity keys on the device descriptor, never the numeric id**, which changes on every
  registration.

**`ADR-006` — the accessibility fallback — was measured and then rejected.** It worked: median 4 ms,
about 242 drag movements a second. It was rejected on product grounds, and the second reason is the
harder one: declaring an accessibility service made Play Protect block installation *for every
user*, confirmed in both directions. A failed experiment is a result and is written down as one.
Kestrel is Shizuku-only for input as a consequence.

### The overlay

A pad on screen, in windows the layout decides. What was learned building it:

- **A pointer belongs to the window that received its DOWN for the life of the gesture.** Sliding
  between controls only works inside one window. This is why grouping exists.
- **A view returning "not handled" does not pass the touch to the application below**, and irregular
  touchable regions are not public API. This is why windows are kept small.
- **Grouping by proximity does not work.** It was tried. On the shipped layout the gap that had to
  mean "together" and the gap that had to mean "apart" were fifteen pixels apart, so the answer
  flipped with rounding and with the size setting. Declared groups replaced it.
- **Anchors and edge margins, not absolute coordinates.** Coordinates computed against the display
  but placed by the window manager inside the usable area moved everything down when the status bar
  appeared, and the pad overlapped itself.
- **Windows are repositioned in place**, not destroyed and recreated, or resizing leaves trails.
- **Controller keys are consumed.** Observing them without handling them made the platform generate
  its own fallback keys, and `B` arrived twice — once as itself and once as Back.

### Layout as a document

`ControllerLayout` with a strict hand-written JSON reader and writer in `:core`. Placement is an
anchor plus inward offsets, sizes are fractions of the **shorter side** so a control keeps its shape
on any screen, and the writer emits **every editable field including nulls and defaults** — a file a
user is invited to edit should show them what there is to edit. Numbers are two decimals.

Built-ins are immutable and the editor duplicates rather than refusing. Storage re-validates the
chosen folder every few seconds and falls back to private storage with an explanation, after a
deleted folder was cached forever.

### Build and toolchain

Gradle with a version catalogue, three modules, CI on every push producing both APKs as artifacts,
lint failing the build. `:core` is Kotlin/JVM on purpose, which makes the architecture boundary a
compile error rather than a review comment.

### Rejected, and why

- **`MANAGE_EXTERNAL_STORAGE`** — measured Play Protect block. Storage access is SAF.
- **`ADR-006` accessibility fallback** — above.
- **Proximity-based grouping** — above.
- **Reverse portrait and sensor portrait** — reverse portrait does not work on the platform;
  sensor portrait is reported useless. `BUG-4` removes what remains.
