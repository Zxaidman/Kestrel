# Kestrel — To-do List

**Document:** `todo-list.md`  
**Status:** Active — the single work queue. Nothing is started that is not on it.  
**Owner of priority:** the project owner. This document orders and describes; it does not decide.  
**Last updated:** build `0.0.25-dev`, second round of owner input

---

## How to read this

Six sections, in the order the project owner asked for them.

| § | Section | What belongs in it |
| --- | --- | --- |
| 1 | **Critical** | Blocks the `v0.1.0` release. Nothing ships until every one is closed or deliberately deferred. |
| 2 | **Errors and bugs** | Something is wrong now. Each carries how it was found and whether it is reproduced. |
| 3 | **Features** | New capability. Each is separable and can be scheduled on its own. |
| 4 | **Working now** | What is built and verified, so nobody rebuilds it and nobody claims more than was measured. |
| 5 | **Pending scope** | Agreed direction, not yet started. |
| 6 | **Owner's list** | Reserved for what the project owner sends next, and everything after. |

**Every entry has an ID** (`CRIT-1`, `BUG-3`, `FEAT-7`) so it can be referred to in one word.

**Evidence vocabulary**, used strictly and matching `AI_DEVELOPMENT_GUIDE.md`:

- **Measured** — observed on the reference device, with the result recorded.
- **Reported** — the project owner saw it; not yet reproduced or diagnosed here.
- **Reasoned** — follows from documented platform behaviour; not observed.
- **Unverified** — believed, with nothing behind it.

**Every entry carries a phase**, so the state of the queue is readable without asking:

| Phase | Meaning |
| --- | --- |
| `pending` | Not started. |
| `building` | Being written now. |
| `testing` | Built and in a build the project owner can install; waiting on a device result. |
| `superseded` | It worked, and something else replaced it. Recorded rather than deleted — what it cost to build is part of the record. |
| `done` | Confirmed on the device, and copied into `done-list.md` with what was done. |

An item leaves this file only by reaching `done`, and it is described in `done-list.md` when it
does. Nothing is deleted to make the list look shorter.

**Anything new is recorded here first.** A requirement, a bug, an idea — it gets an ID in this file
before it is built, argued about, or written into any other document. Standing instruction from the
project owner, and it is what keeps this list the queue rather than a summary of one.

**Reference device for every "measured" claim:** Redmi Note 13 5G, HyperOS 3.0.3, Android 15,
Shizuku shell (uid 2000), no root. One device, one firmware. Nothing here is a claim about other
hardware.

---

## State of the queue — build `0.0.35-dev`

| Phase | Items |
| --- | --- |
| `done` | fifty — every `BUG` from 9 to 33 bar 32 and 34, and `FEAT-10`–`FEAT-36` bar 13, 33 and the pending ones |
| `superseded` | `FEAT-13` |
| `testing` | `CRIT-6`, `CRIT-7`, `BUG-32`, `BUG-34`–`BUG-38`, `FEAT-37`, `FEAT-43`, `FEAT-44` |
| `building` | — |
| `pending` | `CRIT-1`–`CRIT-4`, `BUG-3`–`BUG-8`, `FEAT-1`–`FEAT-9`, `FEAT-30`, `FEAT-33`, `FEAT-38`–`FEAT-42`, `FEAT-45`, `FEAT-46` |

---

## 1. Critical — blocks `v0.1.0`

### `CRIT-1` — A release signing key that is not in this repository

**Phase:** `pending`

**Why it blocks.** The key currently signing every build is committed, and its password is public.
Anyone can sign an APK with it, and the platform will install that APK straight over a user's
Kestrel, inheriting their permissions and their data folder. A signature is the only thing that
makes an update *an update* rather than a different application, and a public key protects nothing.

Acceptable while people are testing builds they fetched themselves from a CI page. **Not acceptable
the moment a build is published as a release**, which is exactly what `v0.1.0` is.

**What it needs.** A key generated once and kept offline, given to CI as encrypted repository
secrets and assembled at build time. The testing key stays for testing. `signing/README.md` carries
the reasoning; the key itself has to be generated and held by the project owner, because if it is
lost no future version can ever update an installed Kestrel.

**Depends on:** the project owner generating and storing the key. Cannot be done unilaterally.

---

### `CRIT-2` — A home screen, and navigation

**Phase:** `pending`

**Why it blocks.** What opens today is a diagnostics harness: every developer control, every raw
number, one long scroll. It was the right thing while the question was "does any of this work". It
cannot be the first thing a user sees.

**What it needs.** A home screen that says what Kestrel is and what state it is in; navigation
between home, controller, layouts, settings and diagnostics; and developer tools moved behind a
deliberate door rather than presented as the product.

**Explicitly requested** by the project owner: *"we can't have our dev build homepage as release
build … hide unnecessary options from the homepage and good navigation setting"*.

**Ordered after** `FEAT-2` (test ground) at the project owner's request.

---

### `CRIT-3` — Modular architecture, as `PROJECT_STRUCTURE.md` already describes it

**Phase:** `pending`

**Why it blocks.** Every line of product code lives in `:app`. `PROJECT_STRUCTURE.md` has described
`feature/`, `platform/` and `data/` modules since before any of it was written, and the gap between
the document and the tree grows with every screen. `CLAUDE.md` §4 allows the packages to be
physically grouped early — it does not allow the boundary to stop meaning anything.

The one boundary that **is** enforced is `:core` being Kotlin/JVM, which makes an illegal import a
compile error rather than a review comment. That is the pattern to extend, not abandon.

**What it needs.** `feature/` for screens, `platform/` for Android-specific implementations, `data/`
for packaged configuration. The input backend behind an interface (`ADR-002`), so a second backend
is possible without the rest of the system noticing.

**Ordered with** `CRIT-2`: doing the navigation first and the modules afterwards would mean moving
the same code twice.

---

### `CRIT-4` — Decide what `v0.1.0` contains

**Phase:** `pending`

**Stated by the project owner:** release and tag `v0.1.0` once **overlay, controller editor, gaming
session and Shizuku** are complete, then push to `main`.

Three of the four are done or nearly so. **Gaming session (Phase 4) is roughly a third built** — it
holds a controller and survives leaving the application, and it cannot launch a target, load a
profile, or notice which target is in front. That is the gap between here and the tag.

The CI workflow already publishes a release with both APKs attached on any `v*` tag, so tagging is
one action once the contents are agreed — and once `CRIT-1` is done.

---

### `CRIT-5` — The editor must draw the phone, not the page

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.

**Kind:** critical. Named first by the project owner, ahead of everything else on this list.  
**Found by:** Reported, build `0.0.25-dev`.

**The fault.** The editing canvas takes the shape of whatever room the screen gives it, which on the
device it was tested on is close to ultrawide. The layout inside it is therefore drawn at an aspect
ratio no phone has. Controls appear to overlap that do not overlap on the device, and — worse in the
other direction — controls that do overlap can look clear. An editor that lies about overlap is
worse than editing numbers in a file, because it invites trust it has not earned.

**What is wanted.**

- A blank canvas with a drawn border, in the **device's own aspect ratio**, standing for the screen.
  The pad is arranged inside that rectangle and nowhere else.
- The rectangle **scaled to fit whole, with no scrolling**, in either orientation of the editor.
- The canvas **docked and fixed to one side** of the screen; the other side a **scrollable panel**
  carrying every editing tool.
- **Both orientations designed for**, not one adapted: in landscape the panel sits beside the
  canvas, in portrait above or below it. The canvas keeps the device ratio in both.

**One decision needed from the project owner:** which ratio the canvas uses. The default proposed
here is *the current device's usable surface* — the same `LayoutSurface` the overlay is placed into,
insets already subtracted — because that is the only ratio Kestrel can measure rather than assume. A
chooser for other ratios is a later addition, not part of this.

**Blocks:** `FEAT-10`, `FEAT-11`, `FEAT-12` and `BUG-9` all happen on this canvas. Doing them first
would mean building each one twice.

---

### `BUG-35` — The buttons could be dragged off the screen

**Phase:** `testing` — built in `0.0.35-dev`, awaiting a device result. What was built is described in `done-list.md`.
**Found by:** Reported, build `0.0.34-dev`.

`FEAT-36` made the block draggable and clamped nothing, so Save and Exit could be pushed over an
edge and left there. Its travel is now half the screen less half of itself, each way.

---

### `BUG-36` — The nine-part lines did not line up with the grid

**Phase:** `testing` — built in `0.0.35-dev`, awaiting a device result. What was built is described in `done-list.md`.
**Asked for:** *"it should not be using new lines instead either it always snap to nearest grid line
or make our grid divide so it lineup with it."*

Two sets of lines that nearly agree read as a mistake. The thirds are snapped to the nearest grid
line — a third of a screen is not a measurement anybody needs to the pixel, while "is this control
on a line" is asked constantly.

---

### `BUG-37` — The pad's stick showed the finger, not the value

**Phase:** `testing` — built in `0.0.35-dev`, awaiting a device result. What was built is described in `done-list.md`.
**Found by:** Reported, build `0.0.34-dev`. *"on Joystick feeling is still the same… but in-game it
is working and feelable. on homepage joystick test it is much more feelable."*

`BUG-34` was fixed and the shaping does reach the game — the project owner confirmed it. What did
not change was the **picture**: the pad drew its knob under the thumb and sent the shaped value, so
the one place somebody looks while tuning a dead zone was the one place the dead zone did not
appear. The diagnostics screen's own stick drew the shaped value, which is why that one felt right.

Two renderers of the same stick disagreeing, again. The pad's knob draws what is being sent: a knob
that does not leave the centre until the dead zone is passed **is** the dead zone, visible.

---

### `FEAT-43` — A dot on the anchor

**Phase:** `testing` — built in `0.0.35-dev`, awaiting a device result. What was built is described in `done-list.md`.
**Asked for:** *"Once the gamepad button is selected mark a red dot on its anchor point."*

An offset is a distance from a point, and that point was named in words and shown nowhere.

---

### `FEAT-44` — The pad gets out of the way on its own

**Phase:** `testing` — built in `0.0.35-dev`, awaiting a device result. What was built is described in `done-list.md`.
**Asked for:** *"Make the K button after not clicking it for 5s halftone de-active… Similarly do it
with a gamepad. But in two stages, in the 5s halftone but still active, another 5s hide it… But give
the user options to toggle it off and increase the timer."*

Two stages for the pad: dimmed but still working, then gone, with the toggle bringing it back —
which is the gesture that has always brought it back. Off by a switch, and the interval is a slider
from 2s to 120s.

