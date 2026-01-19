@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi

@CompatibleRachelApi
actual class ArrayCompatible<T> actual constructor(actual val raw: Array<T>) {
    actual val size: Int get() = raw.size
    actual operator fun get(index: Int): T = raw[index]
    actual operator fun set(index: Int, value: T) { raw[index] = value }
    actual operator fun iterator(): Iterator<T> = raw.iterator()
    actual operator fun contains(e: T): Boolean = raw.contains(e)
}