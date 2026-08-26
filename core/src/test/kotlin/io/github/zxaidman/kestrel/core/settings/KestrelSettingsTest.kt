package io.github.zxaidman.kestrel.core.settings

import io.github.zxaidman.kestrel.core.common.Outcome
import io.github.zxaidman.kestrel.core.configuration.ConfigNode
import io.github.zxaidman.kestrel.core.configuration.ConfigurationError
import io.github.zxaidman.kestrel.core.configuration.Json
import io.github.zxaidman.kestrel.core.input.AnalogProfile
import io.github.zxaidman.kestrel.core.input.DeadzoneShape
import io.github.zxaidman.kestrel.core.storage.MemoryDocumentStore
import io.github.zxaidman.kestrel.core.storage.StoreFolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KestrelSettingsTest {

    private fun <T> value(outcome: Outcome<T>): T {
        assertTrue(outcome is Outcome.Success, "expected success, got $outcome")
        return (outcome as Outcome.Success).value
    }

    private fun error(outcome: Outcome<*>) = (outcome as Outcome.Failure).error

    private fun parse(text: String) = SettingsDocument.read(value(Json.parse(text)))

    // --- how a trigger travels ------------------------------------------------------------------

    /**
     * `FEAT-66`. The ramp is a feel preference and lives beside the stick shaping, not in a layout
     * document — `BUG-8` planned the other home, which would have cost a schema version bump and a
     * migration for something that is not a property of an arrangement.
     */
    @Test
    fun `the trigger ramp survives being written and read again`() {
        val settings = KestrelSettings(
            trigger = TriggerPreferences(
                quickSeconds = 0.08, travelSeconds = 0.42, releaseSeconds = 0.25,
            )
        )
        val text = Json.write(SettingsDocument.write(settings))
        val back = value(parse(text))
        assertEquals(0.08, back.trigger.quickSeconds)
        assertEquals(0.42, back.trigger.travelSeconds)
        assertEquals(0.25, back.trigger.releaseSeconds)
        assertTrue("\"quickSeconds\": 0.08" in text, "padded to two places:\n$text")
    }

    /** An older file has no `trigger` at all, and that is an absence rather than a fault. */
    @Test
    fun `settings written before the trigger existed still load`() {
        val text = """
            {
              "schemaVersion": 1,
              "type": "settings",
              "id": "user.settings",
              "name": "Kestrel settings",
              "controlScale": 1.0
            }
        """.trimIndent()
        val back = value(parse(text))
        assertEquals(TriggerPreferences(), back.trigger)
    }

    /** A ramp of nothing is a step, which is what the two-rate shape exists to avoid. */
    @Test
    fun `a trigger duration outside the range is refused`() {
        val text = """
            {
              "schemaVersion": 1,
              "type": "settings",
              "id": "user.settings",
              "name": "Kestrel settings",
              "trigger": { "quickSeconds": 0.0 }
            }
        """.trimIndent()
        assertTrue(error(parse(text)) is ConfigurationError.OutOfRange)
    }

    // --- the file the project owner had, and why it would not load -----------------------------

    /**
     * `BUG-50`, in the exact form it was reported. `Float` cannot represent 1.2, so a slider whose
     * maximum is 1.2 hands back a value whose `Double` is 1.2000000476837158 — and a strict bound
     * refused it, taking every other setting in the document with it.
     */
    @Test
    fun `a maximum that came back through a Float still loads`() {
        val text = """
            {
              "schemaVersion": 1,
              "type": "settings",
              "id": "user.settings",
              "name": "Kestrel settings",
              "controlScale": 1.2000000476837158,
              "scaleScheme": 2,
              "controlScalePortrait": 1.2000000476837158,
              "layoutId": "user.xbox"
            }
        """.trimIndent()

        val settings = value(parse(text))
        assertEquals(KestrelSettings.MAX_CONTROL_SCALE, settings.controlScale)
        assertEquals(KestrelSettings.MAX_CONTROL_SCALE, settings.controlScalePortrait)
        assertEquals("user.xbox", settings.layoutId, "the rest of the document must survive too")
    }

    /** The tolerance is a millionth, not an amnesty. A value a user could mean is still refused. */
    @Test
    fun `a size well past the ceiling is still refused, with the real limits named`() {
        val text = """
            {
              "schemaVersion": 1,
              "type": "settings",
              "id": "user.settings",
              "name": "Kestrel settings",
              "controlScale": 1.25,
              "scaleScheme": 2
            }
        """.trimIndent()

        val failed = error(parse(text))
        assertTrue(failed is ConfigurationError.OutOfRange, "got $failed")
    }

    /** Nothing float-shaped reaches the file, whichever path put the number in memory. */
    @Test
    fun `a scale carrying float error is written to the precision the slider offers`() {
        val written = SettingsDocument.write(
            KestrelSettings(controlScale = 1.2f.toDouble(), controlScalePortrait = 0.85f.toDouble())
        )
        val obj = written as ConfigNode.Obj
        assertEquals(1.2, (obj["controlScale"] as ConfigNode.Num).value)
        assertEquals(0.85, (obj["controlScalePortrait"] as ConfigNode.Num).value)
    }

    // --- the round trip that makes settings survive an uninstall --------------------------------

    @Test
    fun `settings written to a store come back as what they were`() {
        val store = MemoryDocumentStore()
        val settings = KestrelSettings(
            controlScale = 0.8,
            stickProfile = AnalogProfile(
                deadzone = 0.15,
                outerLimit = 0.95,
                curve = 1.4,
                sensitivity = 1.2,
                invertX = true,
                invertY = false,
                deadzoneShape = DeadzoneShape.AXIAL,
            ),
            layoutId = "user.my-layout",
        )

        value(SettingsDocument.save(store, settings))
        assertEquals(settings, value(SettingsDocument.load(store)))
    }

    @Test
    fun `a first run has no settings file, which is not an error`() {
        assertEquals(KestrelSettings(), value(SettingsDocument.load(MemoryDocumentStore())))
    }

    @Test
    fun `the settings file lands in the folder root, where a person can find it`() {
        val store = MemoryDocumentStore()
        value(SettingsDocument.save(store, KestrelSettings()))
        assertEquals(listOf("settings.json"), value(store.list(StoreFolder.ROOT)))
    }

    @Test
    fun `what is written is readable by a person, not a blob`() {
        val text = Json.write(SettingsDocument.write(KestrelSettings(controlScale = 0.8)))
        assertTrue(text.contains("\"controlScale\": 0.8"), text)
        assertTrue(text.contains("\"type\": \"settings\""), text)
        assertTrue(text.contains("\n"), "settings were written as one line")
    }

    // --- reading somebody else's file, or an older one ------------------------------------------

    @Test
    fun `a missing field is an older build's file, not a broken one`() {
        // Refusing to start because a field was added would make an upgrade a data loss.
        val settings = value(
            parse("""{"schemaVersion":1,"type":"settings","id":"user.settings","name":"S"}""")
        )
        assertEquals(KestrelSettings(), settings)
    }

    @Test
    fun `a field that is present and wrong is reported rather than ignored`() {
        val outcome = parse(
            """{"schemaVersion":1,"type":"settings","id":"user.settings","name":"S",
               "scaleScheme": 2, "controlScale": 40}"""
        )
        val error = error(outcome)
        assertTrue(error is ConfigurationError.OutOfRange, "got $error")
        assertEquals(KestrelSettings.MAX_CONTROL_SCALE, (error as ConfigurationError.OutOfRange).max)
    }

    @Test
    fun `a size written by an older build is judged by the limit it was written under`() {
        // The message a user sees has to name the limit that applied to what they typed. A file
        // from before the scheme changed says 0.40 is allowed, and telling its author that the
        // minimum is 0.50 would be telling them about a rule their file predates.
        val error = error(
            parse(
                """{"schemaVersion":1,"type":"settings","id":"user.settings","name":"S",
                   "controlScale": 40}"""
            )
        )
        assertTrue(error is ConfigurationError.OutOfRange, "got $error")
        assertEquals(1.0, (error as ConfigurationError.OutOfRange).max)
    }

    @Test
    fun `a size written by an older build means the same size after the scheme changed`() {
        // 0.80 was the size the project owner settled on, and 100% is defined as that size. A file
        // holding 0.80 must come back as 1.00, or upgrading silently shrinks somebody's pad.
        val settings = value(
            parse(
                """{"schemaVersion":1,"type":"settings","id":"user.settings","name":"S",
                   "controlScale": 0.80, "controlScalePortrait": 0.40}"""
            )
        )
        assertEquals(1.00, settings.controlScale, 1e-9)
        assertEquals(0.50, settings.controlScalePortrait, 1e-9)
    }

    @Test
    fun `a size written in the current scheme is left alone`() {
        val settings = value(
            parse(
                """{"schemaVersion":1,"type":"settings","id":"user.settings","name":"S",
                   "scaleScheme": 2, "controlScale": 0.90}"""
            )
        )
        assertEquals(0.90, settings.controlScale, 1e-9)
    }

    @Test
    fun `a stick field that is present and wrong is reported with its path`() {
        val outcome = parse(
            """{"schemaVersion":1,"type":"settings","id":"user.settings","name":"S",
               "stick": {"deadzone": 5}}"""
        )
        val error = error(outcome)
        assertTrue(error is ConfigurationError.OutOfRange, "got $error")
        assertEquals("stick.deadzone", (error as ConfigurationError.OutOfRange).path)
    }

    @Test
    fun `a partial stick keeps the defaults for everything it does not mention`() {
        val settings = value(
            parse("""{"schemaVersion":1,"type":"settings","id":"user.settings","name":"S",
                     "stick": {"deadzone": 0.2}}""")
        )
        val defaults = AnalogProfile.DEFAULT_STICK
        assertEquals(0.2, settings.stickProfile.deadzone)
        assertEquals(defaults.curve, settings.stickProfile.curve)
        assertEquals(defaults.deadzoneShape, settings.stickProfile.deadzoneShape)
    }

    @Test
    fun `a document of the wrong type is refused`() {
        val outcome = parse(
            """{"schemaVersion":1,"type":"controller-layout","id":"user.settings","name":"S"}"""
        )
        assertTrue(outcome is Outcome.Failure)
    }

    @Test
    fun `a settings file from a future schema is refused as unsupported`() {
        val outcome = parse(
            """{"schemaVersion":99,"type":"settings","id":"user.settings","name":"S"}"""
        )
        assertTrue(error(outcome) is ConfigurationError.UnsupportedSchemaVersion)
    }

    @Test
    fun `a file that is not JSON at all is reported as such`() {
        val store = MemoryDocumentStore()
        value(store.write(StoreFolder.ROOT, KestrelSettings.DOCUMENT_NAME, "not json"))
        assertTrue(error(SettingsDocument.load(store)) is ConfigurationError.MalformedDocument)
    }

    // --- forward compatibility -------------------------------------------------------------------

    @Test
    fun `a field a newer build wrote survives being read and written by this one`() {
        val store = MemoryDocumentStore()
        value(
            store.write(
                StoreFolder.ROOT,
                KestrelSettings.DOCUMENT_NAME,
                """{"schemaVersion":1,"type":"settings","id":"user.settings","name":"S",
                   "controlScale":0.7,"hapticStrength":"firm"}""",
            )
        )

        val settings = value(SettingsDocument.load(store))
        assertEquals(ConfigNode.Text("firm"), settings.unknownFields["hapticStrength"])

        // The half that matters: writing it back must not delete it.
        value(SettingsDocument.save(store, settings))
        val text = value(store.read(StoreFolder.ROOT, KestrelSettings.DOCUMENT_NAME))
        assertTrue(text.contains("hapticStrength"), text)
    }

    @Test
    fun `an unknown field never overwrites a field this build owns`() {
        val settings = KestrelSettings(
            controlScale = 0.5,
            unknownFields = mapOf("controlScale" to ConfigNode.Num(9.0)),
        )
        val written = SettingsDocument.write(settings) as ConfigNode.Obj
        assertEquals(ConfigNode.Num(0.5), written["controlScale"])
    }

    @Test
    fun `a theme survives being written and read back`() {
        val settings = KestrelSettings(
            display = DisplayPreferences(theme = AppTheme.DARK, trueBlack = true),
        )
        val written = SettingsDocument.write(settings) as ConfigNode.Obj
        val read = value(SettingsDocument.read(written))
        assertEquals(AppTheme.DARK, read.display.theme)
        assertTrue(read.display.trueBlack)
    }

    @Test
    fun `the theme names an earlier build wrote still read`() {
        // Every phone that ran 0.0.30-dev has one of these in its settings file. A file Kestrel
        // wrote must never become a file Kestrel refuses.
        fun themeOf(stored: String) = value(
            SettingsDocument.read(
                ConfigNode.Obj(
                    linkedMapOf(
                        "schemaVersion" to ConfigNode.Num(1.0),
                        "type" to ConfigNode.Text("settings"),
                        "id" to ConfigNode.Text("user.settings"),
                        "name" to ConfigNode.Text("S"),
                        "display" to ConfigNode.Obj(
                            linkedMapOf("theme" to ConfigNode.Text(stored))
                        ),
                    )
                )
            )
        ).display

        assertEquals(AppTheme.DARK, themeOf("dark-grey").theme)
        assertFalse(themeOf("dark-grey").trueBlack)

        assertEquals(AppTheme.DARK, themeOf("dark-amoled").theme)
        assertTrue(themeOf("dark-amoled").trueBlack, "the black someone chose was thrown away")
    }

    @Test
    fun `a settings file written before themes existed keeps the default`() {
        // Every file already on a phone is one of these. Reading one must not fail and must not
        // silently pick a theme the user never chose.
        val read = value(
            SettingsDocument.read(
                ConfigNode.Obj(
                    linkedMapOf(
                        "schemaVersion" to ConfigNode.Num(1.0),
                        "type" to ConfigNode.Text("settings"),
                        "id" to ConfigNode.Text("user.settings"),
                        "name" to ConfigNode.Text("S"),
                        "display" to ConfigNode.Obj(
                            linkedMapOf("fullScreen" to ConfigNode.Bool(true))
                        ),
                    )
                )
            )
        )
        assertEquals(AppTheme.SYSTEM, read.display.theme)
    }

    @Test
    fun `a theme this build does not know is refused rather than guessed at`() {
        val outcome = SettingsDocument.read(
            ConfigNode.Obj(
                linkedMapOf(
                    "schemaVersion" to ConfigNode.Num(1.0),
                    "type" to ConfigNode.Text("settings"),
                    "id" to ConfigNode.Text("user.settings"),
                    "name" to ConfigNode.Text("S"),
                    "display" to ConfigNode.Obj(
                        linkedMapOf("theme" to ConfigNode.Text("sepia"))
                    ),
                )
            )
        )
        assertTrue(outcome is Outcome.Failure, "an unknown theme was accepted: $outcome")
    }
}