**One deviation, and it is deliberate. The toggle only ever dims.** It is the way out. A user who
cannot make the controls go away has lost their phone until they reboot it, which has happened here
once — and a way out that hides itself, or that costs a tap to wake before it will work, is that
same fault with a timer attached. It fades, and it works on the first touch.

---

### `CRIT-6` — The default layout is the project owner's, and 100% means what they set

**Phase:** `testing` — built in `0.0.35-dev`, awaiting a device result. What was built is described in `done-list.md`.
**Decided:** *"make 0.80 scale as new 100%, and scale min as 50% and max as 200%."*

The arrangement sent last round could not ship as a default because it overlapped itself above 89%
while the slider ran to 100%. The project owner's answer moves the scale rather than the layout:
what was 80% is 100%, and the range becomes 50%–200%.

That makes their arrangement clean at the default and up to about 111%, and it changes what the
shipped layout has to promise. **The promise is now: no overlap and nothing off the screen at the
default size and every size below it.** Above the default, a pad is being deliberately enlarged and
what it collides with is the user's business; the editor marks what leaves the screen.

**Every settings file already on a phone holds a number in the old scheme**, so the file carries a
`scaleScheme` marker and a file without one is converted on the way in. Reading an old 0.80 as a new
0.80 would have shrunk somebody's pad by a fifth without telling them.

---

### `CRIT-7` — The sizing and placement proposal, assessed

**Phase:** `testing` — built in `0.0.35-dev`, awaiting a device result. What was built is described in `done-list.md`.
**Source:** `docs/inbox/ideas/CRIT-Gamepade-size-position.md`, pushed by the project owner, §5 of
which invites criticism.

**§2, the nine anchor points: already exactly this.** `Anchor` has all nine, and `resolve` applies
an offset **inwards** from the anchor — so a corner takes positive numbers in both directions, and
an edge or the centre takes signed ones running the way the proposal describes. Nothing to build;
worth saying plainly, because a proposal to build what exists is usually a sign the existing thing
is not visible enough.

**§4.2, changing an anchor must not move the control: a real bug, and it is fixed.** The anchor
changed and the offsets were kept, so a control pinned bottom-left at `0.2, 0.2` became one pinned
top-right at `0.2, 0.2` — the opposite corner. It now keeps its position and the numbers are
recalculated. → `BUG-38`.

**§2 and §4.1 contradict each other, and §4.1 is right.** §2 asks for positions in the phone's
actual pixels; §4.1 asks the system not to be locked to one screen and to scale to tablets and other
phones without stretching. **Those cannot both hold.** A pixel is a different physical size on every
panel, so a layout stored in pixels either moves or stretches on the next device — which is the
requirement §4.1 exists to prevent.

Fractions of the screen's **shorter side** are what §4.1 asks for, written down: a control keeps its
shape, keeps its size relative to the hand, and lands correctly on a phone with a cutout and one
without. That is not a preference; it is the only unit that satisfies §4.1.

**What is genuinely missing is that the numbers are not shown in pixels where they are typed.**
The editor already reports a control as `302 × 302 px` and labels the grid `0.04 · 37 px`, and the
values dialog takes fractions only. Presentation and storage are different questions — this project
has drawn that line twice already — so the dialog gains a unit switch. → `FEAT-45`.

**§3, sizes that vary by layout type:** the limits that exist (`MIN_SIZE`, `MAX_SIZE`) catch nonsense
rather than protect a thumb. → `FEAT-46`.

---

### `BUG-38` — Changing an anchor moved the control

**Phase:** `testing` — built in `0.0.35-dev`, awaiting a device result. What was built is described in `done-list.md`.
**Found by:** the project owner's proposal, §4.2.

The anchor changed and the offsets were kept. Position is preserved now, in the document's own terms
at full size — an offset scales and an anchor does not, so "the same spot" is a different pair of
numbers at every size, and the document is the thing being edited. At the default size, which is now
full size, the two answers coincide.

---

### `FEAT-45` — Type a size in pixels

**Phase:** `pending`
**From:** the proposal's §2, read as what it is useful for.

The values dialog takes fractions of the shorter side, which is what the file holds and not what an
eye measures. A switch between `0.28` and `302 px`, converting on the way in and out, with the file
unchanged either way.

**The file stays in fractions**, for the reason the proposal's own §4.1 gives.

---

### `FEAT-46` — Sizes bounded by a thumb rather than by nonsense

**Phase:** `pending`
**From:** the proposal's §3.

`MIN_SIZE` is 0.01 and `MAX_SIZE` is 2.0 — bounds that catch a corrupt file, not bounds that keep a
control usable. A minimum should be stated in **millimetres**, because that is what a thumb is
measured in and it is the same on every panel; the platform's own guidance is about 9mm. A maximum
matters less and is a fraction of the screen.

**Not per "layout type" as the proposal suggests**, unless a reason appears: a thumb is a thumb
whether the pad is calling itself Xbox or Switch, and a limit that changes with a label is a limit
somebody will trip over without knowing why.

---

## 2. Errors and bugs

### `BUG-1` — The overlay does not draw into the cutout area

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.

**Reported**, `0.0.25-dev`, with a screenshot. Kestrel's own screen now uses the notch area
correctly; **the controller overlay does not**. On the reference device the left-hand controls stop
short of the notch, wasting the space the setting was turned on to claim.

**Likely cause, reasoned and not yet confirmed:** the overlay's windows are separate from the
activity's, and `layoutInDisplayCutoutMode` was applied only to the activity. An overlay window has
its own attributes and its own insets, and the surface it resolves against subtracts the cutout
whether or not the user asked it to.

**Also:** `BUG-2` is the same fault seen from the other side.

---

### `BUG-2` — The "use the notch area" setting does not reach the overlay

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.

**Reported**, `0.0.25-dev`: *"yes, except for gamepad"*. Toggling the setting changes Kestrel's own
screen and leaves the controls where they were. A setting that works in one place and silently does
nothing in another is worse than one that is absent, because it teaches the user it did nothing at
all.

**Fix is shared with `BUG-1`.** The overlay must read the same preference and set its own windows'
cutout mode and insets accordingly.

---

### `BUG-3` — `HOW-TO-EDIT.md` is not written

**Phase:** `pending`

**Reported**, `0.0.25-dev`: *"no, `HOW-TO-EDIT.md` found"* — meaning it was not found.

**Diagnosis, reasoned:** the guide is written by **Copy layout to my folder** only. The project
owner reached the editor through **Edit layout**, which duplicates a built-in by a different path
and never calls the writer. So the code is right and it is on the wrong path.

**What it needs:** the guide written whenever a layout is written into the user's folder, by any
route — and a check that the file is actually there rather than an assumption that the call
succeeded.

---

### `BUG-4` — `sensor-portrait` does nothing and should go

**Phase:** `pending`

**Reported**, `0.0.25-dev`: *"sensor portrait is useless just like reverse portrait discard it"*.

Most phones do not support reverse portrait at all, so `sensor-portrait` behaves exactly like
`portrait` on the device in front of the user. **An option that does nothing is worse than one that
is absent** — the same reasoning already used to leave reverse-portrait out.

**What it needs:** remove `SENSOR_PORTRAIT`, leaving `auto`, `landscape`, `reverse-landscape`,
`sensor-landscape`, `portrait`. A settings file naming the removed value must keep loading, falling
back rather than being refused.

---

### `BUG-5` — A `"shape": "round"` was seen somewhere

**Phase:** `pending`

**Reported**, `0.0.24-dev`, and **not reproduced**. Kestrel only ever writes `circle`, `square` or
`rectangle`, and the reader refuses anything else with the allowed values listed. The only
`round`-adjacent value in any Kestrel document is `deadzoneShape` in `settings.json`, which is
`radial` or `axial`.

**Open question rather than a known fault.** If a layout genuinely contains `"shape": "round"`,
**Reload layout** should have refused the file — so either the file was not the one being read, or
something writes a value this project does not know about. Needs the file, or the sighting
withdrawn.

**Priority:** low, and it stays on the list because an unexplained value in a validated document is
not something to shrug at.

---

### `BUG-6` — Kestrel cannot create its own data folder

**Phase:** `pending`

**Measured** and **not fixable within the current permission set**, recorded so it is not raised as
a bug repeatedly.

Creating a directory at the top of shared storage needs `MANAGE_EXTERNAL_STORAGE` — access to every
file on the phone. Declaring a permission of that class is exactly what got Kestrel **blocked by
Play Protect** when the accessibility service was declared, measured in `ADR-006` and confirmed in
both directions. One tap in the picker, once, versus every user's install.

**Status: accepted limitation.** Reopen only if the project owner decides the Play Protect cost is
worth paying, which would be a measured experiment rather than a code change.

---

### `BUG-7` — The trigger's clockwise border sweep should go

**Phase:** `pending`

**Found by:** Reported, build `0.0.25-dev`.

On a rectangle or a square the trigger shows a bar filling from the bottom up, and the project owner
reports it reads correctly. On a circle there is **also** a sweep running clockwise around the
border, which is a second reading of the same number and does not match the bar.

**Wanted:** remove the clockwise border sweep for every shape. Keep the bottom-to-top fill alone.

Small and isolated — the trigger drawing in `ControllerOverlay.kt`. No schema change, no other
control affected.

---

### `BUG-8` — A trigger takes too long to register

**Phase:** `pending`

**Found by:** Reported, build `0.0.25-dev`.

A trigger ramps to full over 0.5s. That number was chosen deliberately and for a measured reason: a
trigger that jumps straight to full felt broken to the hand. But it also means the press is not
*registered* quickly, which is what the project owner is now hitting.

**The project owner's proposal:** fill the first half fast and the second half at normal speed. The
press registers almost at once; a full pull still takes a moment and still feels like a trigger.

**Assessment — Reasoned, not measured.** Nothing downstream reads the *shape* of the ramp; the
backend is sent an axis value each frame, so a two-rate curve costs nothing structurally. The only
real risk is a target that treats the axis as a switch above some threshold — a fast first half
would then make it *more* responsive, not less. So the proposal looks safe, and it will be measured
on the device rather than asserted.

**Fallback if it does break something:** 0.35s, linear.

**Either way it becomes configurable** — a field in the layout document (which means a schema
version bump and a `docs/CONFIGURATION_SCHEMA.md` update) and a control in settings.

**Do not** reintroduce a per-frame ramp. This was already wrong once: a fixed step per frame gave
0.31s on a 120Hz panel and 0.5s on a 60Hz one. The ramp is measured in time.

---

### `BUG-9` — A square draws as a rectangle, in the editor only

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.

**Found by:** Reported, build `0.0.25-dev`. Reproducible values given: `width 0.24`, `height 0.12`,
shape `square`.

