package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import org.khronos.webgl.Int32Array
import org.khronos.webgl.toInt32Array

@CompatibleRachelApi
actual class IntArrayCompatible actual constructor(actual val raw: IntArray) {
    actual val size: Int get() = raw.size
    actual operator fun get(index: Int): Int = raw[index]
    actual operator fun set(index: Int, value: Int) { raw[index] = value }
    actual operator fun iterator(): IntIterator = raw.iterator()
    actual val asInt32Array: Int32Array get() = raw.toInt32Array()
}