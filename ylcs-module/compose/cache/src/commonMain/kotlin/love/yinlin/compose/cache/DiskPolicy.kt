package love.yinlin.compose.cache

import androidx.compose.runtime.Stable

@Stable
sealed interface DiskPolicy {
    // Persistent 只在显式删除时移除数据
    @Stable
    data object Persistent : DiskPolicy
    // Lru 适合可重新获取的临时资源
    @Stable
    data class Lru(val maxBytes: Long) : DiskPolicy {
        init { require(maxBytes > 0) }
    }
}