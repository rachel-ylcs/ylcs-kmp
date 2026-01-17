package love.yinlin.compatible

import org.khronos.webgl.Float32Array
import org.khronos.webgl.toFloat32Array

actual class FloatArrayCompatible actual constructor(private val raw: FloatArray) {
    actual val size: Int get() = raw.size
    actual operator fun get(index: Int): Float = raw[index]
    actual operator fun set(index: Int, value: Float) { raw[index] = value }
    actual operator fun iterator(): FloatIterator = raw.iterator()
    actual fun toFloat32Array(): Float32Array = raw.toFloat32Array()
}