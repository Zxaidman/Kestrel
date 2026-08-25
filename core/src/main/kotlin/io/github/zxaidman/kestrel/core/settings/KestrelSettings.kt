package io.github.zxaidman.kestrel.core.settings

import io.github.zxaidman.kestrel.core.common.Outcome
import io.github.zxaidman.kestrel.core.common.flatMap
import io.github.zxaidman.kestrel.core.configuration.ConfigNode
import io.github.zxaidman.kestrel.core.configuration.ConfigReader
import io.github.zxaidman.kestrel.core.configuration.ConfigurationError
import io.github.zxaidman.kestrel.core.configuration.DocumentHeader
import io.github.zxaidman.kestrel.core.configuration.DocumentType
import io.github.zxaidman.kestrel.core.configuration.Json
import io.github.zxaidman.kestrel.core.input.AnalogProfile
import io.github.zxaidman.kestrel.core.input.DeadzoneShape
import io.github.zxaidman.kestrel.core.storage.DocumentStore
import io.github.zxaidman.kestrel.core.storage.StoreFolder

/**
 * Everything Kestrel remembers between one run and the next.
 *
 * **A document, not a private key-value store**, and that is the whole point of it. It lives in the
 * folder the user chose, beside their layouts, where they can read it, edit it, copy it to another
 * phone, or keep it when they uninstall. Settings that can only be changed from inside the
 * application are settings that disappear with the application — which is exactly the problem this
 * exists to end.
 *
 * Every field has a default, and reading is **lenient about absence and strict about content**. A
 * settings file with a field missing is a file written by an older build, and refusing to start
 * because of one would make an upgrade a data loss. A field that is present and wrong is a
 * different thing and is reported.
 */
public data class KestrelSettings(
    /** How large the on-screen controls are drawn, as a fraction of the layout's own sizes. */
    public val controlScale: Double = DEFAULT_CONTROL_SCALE,

    /**
     * The same setting for portrait, because it is not the same question.
     *
     * A pad at 85% is right in landscape, where the thumbs are at the far corners of a wide screen.
     * Upright there is less width between them and more height above, so the size that fits the
     * grip is a different number — and one slider for both meant choosing which orientation to be
     * wrong in.
     */
    public val controlScalePortrait: Double = DEFAULT_CONTROL_SCALE,

    /** How the editor was last set up. Working state, kept because a hand should not reset it. */
    public val editor: EditorPreferences = EditorPreferences(),

    /** When an untouched pad gets out of the way. */
    public val idle: IdlePreferences = IdlePreferences(),

    /** The shaping applied to both sticks. */
    public val stickProfile: AnalogProfile = AnalogProfile.DEFAULT_STICK,

    /** Which layout the overlay draws. */
    public val layoutId: String = DEFAULT_LAYOUT_ID,

    /** How much of the screen Kestrel takes, and which way up it sits. */
    public val display: DisplayPreferences = DisplayPreferences(),

    /** Carried through so a newer build's settings survive being read by an older one. */
    public val unknownFields: Map<String, ConfigNode> = emptyMap(),
) {
    public companion object {
        public const val DOCUMENT_ID: String = "user.settings"
        public const val DOCUMENT_NAME: String = "settings.json"

        public const val DEFAULT_LAYOUT_ID: String = "builtin.xbox.default"

        /**
         * How large the controls are drawn, as a fraction of the layout's own sizes.
         *
         * **The maximum is the largest arrangement that still fits.** The shipped layout is
         * authored so that every control is inside the screen and clear of its neighbours at 100%,
         * in both orientations, and `BuiltInLayoutsTest` checks that at every scale on this range
         * rather than trusting it. A setting that can produce an overlapping pad is a setting that
         * will produce one.
         *
         * The default reproduces the arrangement settled by a hand on the reference device. It is
         * not 100% because a size everybody uses should have somewhere to grow.
         */
        public const val DEFAULT_CONTROL_SCALE: Double = 1.00
        public const val MIN_CONTROL_SCALE: Double = 0.50
        /**
         * As far as the size setting goes, and it is deliberately the same as the default.
         *
         * The project owner's judgement, and the measurement agrees with it: 200% was overdoing it,
         * and the shipped arrangement is clean to **1.03** — above that `R3` meets `Start`, and past
         * about 1.2 the left column runs into itself. A maximum the shipped layout cannot survive is
         * a maximum that ships a broken pad to anyone who drags the slider up.
         *
         * The cost, stated: the slider only goes down from the default. Moving `R3` about 0.02
         * further from `Start` would raise this to roughly 1.15, and that is the project owner's
         * arrangement to change rather than this side's.
         */
        public const val MAX_CONTROL_SCALE: Double = 1.00
    }
}

