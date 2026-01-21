package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
actual object PAGDiskCache {
    actual var maxDiskSize: Long = 0L
    actual fun removeAll() { }
}