package love.yinlin.compatible

import org.khronos.webgl.Int8Array

expect class ByteArrayCompatible(raw: ByteArray) {
    val size: Int
    operator fun get(index: Int): Byte
    operator fun set(index: Int, value: Byte)
    operator fun iterator(): ByteIterator

    fun toInt8Array(): Int8Array
}