package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
data class PAGConfig(
    val repeatCount: Int = INFINITY,
    val scaleMode: PAGScaleMode = PAGScaleMode.LetterBox,
    // 下面参数不确定初始值是什么, 后续看 native 源码再改, 请勿主动使用 null 以防后续不兼容
    val cachedEnabled: Boolean? = null,
    val cacheScale: Float? = null,
    val maxFrameRate: Float? = null,

    // Platform.Client
    val isSync: Boolean? = null,
    val videoEnabled: Boolean? = null,
    val useDiskCache: Boolean? = null,

    // Platform.Web
    val useCanvas2D: Boolean = false,

    // PAGImageView
    val cacheAllFramesInMemory: Boolean = false,
    val renderScale: Float = 1f
) {
    companion object {
        const val INFINITY = -1
    }
}