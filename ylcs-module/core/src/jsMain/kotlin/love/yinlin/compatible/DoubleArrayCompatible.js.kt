package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import org.khronos.webgl.Float64Array
import org.khronos.webgl.toFloat64Array

@CompatibleRachelApi
actual class DoubleArrayCompatible actual constructor(actual val raw: DoubleArray) {
    actual val size: Int get() = raw.size
    actual operator fun get(index: Int): Double = raw[index]
    actual operator fun set(index: Int, value: Double) { raw[index] = value }
    actual operator fun iterator(): DoubleIterator = raw.iterator()
    actual val asFloat64Array: Float64Array get() = raw.toFloat64Array()
}