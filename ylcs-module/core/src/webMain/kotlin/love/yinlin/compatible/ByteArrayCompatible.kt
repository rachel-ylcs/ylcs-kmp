package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array

@CompatibleRachelApi
expect class ByteArrayCompatible(raw: ByteArray) {
    val raw: ByteArray
    val size: Int
    operator fun get(index: Int): Byte
    operator fun set(index: Int, value: Byte)
    operator fun iterator(): ByteIterator
    val asInt8Array: Int8Array
    val asUint8Array: Uint8Array
}