package love.yinlin.config

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import love.yinlin.compose.toMutableStateMap
import love.yinlin.extension.replaceAll
import love.yinlin.startup.StartupKV

@Stable
open class MapState<K, V>(
    kv: StartupKV,
    name: String,
    version: String?,
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    defaultFactory: () -> Map<K, V>
) : CollectionState<Map<K, V>, SnapshotStateMap<K, V>>(
    kv = kv,
    name = name,
    version = version,
    serializer = MapSerializer(keySerializer, valueSerializer),
    stateFactory = { it.toMutableStateMap() },
    defaultFactory = defaultFactory
){
    override val size: Int get() = state.size

    operator fun set(key: K, value: V) {
        state[key] = value
        save()
    }

    operator fun get(key: K): V? = state[key]

    inline fun <R> map(transform: (K, V) -> R): List<R> = items.map { transform(it.key, it.value) }

    operator fun iterator(): Iterator<Map.Entry<K, V>> = state.iterator()

    operator fun plusAssign(item: Pair<K, V>) {
        state += item
        save()
    }

    operator fun plusAssign(items: Map<K, V>) {
        state.putAll(items)
        save()
    }

    operator fun minusAssign(key: K) {
        state.remove(key)
        save()
    }

    fun renameKey(key: K, newKey: K, block: (V) -> V = { it }) {
        val value = state.remove(key)
        if (value != null) {
            state += newKey to block(value)
            save()
        }
    }

    fun replaceAll(items: Map<K, V>) {
        state.replaceAll(items)
        save()
    }
}