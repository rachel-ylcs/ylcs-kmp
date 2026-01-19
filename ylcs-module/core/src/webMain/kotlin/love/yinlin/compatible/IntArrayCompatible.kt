package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi

@CompatibleRachelApi
expect class IntArrayCompatible(raw: IntArray) {
    val raw: IntArray
    val size: Int
    operator fun get(index: Int): Int
    operator fun set(index: Int, value: Int)
    operator fun iterator(): IntIterator
    val asWebIntArray: WebIntArray
}