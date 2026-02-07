package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
actual object PAGDiskCache {
    actual var maxDiskSize: Long get() = PlatformPAGDiskCache.maxDiskSize
        set(value) { PlatformPAGDiskCache.maxDiskSize = value }
    actual fun removeAll() = PlatformPAGDiskCache.removeAll()
}