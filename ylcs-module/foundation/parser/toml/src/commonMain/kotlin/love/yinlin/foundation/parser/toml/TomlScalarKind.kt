package love.yinlin.foundation.parser.toml

internal enum class TomlScalarKind {
    String,
    Boolean,
    Integer,
    Float,
    OffsetDateTime,
    LocalDateTime,
    LocalDate,
    LocalTime;

    internal companion object {
        fun isBareKey(char: Char): Boolean =
            char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char == '_' || char == '-'

        fun digit(char: Char): Int = when (char) {
            in '0'..'9' -> char - '0'
            in 'a'..'f' -> char - 'a' + 10
            in 'A'..'F' -> char - 'A' + 10
            else -> -1
        }

        /** Scan digits and underscores without allocating a normalized copy. */
        private fun digitGroup(text: String, start: Int, radix: Int): Int {
            var i = start
            var digitSeen = false
            while (i < text.length) {
                val c = text[i]
                if (digit(c) in 0 until radix) {
                    digitSeen = true
                    ++i
                } else if (c == '_') {
                    if (!digitSeen || i + 1 == text.length || digit(text[i + 1]) !in 0 until radix) return -1
                    digitSeen = false
                    ++i
                } else break
            }
            return if (digitSeen) i else -1
        }

        fun numberKind(text: String): TomlScalarKind? {
            if (text.isEmpty()) return null
            if (text == "inf" || text == "+inf" || text == "-inf" ||
                text == "nan" || text == "+nan" || text == "-nan") return TomlScalarKind.Float
            var i = if (text[0] == '+' || text[0] == '-') 1 else 0
            if (i == text.length) return null
            if (i == 0 && text.length > 2 && text[0] == '0') {
                val radix = when (text[1]) { 'x' -> 16; 'o' -> 8; 'b' -> 2; else -> 0 }
                if (radix != 0) return if (digitGroup(text, 2, radix) == text.length && scalarLong(text) != null)
                    TomlScalarKind.Integer else null
            }
            val start = i
            i = digitGroup(text, i, 10)
            if (i < 0 || text[start] == '0' && i - start > 1) return null
            if (i == text.length) return if (scalarLong(text) != null) TomlScalarKind.Integer else null
            if (text[i] == '.') {
                i = digitGroup(text, i + 1, 10)
                if (i < 0) return null
            }
            if (i < text.length && (text[i] == 'e' || text[i] == 'E')) {
                ++i
                if (i < text.length && (text[i] == '+' || text[i] == '-')) ++i
                i = digitGroup(text, i, 10)
            }
            return if (i == text.length) TomlScalarKind.Float else null
        }

        /** Called after lexical validation. Accumulating negatively includes Long.MIN_VALUE exactly. */
        fun scalarLong(text: String): Long? {
            var i = 0
            val negative = text[0] == '-'
            if (negative || text[0] == '+') ++i
            var radix = 10
            if (i == 0 && text.length > 2 && text[0] == '0') {
                radix = when (text[1]) { 'x' -> 16; 'o' -> 8; 'b' -> 2; else -> 10 }
                if (radix != 10) i = 2
            }
            val limit = if (negative) Long.MIN_VALUE else -Long.MAX_VALUE
            val multiplyLimit = limit / radix
            var result = 0L
            while (i < text.length) {
                val c = text[i++]
                if (c == '_') continue
                val digit = digit(c)
                if (digit !in 0 until radix || result < multiplyLimit) return null
                result *= radix
                if (result < limit + digit) return null
                result -= digit
            }
            return if (negative) result else -result
        }

        private fun decimal(text: String, start: Int, count: Int): Int {
            if (start + count > text.length) return -1
            var result = 0
            for (i in start until start + count) {
                if (text[i] !in '0'..'9') return -1
                result = result * 10 + (text[i] - '0')
            }
            return result
        }

        fun dateTimeKind(text: String, version: TomlVersion): TomlScalarKind? {
            if (text.length < 5) return null
            val hasDate = text.length >= 10 && text[4] == '-' && text[7] == '-'
            var i = 0
            if (hasDate) {
                val year = decimal(text, 0, 4)
                val month = decimal(text, 5, 2)
                val day = decimal(text, 8, 2)
                if (year < 0 || month !in 1..12) return null
                val days = when (month) {
                    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
                    4, 6, 9, 11 -> 30
                    else -> 31
                }
                if (day !in 1..days) return null
                if (text.length == 10) return TomlScalarKind.LocalDate
                if (text[10] != 'T' && text[10] != 't' && text[10] != ' ') return null
                i = 11
            }
            if (decimal(text, i, 2) !in 0..23 || text.getOrNull(i + 2) != ':' ||
                decimal(text, i + 3, 2) !in 0..59) return null
            i += 5
            if (text.getOrNull(i) == ':') {
                if (decimal(text, i + 1, 2) !in 0..59) return null
                i += 3
                if (text.getOrNull(i) == '.') {
                    val start = ++i
                    while (i < text.length && text[i] in '0'..'9') ++i
                    if (i == start) return null
                }
            } else if (version == TomlVersion.V1_0) return null
            if (i == text.length) return if (hasDate) TomlScalarKind.LocalDateTime else TomlScalarKind.LocalTime
            if (!hasDate) return null
            when (text[i]) {
                'z', 'Z' -> if (i + 1 != text.length) return null
                '+', '-' -> if (i + 6 != text.length || decimal(text, i + 1, 2) !in 0..23 ||
                    text[i + 3] != ':' || decimal(text, i + 4, 2) !in 0..59) return null
                else -> return null
            }
            return TomlScalarKind.OffsetDateTime
        }

        fun scalarDouble(text: String, kind: TomlScalarKind?): Double? = when (kind) {
            Integer -> scalarLong(text)?.toDouble()
            Float -> when (text) {
                "inf", "+inf" -> Double.POSITIVE_INFINITY
                "-inf" -> Double.NEGATIVE_INFINITY
                "nan", "+nan", "-nan" -> Double.NaN
                else -> text.replace("_", "").toDoubleOrNull()
            }
            else -> null
        }
    }
}