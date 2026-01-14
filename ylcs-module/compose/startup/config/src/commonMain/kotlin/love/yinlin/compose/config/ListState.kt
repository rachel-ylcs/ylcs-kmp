package love.yinlin.compose.config

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import love.yinlin.extension.replaceAll
import love.yinlin.startup.StartupKV

@Stable
open class ListState<T>(
    kv: StartupKV,
    name: String,
    version: String?,
    itemSerializer: KSerializer<T>,
    defaultFactory: () -> List<T>
) : CollectionState<List<T>, SnapshotStateList<T>>(
    kv = kv,
    name = name,
    version = version,
    serializer = ListSerializer(itemSerializer),
    stateFactory = { it.toMutableStateList() },
    defaultFactory = defaultFactory
) {
    override val size: Int by derivedStateOf { state.size }

    operator fun set(index: Int, item: T) {
        state[index] = item
        save()
    }

    operator fun get(index: Int): T = state[index]

    inline fun <R> map(transform: (T) -> R): List<R> = items.fastMap(transform)

    operator fun iterator(): Iterator<T> = state.iterator()

    fun withIndex(): Iterable<IndexedValue<T>> = state.withIndex()

    inline fun contains(predicate: (T) -> Boolean): Boolean = items.fastAny(predicate)

    operator fun plusAssign(item: T) {
        state += item
        save()
    }

    operator fun minusAssign(item: T) {
        state -= item
        save()
    }

    fun removeAll(predicate: (T) -> Boolean): Boolean {
        val result = state.removeAll(predicate = predicate)
        if (result) save()
        return result
    }

    fun replaceAll(items: List<T>) {
        state.replaceAll(items)
        save()
    }
}