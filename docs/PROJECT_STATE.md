# Kestrel — Project State

**Document:** `docs/PROJECT_STATE.md`
**Status:** Active — **canonical** for where the project actually is
**Owner of:** the phase position, what is verified, and what is still assumed

---

## How to read this

`CLAUDE.md` §1 and `README.md` both summarise this document in a paragraph and point here. **If they
disagree with this file, this file wins and the other is corrected** — the same single-source rule
that governs folder placement in `PROJECT_STRUCTURE.md`.

The neighbouring documents own different questions, and this one does not repeat them:

| Question | Owner |
| --- | --- |
| What is queued, and in what order | `todo-list.md` |
| What is finished, and what finished means for each item | `done-list.md` |
| What was established, build by build | `CHANGELOG.md` |
| Where a file belongs | `PROJECT_STRUCTURE.md` |
| Why a choice was made | `docs/adr/` |

**Verification vocabulary** (`AI_DEVELOPMENT_GUIDE.md`): `Unverified` · `Experimental` · `Tested` ·
`Supported`. A claim here carries one, and "it compiles" is not one of them.

**Reference device for every "Tested" claim:** Xiaomi Redmi Note 13 5G, HyperOS 3.0.3, Android 15,
Shizuku shell (uid 2000), no root. **One device, one firmware, one person testing.** Nothing here is
a claim about other hardware.

---

## Where the project is — build `0.0.44-dev`

**Kestrel is a working virtual controller with a working layout editor, and is not yet an
application anybody could be handed.** Both halves of that sentence matter.

What a person can do today, on the reference device: start a session, get a virtual gamepad on
screen over another application, arrange every control by dragging or by typing numbers, keep
separate arrangements for landscape and portrait, tune stick shaping and trigger feel, and prove
every control end to end on a test screen.

What they cannot do: **launch a game from Kestrel.** There is no home screen, no application
discovery, and no launcher. The product's front door has not been built.

---

## Phase position

`PRD.md` §27–§34 and `CLAUDE.md` §6 define eight phases. **The work has not followed them in
order**, and that is a deliberate consequence of how it has been driven — the project owner tests
each build on a real device and directs the next one, so the parts that could be felt got built
first. This table is the honest position rather than the planned one.

| Phase | Position | Evidence |
| --- | --- | --- |
| **0 — Input feasibility** | **Complete** | `Tested`. `ADR-INPUT-001` Accepted, scoped to the reference device. `docs/phase0/results/` |
| **1 — Core application** | **Part built, and it is the gap** | Configuration, JSON schema, settings, storage and the layout repository are `Tested`. **Application shell, navigation, discovery and launching do not exist** — `CRIT-2`, `CRIT-3`, `FEAT-5` |
| **2 — Controller engine** | **Largely built** | `Tested`: overlay windows, touch model, analog shaping, two-rate triggers, idle fade and hide, per-orientation scale |
| **3 — Layout editor** | **Built, and the most exercised part of the product** | `Tested` across twenty build-and-test rounds. Canvas, grid, snapping, anchors, windows, typed values, per-orientation save |
| **4 — Gaming session** | **Part built** | Session service, notification and lease exist. `Tested` as a session; not as a *gaming* session, because there is nothing to launch |
| **5 — Shizuku** | **Part built** | Capability binding and privilege detection exist. `Tested` on the reference device only |
| **6 — Skins** | **Not started** | Artwork licensed and assessed (CC0). `docs/SKIN_ASSETS.md` |
| **7 — Community** | **Not started** | — |

**Warning, and it is the main risk this table carries.** Phases 2 and 3 running ahead of Phase 1
means the editor and the overlay were built against a diagnostics screen rather than a product
shell. `CRIT-2` and `CRIT-3` — the home screen and the module restructure — therefore cost more now
than they would have cost early, and the cost grows with every round that adds code. That is why
they are Build 2 in the re-sorted order of work and not later.

---

## Testing phase

**`0.0.44-dev` is a development build, not a release candidate.** It is debug-signed, its version
is below `0.1.0`, and `CRIT-1` — a release signing key that is not committed to this repository —
is open. **Do not treat any build from this branch as distributable.**

### How a change is verified here

The loop is: build → install on the reference device → the project owner tests against a written
procedure → results recorded in `todo-list.md` → the next build. Twenty rounds have run this way,
from `0.0.25-dev` to `0.0.44-dev`.

| Level | State |
| --- | --- |
| **Unit tests** (`:core`, JVM) | **272 tests, passing.** Geometry, JSON, schema validation, layout rules, analog shaping, capability selection, control proofs |
| **Lint** | Passing; lint errors fail the build by design |
| **Instrumentation tests** | **None.** Nothing automated covers lifecycle, services or overlays |
| **Device tests** | **Manual only**, by the project owner, on one device, against a written procedure per build |
| **Other devices** | **None.** Everything outside the reference device is `Untested` |
| **Latency** | **Never measured.** Claimed nowhere |

### What is awaiting a device result right now

Build 1 of the re-sorted order, shipped in `0.0.44-dev` and `Unverified`:

- `FEAT-3` — the test ground
- `BUG-3` — the editing guide written on every path that writes a layout
- `BUG-4` — `sensor-portrait` removed, old files still readable
- `BUG-54` — a window can no longer outgrow the screen
- `FEAT-67` — rows labelled at the row
- `FEAT-68` — the measured trigger defaults

### The honest limits

- **One device, one firmware, one tester.** Every "working" in this repository means working there.
- **No automated coverage of anything Android-side.** The unit tests prove the domain and nothing
  about a phone.
- **No latency figure exists.** Phase 0 proved that input arrives, not how fast.
- **No fallback for a user without Shizuku has been tested at all.** `ADR-006` was measured and
  rejected on product grounds; nothing replaced it.
- **`BUG-30` is unfixable and recorded as such**: controls under the system bars get no touches,
  because the bars are the system's own windows above every overlay.

---

## What comes next

The order of work is in `todo-list.md` and was re-sorted around what a round costs rather than what
is most wanted. In short:

1. **Build 1** — the test ground, so every later round is cheaper to confirm. *Shipped, awaiting a
   device result.*
2. **Build 2** — `CRIT-3` modules and `CRIT-2` home screen, together. Changes nothing visible, and
   delaying it compounds.
3. **Build 3** — the small editor batch, sharing one schema version bump.
4. **Build 4** — `FEAT-5` discovery and launching, which is the gap between here and a product.

**Running alongside, and needing the project owner rather than an agent:** `CRIT-1` the release
signing key, and `CRIT-4` deciding what `v0.1.0` contains.
