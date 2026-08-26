package io.github.zxaidman.kestrel.core.configuration

import io.github.zxaidman.kestrel.core.common.Outcome

/**
 * Typed reads from a parsed document, each returning an error rather than throwing.
 *
 * Every function here answers one of the checks `docs/CONFIGURATION_SCHEMA.md` requires on import —
 * required fields, field types, numeric ranges, enum values, collection sizes — and answers it the
 * same way everywhere, so a malformed document produces the same class of error whichever field it
 * is malformed in.
 */
public object ConfigReader {

    private fun kindOf(node: ConfigNode): String = when (node) {
        is ConfigNode.Text -> "text"
        is ConfigNode.Num -> "a number"
        is ConfigNode.Bool -> "true or false"
        is ConfigNode.Arr -> "a list"
        is ConfigNode.Obj -> "an object"
        ConfigNode.Null -> "empty"
    }

    private fun <T> missing(path: FieldPath): Outcome<T> =
        Outcome.Failure(ConfigurationError.MissingField(path))

    private fun <T> wrongType(path: FieldPath, expected: String, node: ConfigNode): Outcome<T> =
        Outcome.Failure(ConfigurationError.WrongType(path, expected, kindOf(node)))

    /** The document itself, which must be an object before anything else can be true of it. */
    public fun asObject(node: ConfigNode, path: FieldPath = ""): Outcome<ConfigNode.Obj> =
        when (node) {
            is ConfigNode.Obj -> Outcome.Success(node)
            else -> Outcome.Failure(ConfigurationError.NotADocument(path))
        }

    public fun text(obj: ConfigNode.Obj, field: String, path: FieldPath = ""): Outcome<String> {
        val at = path.child(field)
        return when (val node = obj[field]) {
            null, ConfigNode.Null -> missing(at)
            is ConfigNode.Text -> Outcome.Success(node.value)
            else -> wrongType(at, "text", node)
        }
    }

    public fun optionalText(obj: ConfigNode.Obj, field: String, path: FieldPath = ""): Outcome<String?> =
        when (val node = obj[field]) {
            null, ConfigNode.Null -> Outcome.Success(null)
            is ConfigNode.Text -> Outcome.Success(node.value)
            else -> wrongType(path.child(field), "text", node)
        }

    /**
     * A whole number.
     *
     * JSON has one number type, so `1.0` and `1` are the same value and both are accepted; `1.5`
     * is not, because a schema version or a count that is not whole is a different mistake and
     * deserves to be named as one.
     */
    public fun integer(obj: ConfigNode.Obj, field: String, path: FieldPath = ""): Outcome<Int> {
        val at = path.child(field)
        return when (val node = obj[field]) {
            null, ConfigNode.Null -> missing(at)
            is ConfigNode.Num ->
                if (node.value % 1.0 == 0.0 && node.value in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()) {
                    Outcome.Success(node.value.toInt())
                } else {
                    wrongType(at, "a whole number", node)
                }
            else -> wrongType(at, "a whole number", node)
        }
    }

    /** A number that must fall within a range — coordinates, opacity, rotation. */
    /**
     * How far outside a range a number may be and still be read as the boundary.
     *
     * A millionth. Small enough that nothing a person could have typed differently slips through,
     * large enough to cover the `Float`-to-`Double` error every slider value carries.
     */
    public const val EDGE_TOLERANCE: Double = 1e-6

    public fun number(
        obj: ConfigNode.Obj,
        field: String,
        min: Double,
        max: Double,
        path: FieldPath = "",
    ): Outcome<Double> {
        val at = path.child(field)
        return when (val node = obj[field]) {
            null, ConfigNode.Null -> missing(at)
            is ConfigNode.Num -> {
                val found = node.value
                when {
                    found in min..max -> Outcome.Success(found)
                    // Outside by less than a rounding error is at the boundary, not out of range.
                    //
                    // `Float` cannot represent most two-decimal values. A slider that offers 1.2 as
                    // its maximum hands back a `Float` whose `Double` is 1.2000000476837158, and a
                    // strict comparison refuses it — refusing the whole document over a difference
                    // no user typed and none can see. That happened, to a settings file, and every
                    // setting in it reverted (`BUG-50`).
                    //
                    // This is not a loosening of validation. A value a millionth outside a bound is
                    // read *as* that bound, so nothing out of range ever reaches a caller; anything
                    // a user could have meant differently is still refused, with the real limits
                    // named.
                    found > max && found - max <= EDGE_TOLERANCE -> Outcome.Success(max)
                    found < min && min - found <= EDGE_TOLERANCE -> Outcome.Success(min)
                    else -> Outcome.Failure(ConfigurationError.OutOfRange(at, found, min, max))
                }
            }
            else -> wrongType(at, "a number", node)
        }
    }

    public fun boolean(
        obj: ConfigNode.Obj,
        field: String,
        default: Boolean,
        path: FieldPath = "",
    ): Outcome<Boolean> = when (val node = obj[field]) {
        null, ConfigNode.Null -> Outcome.Success(default)
        is ConfigNode.Bool -> Outcome.Success(node.value)
        else -> wrongType(path.child(field), "true or false", node)
    }

    /**
     * A list, with a size limit.
     *
     * The limit is not a formality. An imported document is untrusted input, and a list of a
     * million elements is a way to make an application unusable without any code being executed
     * (`SECURITY.md`).
     */
    public fun list(
        obj: ConfigNode.Obj,
        field: String,
        limit: Int,
        path: FieldPath = "",
    ): Outcome<List<ConfigNode>> {
        val at = path.child(field)
        return when (val node = obj[field]) {
            null, ConfigNode.Null -> Outcome.Success(emptyList())
            is ConfigNode.Arr ->
                if (node.items.size > limit) {
                    Outcome.Failure(ConfigurationError.TooManyItems(at, node.items.size, limit))
                } else {
                    Outcome.Success(node.items)
                }
            else -> wrongType(at, "a list", node)
        }
    }

    /** One of a fixed set of values, reported with the allowed set so the message is actionable. */
    public fun <T : Enum<T>> enum(
        obj: ConfigNode.Obj,
        field: String,
        values: Array<T>,
        wireName: (T) -> String,
        path: FieldPath = "",
    ): Outcome<T> {
        val at = path.child(field)
        return when (val raw = text(obj, field, path)) {
            is Outcome.Failure -> raw
            is Outcome.Success -> values.firstOrNull { wireName(it) == raw.value }
                ?.let { Outcome.Success(it) }
                ?: Outcome.Failure(
                    ConfigurationError.UnknownValue(at, raw.value, values.map(wireName).toSet())
                )
        }
    }
}
