package love.yinlin.compose.graphics

import love.yinlin.data.MimeType
import love.yinlin.extension.catchingNull
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.impl.use

private class SkikoAnimatedWebp(
    width: Int,
    height: Int,
    frameCount: Int,
    private val codec: Codec
) : AnimatedWebp(width, height, frameCount) {
    override fun close() { codec.close() }
}

actual fun ByteArray.decodeAnimatedWebp(): AnimatedWebp? = catchingNull {
    require(AnimatedWebp.check(this))
    Data.makeFromBytes(this).use { rawData ->
        val codec = Codec.makeFromData(rawData)
        val info = codec.imageInfo
        val frameCount = codec.frameCount
        val mimeType = "image/${codec.encodedImageFormat.name.lowercase()}"
        val width = info.width
        val height = info.height
        if (width > 0 && height > 0 && frameCount > 1 && mimeType == MimeType.WEBP) {
            SkikoAnimatedWebp(width, height, frameCount, codec)
        }
        else {
            codec.close()
            null
        }
    }
}