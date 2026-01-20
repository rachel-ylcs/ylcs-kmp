package love.yinlin.compose.ui

actual object PAGDiskCache {
    actual var maxDiskSize: Long get() = PlatformPAGDiskCache.MaxDiskSize()
        set(value) { PlatformPAGDiskCache.SetMaxDiskSize(value) }
    actual fun removeAll() = PlatformPAGDiskCache.RemoveAll()
}