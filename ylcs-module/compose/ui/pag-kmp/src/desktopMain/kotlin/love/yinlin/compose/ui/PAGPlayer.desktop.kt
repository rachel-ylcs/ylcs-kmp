package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Matrix
import love.yinlin.compose.graphics.asComposeMatrix
import love.yinlin.compose.graphics.asSkiaMatrix33
import org.jetbrains.skia.Matrix33

@Stable
actual class PAGPlayer(private val delegate: PlatformPAGPlayer) {
    actual constructor() : this(PlatformPAGPlayer())

    actual var surface: PAGSurface? get() = delegate.surface?.let(::PAGSurface)
        set(value) { delegate.surface = value?.delegate }
    actual var composition: PAGComposition? get() = delegate.composition?.let(::PAGComposition)
        set(value) { delegate.composition = value?.delegate }
    actual var videoEnabled: Boolean get() = delegate.videoEnabled
        set(value) { delegate.videoEnabled = value }
    actual var cacheEnabled: Boolean get() = delegate.cacheEnabled
        set(value) { delegate.cacheEnabled = value }
    actual var useDiskCache: Boolean get() = delegate.useDiskCache
        set(value) { delegate.useDiskCache = value }
    actual var cacheScale: Float get() = delegate.cacheScale
        set(value) { delegate.cacheScale = value }
    actual var maxFrameRate: Float get() = delegate.maxFrameRate
        set(value) { delegate.maxFrameRate = value }
    actual var scaleMode: PAGScaleMode get() = PAGScaleMode.entries[delegate.scaleMode]
        set(value) { delegate.scaleMode = value.ordinal }
    actual var matrix: Matrix get() = Matrix33(*delegate.matrix).asComposeMatrix()
        set(value) { delegate.matrix = value.asSkiaMatrix33().mat }
    actual val duration: Long get() = delegate.duration
    actual var progress: Double get() = delegate.progress
        set(value) { delegate.progress = value }
    actual val currentFrame: Long get() = delegate.currentFrame
    actual fun prepare() { delegate.prepare() }
    actual fun flush() { delegate.flush() }
    actual fun flushAndFenceSync(syncArray: LongArray) { delegate.flushAndFenceSync(syncArray) }
    actual fun waitSync(sync: Long) { delegate.waitSync(sync) }
    actual fun hitTestPoint(layer: PAGLayer, x: Float, y: Float, pixelHitTest: Boolean): Boolean =
        delegate.hitTestPoint(layer.delegate, x, y, pixelHitTest)
    actual val renderingTime: Long get() = delegate.renderingTime
    actual val imageDecodingTime: Long get() = delegate.imageDecodingTime
    actual val presentingTime: Long get() = delegate.presentingTime
    actual val graphicsMemory: Long get() = delegate.graphicsMemory
    actual fun close() = delegate.close()
}