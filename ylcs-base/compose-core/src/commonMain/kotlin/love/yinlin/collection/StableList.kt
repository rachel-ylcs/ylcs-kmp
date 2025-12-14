package love.yinlin.collection

import androidx.compose.runtime.Stable

@Stable
class StableList<T>(private val instance: List<T>) : List<T> by instance

inline fun <reified T> emptyStableList(): StableList<T> = StableList(emptyList())
inline fun <reified T> stableListOf(): StableList<T> = StableList(emptyList())
inline fun <reified T> stableListOf(element: T): StableList<T> = StableList(listOf(element))
inline fun <reified T> stableListOf(vararg element: T): StableList<T> = StableList(listOf(*element))