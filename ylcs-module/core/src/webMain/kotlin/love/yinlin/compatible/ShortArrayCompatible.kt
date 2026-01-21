package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import org.khronos.webgl.Int16Array

@CompatibleRachelApi
expect class ShortArrayCompatible(raw: ShortArray) {
    val raw: ShortArray
    val size: Int
    operator fun get(index: Int): Short
    operator fun set(index: Int, value: Short)
    operator fun iterator(): ShortIterator
    val asInt16Array: Int16Array
}