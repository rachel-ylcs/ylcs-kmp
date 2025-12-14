package love.yinlin.collection

import androidx.compose.runtime.Stable

@Stable
class StableSet<T>(private val instance: Set<T>) : Set<T> by instance

inline fun <reified T> Set<T>.toStableSet(): StableSet<T> = StableSet(this)
inline fun <reified T> emptyStableSet(): StableSet<T> = StableSet(emptySet())
inline fun <reified T> stableSetOf(): StableSet<T> = StableSet(emptySet())
inline fun <reified T> stableSetOf(element: T): StableSet<T> = StableSet(setOf(element))
inline fun <reified T> stableSetOf(vararg element: T): StableSet<T> = StableSet(setOf(*element))