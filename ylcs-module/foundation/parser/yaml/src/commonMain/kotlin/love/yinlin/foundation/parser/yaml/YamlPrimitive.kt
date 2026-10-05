package love.yinlin.foundation.parser.yaml

sealed class YamlPrimitive : YamlElement() {
    abstract val content: String
    abstract val isString: Boolean
    internal abstract val kind: YamlScalarKind

    internal val internalBoolean: Boolean? get() = when (content) {
        "true", "True", "TRUE" -> true
        "false", "False", "FALSE" -> false
        else -> null
    }

    internal val internalLong: Long? get() = YamlScalarKind.scalarLong(content)
    internal val internalInt: Int? get() = internalLong?.toInt()
    internal val internalDouble: Double? get() = YamlScalarKind.scalarDouble(content)
    internal val internalFloat: Float? get() = internalDouble?.toFloat()

    companion object {
        operator fun invoke(value: String?): YamlPrimitive =
            if (value == null) YamlNull else YamlLiteral(value, YamlScalarKind.String)

        operator fun invoke(value: Boolean?): YamlPrimitive =
            if (value == null) YamlNull else YamlLiteral(value.toString(), YamlScalarKind.Boolean)

        operator fun invoke(value: Number?): YamlPrimitive {
            if (value == null) return YamlNull
            val text = when (value) {
                is Double -> when {
                    value.isNaN() -> ".nan"
                    value == Double.POSITIVE_INFINITY -> ".inf"
                    value == Double.NEGATIVE_INFINITY -> "-.inf"
                    else -> value.toString()
                }
                is Float -> when {
                    value.isNaN() -> ".nan"
                    value == Float.POSITIVE_INFINITY -> ".inf"
                    value == Float.NEGATIVE_INFINITY -> "-.inf"
                    else -> value.toString()
                }
                else -> value.toString()
            }
            val kind = YamlScalarKind.fromString(text)
            require(kind == YamlScalarKind.Integer || kind == YamlScalarKind.Float) { "Not a YAML number: $text" }
            return YamlLiteral(text, kind)
        }

        @Suppress("UNUSED_PARAMETER")
        operator fun invoke(value: Nothing?): YamlPrimitive = YamlNull
    }
}