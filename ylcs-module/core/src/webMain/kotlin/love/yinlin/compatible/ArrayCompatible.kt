@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compatible

import love.yinlin.annotation.CompatibleRachelApi
import kotlin.js.*

@CompatibleRachelApi
expect class ArrayCompatible<T>(raw: Array<T>) {
    val raw: Array<T>
    val size: Int
    operator fun get(index: Int): T
    operator fun set(index: Int, value: T)
    operator fun iterator(): Iterator<T>
    operator fun contains(e: T): Boolean
}