package love.yinlin.compatible

import org.khronos.webgl.Int8Array
import org.khronos.webgl.toInt8Array

actual class ByteArrayCompatible actual constructor(private val raw: ByteArray) {
    actual val size: Int get() = raw.size
    actual operator fun get(index: Int): Byte = raw[index]
    actual operator fun set(index: Int, value: Byte) = raw.set(index, value)
    actual operator fun iterator(): ByteIterator = raw.iterator()

    actual fun toInt8Array(): Int8Array = raw.toInt8Array()
}