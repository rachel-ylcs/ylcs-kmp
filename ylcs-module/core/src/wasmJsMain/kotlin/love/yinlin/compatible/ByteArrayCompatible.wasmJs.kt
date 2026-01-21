package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.toInt8Array
import org.khronos.webgl.toUint8Array

@CompatibleRachelApi
actual class ByteArrayCompatible actual constructor(actual val raw: ByteArray) {
    actual val size: Int get() = raw.size
    actual operator fun get(index: Int): Byte = raw[index]
    actual operator fun set(index: Int, value: Byte) = raw.set(index, value)
    actual operator fun iterator(): ByteIterator = raw.iterator()
    actual val asInt8Array: Int8Array get() = raw.toInt8Array()
    @OptIn(ExperimentalUnsignedTypes::class)
    actual val asUint8Array: Uint8Array get() = raw.toUByteArray().toUint8Array()
}