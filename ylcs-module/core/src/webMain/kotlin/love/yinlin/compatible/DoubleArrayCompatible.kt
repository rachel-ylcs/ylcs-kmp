package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi

@CompatibleRachelApi
expect class DoubleArrayCompatible(raw: DoubleArray) {
    val raw: DoubleArray
    val size: Int
    operator fun get(index: Int): Double
    operator fun set(index: Int, value: Double)
    operator fun iterator(): DoubleIterator
    val asWebDoubleArray: WebDoubleArray
}