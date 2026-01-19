package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi

@CompatibleRachelApi
expect class ShortArrayCompatible(raw: ShortArray) {
    val raw: ShortArray
    val size: Int
    operator fun get(index: Int): Short
    operator fun set(index: Int, value: Short)
    operator fun iterator(): ShortIterator
    val asWebShortArray: WebShortArray
}