In the editor preview that control draws as a rectangle. Saved and run, the overlay draws it as a
proper square. So the **overlay is right and the editor is wrong** — the preview is drawing the
placement box as given instead of applying the square rule the renderer applies, where a square
takes the smaller of the two extents for both.

Circle and rectangle are unaffected and were checked.

Same drawing path as `CRIT-5`, so it is fixed there rather than separately.

---

### `BUG-10` — The canvas is the usable area, not the phone, so the pad does not match it

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported and reproduced from the numbers, build `0.0.26-dev`.

**The fault, exactly.** The canvas draws **2289 × 927** — the usable area, with the system bars and
the cutout already subtracted. The screen is **2400 × 1080**. Those are not the same shape: 2.47 : 1
against 2.22 : 1. So `CRIT-5` fixed the *scale* of the lie and not the lie: the canvas is still more
elongated than the phone, and it is not "showcasing the device" as asked.

**And it is why the pad still does not match** (test 12). Controls are drawn hanging over the top
edge of the canvas — visible in the project owner's first image on both shoulder clusters. On the
canvas they hang into nothing. On the phone the window manager will not put a window outside the
area it laid out, so the same control is pushed back in. Editor and pad disagree, which is the exact
failure `CRIT-5` exists to end.

**The fix.** Draw the **whole screen**, and draw the bars and the cutout as marked-off bands inside
it. `LayoutSurface` already carries insets and `resolve` already places controls inside them, so the
arrangement lands exactly where the overlay puts it while the rectangle finally has the phone's own
proportions. A control that leaves the usable area is then visibly leaving it, and is marked.

---

### `BUG-11` — `Edit layout` cannot be reached in portrait

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.26-dev`.

Three buttons — *Copy layout to my folder*, *Reload layout*, *Edit layout* — sit in one row that
does not wrap. In portrait the third is off the edge of the screen and there is nothing to scroll
sideways. The editor is unreachable without turning the phone.

---

### `BUG-12` — Turning the phone inside the editor throws you back to the home page

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.26-dev`.

The activity is recreated on a configuration change, so every piece of editor state — which page is
open, which control is selected, **and every unsaved edit** — is thrown away. Losing an arrangement
because the phone turned is worse than the navigation fault it looks like.

The fix is for the activity to handle the configuration change rather than be rebuilt by it. Compose
re-lays-out on its own, and `LocalConfiguration` still updates, so the editor re-measures the screen
without losing a thing.

---

### `BUG-13` — The numbers dialog does not fit in landscape and will not scroll

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.26-dev`.

Four fields stacked vertically in a dialog on a short landscape screen: **width** and **height** are
below the fold, and the dialog body does not scroll, so they cannot be reached at all. The feature
works and is unusable in the orientation the pad is mostly edited in.

Two fields per row, and the body scrolls.

---

### `BUG-14` — A minus sign cannot be typed on the numeric keyboard

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.26-dev`: *"The Android keyboard only shows numbers."*

The fields ask for a decimal keyboard. On this device's keyboard that appears to be digits and a
decimal point, with no minus sign — and an offset may legitimately be negative when a control is
meant to sit outside its anchor. Pasting works, which is a workaround and not an answer.

**Not fully diagnosed.** It is not yet confirmed whether the decimal point is available or only
digits; the project owner entered `0.24` successfully in the same round, which suggests the point is
there and only the sign is missing. A `±` button beside each offset field removes the doubt without
depending on which keyboard someone uses.

---

### `BUG-15` — The pad should use the whole screen, and both sides should agree on which screen

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Decided by the project owner, build `0.0.27-dev`:** *"i want the gamepad and it's window to always
use the whole screen 2400x1080."*

**What was wrong with the previous answer.** `BUG-10` made the canvas the whole screen and then kept
placing the pad inside the usable area, so four controls sitting perfectly well on the screen were
reported as *"outside the usable screen"* and outlined in orange. The report was true of the usable
area and false of the phone, which is worse than no report: the images show them plainly on screen.

**The decision, and it settles three open items at once.** The pad is laid out against the **whole
display**, cutout and bars included. That is what the project owner wants, it is what the canvas is
already drawing, and it is what `BUG-1` and `BUG-2` were asking for from the other direction — the
notch setting existed and never reached the overlay.

**What it takes.** Overlay windows need `FLAG_LAYOUT_IN_SCREEN` and `FLAG_LAYOUT_NO_LIMITS` to be
placed outside the area the system hands out, and a cutout mode that lets them into the notch. The
editor resolves against the same surface, so the orange warning fires only for a control genuinely
off the display.

**The cost, stated once.** A control under the status bar shares that space with the system: a swipe
down from the top edge still opens the shade, and a control there will sometimes be pulled at
instead of pressed. That is the trade being taken deliberately, and the band stays drawn on the
canvas so it can be seen while arranging rather than discovered while playing.

---

### `BUG-16` — `⋮ values` disappears in landscape

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.27-dev`. Present in portrait, gone in landscape.

The control tools are a row that does not wrap. With the panel at a quarter of a landscape screen
there is no room for the seventh button, so it is simply not on screen — the same fault as `BUG-11`,
in a different row. Tools have to wrap rather than run off an edge.

---

### `BUG-17` — The editor drew the document; the pad draws the document at the size setting

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported as an observation and it was the right one — *"buttons size representation in
Canva is not aligned with actual gamepad size maybe this could be why outside warning is shown."*

**It is exactly why.** The overlay resolves `placement.scaledBy(controlScale)` — 0.85 by default.
The editor resolved `placement` and nothing else. So every control on the canvas was drawn about
17% larger than the pad draws it, the arrangement was subtly wrong everywhere, and four controls that
fit on the phone at 85% were flagged as leaving the screen at 100%.

Two rounds of "the pad does not match the editor" had a second cause sitting underneath the one that
was found, and the project owner spotted it from a screenshot before this side did.

**The fix.** The canvas draws at the same size the pad is showing, and dragging still writes the
**unscaled** number to the file — the document is the pad at full size and the setting is applied on
top of it, which must not change by editing.

---

### `BUG-18` — The canvas is a picture of the screen and should be the size of it

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported with screenshots, build `0.0.28-dev`.

The canvas is fitted with a 4% margin, left over from when it shared the screen with a panel. It has
the screen to itself now and the same shape as the screen, so the margin does nothing but make the
picture smaller than the thing it is a picture of.

At no margin the scale becomes exactly 1 : 1 while previewing the orientation the phone is in, which
makes "does the pad match the editor" a question anyone can answer by looking.

---

### `BUG-19` — The home page header does not scroll

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"i don't want kestrel on homescreen as freeze header across scroll."*

The title sits outside the scrolling area, so it holds a band of a small screen permanently. It
belongs in the scroll with everything else.

---

### `BUG-20` — The canvas border has nothing left to mark

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.29-dev`. *"there 1px maybe still shows as white."*

The stroke around the canvas existed to say where the picture of the phone ended. The picture is now
the whole screen at 1 : 1, so the only thing the border marks is the edge of the screen — which the
screen already marks. It goes.

---

### `BUG-21` — A white band across the top and bottom of the home page

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.29-dev`. Sides are fine; top and bottom are not.

Vertical padding on a full-screen page, applied outside the scrolling area, so it is a permanent
band rather than a margin that scrolls away. The horizontal padding is doing a real job and stays.

---

### `BUG-22` — The long-press menu opens off the screen

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.29-dev`, and it made `FEAT-19`'s point 12 untestable.

The menu is clamped against a **guessed** height, so a menu taller than the guess still runs off the
bottom — and clamping is the wrong idea anyway. A menu for a control at the bottom of the screen
should open **upwards**, and one at the right edge should open **leftwards**, rather than being
slid back over the control it belongs to.

The fix is to measure the menu rather than guess it, and to choose a side per axis from where the
control actually is. Every control worth long-pressing is in a corner or against an edge, because
that is where thumbs are — so this is the normal case, not the edge case.

---

### `BUG-23` — The menu appears off screen and then jumps

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.30-dev`. *"it open outside of screen then suddenly popup in right
place."*

`BUG-22` measures the menu to decide which side it opens on, and a thing has no measurement until it
has been drawn once. So the first frame is drawn at the raw touch point — off the edge — and the
second frame is right. The fix is to not show the first frame: it is laid out, measured and only
then made visible, which costs a frame nobody can see instead of a frame everybody can.

---

### `BUG-24` — The close button in the menu is too small to hit

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.30-dev`.

A `×` in a text button, which is a glyph with almost no target around it, in a menu opened by a
thumb. It needs a real target — and the menu should also close by touching the canvas away from it,
which is what people try first.

---

### `BUG-25` — The toggle sits on the camera in portrait

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.30-dev`.

Since the pad took the whole screen, the `K` toggle in portrait sits directly over the front camera —
where the glass is a different shape and a finger is not reliably on the button. In portrait it moves
down by its own height, which clears the cutout without moving it anywhere anyone has to look for it.

Landscape is unaffected: the cutout is on a short edge there and the toggle is not near it.

---

### `BUG-26` — The toggle keeps its portrait offset after turning the phone

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.31-dev`. *"portrait now it is clear but it also made changes in
landscape."*

`BUG-25` moves the toggle down by its own height in portrait, and the margin is decided **once**,
when the toggle window is created. `refresh()` re-measures the control windows on rotation and never
touched the toggle, so a toggle put up in portrait keeps its portrait offset in landscape — and one
put up in landscape stays on the camera when the phone is turned.

The toggle is repositioned with everything else.

---

### `BUG-27` — The close button is still too small

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported with a screenshot, build `0.0.31-dev`.

44dp is the platform's minimum, which is a floor rather than a size. In a menu opened by a thumb and
closed by the same thumb it should be obviously larger than the smallest thing allowed.

---

### `BUG-28` — The band caption is in the wrong place and across the pad

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported with a screenshot, build `0.0.31-dev`. *"caption is not even above floating
button"*, and it runs the width of the screen across the controls.

It was added below the buttons rather than above them, and given no width limit, so it is a bar of
text lying over the pad it is describing. Above the buttons, and no wider than they are.

---

### `BUG-29` — Typing a number edited the wrong orientation

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported, build `0.0.32-dev`. Dragging in portrait moved the portrait arrangement;
typing in the values dialog moved the landscape one, from the same screen.

The dialog read and wrote `placement` directly while everything else had moved to
`placementFor(portrait)`. It is the worst shape a fault can have — the same screen doing two
different things depending on how you ask it — and it is the second time this project has been
caught by one copy of a rule not being updated with the other.

---

### `BUG-30` — A control under the system bars cannot be touched while they are showing

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported with screenshots, build `0.0.32-dev`. A control dragged into the band gets no
touches.