/** Reads and writes [KestrelSettings] as a document in a [DocumentStore]. */
public object SettingsDocument {

    private val KNOWN_FIELDS = setOf(
        "schemaVersion", "type", "id", "name",
        "controlScale", "controlScalePortrait", "scaleScheme", "editor", "idle", "layoutId", "stick",
        "display",
    )
    private val KNOWN_STICK_FIELDS = setOf(
        "deadzone", "outerLimit", "curve", "sensitivity", "invertX", "invertY", "deadzoneShape",
    )

    /**
     * Loads settings, or returns the defaults when there is nothing to load.
     *
     * A first run has no settings file and that is not an error — it is a first run. Only a file
     * that exists and cannot be understood is worth reporting, because that one means something the
     * user may want to fix rather than something Kestrel should quietly overwrite.
     */
    public fun load(store: DocumentStore): Outcome<KestrelSettings> {
        if (!store.exists(StoreFolder.ROOT, KestrelSettings.DOCUMENT_NAME)) {
            return Outcome.Success(KestrelSettings())
        }
        return store.read(StoreFolder.ROOT, KestrelSettings.DOCUMENT_NAME)
            .flatMap { Json.parse(it) }
            .flatMap { read(it) }
    }

    public fun save(store: DocumentStore, settings: KestrelSettings): Outcome<Unit> =
        store.write(
            StoreFolder.ROOT,
            KestrelSettings.DOCUMENT_NAME,
            Json.write(write(settings)),
        )

    public fun read(node: ConfigNode): Outcome<KestrelSettings> {
        val header = when (val h = DocumentHeader.read(node, DocumentType.SETTINGS)) {
            is Outcome.Failure -> return h
            is Outcome.Success -> h.value
        }
        // The header is validated and then not kept: settings have exactly one identity, so storing
        // the one read from the file would let a hand-edited id become the one Kestrel uses.
        check(header.type == DocumentType.SETTINGS)

        val obj = when (val o = ConfigReader.asObject(node)) {
            is Outcome.Failure -> return o
            is Outcome.Success -> o.value
        }

        val defaults = KestrelSettings()

        // Which scheme the stored sizes are in.
        //
        // The project owner reset what 100% means: what used to be 80% is now 100%, and the slider
        // runs from 50% to 200%. Every settings file already on a phone holds a number in the old
        // scheme, and reading one as if it were the new scheme would shrink somebody's pad by a
        // fifth without telling them — a silent change to a thing they can see.
        //
        // Absent means the old scheme, because that is what a file without the marker is.
        val storedScheme = when (
            val v = optionalNumber(obj, "scaleScheme", 1.0, SCALE_SCHEME.toDouble(), 1.0)
        ) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value.toInt()
        }

        // Checked against the range the number was written in, then converted. Validating an old
        // number against the new range would refuse a perfectly good 0.40, and validating it
        // against a loose range would report a limit that is not the real one — the message a user
        // sees has to name the limit that actually applies to what they typed.
        val old = storedScheme < SCALE_SCHEME
        val min = if (old) OLD_MIN_CONTROL_SCALE else KestrelSettings.MIN_CONTROL_SCALE
        val max = if (old) OLD_MAX_CONTROL_SCALE else KestrelSettings.MAX_CONTROL_SCALE

