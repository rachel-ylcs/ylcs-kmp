package love.yinlin.compose.image.cache

import love.yinlin.compose.image.data.ImageResult

internal class ImageMemoryCacheEntry(val group: String, result: ImageResult) {
    val result: ImageResult = result.forCache()
    // 命中结果只改变来源标记；多次命中共用包装，避免反复分配及查询平台位图信息。
    val memoryResult: ImageResult = this.result.fromMemory()
}