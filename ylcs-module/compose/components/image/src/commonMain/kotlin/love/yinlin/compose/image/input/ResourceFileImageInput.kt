package love.yinlin.compose.image.input

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageInputData
import love.yinlin.compose.image.data.ImageOrigin
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes

@Stable
object ResourceFileImageInput : ImageInput<ImageSource.Resource> {
    override val sourceClass = ImageSource.Resource::class

    override fun cacheKey(source: ImageSource.Resource): String = "resource:${source.path}"

    @OptIn(InternalResourceApi::class)
    override suspend fun read(source: ImageSource.Resource, context: ImageInputContext): ImageInputData {
        val bytes = readResourceBytes(source.path)
        require(bytes.size in 1 .. context.maxBytes) { "Image resource exceeds the byte budget" }
        return ImageInputData(bytes, ImageOrigin.Resource)
    }
}
