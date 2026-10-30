package love.yinlin.kv

internal fun ByteArray.decodeKVString(): String {
    var output: StringBuilder? = null
    var start = 0
    var index = 0
    while (index < size) {
        val first = this[index].toInt() and 0xff
        val legacyNull = first == 0xc0 && index + 1 < size && this[index + 1].toInt() and 0xff == 0x80
        val legacySurrogate = first == 0xed && index + 2 < size && this[index + 1].toInt() and 0xff in 0xa0 .. 0xbf && this[index + 2].toInt() and 0xff in 0x80 .. 0xbf
        if (legacyNull || legacySurrogate) {
            val result = output ?: StringBuilder(size).also { output = it }
            result.append(decodeToString(start, index))
            if (legacyNull) {
                result.append('\u0000')
                index += 2
            }
            else {
                result.append((((first and 0x0f) shl 12) or ((this[index + 1].toInt() and 0x3f) shl 6) or (this[index + 2].toInt() and 0x3f)).toChar())
                index += 3
            }
            start = index
        }
        else ++index
    }
    return output?.append(decodeToString(start, size))?.toString() ?: decodeToString()
}
