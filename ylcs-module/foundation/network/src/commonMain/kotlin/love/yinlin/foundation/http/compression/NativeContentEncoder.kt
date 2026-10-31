package love.yinlin.foundation.http.compression

import io.ktor.util.ContentEncoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.invokeOnCompletion
import io.ktor.utils.io.readBuffer
import io.ktor.utils.io.reader
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.io.readByteArray
import love.yinlin.foundation.compression.CompressionCodec
import kotlin.coroutines.CoroutineContext

// Kotlin Native 上 ktor 暂不支持 gzip 和 zlib 压缩
internal class NativeContentEncoder(override val name: String, private val codec: CompressionCodec) : ContentEncoder {
    override fun encode(source: ByteReadChannel, coroutineContext: CoroutineContext): ByteReadChannel = transform(source, coroutineContext, true)

    override fun decode(source: ByteReadChannel, coroutineContext: CoroutineContext): ByteReadChannel = transform(source, coroutineContext, false)

    private fun transform(source: ByteReadChannel, context: CoroutineContext, encode: Boolean): ByteReadChannel {
        val writer = CoroutineScope(context).writer {
            val bytes = source.readBuffer().use { it.readByteArray() }
            ensureActive()
            val result = if (encode) codec.compress(bytes) else codec.decompress(bytes)
            ensureActive()
            channel.writeFully(result)
        }

        writer.invokeOnCompletion { failure ->
            source.cancel(failure)
        }

        return object : ByteReadChannel by writer.channel {
            override fun cancel(cause: Throwable?) {
                source.cancel(cause)
                writer.channel.cancel(cause)
                writer.job.cancel(CancellationException("HTTP content transformation cancelled", cause))
            }
        }
    }

    override fun encode(source: ByteWriteChannel, coroutineContext: CoroutineContext): ByteWriteChannel {
        val reader = CoroutineScope(coroutineContext).reader {
            try {
                val bytes = channel.readBuffer().use { it.readByteArray() }
                ensureActive()
                val result = codec.compress(bytes)
                ensureActive()
                source.writeFully(result)
                source.flushAndClose()
            }
            catch (failure: Throwable) {
                source.cancel(failure)
                throw failure
            }
        }

        reader.invokeOnCompletion { failure ->
            if (failure != null) source.cancel(failure)
        }

        return object : ByteWriteChannel by reader.channel {
            override fun cancel(cause: Throwable?) {
                source.cancel(cause)
                reader.channel.cancel(cause)
                reader.job.cancel(CancellationException("HTTP content encoding cancelled", cause))
            }
        }
    }
}