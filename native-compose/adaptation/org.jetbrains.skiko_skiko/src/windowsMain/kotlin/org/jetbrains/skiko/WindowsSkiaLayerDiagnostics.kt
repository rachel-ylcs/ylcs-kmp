package org.jetbrains.skiko

import org.jetbrains.skia.PixelGeometry

@InternalSkikoApi
data class WindowsSkiaLayerDiagnostics(
    val renderApi: GraphicsApi,
    val rendererDescription: String?,
    val deviceName: String?,
    val renderedFrameCount: Long,
    val fallbackCount: Int,
    val contextRecoveryCount: Int,
    val lastFailure: String?,
    val isVsyncEnabled: Boolean,
    val frameBuffering: FrameBuffering,
    val effectiveFrameBufferCount: Int?,
    val transparencyRequested: Boolean,
    val hasTransparentWindowBuffer: Boolean,
    val adapterPriority: GpuPriority,
    val gpuResourceCacheLimit: Long,
    val pixelGeometry: PixelGeometry,
    val fpsAverage: Int?,
    val fpsMinimum: Int?,
    val fpsMaximum: Int?,
)