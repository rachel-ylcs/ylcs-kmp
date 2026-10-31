package love.yinlin.media

import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Image

internal expect object VideoNative {
    val requiresRenderingContext: Boolean
    fun create(callback: (Long, Int, Long, String?) -> Unit): Long
    fun info(handle: Long, revision: Long): VideoFrameInfo?
    fun renderingContext(context: DirectContext?): DirectContext?
    fun snapshot(handle: Long, revision: Long, context: DirectContext?, info: VideoFrameInfo): Image?
}
