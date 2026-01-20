package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import org.khronos.webgl.Float64Array

@CompatibleRachelApi
expect class DoubleArrayCompatible(raw: DoubleArray) {
    val raw: DoubleArray
    val size: Int
    operator fun get(index: Int): Double
    operator fun set(index: Int, value: Double)
    operator fun iterator(): DoubleIterator
    val asFloat64Array: Float64Array
}