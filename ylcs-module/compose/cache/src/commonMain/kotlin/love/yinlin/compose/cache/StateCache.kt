package love.yinlin.compose.cache

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import love.yinlin.compose.extension.rememberState
import love.yinlin.extension.catching

class StateCache<S : Any, V : Any>(
    private val key: (S) -> Any = { it },
    private val fetcher: suspend (source: S) -> V
) {
    private val delegate = mutableMapOf<Any, V>()

    @Composable
    operator fun get(source: S): V? {
        val sourceKey = key(source)
        var target: V? by rememberState(sourceKey) { delegate[sourceKey] }

        LaunchedEffect(sourceKey) {
            if (target == null) {
                catching {
                    val newValue = fetcher(source)
                    delegate[sourceKey] = newValue
                    target = newValue
                }
            }
        }

        return target
    }
}