        fun scaleIn(field: String, fallback: Double): Outcome<Double> {
            // Absent means the default, and a default is already in the current scheme. Converting
            // it would move a setting nobody has ever touched.
            if (!obj.has(field)) return Outcome.Success(fallback)
            val raw = when (val v = ConfigReader.number(obj, field, min, max)) {
                is Outcome.Failure -> return v
                is Outcome.Success -> v.value
            }
            val converted = if (old) raw / SCALE_SCHEME_2_FACTOR else raw
            return Outcome.Success(
                converted.coerceIn(
                    KestrelSettings.MIN_CONTROL_SCALE, KestrelSettings.MAX_CONTROL_SCALE,
                )
            )
        }

        val scale = when (val v = scaleIn("controlScale", defaults.controlScale)) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value
        }

        val layoutId = when (val v = ConfigReader.optionalText(obj, "layoutId")) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value ?: defaults.layoutId
        }

        val stick = when (val v = readStick(obj, defaults.stickProfile)) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value
        }

        val display = when (val v = readDisplay(obj, defaults.display)) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value
        }

        return Outcome.Success(
            KestrelSettings(
                controlScale = scale,
                controlScalePortrait = when (
                    val v = scaleIn("controlScalePortrait", defaults.controlScalePortrait)
                ) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value
                },
                editor = when (val v = readEditor(obj, defaults.editor)) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value
                },
                idle = when (val v = readIdle(obj, defaults.idle)) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value
                },
                stickProfile = stick,
                layoutId = layoutId,
                display = display,
                unknownFields = obj.unknownFields(KNOWN_FIELDS),
            )
        )
    }

    public fun write(settings: KestrelSettings): ConfigNode {
        val stick = settings.stickProfile
        val fields = linkedMapOf<String, ConfigNode>(
            "schemaVersion" to ConfigNode.Num(DocumentHeader.CURRENT_SCHEMA_VERSION.toDouble()),
            "type" to ConfigNode.Text(DocumentType.SETTINGS.wireName),
            "id" to ConfigNode.Text(KestrelSettings.DOCUMENT_ID),
            "name" to ConfigNode.Text("Kestrel settings"),
            "controlScale" to ConfigNode.Num(settings.controlScale),
            "scaleScheme" to ConfigNode.Num(SCALE_SCHEME.toDouble()),
            "controlScalePortrait" to ConfigNode.Num(settings.controlScalePortrait),
            "idle" to ConfigNode.Obj(
                linkedMapOf(
                    "enabled" to ConfigNode.Bool(settings.idle.enabled),
                    "controlsSeconds" to ConfigNode.Num(settings.idle.controlsSeconds.toDouble()),
                    "toggleSeconds" to ConfigNode.Num(settings.idle.toggleSeconds.toDouble()),
                )
            ),
            "editor" to ConfigNode.Obj(
                linkedMapOf(
                    "gridUnit" to ConfigNode.Num(settings.editor.gridUnit),
                    "snapToGrid" to ConfigNode.Bool(settings.editor.snapToGrid),
                    "snapToEdges" to ConfigNode.Bool(settings.editor.snapToEdges),
                )
            ),
            "layoutId" to ConfigNode.Text(settings.layoutId),
            "display" to ConfigNode.Obj(
                linkedMapOf(
                    "fullScreen" to ConfigNode.Bool(settings.display.fullScreen),
                    "drawUnderCutout" to ConfigNode.Bool(settings.display.drawUnderCutout),
                    "orientation" to ConfigNode.Text(settings.display.orientation.wireName),
                    "theme" to ConfigNode.Text(settings.display.theme.wireName),
                    "trueBlack" to ConfigNode.Bool(settings.display.trueBlack),
                )
            ),
            "stick" to ConfigNode.Obj(
                linkedMapOf(
                    "deadzone" to ConfigNode.Num(stick.deadzone),
                    "outerLimit" to ConfigNode.Num(stick.outerLimit),
                    "curve" to ConfigNode.Num(stick.curve),
                    "sensitivity" to ConfigNode.Num(stick.sensitivity),
                    "invertX" to ConfigNode.Bool(stick.invertX),
                    "invertY" to ConfigNode.Bool(stick.invertY),
                    "deadzoneShape" to ConfigNode.Text(stick.deadzoneShape.name.lowercase()),
                )
            ),
        )
        // Anything a newer build wrote is put back, so an older build reading and rewriting this
        // file does not silently delete what it did not understand.
        settings.unknownFields.forEach { (key, value) -> fields.putIfAbsent(key, value) }
        return ConfigNode.Obj(fields)
    }

    private fun readDisplay(
        obj: ConfigNode.Obj,
        defaults: DisplayPreferences,
    ): Outcome<DisplayPreferences> {
        val display = when (val node = obj["display"]) {
            null, ConfigNode.Null -> return Outcome.Success(defaults)
            else -> when (val o = ConfigReader.asObject(node, "display")) {
                is Outcome.Failure -> return o
                is Outcome.Success -> o.value
            }
        }

        val orientation = if (!display.has("orientation")) {
            defaults.orientation
        } else {
            when (
                val v = ConfigReader.enum(
                    display, "orientation", AppOrientation.entries.toTypedArray(),
                    { it.wireName }, "display",
                )
            ) {
                is Outcome.Failure -> return v
                is Outcome.Success -> v.value
            }
        }

        // Read through `AppTheme.of`, which still understands the two names an earlier build
        // wrote, rather than through the enum reader, which would refuse them.
        val storedTheme = when (val v = ConfigReader.optionalText(display, "theme", "display")) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value
        }
        val theme = if (storedTheme == null) {
            defaults.theme
        } else {
            AppTheme.of(storedTheme) ?: return Outcome.Failure(
                ConfigurationError.UnknownValue(
                    "display.theme",
                    storedTheme,
                    AppTheme.entries.map { it.wireName }.toSet(),
                )
            )
        }
        val trueBlackDefault = storedTheme?.let { AppTheme.trueBlackFrom(it) } ?: defaults.trueBlack

        return Outcome.Success(
            DisplayPreferences(
                fullScreen = when (
                    val v = ConfigReader.boolean(display, "fullScreen", defaults.fullScreen, "display")
                ) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value
                },
                drawUnderCutout = when (
                    val v = ConfigReader.boolean(
                        display, "drawUnderCutout", defaults.drawUnderCutout, "display",
                    )
                ) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value
                },
                orientation = orientation,
                theme = theme,
                trueBlack = when (
                    val v = ConfigReader.boolean(display, "trueBlack", trueBlackDefault, "display")
                ) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value
                },
            )
        )
    }

    private fun readIdle(
        obj: ConfigNode.Obj,
        defaults: IdlePreferences,
    ): Outcome<IdlePreferences> {
        val idle = when (val node = obj["idle"]) {
            null, ConfigNode.Null -> return Outcome.Success(defaults)
            else -> when (val o = ConfigReader.asObject(node, "idle")) {
                is Outcome.Failure -> return o
                is Outcome.Success -> o.value
            }
        }
        return Outcome.Success(
            IdlePreferences(
                enabled = when (
                    val v = ConfigReader.boolean(idle, "enabled", defaults.enabled, "idle")
                ) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value
                },
                controlsSeconds = when (
                    val v = optionalNumber(
                        idle, "controlsSeconds",
                        IdlePreferences.MIN_SECONDS.toDouble(),
                        IdlePreferences.MAX_SECONDS.toDouble(),
                        defaults.controlsSeconds.toDouble(), "idle",
                    )
                ) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value.toInt()
                },
                toggleSeconds = when (
                    val v = optionalNumber(
                        idle, "toggleSeconds",
                        IdlePreferences.MIN_SECONDS.toDouble(),
                        IdlePreferences.MAX_SECONDS.toDouble(),
                        defaults.toggleSeconds.toDouble(), "idle",
                    )
                ) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value.toInt()
                },
            )
        )
    }

    private fun readEditor(
        obj: ConfigNode.Obj,
        defaults: EditorPreferences,
    ): Outcome<EditorPreferences> {
        val editor = when (val node = obj["editor"]) {
            null, ConfigNode.Null -> return Outcome.Success(defaults)
            else -> when (val o = ConfigReader.asObject(node, "editor")) {
                is Outcome.Failure -> return o
                is Outcome.Success -> o.value
            }
        }
        return Outcome.Success(
            EditorPreferences(
                gridUnit = when (
                    val v = optionalNumber(
                        editor, "gridUnit",
                        EditorPreferences.MIN_GRID, EditorPreferences.MAX_GRID,
                        defaults.gridUnit, "editor",
                    )
                ) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value
                },
                snapToGrid = when (
                    val v = ConfigReader.boolean(
                        editor, "snapToGrid", defaults.snapToGrid, "editor",
                    )
                ) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value
                },
                snapToEdges = when (
                    val v = ConfigReader.boolean(
                        editor, "snapToEdges", defaults.snapToEdges, "editor",
                    )
                ) {
                    is Outcome.Failure -> return v
                    is Outcome.Success -> v.value
                },
            )
        )
    }

    private fun readStick(obj: ConfigNode.Obj, defaults: AnalogProfile): Outcome<AnalogProfile> {
        val stick = when (val node = obj["stick"]) {
            null, ConfigNode.Null -> return Outcome.Success(defaults)
            is ConfigNode.Obj -> node
            // Present and not an object: a real mistake in the file, reported rather than ignored.
            else -> return when (val o = ConfigReader.asObject(node, "stick")) {
                is Outcome.Failure -> o
                is Outcome.Success -> Outcome.Success(defaults)
            }
        }

        fun number(field: String, min: Double, max: Double, fallback: Double): Outcome<Double> =
            optionalNumber(stick, field, min, max, fallback, "stick")

        val deadzone = when (val v = number("deadzone", 0.0, 0.9, defaults.deadzone)) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value
        }
        val outerLimit = when (val v = number("outerLimit", 0.1, 1.0, defaults.outerLimit)) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value
        }
        val curve = when (val v = number("curve", 0.2, 5.0, defaults.curve)) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value
        }
        val sensitivity = when (val v = number("sensitivity", 0.1, 3.0, defaults.sensitivity)) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value
        }
        val invertX = when (val v = ConfigReader.boolean(stick, "invertX", defaults.invertX, "stick")) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value
        }
        val invertY = when (val v = ConfigReader.boolean(stick, "invertY", defaults.invertY, "stick")) {
            is Outcome.Failure -> return v
            is Outcome.Success -> v.value
        }
        val shape = if (!stick.has("deadzoneShape")) {
            defaults.deadzoneShape
        } else {
            when (
                val v = ConfigReader.enum(
                    stick, "deadzoneShape", DeadzoneShape.entries.toTypedArray(),
                    { it.name.lowercase() }, "stick",
                )
            ) {
                is Outcome.Failure -> return v
                is Outcome.Success -> v.value
            }
        }

        // Unknown fields inside the stick are dropped rather than carried. They are numbers with no
        // home in AnalogProfile, and pretending to preserve them would mean claiming a fidelity
        // this shape cannot offer.
        check(KNOWN_STICK_FIELDS.isNotEmpty())

        return Outcome.Success(
            AnalogProfile(deadzone, outerLimit, curve, sensitivity, invertX, invertY, shape)
        )
    }

    /**
     * The scheme the numbers in this file are in.
     *
     * `1` is what every file written before this change holds, whether or not it says so. `2` is
     * the project owner's scheme, where the old 80% is 100%.
     */
    private const val SCALE_SCHEME = 2

    /** What the old numbers are divided by to mean the same size in the new scheme. */
    private const val SCALE_SCHEME_2_FACTOR = 0.80

    /** The range the old scheme's numbers were written and validated in. */
    private const val OLD_MIN_CONTROL_SCALE = 0.40
    private const val OLD_MAX_CONTROL_SCALE = 1.00

    /** Absent means the default; present means it has to be right. */
    private fun optionalNumber(
        obj: ConfigNode.Obj,
        field: String,
        min: Double,
        max: Double,
        fallback: Double,
        path: String = "",
    ): Outcome<Double> =
        if (!obj.has(field)) Outcome.Success(fallback) else ConfigReader.number(obj, field, min, max, path)
}
