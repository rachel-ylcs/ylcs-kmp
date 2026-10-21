package love.yinlin.compose.image.input

import androidx.compose.runtime.Stable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import love.yinlin.compose.image.data.ImageInputData
import love.yinlin.compose.image.data.ImageOrigin
import love.yinlin.coroutines.Coroutines

@Stable
object LocalFileImageInput : ImageInput<ImageSource.LocalFile> {
    override val sourceClass = ImageSource.LocalFile::class

    override fun cacheKey(source: ImageSource.LocalFile): String = "file:${source.file.path}"

    override suspend fun read(source: ImageSource.LocalFile, context: ImageInputContext): ImageInputData = Coroutines.io {
        val size = source.file.fileSize()
        require(size in 1L .. context.maxBytes.toLong()) { "Image file is empty or exceeds the byte budget" }

        val bytes = source.file.read { input ->
            val buffer = Buffer()
            val chunk = ByteArray(8192)

            while (!input.exhausted()) {
                currentCoroutineContext().ensureActive()
                val count = input.readAtMostTo(chunk)
                require(buffer.size + count <= context.maxBytes) { "Image file exceeds the byte budget" }
                buffer.write(chunk, 0, count)
            }

            buffer.readByteArray()
        }

        ImageInputData(bytes, ImageOrigin.Local)
    }
}