**This cannot be fixed, and pretending otherwise would be worse than saying so.** The status bar and
the gesture bar are the system's own windows and they sit above every application overlay. A touch
that lands on one goes to the system; Kestrel is not offered it and cannot ask to be.

**What is true and useful:** while a game is full screen — which is nearly always, and is what a
handheld is for — the bars are not there and a control in that strip works normally. The failure is
specific to the bars being visible.

So the editor **names it**: it counts the controls in the band and says, in the band's own caption,
that the system takes those touches while the bars are showing. A caption that only said "the
lighter band is the system bars" was a fact with no consequence attached to it.

---

### `BUG-32` — A shape offered for a control that ignores it

**Phase:** `testing` — built in `0.0.35-dev`, awaiting a device result. What was built is described in `done-list.md`.
**Asked for:** *"remove the shape from gamepad which doesn't support it."*

A stick and a pad are drawn and pressed as circles whatever the document says, for a reason recorded
in `ControlShape`. Offering the three shape buttons for them was offering a control that does
nothing — and the layout the project owner sent has `"shape": "square"` on a stick and
`"shape": "rectangle"` on the pad, which is what trying it looks like.

The choice is not shown for those kinds.

---

### `BUG-33` — The camera cutout is not a band

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported with a screenshot, build `0.0.33-dev`. *"in landscape mode the left bar is not
system band it is the camera notch area which is useable whether on fullscreen or not."*

Correct, and the distinction matters. The status bar and the gesture bar are the system's **own
windows**: they sit above every overlay and take the touches that land on them. A display cutout is
not a window — it is a hole in the panel with nothing drawn over it, and a control placed beside it
works whether the phone is full screen or not.

Shading both as one band said "you cannot use this" about a strip that is perfectly usable, and put
seven controls into a warning that did not apply to them.

The band is the system bars alone.

---

### `BUG-34` — Dead zone, curve and sensitivity changed nothing you could feel

**Phase:** `testing` — built in `0.0.35-dev`, awaiting a device result. What was built is described in `done-list.md`.
**Found by:** Reported, build `0.0.33-dev`. *"how can I feel it immediately… while control joystick
works the same."*

The overlay is handed an analog profile when it is built and keeps it. `ControllerOverlay.update`
exists to replace it and **was never called** — so every slider moved a number in a file and nothing
in the hand, and the only way to feel a change was to put the controls up again.

It is called now, on every change.

---

## 3. Features

### `FEAT-1` — Face buttons as one cluster, like the d-pad

**Phase:** `pending`

**Requested**, with a reference image: four face buttons on a **shared round plate**, read as one
group rather than four independent circles.

The project owner's words: *"on DPAD yes I mean it like option a … but i like the sound of option b
also"*. Option (a) is this entry; option (b) is `FEAT-2`.

**What it means concretely.** The buttons already share a window, so this is presentation plus
grouping: a plate drawn behind the diamond, the cluster editable and movable as one thing, and the
individual buttons still separately pressable.

**Both styles must remain available**, and each needs its own editing — the project owner asked for
the choice rather than a replacement.

---

### `FEAT-2` — An eight-way face pad

**Phase:** `pending`

**Requested as a nice-to-have.** A single face control read like the cross: one thumb, eight
directions, a diagonal pressing two buttons at once.

The project owner's own caveat, recorded because it sets the priority: *"no game i know use that
except for few. but it is nice to have"*.

**What it needs:** a new control kind — one element binding four controls — which is a schema
addition and its own editing. Not a variation on an existing kind.

---

### `FEAT-3` — A test ground for every control

**Phase:** `pending`

**Requested, and the project owner's stated next priority**, with a reference image: a screen
showing the whole pad with every control lighting as it is pressed, every axis printing its value
live, so each one can be proven in one place instead of inside a target application.

**What makes it worth building rather than nice:** every input fault this project has found was
found by a person pressing something and reading a number. This is that loop, in the product, on one
screen — and it is also what turns "it works" into something a second person can check.

Should show: every button lit on press, both sticks with their live values, both triggers with their
analog value, the pad's eight directions, and what the platform reports back.

---

### `FEAT-4` — Skins

**Phase:** `pending`

