package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import org.khronos.webgl.toInt16Array

@CompatibleRachelApi
actual class ShortArrayCompatible actual constructor(actual val raw: ShortArray) {
    actual val size: Int get() = raw.size
    actual operator fun get(index: Int): Short = raw[index]
    actual operator fun set(index: Int, value: Short) { raw[index] = value }
    actual operator fun iterator(): ShortIterator = raw.iterator()
    actual val asWebShortArray: WebShortArray get() = raw.toInt16Array()
}