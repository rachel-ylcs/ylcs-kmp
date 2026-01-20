package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import org.khronos.webgl.Int32Array

@CompatibleRachelApi
expect class IntArrayCompatible(raw: IntArray) {
    val raw: IntArray
    val size: Int
    operator fun get(index: Int): Int
    operator fun set(index: Int, value: Int)
    operator fun iterator(): IntIterator
    val asInt32Array: Int32Array
}