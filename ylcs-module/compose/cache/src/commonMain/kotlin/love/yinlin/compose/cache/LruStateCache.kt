package love.yinlin.compose.cache

import androidx.collection.lruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.extension.catching

/**
 * 可追踪状态的LruCache
 */
class LruStateCache<S : Any, V : Any>(
    maxSize: Int,
    private val key: (S) -> Any,
    private val fetcher: suspend (source: S) -> V
) {
    private val observedMap = mutableStateMapOf<Any, V>()
    private val delegate = lruCache<Any, V>(maxSize, onEntryRemoved = { _, key, _, newValue ->
        if (newValue == null) observedMap.remove(key)
    })

    @Composable
    operator fun get(source: S): V? {
        val sourceKey = key(source)
        val target by rememberDerivedState(sourceKey) { observedMap[sourceKey] }

        LaunchedEffect(sourceKey, target) {
            if (target == null) {
                catching {
                    val newValue = fetcher(source)
                    delegate.put(sourceKey, newValue)
                    observedMap[sourceKey] = newValue
                }
            }
        }

        return target
    }
}