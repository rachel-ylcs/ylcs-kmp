package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import org.khronos.webgl.Float32Array

@CompatibleRachelApi
expect class FloatArrayCompatible(raw: FloatArray) {
    val raw: FloatArray
    val size: Int
    operator fun get(index: Int): Float
    operator fun set(index: Int, value: Float)
    operator fun iterator(): FloatIterator
    val asFloat32Array: Float32Array
}