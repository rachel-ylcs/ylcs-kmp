package love.yinlin.compose.image.input

import androidx.compose.runtime.Stable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.Buffer
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
            val bytes = ByteArray(size.toInt())
            var offset = 0

            while (offset < bytes.size) {
                currentCoroutineContext().ensureActive()
                val count = input.readAtMostTo(bytes, offset, offset + minOf(bytes.size - offset, 8192))
                if (count < 0) break
                offset += count
            }

            if (offset < bytes.size) bytes.copyOf(offset)
            else if (input.exhausted()) bytes
            else {
                val extra = Buffer()
                val chunk = ByteArray(8192)

                while (!input.exhausted()) {
                    currentCoroutineContext().ensureActive()
                    val count = input.readAtMostTo(chunk)
                    require(bytes.size + extra.size + count <= context.maxBytes) { "Image file exceeds the byte budget" }
                    extra.write(chunk, 0, count)
                }

                val result = bytes.copyOf(bytes.size + extra.size.toInt())
                var position = bytes.size

                while (!extra.exhausted()) {
                    position += extra.readAtMostTo(result, position, result.size)
                }

                result
            }
        }

        require(bytes.isNotEmpty()) { "Empty image file" }
        ImageInputData(bytes, ImageOrigin.Local)
    }
}
