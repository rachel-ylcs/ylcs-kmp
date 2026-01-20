package love.yinlin.compose.ui

expect object PAGDiskCache {
    var maxDiskSize: Long
    fun removeAll()
}