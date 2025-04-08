package love.yinlin.extension

object UriEx {
    private val hexDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
    private val defaultSet = charArrayOf('_', '-', '!', '.', '~', '\'', '(', ')', '*')
    private fun isAllowed(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c in defaultSet

    fun encode(str: String): String {
        val encoded = StringBuilder()
        val oldLength: Int = str.length
        var current = 0
        while (current < oldLength) {
            var nextToEncode = current
            while (nextToEncode < oldLength && isAllowed(str[nextToEncode])) nextToEncode++
            if (nextToEncode == oldLength) {
                return if (current == 0) str else {
                    encoded.append(str, current, oldLength)
                    encoded.toString()
                }
            }
            if (nextToEncode > current) encoded.append(str, current, nextToEncode)
            current = nextToEncode
            var nextAllowed = current + 1
            while (nextAllowed < oldLength && !isAllowed(str[nextAllowed])) nextAllowed++
            val toEncode = str.substring(current, nextAllowed)
            try {
                val bytes: ByteArray = toEncode.encodeToByteArray()
                val bytesLength = bytes.size
                for(i in 0 until bytesLength) {
                    encoded.append('%')
                    encoded.append(hexDigits[bytes[i].toInt() and 0xf0 shr 4])
                    encoded.append(hexDigits[bytes[i].toInt() and 0xf])
                }
            } catch (_: Exception) {
                return str
            }
            current = nextAllowed
        }
        return encoded.toString()
    }

    private class ByteBuffer(private val size: Int) {
        private val buffer by lazy { ByteArray(size) { 0 } }

        var writePosition = 0
            private set

        fun writeByte(byte: Byte) {
            buffer[writePosition++] = byte
        }

        fun decodeToStringAndReset() = try {
            buffer.decodeToString(startIndex = 0, endIndex = writePosition, throwOnInvalidSequence = false)
        } finally {
            writePosition = 0
        }

        fun flushDecodingByteAccumulator(builder: StringBuilder) {
            if (writePosition == 0) return
            try {
                builder.append(decodeToStringAndReset())
            }
            catch (_: Exception) {
                builder.append('\ufffd')
            }
        }
    }

    fun decode(str: String): String {
        val length = str.length
        val builder = StringBuilder(length)
        ByteBuffer(length).apply {
            var i = 0
            while (i < length) {
                when (val c = str[i++]) {
                    '+' -> {
                        flushDecodingByteAccumulator(builder)
                        builder.append('+')
                    }
                    '%' -> {
                        var hexValue: Byte = 0
                        for (@Suppress("Unused") j in 0..1) {
                            if (i >= length) {
                                flushDecodingByteAccumulator(builder)
                                builder.append('\ufffd')
                                return builder.toString()
                            }
                            val nextC = str[i++]
                            val newDigit: Int = when (nextC) {
                                in '0'..'9' -> nextC.code - '0'.code
                                in 'a'..'f' -> 10 + nextC.code - 'a'.code
                                in 'A'..'F' -> 10 + nextC.code - 'A'.code
                                else -> -1
                            }
                            if (newDigit < 0) {
                                flushDecodingByteAccumulator(builder)
                                builder.append('\ufffd')
                                break
                            }
                            hexValue = (hexValue * 0x10 + newDigit).toByte()
                        }
                        writeByte(hexValue)
                    }
                    else -> {
                        flushDecodingByteAccumulator(builder)
                        builder.append(c)
                    }
                }
            }
            flushDecodingByteAccumulator(builder)
        }
        return builder.toString()
    }

    fun parameters(str: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val decodeUri = decode(str)
        val params = decodeUri.substringAfterLast('?')
        if (params != decodeUri) {
            val items = params.split('&')
            for (item in items) {
                val key = item.substringBefore('=')
                val value = item.substringAfter('=')
                if (item.contains('=') && key.isNotEmpty()) map[key] = value
            }
        }
        return map
    }
}