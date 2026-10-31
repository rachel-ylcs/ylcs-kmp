package love.yinlin.media

import love.yinlin.foundation.LibLoader
import love.yinlin.foundation.NativeLibApi
import org.jetbrains.skia.*
import org.jetbrains.skia.impl.use
import java.util.function.BiConsumer

@NativeLibApi
internal actual object VideoNative {
    init {
        LibLoader.resource("ylcs-module.compose.components.media")
    }

    actual val requiresRenderingContext: Boolean = false
    private val metadata = ThreadLocal.withInitial { LongArray(3) }
    private external fun createPlayer(callback: BiConsumer<LongArray, ByteArray?>): Long
    private external fun readInfo(handle: Long, revision: Long, result: LongArray): Boolean
    private external fun copyFrame(handle: Long, revision: Long, pixels: Long, capacity: Long, width: Int, height: Int): Boolean

    actual fun create(callback: (Long, Int, Long, String?) -> Unit): Long = createPlayer { event, message ->
        callback(event[0], event[1].toInt(), event[2], message?.decodeToString())
    }

    actual fun info(handle: Long, revision: Long): VideoFrameInfo? {
        val value = metadata.get()
        return if (readInfo(handle, revision, value)) VideoFrameInfo(value[0].toInt(), value[1].toInt(), value[2]) else null
    }

    actual fun renderingContext(context: DirectContext?): DirectContext? = context

    actual fun snapshot(handle: Long, revision: Long, context: DirectContext?, info: VideoFrameInfo): Image? {
        val size = info.width.toLong() * info.height * 4L
        if (size !in 1L .. Int.MAX_VALUE.toLong()) return null
        return Data.makeUninitialized(size.toInt()).use { pixels ->
            if (!copyFrame(handle, revision, pixels.writableData(), size, info.width, info.height)) return@use null
            Image.makeRaster(ImageInfo(info.width, info.height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE), pixels, info.width * 4)
        }
    }
}
