package love.yinlin.foundation.parser.yaml

internal enum class YamlScalarKind {
    Null,
    String,
    Boolean,
    Integer,
    Float;

    internal companion object {
        fun fromString(text: String): YamlScalarKind {
            when (text) {
                "", "~", "null", "Null", "NULL" -> return Null
                "true", "True", "TRUE", "false", "False", "FALSE" -> return Boolean
                ".inf", ".Inf", ".INF", "+.inf", "+.Inf", "+.INF",
                "-.inf", "-.Inf", "-.INF", ".nan", ".NaN", ".NAN" -> return Float
            }
            var i = if (text[0] == '+' || text[0] == '-') 1 else 0
            if (i == text.length) return String
            if (i + 2 < text.length && text[i] == '0') {
                val radix = when (text[i + 1]) { 'x' -> 16; 'o' -> 8; else -> 0 }
                if (radix != 0) {
                    i += 2
                    while (i < text.length) {
                        if (text[i++].digitToIntOrNull(radix) == null) return String
                    }
                    return Integer
                }
            }
            var digits = 0
            while (i < text.length && text[i] in '0'..'9') { i++; digits++ }
            var floating = false
            if (i < text.length && text[i] == '.') {
                floating = true
                i++
                while (i < text.length && text[i] in '0'..'9') { i++; digits++ }
            }
            if (digits == 0) return String
            if (i < text.length && (text[i] == 'e' || text[i] == 'E')) {
                floating = true
                i++
                if (i < text.length && (text[i] == '+' || text[i] == '-')) i++
                val exponent = i
                while (i < text.length && text[i] in '0'..'9') i++
                if (i == exponent) return String
            }
            return if (i != text.length) String else if (floating) Float else Integer
        }

        fun resolveScalar(text: String): YamlPrimitive = when (val kind = fromString(text)) {
            Null -> YamlNull
            Boolean -> YamlPrimitive(text[0] == 't' || text[0] == 'T')
            else -> YamlLiteral(text, kind)
        }

        fun scalarLong(text: String): Long? {
            if (text.isEmpty()) return null
            val sign = if (text[0] == '+' || text[0] == '-') 1 else 0
            if (sign + 2 < text.length && text[sign] == '0') {
                val radix = when (text[sign + 1]) { 'x' -> 16; 'o' -> 8; else -> 0 }
                if (radix != 0) {
                    val digits = text.substring(sign + 2)
                    return (if (sign == 0) digits else text[0] + digits).toLongOrNull(radix)
                }
            }
            return text.toLongOrNull()
        }

        fun scalarDouble(text: String): Double? {
            return when (text) {
                ".inf", ".Inf", ".INF", "+.inf", "+.Inf", "+.INF" -> Double.POSITIVE_INFINITY
                "-.inf", "-.Inf", "-.INF" -> Double.NEGATIVE_INFINITY
                ".nan", ".NaN", ".NAN" -> Double.NaN
                else -> {
                    if (text.isEmpty()) null
                    else {
                        val sign = if (text[0] == '+' || text[0] == '-') 1 else 0
                        val radix = if (sign + 2 < text.length && text[sign] == '0') {
                            when (text[sign + 1]) { 'x' -> 16; 'o' -> 8; else -> 0 }
                        } else 0
                        if (radix == 0) text.toDoubleOrNull()
                        else {
                            var value = 0.0
                            for (i in sign + 2 until text.length) {
                                val digit = text[i].digitToIntOrNull(radix) ?: return null
                                value = value * radix + digit
                            }
                            if (sign == 1 && text[0] == '-') -value else value
                        }
                    }
                }
            }
        }
    }
}