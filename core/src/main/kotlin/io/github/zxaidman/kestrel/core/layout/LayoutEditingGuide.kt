package io.github.zxaidman.kestrel.core.layout

/**
 * The guide written beside a user's layouts, because JSON cannot carry a comment.
 *
 * The project owner copied a layout and reported the offsets as "a little confusing", which they
 * are: `offsetX: 0.24` says nothing about where a control is unless you already know it is measured
 * inwards from an anchor as a fraction of the screen's shorter side. A schema document in the
 * repository does not help somebody holding a phone with a file manager open.
 *
 * **In the domain, not on a screen, because that is what fixed `BUG-3`.** It used to be written by
 * one button — *Copy layout to my folder* — and the project owner reached the editor through *Edit
 * layout*, which writes a copy by a different path and never called it. The guide was correct code
 * on the wrong path. It is written by [LayoutRepository] now, so every route that puts a layout in
 * the user's folder puts this beside it, including any route added later.
 *
 * Overwritten each time, so it describes the build that wrote it rather than an older one.
 */
public object LayoutEditingGuide {

    /** The file the guide is written to, beside the layouts it describes. */
    public const val FILE_NAME: String = "HOW-TO-EDIT.md"

    private val LINE_END: String = System.lineSeparator()

    public fun text(): String =
        """
        # Editing a Kestrel layout

        Each file in this folder is one controller layout. Edit it in any text editor, then press
        **Reload layout** in Kestrel — or use **Edit layout**, which does the same thing by dragging.

        ## Where a control is

        Position is **not** measured from the corner of the screen in pixels. It is measured from an
        **anchor**, so a layout made on one phone still fits a hand on a different one.

        - `anchor` — the edge or corner the control is pinned to: `bottom-left`, `bottom-right`,
          `top-left`, `top-right`, `bottom-center`, `top-center`, `center-left`, `center-right`.
        - `offsetX`, `offsetY` — how far the control's **centre** sits **inwards from that anchor**.

        Inwards is the part that surprises people. A control anchored `bottom-right` with larger
        offsets moves **left and up**, towards the middle. That is what makes the same numbers work
        in portrait and landscape: they always mean "this far in from my own corner".

        ## What the numbers are measured in

        Fractions of the screen's **shorter side** — never pixels, and never the longer side. So
        `width: 0.13` is thirteen hundredths of the short edge, which is the same physical size
        whichever way the phone is held. Two decimals is as fine as anything needs to be; it is
        about one pixel on a 1080-wide screen.

        ## Every field on a control

        | Field | What it does |
        | --- | --- |
        | `id` | Its name in this file. Must be unique. |
        | `kind` | What it **is**: `button`, `dpad`, `stick`, `analog-trigger`, `digital-trigger`, `decoration`. |
        | `binds` | Which control on the pad it drives, such as `a`, `left-stick`, `left-trigger`. |
        | `label` | What is drawn on it. `null` uses the control's own name. |
        | `group` | Controls sharing a group share one window — **and only controls in one window can be slid between**. That is what lets a thumb roll across the face buttons, or hold `L3` and then move the stick. `null` means a window of its own. |
        | `shape` | `circle`, `square` or `rectangle`. It decides where the control can be **pressed**, not only how it is drawn. |
        | `anchor`, `offsetX`, `offsetY` | Where it is, as above. |
        | `width`, `height` | How big, as fractions of the short side. |
        | `rotation` | Degrees clockwise. Affects pressing as well as drawing. |

        ## Rules Kestrel will not let you break

        - A `stick` must bind to a stick and a `dpad` to the pad. A stick bound to `a` draws
          correctly and does nothing, so it is refused instead.
        - A `decoration` must not bind to anything. Artwork that sends input is a mislabelled
          control.
        - Two controls cannot share an `id`.
        - Sticks and pads are always drawn round, whatever `shape` says.

        If a file cannot be read, **Reload layout** says why and names the field. Nothing is changed
        until it can be read, so a mistake here costs a reload rather than a layout.
    """.trimIndent() + LINE_END
}
