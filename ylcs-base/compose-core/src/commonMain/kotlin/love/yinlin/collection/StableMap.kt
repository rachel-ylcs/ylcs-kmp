package love.yinlin.collection

import androidx.compose.runtime.Stable

@Stable
class StableMap<K, V>(private val instance: Map<K, V>) : Map<K, V> by instance

inline fun <reified K, reified V> Map<K, V>.toStableMap(): StableMap<K, V> = StableMap(this)
inline fun <reified K, reified V> emptyStableMap(): StableMap<K, V> = StableMap(emptyMap())
inline fun <reified K, reified V> stableMapOf(): StableMap<K, V> = StableMap(emptyMap())
inline fun <reified K, reified V> stableMapOf(element: Pair<K, V>): StableMap<K, V> = StableMap(mapOf(element))
inline fun <reified K, reified V> stableMapOf(vararg element: Pair<K, V>): StableMap<K, V> = StableMap(mapOf(*element))