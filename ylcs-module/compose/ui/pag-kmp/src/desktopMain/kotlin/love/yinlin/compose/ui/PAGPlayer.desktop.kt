package love.yinlin.compose.ui

import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.graphics.asComposeMatrix
import love.yinlin.compose.graphics.asSkiaMatrix33
import org.jetbrains.skia.Matrix33

actual class PAGPlayer(private val delegate: PlatformPAGPlayer) {
    actual var surface: PAGSurface? get() = delegate.surface?.let(::PAGSurface)
        set(value) { delegate.surface = value?.delegate }
    actual var composition: PAGComposition? get() = delegate.composition?.let(::PAGComposition)
        set(value) { delegate.composition = value?.delegate }
    actual var videoEnabled: Boolean by delegate::videoEnabled
    actual var cacheEnabled: Boolean by delegate::cacheEnabled
    actual var useDiskCache: Boolean by delegate::useDiskCache
    actual var cacheScale: Float by delegate::cacheScale
    actual var maxFrameRate: Float by delegate::maxFrameRate
    actual var scaleMode: PAGScaleMode get() = PAGScaleMode.entries[delegate.scaleMode]
        set(value) { delegate.scaleMode = value.ordinal }
    actual var matrix: Matrix get() = Matrix33(*delegate.matrix).asComposeMatrix()
        set(value) { delegate.matrix = value.asSkiaMatrix33().mat }
    actual val duration: Long by delegate::duration
    actual var progress: Double by delegate::progress
    actual val currentFrame: Long by delegate::currentFrame
    actual fun prepare() { delegate.prepare() }
    actual fun flush() { delegate.flush() }
    actual fun flushAndFenceSync(syncArray: LongArray) { delegate.flushAndFenceSync(syncArray) }
    actual fun waitSync(sync: Long) { delegate.waitSync(sync) }
    actual fun hitTestPoint(layer: PAGLayer, x: Float, y: Float, pixelHitTest: Boolean): Boolean =
        delegate.hitTestPoint(layer.delegate, x, y, pixelHitTest)
    actual val renderingTime: Long by delegate::renderingTime
    actual val imageDecodingTime: Long by delegate::imageDecodingTime
    actual val presentingTime: Long by delegate::presentingTime
    actual val graphicsMemory: Long by delegate::graphicsMemory
    actual fun close() = delegate.close()
}