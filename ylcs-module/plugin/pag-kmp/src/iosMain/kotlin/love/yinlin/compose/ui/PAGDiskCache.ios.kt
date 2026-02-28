@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
actual object PAGDiskCache {
    actual var maxDiskSize: Long get() = PlatformPAGDiskCache.MaxDiskSize().toLong()
        set(value) { PlatformPAGDiskCache.SetMaxDiskSize(value.toULong()) }
    actual fun removeAll() { PlatformPAGDiskCache.RemoveAll() }
}