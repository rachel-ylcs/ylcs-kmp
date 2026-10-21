package love.yinlin.compose.image.input

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageInputData

@Stable
object WebImageInput : ImageInput<ImageSource.Network> {
    override val sourceClass = ImageSource.Network::class

    override fun cacheKey(source: ImageSource.Network): String = buildString {
        append("web:").append(source.url.length).append(':').append(source.url)
        for ([key, value] in source.headers.entries.sortedBy { it.key.lowercase() }) {
            val lowerKey = key.lowercase()
            append(lowerKey.length).append(':').append(lowerKey)
            append(value.length).append(':').append(value)
        }
    }

    override suspend fun read(source: ImageSource.Network, context: ImageInputContext): ImageInputData {
        require(source.url.startsWith("https://", true) || source.url.startsWith("http://", true)) { "Expected an HTTP image URL" }
        return context.network(source)
    }
}
