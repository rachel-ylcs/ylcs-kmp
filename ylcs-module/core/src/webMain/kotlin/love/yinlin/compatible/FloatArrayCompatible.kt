package love.yinlin.compatible

import org.khronos.webgl.Float32Array

expect class FloatArrayCompatible(raw: FloatArray) {
    val size: Int
    operator fun get(index: Int): Float
    operator fun set(index: Int, value: Float)
    operator fun iterator(): FloatIterator

    fun toFloat32Array(): Float32Array
}