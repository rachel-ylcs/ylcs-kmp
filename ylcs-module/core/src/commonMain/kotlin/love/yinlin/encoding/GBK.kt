package love.yinlin.encoding

private object GBKMapping {
    private var index = 0
    private var buffer = 0
    private var available = 0
    val decode = CharArray(126 * 190)
    val encode = CharArray(0x10000)

    init {
        initTable()
    }

    fun read(count: Int): Int {
        while (available < count) {
            val pair = digit() + digit() * 91
            buffer = buffer or (pair shl available)
            available += if (pair and 8191 > 88) 13 else 14
        }
        val value = buffer and ((1 shl count) - 1)
        buffer = buffer ushr count
        available -= count
        return value
    }

    private fun digit(): Int {
        var code: Int
        do {
            code = GBKTableData[index++].code
        } while (code <= 32)
        return code - 33 - (if (code > 34) 1 else 0) - (if (code > 36) 1 else 0) - (if (code > 92) 1 else 0)
    }

    private fun corePointer(index: Int): Int = 9026 + index / 94 * 190 + index % 94

    private fun put(pointer: Int, code: Int) {
        if (code == 0) return
        decode[pointer] = code.toChar()
        val column = pointer % 190
        val trail = 0x40 + column + if (column >= 63) 1 else 0
        encode[code] = (((pointer / 190 + 0x81) shl 8) or trail).toChar()
    }

    fun block(size: Int, base: Int, width: Int): CharArray {
        val bits = read(4)
        val limit = read(4) + 1
        val chars = CharArray(size)
        val zeroRun = (1 shl width) - 1
        val incrementRun = zeroRun - 1
        var output = 0
        var previous = 0
        while (output < size) {
            var quotient = 0
            while (quotient < limit && read(1) == 0) quotient++
            if (quotient < limit) {
                val delta = (quotient shl bits) or read(bits)
                previous += (delta ushr 1) xor -(delta and 1)
                chars[output++] = previous.toChar()
            }
            else when (val literal = read(width)) {
                zeroRun -> {
                    output += read(12)
                    previous = 0
                }
                incrementRun -> {
                    val end = output + read(12)
                    while (output < end) {
                        chars[output++] = (++previous).toChar()
                    }
                }
                else -> {
                    previous = base + literal
                    chars[output++] = previous.toChar()
                }
            }
        }
        return chars
    }

    fun initTable() {
        val symbols = block(1710, 0, 16)
        for (index in symbols.indices) put(6080 + index, symbols[index].code)

        for (index in 0 ..< 3755) put(corePointer(index), 0x4E00 + read(15))
        val level2 = block(3008, 0x4E00, 15)
        for (index in level2.indices) put(corePointer(3760 + index), level2[index].code)

        var pointer = 0
        for (code in 0x4E00 .. 0x9FA5) {
            if (encode[code] == '\u0000') {
                put(pointer++, code)
                if (pointer == 6080) pointer = 7790
                else if (pointer >= 7790 && pointer % 190 == 96) pointer += 94
            }
        }

        val extra = block(21, 0xF900, 9)
        for (char in extra) {
            put(pointer++, char.code)
            if (pointer % 190 == 96) pointer += 94
        }
    }
}

internal fun convertGBK(data: ByteArray): String {
    if (data.isEmpty()) return ""
    var asciiLength = 0
    while (asciiLength < data.size && data[asciiLength] >= 0) asciiLength++
    if (asciiLength == data.size) return data.decodeToString()
    val chars = CharArray(data.size)
    for (index in 0 ..< asciiLength) chars[index] = data[index].toInt().toChar()
    var input = asciiLength
    var output = asciiLength
    while (input < data.size) {
        val lead = data[input++].toInt() and 0xFF
        when {
            lead < 0x80 -> chars[output++] = lead.toChar()
            lead == 0x80 -> chars[output++] = '\u20AC'
            lead in 0x81..0xFE && input < data.size -> {
                val trail = data[input].toInt() and 0xFF
                if (trail in 0x40 .. 0xFE && trail != 0x7F) {
                    val column = trail - 0x40 - if (trail > 0x7F) 1 else 0
                    val char = GBKMapping.decode[(lead - 0x81) * 190 + column]
                    if (char != '\u0000') {
                        chars[output++] = char
                        input++
                    }
                    else {
                        chars[output++] = '\uFFFD'
                        if (trail >= 0x80) input++
                    }
                }
                else chars[output++] = '\uFFFD'
            }
            else -> chars[output++] = '\uFFFD'
        }
    }
    return chars.concatToString(0, output)
}

internal fun convertGBK(data: String): ByteArray {
    val length = data.length
    var asciiLength = 0
    while (asciiLength < length && data[asciiLength].code < 0x80) asciiLength++
    if (asciiLength == length) return data.convert(Encoding.UTF8)

    val remaining = length - asciiLength
    if (remaining > Int.MAX_VALUE - length) return ByteArray(0)
    val bytes = ByteArray(length + remaining)
    for (index in 0 ..< asciiLength) bytes[index] = data[index].code.toByte()
    var input = asciiLength
    var output = asciiLength
    while (input < length) {
        val code = data[input++].code
        when {
            code < 0x80 -> bytes[output++] = code.toByte()
            code == 0x20AC -> bytes[output++] = 0x80.toByte()
            code in 0xD800 .. 0xDFFF -> {
                if (code <= 0xDBFF && input < length && data[input].code in 0xDC00..0xDFFF) input++
                bytes[output++] = 0x3F
            }
            else -> {
                val packed = GBKMapping.encode[code].code
                if (packed == 0) bytes[output++] = 0x3F
                else {
                    bytes[output++] = (packed shr 8).toByte()
                    bytes[output++] = packed.toByte()
                }
            }
        }
    }
    return if (output == bytes.size) bytes else bytes.copyOf(output)
}