package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
actual object PAGDiskCache {
    actual var maxDiskSize: Long get() = PlatformPAGDiskCache.MaxDiskSize()
        set(value) { PlatformPAGDiskCache.SetMaxDiskSize(value) }
    actual fun removeAll() = PlatformPAGDiskCache.RemoveAll()
}