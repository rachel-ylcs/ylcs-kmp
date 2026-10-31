@file:OptIn(ExperimentalForeignApi::class, InternalSkikoApi::class)
package love.yinlin.media

import kotlinx.cinterop.*
import love.yinlin.media.internal.*
import love.yinlin.extension.catchingNull
import love.yinlin.platform.Platform
import love.yinlin.platform.platform
import org.jetbrains.skia.*
import org.jetbrains.skia.impl.getPtr
import org.jetbrains.skia.impl.use
import org.jetbrains.skiko.InternalSkikoApi

internal actual object VideoNative {
    private const val BGRA8_UNORM = 87

    actual val requiresRenderingContext: Boolean = platform == Platform.WindowsNative
    actual fun create(callback: (Long, Int, Long, String?) -> Unit): Long = AudioNative.createVideo(callback)

    actual fun info(handle: Long, revision: Long): VideoFrameInfo? = memScoped {
        val value = alloc<RachelVideoFrameInfo>()
        if (rachel_video_info(handle.toCPointer<CPointed>(), revision, value.ptr)) VideoFrameInfo(value.width, value.height, value.serial) else null
    }

    actual fun renderingContext(context: DirectContext?): DirectContext? = context ?: nativeVideoGpuContext()

    actual fun snapshot(handle: Long, revision: Long, context: DirectContext?, info: VideoFrameInfo): Image? {
        val rendering = renderingContext(context)
        if (rendering != null) catchingNull { gpuSnapshot(handle, revision, rendering) }?.let { return it }
        val size = info.width.toLong() * info.height * 4L
        if (size !in 1L .. Int.MAX_VALUE.toLong()) return null
        return Data.makeUninitialized(size.toInt()).use { pixels ->
            if (!rachel_video_copy_frame(handle.toCPointer<CPointed>(), revision, pixels.writableData().toLong().toCPointer<CPointed>(), size, info.width, info.height)) return@use null
            Image.makeRaster(ImageInfo(info.width, info.height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE), pixels, info.width * 4)
        }
    }

    private fun gpuSnapshot(handle: Long, revision: Long, context: DirectContext): Image? = memScoped {
        val info = alloc<RachelVideoFrameInfo>()
        val frame = rachel_video_gpu_frame(handle.toCPointer<CPointed>(), revision, getPtr(context).toLong().toCPointer<CPointed>(), info.ptr) ?: return@memScoped null
        var target: BackendRenderTarget? = null
        var surface: Surface? = null
        var image: Image? = null
        try {
            val texture = checkNotNull(info.texture)
            target = BackendRenderTarget.makeDirect3D(info.width, info.height, texture.rawValue, BGRA8_UNORM, 1, 1)
            surface = checkNotNull(Surface.makeFromBackendRenderTarget(context, target, SurfaceOrigin.TOP_LEFT, SurfaceColorFormat.BGRA_8888, ColorSpace.sRGB)) { "Cannot wrap the video Direct3D surface" }
            image = surface.makeImageSnapshot()
            image
        }
        finally {
            try {
                if (surface != null && target != null) {
                    context.flush()
                    context.submit(false)
                    check(rachel_video_gpu_complete(frame, getPtr(target).toLong().toCPointer<CPointed>())) { "Cannot synchronize the video Direct3D texture" }
                }
                else rachel_video_gpu_complete(frame, null)
            }
            catch (failure: Throwable) {
                image?.close()
                throw failure
            }
            finally {
                surface?.close()
                target?.close()
                rachel_video_gpu_release(frame)
            }
        }
    }
}

internal expect fun nativeVideoGpuContext(): DirectContext?
