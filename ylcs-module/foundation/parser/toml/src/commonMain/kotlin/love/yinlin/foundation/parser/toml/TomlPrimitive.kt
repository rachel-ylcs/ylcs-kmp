package love.yinlin.foundation.parser.toml

sealed class TomlPrimitive : TomlElement() {
    abstract val content: String
    abstract val isString: Boolean
    internal abstract val kind: TomlScalarKind

    val isDateTime: Boolean get() = when (kind) {
        TomlScalarKind.OffsetDateTime, TomlScalarKind.LocalDateTime, TomlScalarKind.LocalDate, TomlScalarKind.LocalTime -> true
        else -> false
    }

    internal val internalBoolean: Boolean? get() = when (content) {
        "true" -> true
        "false" -> false
        else -> null
    }
    internal val internalLong: Long? get() = if (kind == TomlScalarKind.Integer || isString && TomlScalarKind.numberKind(content) == TomlScalarKind.Integer) TomlScalarKind.scalarLong(content) else null
    internal val internalInt: Int? get() = internalLong?.toInt()
    internal val internalDouble: Double? get() = TomlScalarKind.scalarDouble(content, if (isString) TomlScalarKind.numberKind(content) else kind)
    internal val internalFloat: Float? get() = internalDouble?.toFloat()

    companion object {
        operator fun invoke(value: String): TomlPrimitive = TomlLiteral(value, TomlScalarKind.String)
        operator fun invoke(value: Boolean): TomlPrimitive = TomlLiteral(value.toString(), TomlScalarKind.Boolean)

        operator fun invoke(value: Number): TomlPrimitive {
            val text = when (value) {
                is Double -> when {
                    value.isNaN() -> "nan"
                    value == Double.POSITIVE_INFINITY -> "inf"
                    value == Double.NEGATIVE_INFINITY -> "-inf"
                    else -> value.toString()
                }
                is Float -> when {
                    value.isNaN() -> "nan"
                    value == Float.POSITIVE_INFINITY -> "inf"
                    value == Float.NEGATIVE_INFINITY -> "-inf"
                    else -> value.toString()
                }
                else -> value.toString()
            }
            return number(text)
        }

        fun number(value: String): TomlPrimitive {
            val kind = TomlScalarKind.numberKind(value)
            require(kind != null) { "Invalid TOML number or integer outside signed 64-bit range: $value" }
            return TomlLiteral(value, kind)
        }

        fun dateTime(value: String, version: TomlVersion = TomlVersion.V1_1): TomlPrimitive {
            val kind = TomlScalarKind.dateTimeKind(value, version)
            require(kind != null) { "Invalid TOML date/time: $value" }
            return TomlLiteral(value, kind)
        }
    }
}