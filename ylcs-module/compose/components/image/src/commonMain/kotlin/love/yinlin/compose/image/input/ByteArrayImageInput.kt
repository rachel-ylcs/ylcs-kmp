package love.yinlin.compose.image.input

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageInputData
import love.yinlin.compose.image.data.ImageOrigin

@Stable
object ByteArrayImageInput : ImageInput<ImageSource.Bytes> {
    override val sourceClass = ImageSource.Bytes::class

    override fun cacheKey(source: ImageSource.Bytes): String = "bytes:${source.contentKey}"

    override suspend fun read(source: ImageSource.Bytes, context: ImageInputContext): ImageInputData {
        require(source.data.size in 1 .. context.maxBytes)
        return ImageInputData(source.data, ImageOrigin.Local)
    }
}
