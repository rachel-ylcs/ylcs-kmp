package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Matrix
import love.yinlin.platform.unsupportedPlatform
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@Stable
actual class PAGPlayer(private val delegate: PlatformPAGPlayer) {
    actual constructor() : this(PlatformPAGPlayer.create())

    actual var surface: PAGSurface? get() = delegate.getSurface()?.let(::PAGSurface)
        set(value) { value?.delegate?.let { delegate.setSurface(it) } }
    actual var composition: PAGComposition? get() = delegate.getComposition()?.let(::PAGComposition)
        set(value) { value?.delegate?.let { delegate.setComposition(it) } }
    actual var videoEnabled: Boolean get() = delegate.videoEnabled()
        set(value) { delegate.setVideoEnabled(value) }
    actual var cacheEnabled: Boolean get() = delegate.cacheEnabled()
        set(value) { delegate.setCacheEnabled(value) }
    actual var useDiskCache: Boolean get() = false
        set(value) {}
    actual var cacheScale: Float get() = delegate.cacheScale()
        set(value) { delegate.setCacheScale(value) }
    actual var maxFrameRate: Float get() = delegate.maxFrameRate()
        set(value) { delegate.setMaxFrameRate(value) }
    actual var scaleMode: PAGScaleMode get() = PAGScaleMode.entries[delegate.scaleMode()]
        set(value) { delegate.setScaleMode(value.ordinal) }
    actual var matrix: Matrix get() = delegate.matrix().asComposeMatrix()
        set(value) { delegate.setMatrix(value.asPAGMatrix()) }
    actual val duration: Long get() = delegate.duration().toLong()
    actual var progress: Double get() = delegate.getProgress()
        set(value) { delegate.setProgress(value) }
    actual val currentFrame: Long get() = delegate.currentFrame().toLong()
    actual fun prepare() { delegate.prepare() }
    actual fun flush() { delegate.flush() }
    actual fun flushAndFenceSync(syncArray: LongArray) { unsupportedPlatform() }
    actual fun waitSync(sync: Long) { unsupportedPlatform() }
    actual fun hitTestPoint(layer: PAGLayer, x: Float, y: Float, pixelHitTest: Boolean): Boolean =
        delegate.hitTestPoint(layer.delegate, x.toDouble(), y.toDouble(), pixelHitTest)
    actual val renderingTime: Long = delegate.renderingTime().toLong()
    actual val imageDecodingTime: Long = delegate.imageDecodingTime().toLong()
    actual val presentingTime: Long = delegate.presentingTime().toLong()
    actual val graphicsMemory: Long = delegate.graphicsMemory().toLong()
    actual fun close() = delegate.destroy()
}