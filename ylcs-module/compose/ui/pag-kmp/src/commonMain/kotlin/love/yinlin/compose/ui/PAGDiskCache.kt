package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
expect object PAGDiskCache {
    var maxDiskSize: Long
    fun removeAll()
}