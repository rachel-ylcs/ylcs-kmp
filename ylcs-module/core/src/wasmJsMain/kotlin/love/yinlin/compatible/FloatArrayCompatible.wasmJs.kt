package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import org.khronos.webgl.toFloat32Array

@CompatibleRachelApi
actual class FloatArrayCompatible actual constructor(actual val raw: FloatArray) {
    actual val size: Int get() = raw.size
    actual operator fun get(index: Int): Float = raw[index]
    actual operator fun set(index: Int, value: Float) { raw[index] = value }
    actual operator fun iterator(): FloatIterator = raw.iterator()
    actual val asWebFloatArray: WebFloatArray get() = raw.toFloat32Array()
}