Artwork licensed and cleared (**CC0**, *Xelu's Free Controller Prompts*), 233 files assessed, format
not started. `docs/SKIN_ASSETS.md` carries the assessment and the open questions.

**Decided already:** the skin format comes from building Kestrel's own skin first and then judging
packs against what it needed — not from the shape of the pack that happens to be in the inbox.

**Blocked on** nothing technical; scheduled after the editor and the home screen.

---

### `FEAT-5` — Target discovery and launching

**Phase:** `pending`

Phase 1 and Phase 4 of `PRD.md`, and the largest single gap between what exists and the MVP flow.
Kestrel cannot list an installed target, add one by hand, or launch one.

---

### `FEAT-6` — Profiles: a layout per target

**Phase:** `pending`

A gaming profile selects a layout, a controller definition and a display mode for a named target.
`core/profile/ProfileMatching.kt` exists and nothing uses it.

---

### `FEAT-7` — Haptics

**Phase:** `pending`

Listed in `PRD.md` Phase 2 and never started. Small, and worth doing while the controller engine is
still fresh.

---

### `FEAT-8` — The input backend behind an interface

**Phase:** `pending`

`ADR-002` requires it and `ADR-006`'s rejection removed the only second backend that was planned. So
there is exactly one implementation and no interface, which is honest — but the interface is what
lets a future backend arrive without the rest of the system noticing, and it is cheaper to add while
there is one implementation than when there are two.

---

### `FEAT-9` — Community system

**Phase:** `pending`

`PRD.md` Phase 7. Not started, and correctly last: it distributes what the earlier phases produce.

---

### `FEAT-10` — A window editor, on the same screen

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.

**Asked for:** build `0.0.25-dev` round.

A toggle on the editor page switching between **editing controls** and **editing windows**, with
whichever is not active greyed out rather than hidden, so it is always visible that the other mode
exists.

**What it actually edits:** each element's `group` — which controls share one window on screen.
Today that is editable only by opening the file in a text editor, and it is the single setting with
the largest effect on how much of the screen the pad takes away from the target.

**Why it matters** is recorded in full as the answer to the project owner's question 2 in §6: a
window is the enclosing rectangle of everything in its group, and every pixel of that rectangle that
is not a control is dead to whatever is underneath. Two controls in one group at opposite corners
produce one window covering the screen.

**Should show:** each window's rectangle drawn on the canvas, its area as a percentage of the
screen, and a warning past some fraction. The shipped layout's tests already assert no window
exceeds a quarter of the screen; the editor should make a user's layout answerable to the same
question rather than letting them discover it in a game.

---

### `FEAT-11` — A grid, and snapping

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.

**Asked for:** build `0.0.25-dev` round.

- A drop-down for grid size: **32px, 64px, 128px, 256px**.
- A checkbox: **snap to the grid**.
- A checkbox: **snap to gamepad edges** — align a control with the edges of the controls already
  placed.

**One honest caveat to show in the interface.** The document stores fractions of the screen's
shorter side, not pixels, and rounds them to two decimals so the file stays readable. A pixel grid
is therefore a *screen-space aid*: snapping happens in pixels, converts back to a fraction, and the
rounding can leave the control a pixel off the line on a different screen. That is the right
trade — a readable file is worth more than an exact grid — but the user should be told rather than
finding it.

---

### `FEAT-12` — Type the numbers

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.

**Asked for:** build `0.0.25-dev` round.

A three-dot menu on the selected control, giving direct entry of **offset X, offset Y, width,
height**. Dragging is for arranging; typing is for the moment someone knows the number they want.

Validation already exists and already names the field at fault (`Placement.of`), so a bad entry
reports which value was wrong and why rather than being silently clamped.

This is also the right place to state **what each number means**, which the project owner reported
as confusing in the previous round: an offset runs from the anchor to the control's **centre**,
inwards, measured in fractions of the screen's shorter side.

---

### `FEAT-13` — Three parts canvas to one part tools, and the numbers button where the hand is

**Phase:** `superseded` — built and confirmed working in `0.0.27-dev`, then replaced by `FEAT-16`. In `done-list.md`.
**Asked for:** *"use maximum length for Canva 3 ratio for Canva area while 1 ratio for tools. And
three dots are barely visible, I want you to make it the same and along side button edit tool + / -
etc"*

The canvas takes three quarters of the screen and the tools one quarter, in both orientations. The
`⋮` becomes a full button in the same row as `−`, `+`, `taller` and `shorter`, at the same size —
a text button beside a row of filled ones is a control most people never find.

**One thing this does not fix, and it should be said.** Previewing *portrait* while the editor
itself is in *landscape* leaves a tall narrow strip, because the canvas can only be as tall as the
dock. Three quarters of the width does not help a rectangle limited by height. Editing a portrait
pad is best done with the phone in portrait — which `BUG-12` currently makes impossible.

---

### `FEAT-14` — The grid measured in the layout's own unit

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"the button is 0.12 and the grid is 32px both are different scales so it is not very
helpful. Make it both are the same scale"*

Correct, and the fix is to move the grid rather than the control. Sizes in the document are
fractions of the screen's shorter side; a grid in device pixels is a second unit that nothing else
uses, and comparing `0.12` with `32px` requires arithmetic nobody should be doing while arranging a
pad.

The grid becomes **0.01, 0.02, 0.05, 0.10, 0.25 of the shorter side**, with the pixel equivalent for
this phone shown beside it. A step of `0.01` is exactly the precision the file is rounded to, so a
snapped control lands on a number the file can hold — which the pixel grid could not promise, and
which was written up as a limitation of `FEAT-11` when it was really a symptom of the wrong unit.

The selected control's size is shown in both units for the same reason.

---

### `FEAT-15` — One layout, two orientations

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"config should now have both orientation segregation."*

Today a layout is one arrangement, and both orientations are derived from it by anchors and by
sizes measured against the shorter side. That travels between phones well and it does not solve the
real problem: a pad that is right in landscape is not simply the same pad in portrait — the thumbs
are in different places and there is a different amount of room between them.

**The decision needed**, because it changes the file format either way and the wrong choice is
expensive to undo:

- **(a) One document, two sets of placements.** `landscape` and `portrait` blocks inside the same
  layout, sharing one list of controls, their bindings, shapes and groups. One file to copy, one
  file to share, and a control cannot exist in one orientation and vanish in the other.
- **(b) Two documents, one per orientation**, tied together by the profile. Simpler to read; but
  everything except position is duplicated, and the two can drift apart.

**Recommended: (a).** What differs between orientations is *where things are*, not *what they are*,
and the format should say so. `LayoutOrientation` already exists in the schema for a document that
declares itself landscape-only or portrait-only, so (a) extends what is there rather than
contradicting it.

**Added after the decision:** the **pad size slider is per orientation too**. A pad that is right at
85% in landscape is not right at 85% in portrait, where there is less width and more height to reach
across, so the setting belongs beside the arrangement rather than above both of them.

Either way this is a **schema change**: a version bump, a migration for layouts already saved, tests,
and `docs/CONFIGURATION_SCHEMA.md`. It also touches the repository (pick the arrangement for the
orientation in use), the overlay (swap on rotation without dropping a held control) and the editor
(edit one at a time, and say which).

---

### `FEAT-16` — The editor is the canvas, with three buttons floating on it

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"instead of 3:1 canvas whole screen display the Canva and with three floating
button, options (where all the tools and options will go as overlay menu), save, exit. in both
orientation."*

The canvas takes the entire screen. Three buttons float on it — **Tools**, **Save**, **Exit** — and
everything that was in the side panel moves into a menu that opens over the canvas and closes again.

They float **in the middle of the screen**, which is the one region a pad never occupies: controls
belong to the corners and edges where thumbs are, and the centre is the part of the screen a game is
played through. Anywhere else and the buttons would sit on top of the thing being arranged.

This also ends `FEAT-13`'s three-to-one split after one round. Three quarters of the screen was
better than half and still an answer to the wrong question — the canvas does not want *most* of the
screen, it wants the screen, because it is a picture of the screen.

---

### `FEAT-17` — Previewing the other orientation turns the phone

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"pressing portrait preview should rotate the app to portrait view for editing period
only once outside app should follow chosen orientation."*

Right, and it removes an estimate rather than adding a feature. Drawing a portrait phone inside a
landscape editor gives a strip too narrow to work in, and the bars in that preview were guessed at,
because only the orientation the phone is actually in can be measured.

So the toggle asks the activity to turn instead. The editor then measures the orientation it is
really in — no estimate, no strip — and on leaving the editor the orientation goes back to whatever
the display settings say.

---

### `FEAT-18` — Rotation as a fourth floating button

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"add a 4th floating button next to tools for rotation orientation. and then remove it
from the tools section. it should show rotation icon."*

Turning the phone is something done *while* arranging, not something configured — it does not belong
two taps deep in a sheet. A fourth button beside Tools, marked with a rotation glyph, and the
orientation section leaves the tools entirely.

---

### `FEAT-19` — Long press a control for the things done to one control

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"Long press on the gamepad button should open the small floating menu near it with
four options: size (direct open :value dialog), shape (showing rect., circle, square), copy (only
size & shape) and only show paste option once copy to supported buttons."*

A small menu at the control, holding **size**, **shape**, **copy** and — only when there is something
compatible on the clipboard — **paste**. Copy takes size and shape and nothing else: position is the
one thing that must never be copied, since two controls in the same place are two controls one of
which cannot be pressed.

**Paste is offered only within a family**, which is the project owner's rule and a good one:

- **Directional** — the sticks and the d-pad. They are the same kind of object and are sized against
  the same thumb.
- **Buttons** — the face buttons, the shoulders and the menu buttons.
- **Triggers** — their own family, decided in round `0.0.29-dev`. A trigger is a long rectangle with
  a fill in it and nothing else on a pad is shaped like one.

A face button's size means nothing on a stick, so the option is not shown rather than shown and
refused. The menu says which family a control is in, so nobody has to guess why paste is missing.

---

### `FEAT-20` — A shape should look like the shape

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"instead of text use icon or shape to represent it, in both tools and popup. and
this should be true for anything which can use visual instead of text just like rotation. (keep it
bold & size visible)"*

`circle`, `square` and `rectangle` written out are three words that all mean "look at the picture
you are already looking at". They become the shapes themselves, drawn, at a size that can be seen.
The same rule everywhere it applies — the rotation button already works this way and is the model.

**Where it does not apply, and this is the limit worth stating:** a label is not worse than an icon
when the icon has to be learned. `own window`, `snap to the grid` and the anchor names have no
picture that is faster to read than the words, and a project with no icon vocabulary yet should not
invent one control at a time.

---

### `FEAT-21` — The same menu in window mode

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"add similar popup dialog for window mode editor which only show options, not copy
and paste."*

Long press in window mode gives the window options at the control: which window it is in, stepped
through the same list the sheet offers, and its own window. No copy and no paste — a group is a name
shared between controls, and copying a name is just joining the group, which is what the list
already does.

---

### `FEAT-22` — Material design, and three ways to be dark

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"Material UI design for whole app with light mode, grey dark mode, amoled dark
mode."*

Three schemes and a system setting:

- **Light.**
- **Grey dark** — the ordinary dark surface, where an unlit pixel is still grey.
- **AMOLED dark** — true black, so the pixels are actually off. On the reference device's panel that
  is a real difference in what the screen draws, not a style.
- **Follow the system**, which is the default, resolving to light or grey dark.

**What this is and is not.** It is a colour scheme applied through Material 3, which is what the
application is already built from — so every screen, dialog, sheet and button follows it at once.
It is **not** a redesign of the home page: that is `CRIT-2`, it is still a developer's diagnostics
screen, and painting it does not make it a product.

**The overlay keeps its own palette on purpose.** A pad is drawn over somebody else's application,
so it is legible against a white page and a black one both — its colours answer to that, not to a
theme. Making the pad follow the application's theme would make it invisible half the time.

---

### `FEAT-23` — AMOLED is a property of dark, not a third theme

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"make Amoled toggle rather than options in dark mode only. Also make all binary
option as a Android capsule toggle design."*

Right, and the first version had the shape of the setting wrong. There are two questions —
*light or dark* and *how dark* — and offering them as three buttons in a row makes them look like
one. So: **system, light, dark**, and a **true black** switch that is only live when the answer is
dark.

Every binary setting becomes a switch. A checkbox is a form control; a switch is a thing that is on
or off, which is what these are — the grid and edge snapping were checkboxes and should not have
been.

---

### `FEAT-24` — The sheet is settings now, because editing moved to the control

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"now that we can do many layout editing via long press popup, it is time we remove
it from tools sheet and rename it settings use gear icon here. and make in landscape view reduce its
width. and same in portrait reduce its height."*

The sheet was a panel with everything in it, then a sheet with everything in it. With `FEAT-19` and
`FEAT-21` the per-control work happens **at the control**, which is where it belongs, and what is
left in the sheet is genuinely settings: which mode, the grid, snapping, and what the canvas is.

So the sheet keeps those, is named for them, opens from a gear, and gets smaller — it no longer has
to hold two editors.

**What this forces, and it is the right forcing.** The long-press menu has to be complete: it gains
the size steppers, taller and shorter, and the anchor. If a control cannot be fully edited from its
own menu then removing the tools loses something, and losing something quietly is how an editor gets
worse while looking cleaner.

---

### `FEAT-25` — Snapping is remembered, and now across restarts

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"snapping setting in editor should be saved per session."*

Grid size and both snapping switches survive closing and reopening the editor. Somebody who turns
edge snapping on wants it on for the arranging they are doing, not for the next control only.

**Reopened in round `0.0.31-dev`.** The session-only version was built, worked, and the project
owner asked for the stronger thing — so it goes in `settings.json` after all, with the version and
the migration that implies. The argument for keeping working state out of a preference file was
real; the request settles it.

---

### `FEAT-26` — Buttons are rounded rectangles

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"app ui buttons should be rounded rectangle Instead of capsule like or maybe give
option for this in settings."*

Material 3 draws a filled button as a capsule, and the platform does not let that be changed through
the theme — the token maps to a full corner whatever the theme's shapes say. So the buttons are
wrapped once, in `ui/theme`, and the application uses the wrappers. One place to change the corner,
which is also what would make a setting cheap later.

**No setting for now.** It was offered as a "maybe", and a preference nobody has asked for twice is a
preference that costs more to keep than to add.

---

### `FEAT-27` — Say what the lighter band is

**Phase:** `pending`
**Asked for:** *"In the layout editor mentioned above the floating button the what lighter canva
area represents."*

A line above the floating buttons, shown only when there is a band: it is where the system bars and
the camera cutout are, controls placed there work, and they share that strip with the system. It has
been drawn since `0.0.28-dev` and explained only in a changelog nobody reads while holding a phone.

---

### `FEAT-28` — The long-press menu opens in the middle, with everything else out of the way

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"Long press popup should always open in the mid center with everything blurred or
darkened behind it except the selected button… make identify header bigger, boulder and more
noticeable… The portrait view should makes a popup horizontal and in landscape view keep it
vertical."*

Three changes and they are one idea: while the menu is open, the only two things on screen are the
control being edited and the menu editing it.

- **Centre**, always. `BUG-22` and `BUG-23` were both about a menu that follows the finger into a
  corner; a menu that does not follow the finger has neither problem.
- **Everything behind is darkened except the selected control**, which stays lit. The control is the
  subject of the menu and it should be the only thing left to look at.
- **The header is the identity of the thing being edited**, so it is set large and bold rather than
  as the smallest line in the panel.
- **Vertical in landscape, horizontal in portrait.** A tall menu in the middle of a landscape screen
  leaves the sides showing; a wide one in portrait leaves the top and bottom. Either way the pad
  stays visible around it.

---

### `FEAT-29` — Real icons, not glyphs from a font

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"use icons from free copyright google icons, icons8 and flatcons website. don't
hardcode it."*

`⚙`, `⟳` and `×` are characters, and a character is whatever the phone's font happens to have — a
different weight and a different shape on a different phone, and sometimes a box.

Google's Material icons are used instead. They come with Material 3 through
`material-icons-core`, which is **already on the dependency list** — Apache 2.0, no new download, no
size added, and drawn as vectors rather than typeset.

**The limit worth stating:** `material-icons-core` carries a small set. Settings, refresh, close and
the arrows are in it. A **gamepad** icon is not, so `FEAT-30` will need a vector drawable of its own,
and adding one is a licence question to answer at the time rather than in advance.

---

### `FEAT-30` — The toggle is part of the layout

**Phase:** `pending`
**Asked for:** *"make K button editable both editor and json and customisable theme like rest of
gamepad. use gamepad icon."*

The toggle is the only thing on screen that is not in the document: its position is a constant in
the code, its size is a fraction of the screen, and it is drawn with a letter. It should be an
element like any other — placed, sized, shaped and moved in the editor, written to the file, drawn
in the pad's own palette, and marked with a gamepad icon rather than a `K`.

**Held back on purpose, and the reason is the schema.** `FEAT-15` is changing what a layout holds
this round. Adding a second new thing to the same file in the same round means one migration to
write and two ways to be wrong. It is next.

**One rule it must not break:** the toggle is the way out. A user who cannot make the controls go
away has lost their phone until they reboot it, which has happened once here. So whatever the
document says, the toggle stays reachable — it is not scaled by the size setting, and a layout that
puts it off the screen is a layout the editor refuses rather than warns about.

---

### `FEAT-31` — A shape belongs to an orientation too

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"move the shape to portrait also, because it is customisable."*

Right, and it corrects the line drawn in `FEAT-15`. Shape went with identity on the grounds that
what a control *is* does not change when the phone turns. But a shape is presentation, and
presentation is exactly what an orientation is allowed to differ in: a shoulder button that is a
wide rectangle across a landscape screen has no width to be wide in upright.

Kind, binding and group still cannot differ — those are the fields that would make a control a
different control.

---

### `FEAT-32` — A way back for a control that has left the screen

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"add one more floating button on which only appears if gamepad is out of bound to
resets only his position to default placement without effecting size and shape etc."*

A fifth floating button that exists only while something is off the screen, and puts those controls
back where the shipped layout has them. **Position only** — not size, not shape, not which window
they are in. A control dragged off the edge needs rescuing, and rescuing it by resetting everything
about it throws away work that was never the problem.

The built-in is the source of the answer because it is the only arrangement Kestrel can be *sure*
is on the screen — `BuiltInLayoutsTest` checks exactly that, at every size, in both orientations.

---

### `FEAT-33` — A font for the application, including one somebody brings

**Phase:** `pending`
**Asked for:** *"Add option for app font style… System default… few Games related font which are
available on google font or any free license font… And lastly user custom font file from file
picker (.tff and any support one)."*

Three parts, and they are three different sizes of job:

1. **The system font**, which is what happens now.
2. **A few bundled faces** with a licence that permits redistribution — SIL Open Font License, which
   is what most of Google Fonts uses. Each one bundled is a file in the repository, an entry in
   `THIRD_PARTY_LICENSES.md`, and about 100–400 KB in the APK.
3. **A face the user supplies**, chosen through the document picker. This one is not a font
   question, it is a storage question: a `.ttf` picked from anywhere has to be copied into Kestrel's
   own folder — a picker gives a URI that may not be readable tomorrow — validated as a real font
   before it is used, and it must fail to a readable screen rather than to a blank one.

**Held for its own round**, because the third part is the whole of it and it deserves being done
properly rather than alongside eight other things.

---

### `FEAT-34` — The menu fits, and one button does one thing

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported with screenshots, build `0.0.32-dev`.

`size` and `⋮ values` opened the same dialog, which is two buttons for one action and a third of the
menu's height spent saying so. With every other control on a full-width row the menu was taller than
a portrait screen, and `copy` was simply off the bottom of it — a button nobody could reach, in a
menu whose whole point is being reachable.

One `values` button, sharing its row with the anchor. The steppers on one wrapping row. Nothing
full-width that does not need to be.

---

### `FEAT-35` — The pad's own settings live where the pad is

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for:** *"Control size and deadzone, curve, sensitivity and invert x and y should live inside
the editor setting menu."*

They were on the diagnostics screen, which is the one place they cannot be judged: nothing is being
played there and the pad is not on screen. In the editor's settings sheet they are a slider away
from the thing they change.

**Size is per orientation and the shaping is not**, deliberately. A pad's size is a matter of where
the thumbs are, which the orientation decides. A dead zone is a matter of the hardware and the hand,
which it does not.

---

### `BUG-31` — The menu header and close button were never actually changed

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Found by:** Reported with screenshots, build `0.0.32-dev`, after being reported as done.

`FEAT-28` wrote a `MenuHeader` with a large bold title and a 56dp icon button. The window menu got
it. **The control menu did not** — the edit that was supposed to replace its header matched nothing,
made no change, and reported no error, so the old small text and the old `×` stayed exactly where
they were while the changelog said otherwise.

**Do:** when an edit replaces existing text, check the text is gone afterwards. A search-and-replace
that finds nothing is not a no-op, it is a silent failure to do the work.

---

### `FEAT-36` — The buttons are one block, and it gets out of the way

**Phase:** `done` — confirmed on the device. Written up in `done-list.md`.
**Asked for, as the answer to `BUG-30`:** *"Make floating btn and notes as one window then put it
only within center-mid without overflow… then the user can hold and drag it across the screen. When
the user long presses it, give it an option to hide it, and the window will turn as a halftone
overlay which does not accept touch until the user selects the gamepad button."*

The buttons and their captions are one block now. It starts in the middle, which is the one place a
pad never is — right up until a control is dragged there, and then it sits on top of the thing being
edited with no way to move either. So it can be **dragged anywhere**, and long-pressed to **hide**:
faded to a fifth and taking no touches at all, so the pad underneath can be worked on through it.

**Touching any control brings it back.** A hidden panel recoverable only from a menu inside itself
would be a way to lose Save and Exit.

---

### `FEAT-37` — The screen in nine

**Phase:** `testing` — built in `0.0.35-dev`, awaiting a device result. What was built is described in `done-list.md`.
**Asked for:** *"We have divided the whole screen into 9 parts. Give bold light color to the grid
line across the screen to show it."*

Two brighter lines each way. A layout is talked about in those terms — top-left, bottom-middle —
and the fine grid is for placing a control rather than for saying where on the screen it is.

---

### `FEAT-38` — Opacity, for the pad and for one control

**Phase:** `pending`
**Asked for:** *"Whole Gamepad opacity. Per buttons also."*

A pad is drawn over somebody else's application, so how much of it shows through is the setting that
decides whether the game or the pad is easier to see. Two levels: one for the pad, and a per-control
override for the few that want to be fainter than the rest.

Schema: a nullable `opacity` on an element, and a setting for the whole. Both need a floor — a
control at zero is a control that cannot be found, and a pad nobody can see is a phone that has
stopped responding for reasons its owner cannot guess.

---

### `FEAT-39` — Turning a control off

**Phase:** `pending`
**Asked for:** *"Ability to toggle on or off specific buttons."*

A control that is off is not drawn, takes no touches and sends nothing — but stays in the document
with its position, so turning it back on returns it where it was rather than to a default.

**Not the same as deleting it**, and the difference is the point: somebody playing a game with no
triggers wants them gone for that game, not gone.

---

### `FEAT-40` — Layout profiles: platform, then variant

**Phase:** `pending`
**Asked for:** two drop-downs — **Xbox / PlayStation / Switch**, and for each, **default** plus every
copy the user has made.

The schema already has most of this: layouts are documents with stable ids, `builtin.<platform>.<x>`
and `user.<uuid>`, and `docs/CONTROLLER_FAMILIES.md` says where a family belongs. What is missing is
two shipped layouts and a screen to choose from.

**What must not be quietly assumed:** the three families differ in **labels and positions**, not in
what they send. A PlayStation pad's cross is the same control as an Xbox pad's `A`, and the file
already says so — `binds` is the control, the label is presentation. A layout that changed the
binding to change the family would be a layout that breaks every target application.

---

### `FEAT-41` — Import and export a layout

**Phase:** `pending`
**Asked for:** *"Import and Export Layout Profile."*

Export is nearly free — the document is already a file the user owns. Import is the half with the
work in it: `docs/CONFIGURATION_SCHEMA.md` requires every field validated, an id that does not
collide silently, unknown fields preserved, and a typed error rather than a crash for anything
malformed. A layout arriving from someone else is untrusted input.

---

### `FEAT-42` — Keyboard and mouse

**Phase:** `pending`

**Asked for as an experiment, and it must stay one.** Everything Kestrel knows about delivering
input was established by measurement, and none of it was about a keyboard or a mouse: whether a
virtual keyboard device reaches a target the way a virtual pad does is an open question, not a
smaller version of a solved one. It gets a Phase-0-style feasibility test of its own before any of
it is designed.

---

## 4. Working now

Everything below is **measured on the reference device** unless marked otherwise. This section
exists so nothing here is rebuilt, and so nothing is claimed beyond what was observed.

### Input, the hard part

- A **virtual controller** created through the Shizuku shell, recognised by five emulators, a
  browser's gamepad API, and a Windows host through Artemis/Apollo. `ADR-INPUT-001`, Accepted,
  scoped to this device.
- **Face buttons** arrive as 96, 97, 99, 100.
- **The pad's diagonals work in play** — the platform derives key codes 268–271 from the hat, and a
  character moves diagonally in a running title. Binding screens showing one axis are a property of
  binding screens, not of the pad.
- **Analog triggers** send intermediate values; 34 steps measured on the way up.
- **Latency indistinguishable from a real controller** in play, on this device.
- **A session survives leaving Kestrel**, and ends on force-stop, clear-data and uninstall within
  10–20 seconds, enforced by a privileged watchdog.

### The overlay

- Drawn from a **layout document**, not from code.
- **Multi-touch across windows** — `FLAG_SPLIT_TOUCH`. Without it, holding the stick froze the
  phone.
- **Sliding between controls** in one window; **holding `L3` and then moving the stick**.
- **Eight-way pad** with real diagonals.
- **Shapes**: circle, square, rectangle, deciding where a control can be pressed and not only how it
  is drawn.
- **Correct placement** with the system bars up, in both orientations, at every size on the slider.
- **Resizing moves the windows** rather than replacing them; nothing held is dropped.
- **Rotation rebuilds the pad** without hiding and showing it.

### Files and settings

- Everything lives in a **folder the user chooses**, beside `Android` rather than inside it, and
  **survives uninstalling Kestrel**.
- A deleted folder is **noticed and reported**, with a fallback to Kestrel's own directory, and
  recovers when the folder returns.
- `settings.json` and layouts are readable and editable by hand; numbers are two decimals.
- **Install-over-the-top works** — one signing key for every build.

### Editing

- **A layout editor on its own page**: select, drag, size, height, shape, anchor, save.
- A built-in is duplicated rather than edited.
- **Copy layout to my folder** and **Reload layout** — edit the file in a text editor, see the pad
  change.

### Diagnostics

- A report carrying **what was sent and what was received**, in order, so a fault can be placed
  above or below the virtual device.
- 212 `:core` tests, all passing; `./gradlew build` green; CI builds both APKs on every push.

### Setup

- A **setup page** listing what is missing with one action each, skippable, returning when the state
  is still incomplete.
- Full screen, cutout and orientation as settings — **except for the overlay**, see `BUG-1`/`BUG-2`.

---

## 5. Pending scope

Agreed direction, nothing started, no blockers other than order.

| Item | Where it is written down |
| --- | --- |
| Application shell, Compose navigation, launcher | `PRD.md` Phase 1 |
| Target discovery, manual target addition | `PRD.md` Phase 1, `ARCHITECTURE.md` §18 |
| Controller definitions as documents | `PRD.md` Phase 2, `docs/CONFIGURATION_SCHEMA.md` |
| Gaming session: launch, profile, orientation, display mode | `PRD.md` Phase 4 |
| Foreground-target monitoring | `ARCHITECTURE.md` §17 |
| Skin format, selector, import/export | `PRD.md` Phase 6, `docs/SKIN_ASSETS.md` |
| Community repository, manifests, checksums | `PRD.md` Phase 7 |
| Compatibility registry filled from real runs | `docs/COMPATIBILITY.md` |
| Second device, second firmware, second OEM | Everything measured is one device |

**Standing constraints that shape all of it**, so they are not rediscovered:

- **Input needs Shizuku.** `ADR-006` is Rejected — the fallback worked and was not worth shipping.
  Without Shizuku, Kestrel is a launcher, an editor and a skin manager that says so plainly.
- **Configuration is data, never executable.** No shell, no code, no downloaded plugins.
- **Built-ins are immutable**, enforced in the domain rather than by hiding a button.
- **One layout across capability tiers** (`ADR-007`); unavailable controls are disabled, never
  removed or substituted.
- **A window is dead everywhere its controls are not**, and the platform's remedy is not public API.
  Keep windows small.

---

## 6. Owner's list

Reserved for the list the project owner is sending next, and everything after it.

Entries added here get an ID in the same scheme and are then sorted into §1–§3 by kind, with the
original wording kept alongside so nothing is lost in the paraphrase.

### Round `0.0.25-dev` — test results, recorded

| # | Result |
| --- | --- |
| 1 | Main application interface correct. **Overlay still not using the notch area** → `BUG-1` |
| 2 | Notch toggle works, **except for the overlay** → `BUG-2` |
| 3 | Orientation works. **`sensor-portrait` useless, discard** → `BUG-4` |
| 4 | Editor: tap and drag a control — **working** |
| 5 | Editor: size, taller, shorter, shape, anchor — **working** |
| 6 | Editor: save, and the pad matches — **working** |
| 7 | `HOW-TO-EDIT.md` — **not found** → `BUG-3` |
| 8 | Regression: sliding, diagonals, `L3` onto stick, trigger fill — **working** |

### Decisions taken this round

- **Face buttons:** option (a), a shared plate like the pad → `FEAT-1`. Option (b), an eight-way
  face pad, wanted as well but lower → `FEAT-2`.
- **Order for what follows:** layout editor *(done)* → test ground (`FEAT-3`) → home screen
  (`CRIT-2`) and modules (`CRIT-3`).


### Round `0.0.25-dev` — second message: questions, features, bugs

**Two questions asked, and answered from the code rather than from memory.**

**Q1 — Is an offset measured to the control's centre, or to its corner?**

**To the centre.** `Placement.resolve` in `core/layout/Geometry.kt` computes
`centerX = origin + offsetX × shortSide × inward`. The offset runs from the anchor point to the
**centre** of the control, inwards, in fractions of the screen's **shorter side** — the same unit
`width` and `height` use, which is what lets a control and its offsets scale together. A corner-edge
offset would have made the size setting move controls as it resized them.

*Consequence worth knowing:* the distance from the screen edge to the *edge* of a control is
`offset − width/2`. A control with `width 0.24` and `offsetX 0.10` hangs off the screen, and the
editor is allowed to show that rather than refuse it.

**Q2 — Left stick bottom-left, `L3` top-right, anchors unchanged. What happens to the window?**

**It becomes one window covering nearly the whole screen, and that area stops working for the
target.** Neither of the two outcomes guessed at happens: the control is not discarded, and it does
not join the right-hand window.

Why, precisely:

1. **Windows are decided by the declared `group`, never by distance.** `stick.left` and
   `stick.left.press` both carry `group: "left-stick"`, so they stay together wherever they are put.
   The right-hand window is `group: "right-top"` and never takes in a stranger. Proximity grouping
   was tried and abandoned — the gap that had to mean "together" and the gap that had to mean
   "apart" were fifteen pixels apart, so the answer flipped with the size setting.
2. **A window is the enclosing rectangle of its group.** `Clustering.enclosing` takes the smallest
   box containing both, which for opposite corners is the screen.
3. **The gap inside that box is dead.** A touch landing in it is refused by the overlay, and —
   measured on the reference device — a refused touch is **not** handed to the application below. So
   yes: swipes and taps across that whole rectangle stop reaching the target.

Nothing prevents this today. It is exactly why `FEAT-10` exists, and why the window editor has to
show the rectangle and its share of the screen rather than only the `group` name.

**Added from this message**

| Item | Kind | Wording it came from |
| --- | --- | --- |
| `CRIT-5` | critical, first | device-ratio canvas, docked panel, no scrolling, both orientations |
| `FEAT-10` | feature | window editor on the same screen, toggle greys out the other |
| `FEAT-11` | feature | grid 32→256px, snap to grid, snap to gamepad edges |
| `FEAT-12` | feature | three-dot direct entry of offset and size |
| `BUG-7` | bug | remove the clockwise border fill, keep the bottom-to-top bar |
| `BUG-8` | bug | trigger registers too slowly; fast first half, or 0.35s, configurable |
| `BUG-9` | bug | square draws as a rectangle in the editor, correct once saved |



### Round `0.0.26-dev` — block 1 tested

| # | Item | Result |
| --- | --- | --- |
| 1 | Canvas shape | **Failed.** Canvas is 2289 × 927, the phone is 2400 × 1080, and controls draw outside the rectangle → `BUG-10` |
| 2 | Dock and panel | **Failed twice.** `Edit layout` unreachable in portrait → `BUG-11`. Turning the phone inside the editor returns to the home page → `BUG-12`. Proportions wanted 3 : 1 and the `⋮` promoted → `FEAT-13` |
| 3 | Preview toggle | Works, but inherits `BUG-10`, and a portrait preview inside a landscape editor is too small to edit → `FEAT-13` |
| 4 | Square bug | **Fixed** — draws as a square. Dialog does not fit or scroll in landscape → `BUG-13` |
| 5 | Typing the numbers | Validation works. Keyboard offers digits only, no minus sign → `BUG-14` |
| 6 | Grid sizes | Work, but px against a `0.12` control is the wrong unit → `FEAT-14` |
| 7 | Grid snapping | **Working** |
| 8 | Edge snapping and guides | **Working** |
| 9 | Window mode, greying out | **Working** |
| 10 | Changing a window | **Working** |
| 11 | Screen-covering warning | **Working** |
| 12 | Save and play | Values save and the pad plays, but it still does not match the editor → `BUG-10` |

**Closed by this round:** `BUG-9` (the square) and `FEAT-10` (the window editor). Both are in
`done-list.md`.

**Still `testing`, because part of what they promised is not true yet:** `CRIT-5` (`BUG-10`,
`FEAT-13`), `FEAT-11` (`FEAT-14`), `FEAT-12` (`BUG-13`, `BUG-14`).



### Round `0.0.27-dev` — the whole-screen decision

| # | Item | Result |
| --- | --- | --- |
| 1–3 | Canvas, bands, the match | **Wrong answer, right question.** Four controls on the screen were reported as outside it, because the pad was still placed in the usable area while the canvas drew the display. Decided: **the pad uses the whole screen** → `BUG-15`, which also closes `BUG-1` and `BUG-2` |
| 4, 7 | Numbers dialog, values button | Dialog **working**. `⋮ values` present in portrait, missing in landscape → `BUG-16` |
| 5 | `±` buttons | **Working** |
| 6 | Three-to-one split | Superseded: whole-screen canvas with three floating buttons → `FEAT-16`. Preview should turn the phone → `FEAT-17` |
| 8, 9, 10 | Dialog in landscape, `±`, grid | **Working.** `0.01` and `0.25` are useless extremes → grid steps narrowed under `FEAT-14` |
| 11 | Regression | **None.** "Guides" was unexplained on this side: the yellow line that appears while dragging with edge snapping on, showing what the control has lined up with |
| — | Orientation | The layout should hold a separate arrangement per orientation → `FEAT-15`, which needs a format decision first |



### Round `0.0.28-dev` — the whole-screen change, tested

| # | Item | Result |
| --- | --- | --- |
| 1–3 | The match, the whole screen, the false warning | **Still failing, and the project owner found the cause.** The canvas draws the document; the pad draws the document *scaled by the size setting* → `BUG-17`. The canvas also keeps a 4% margin it no longer needs → `BUG-18` |
| 4 | Notch | **Working** — the pad uses the notch strip |
| 5 | Editor fills the screen | **Working.** Separately: the home page header should scroll → `BUG-19` |
| 6 | Floating buttons | **Working.** Rotation wanted as a fourth button, out of the tools → `FEAT-18` |
| 7–12 | Unsaved exit, tools in landscape, rotation, grid, guides, regression | **All working** |
| — | `FEAT-15` | **Decided: (a)** — one document, two placement sets |
| — | New | Long press a control for size, shape, copy and paste → `FEAT-19` |



### Round `0.0.29-dev` — the pad matches

| # | Item | Result |
| --- | --- | --- |
| 1–3 | The match, the warning, the sizes | **Working.** *"now both are perfectly aligned"*, *"actually same size"* → `CRIT-5`, `BUG-10`, `BUG-15`, `BUG-17` all close after four rounds |
| 4 | Canvas edge to edge | Working, less a 1px border with nothing left to mark → `BUG-20` |
| 5 | Size slider | **Working** |
| 6 | Header scrolls | Working; a white band remains top and bottom → `BUG-21` |
| 7 | Rotation button | **Working** |
| 8, 14 | Long-press menu | Opens off screen; needs to open away from the edge it is near → `BUG-22` |
| 9, 11, 13 | size, copy, paste, families | **Working** |
| 10 | Shape buttons | Working; should be drawn shapes rather than words, and that rule applies wherever a picture beats a label → `FEAT-20` |
| 12 | Wrong-family paste | Working, seen poorly because of `BUG-22` |
| 15 | Regression | **None.** Window mode wants the same long-press menu, without copy and paste → `FEAT-21` |

### Decisions taken this round

- **Triggers are their own family** for copy and paste. Three families, not two.
- **`FEAT-15` grows a requirement:** the pad size slider is per orientation as well as the
  arrangement.
- **New:** Material design with light, grey dark and AMOLED dark → `FEAT-22`.



### Round `0.0.30-dev` — themes and the menu

| # | Item | Result |
| --- | --- | --- |
| 1, 2 | Canvas edge, home page band | **Working** → `BUG-18`, `BUG-19`, `BUG-20`, `BUG-21` close |
| 3, 4, 5 | Menu placement | Working, but it draws off screen for a frame first → `BUG-23` |
| 6 | Trigger family | **Working** |
| 7 | Drawn shapes | Working; the `×` in the menu is too small → `BUG-24` |
| 8 | Window-mode menu | **Working** |
| 9–15 | All four themes, bar icons, persistence, and the pad keeping its own colours | **All working** |
| 16 | Regression | **None** |

### Decisions and requests this round

- **AMOLED becomes a switch inside dark**, and every binary setting becomes a switch → `FEAT-23`
- **The tools sheet becomes settings**, with a gear and a smaller footprint, now that editing happens
  at the control → `FEAT-24`
- **Snapping is remembered for the session** → `FEAT-25`
- **Buttons are rounded rectangles** rather than capsules → `FEAT-26`
- **The `K` toggle clears the camera in portrait** → `BUG-25`
- **The lighter band gets a caption** → `FEAT-27`



### Round `0.0.31-dev` — the sheet became settings

| # | Item | Result |
| --- | --- | --- |
| 1 | The menu opens in place, no flash | **Working** → `BUG-22`, `BUG-23` close |
| 2 | Close button | Still too small → `BUG-27`. Tapping the canvas closes it: working |
| 3–6 | Complete menu, gear, sheet size, windows read-out | **All working** → `FEAT-24` closes |
| 7 | Session memory | Working, and it should survive a restart → `FEAT-25` becomes persistent |
| 8–11 | Switches, theme shape, migration, rounded buttons | **All working** → `FEAT-23`, `FEAT-26` close |
| 12 | `K` in portrait | Clear now, but landscape changed with it → `BUG-26`. The toggle should be part of the layout → `FEAT-30` |
| 13 | Band caption | Below the buttons and lying across the pad → `BUG-28` |
| 14 | Regression | **None** |

### Decisions this round

- **Snapping becomes persistent**, not session-only → `FEAT-25` reopened.
- **The long-press menu moves to the centre**, with everything behind it dimmed except the selected
  control → `FEAT-28`.
- **Icons come from Material icons**, already on the dependency list → `FEAT-29`.
- **`FEAT-15` is started**: one document, two placement sets, and a size setting per orientation.



### Round `0.0.32-dev` — two arrangements, tested

| # | Item | Result |
| --- | --- | --- |
| 1, 2 | Portrait gets its own arrangement, as a copy | **Working** |
| 3, 4 | Editing one orientation | Dragging **works**; typing edited landscape from the portrait screen → `BUG-29` |
| 5–8 | Landscape untouched, saved, in the file, dropped again | **Working.** Shape should be per orientation too → `FEAT-31` |
| 9, 10 | Size per orientation | **Working.** Size, dead zone, curve, sensitivity and inversion should live in the editor's settings → `FEAT-35` |
| 11 | Centred menu with everything else dimmed | **Working** |
| 12 | Header and close button | **Not applied at all** — the change was written and never reached `ControlMenu` → `BUG-31`. Menu too tall, `copy` off the bottom, and `size` and `⋮ values` opened the same dialog → `FEAT-34` |
| 13, 14 | Popup shape by orientation | **Working**, bar the close icon |
| 15 | Icons | Working except the close `×`, for the same reason as `BUG-31` |
| 16, 17 | Snapping persists, toggle repositions | **Working** |
| 18 | Controls in the system-bar band | No touches there → `BUG-30`, and it cannot be fixed, only told |
| 19 | Regression | **None.** A way back for a stray control → `FEAT-32` |

### Decisions this round

- **Shape is per orientation** → `FEAT-31`, correcting where `FEAT-15` drew the line.
- **The pad's own settings move into the editor** → `FEAT-35`.
- **Fonts get their own round** → `FEAT-33`.



### Round `0.0.33-dev` — the arrangement is right, the feel was not

| # | Item | Result |
| --- | --- | --- |
| 1–5 | Typing edits the orientation on screen, shape per orientation | **Working.** Shape should not be offered where it does nothing → `BUG-32` |
| 6 | Menu fits | **Working**; landscape could be wider |
| 7–11 | Header, close button, stray button | **Working.** A new default layout supplied → see below |
| 12, 13 | The band | Behaves as described, and the diagnosis was wrong: the trouble is dragging a control **out** of the band, and the landscape cutout strip is not a band at all → `BUG-33`, `FEAT-36`, `FEAT-37` |
| 14–16 | Pad settings in the editor | Present and saved, but **nothing could be felt** → `BUG-34` |

### The supplied default, and why it is not shipped as one

The project owner sent `user.xbox.json` and two screenshots as the new built-in. It was taken
apart and checked against the rules the shipped layout has to keep, and it **fails one of them**:

- `stick.right.press` overlaps `menu.start` — 87px apart where 110px is touching, at the default
  size.
- `dpad` overlaps `menu.select` and `shoulder.l2` above **89%** — the left column has the pad,
  `L3`, `L1`, `Select` and `L2` in one strip, and at 100% there is not room for them.

It is a fine layout **at the size it was arranged at**, and the default is 85%, which is why nothing
looks wrong. `MAX_CONTROL_SCALE` is 1.00, so a user who drags the size slider up gets a pad with its
d-pad under the Select button — which is exactly the fault `BuiltInLayoutsTest` exists to catch, and
it caught it.

**It is on the device as `user.xbox` and nothing about it has been touched.** What is not done is
promoting it to the layout everybody gets. Three ways forward, and the choice is the project
owner's:

1. **Adjust three controls** — `R3` beside the right stick rather than above it, and the two
   triggers 0.08 further in. Smallest change; the arrangement stays recognisably theirs.
2. **Lower `MAX_CONTROL_SCALE` to 0.85** and ship the arrangement exactly as sent. Honest, and it
   takes the size headroom away from everyone.
3. **Leave the built-in as it is.** The supplied arrangement stays a personal layout, which is what
   the built-in → duplicate → edit workflow is for.



### Round `0.0.34-dev` — the shaping arrives, and the picture did not

| # | Item | Result |
| --- | --- | --- |
| 1–4 | Stick shaping | **Reaches the game** — confirmed in play. The pad's own knob still showed the raw finger → `BUG-37` |
| 5–7 | The band, the cutout | **Working** |
| 8, 10–13 | Drag, hide, hide-through, recentre | **Working** |
| 9 | Clamping | Missing — the block slides off the screen → `BUG-35` |
| 14 | The screen in nine | Working; the lines must agree with the grid → `BUG-36` |
| 15 | Shape gating | **Working** |
| 16 | Menu width | Still not comfortable — wider again |
| 17 | Regression | None seen; not a full pass |

### Decisions this round

- **100% is what 80% was**, and the range is 50%–200% → `CRIT-6`, which also makes the project
  owner's arrangement the shipped default.
- **A red dot on the anchor** → `FEAT-43`.
- **The pad fades and then goes when untouched** → `FEAT-44`.

### Not found

**Found, after this was written.** The item arrived on the branch as `e1ec4fc` while this round was
being built — `docs/inbox/ideas/CRIT-Gamepade-size-position.md`. It is assessed as `CRIT-7`, and one
real bug came out of it (`BUG-38`). What follows is what was true when it was first looked for, kept
because the search itself is worth recording: `main` is at
`6e64091` and unchanged, this branch's newest commit is this side's, there are no other branches
except `cline/android-app-implementation` from 18 August, no issues, and no comments on the pull
request. The only thing under `docs/inbox/ideas/` is the Game Stage spec, assessed on 20 August and
already recorded in `CHANGELOG.md`. Nothing has been acted on and nothing has been assumed.


### Awaiting

- Anything further the project owner sends; it is added here and sorted into §1–§3 by kind.
- Confirmation of the order below, and of the one open decision in `CRIT-5` (which aspect ratio the
  canvas uses).

---

## Order of work

The project owner named `CRIT-5` first. Everything else below is a recommendation, with the
reasoning visible, and can be reordered.

**1 — The editor becomes truthful, then gains tools.** One block, because all of it is the same
canvas and splitting it means drawing that canvas three times.

| # | Item | Why here |
| --- | --- | --- |
| 1 | `CRIT-5` device-ratio canvas | Named first by the project owner. Everything else in this block is drawn on it. **`testing` in `0.0.26-dev`.** |
| 2 | `BUG-9` square draws as a rectangle | Same drawing path. Fixed while it is open, not after. **`testing` in `0.0.26-dev`.** |
| 3 | `FEAT-11` grid and snapping | Cheapest of the three tools, and the one that makes dragging accurate. **`testing` in `0.0.26-dev`.** |
| 4 | `FEAT-12` type the numbers | Small once the canvas exists, and it answers the offset confusion directly. **`testing` in `0.0.26-dev`.** |
| 5 | `FEAT-10` window editor | Largest of the three, and the one with the most to explain on screen. Last in the block for that reason. **`testing` in `0.0.26-dev`.** |

**2 — The triggers.** Both small, both reported, neither touching the editor.

| # | Item | Why here |
| --- | --- | --- |
| 6 | `BUG-7` remove the clockwise sweep | Drawing only. No schema, no measurement needed. |
| 7 | `BUG-8` trigger response | Needs a device measurement and a schema field, so it follows rather than leads. |

**3 — The overlay and the screen it is given.**

| # | Item | Why here |
| --- | --- | --- |
| 8 | `BUG-1` + `BUG-2` cutout | One fault in two places. Makes a setting that already ships honest. |
| 9 | `BUG-3` `HOW-TO-EDIT.md`, `BUG-4` drop sensor-portrait | Both small, both reported. |

**4 — New ground.**

| # | Item | Why here |
| --- | --- | --- |
| 10 | `FEAT-3` test ground | The project owner's stated next priority, and it makes every later change checkable in one place. |
| 11 | `FEAT-1` face buttons as one plate | Asked for, self-contained, and it exercises the new window editor. |
| 12 | `CRIT-2` + `CRIT-3` home screen and modules | Together, because doing either first means moving the same code twice. |
| 13 | `FEAT-5` discovery and launch | The gap between here and the MVP flow, and the bulk of `CRIT-4`. |
| 14 | `CRIT-1` release signing key | Last in build order, first in importance, and it needs the project owner rather than the agent. |

**After `v0.1.0` unless pulled forward:** `FEAT-2` eight-way face pad, `FEAT-4` skins, `FEAT-6`
profiles, `FEAT-7` haptics, `FEAT-8` backend interface, `FEAT-9` community, `BUG-5` the unexplained
`"shape": "round"`, `BUG-6` Kestrel creating its own folder.
