@file:OptIn(BetaInteropApi::class)

package org.jetbrains.skiko.redrawer

import kotlinx.cinterop.*
import org.jetbrains.skiko.SkiaLayer
import platform.CoreFoundation.CFTimeInterval
import platform.CoreGraphics.CGRectMake
import platform.CoreVideo.CVTimeStamp
import platform.OpenGLCommon.CGLContextObj
import platform.OpenGLCommon.CGLPixelFormatObj
import platform.OpenGLCommon.CGLSetCurrentContext
import platform.QuartzCore.*

internal class MacosGLLayer : CAOpenGLLayer {
    private lateinit var skiaLayer: SkiaLayer
    private lateinit var redrawer: MacOsOpenGLRedrawer

    @OverrideInit
    constructor(): super()
    @OverrideInit
    constructor(layer: Any): super(layer)

    fun init(layer: SkiaLayer, redrawer: MacOsOpenGLRedrawer) {
        skiaLayer = layer
        this.redrawer = redrawer
        this.setNeedsDisplayOnBoundsChange(true)
        this.removeAllAnimations()
        this.setAutoresizingMask(kCALayerWidthSizable or kCALayerHeightSizable )
        skiaLayer.nsView.layer = this
        skiaLayer.nsView.wantsLayer = true
        this.contentsGravity = kCAGravityTopLeft
    }

    fun setFrame(x: Int, y: Int, width: Int, height: Int) {
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        this.frame = CGRectMake(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        CATransaction.commit()
        CATransaction.flush()
    }

    fun dispose() {
        this.removeFromSuperlayer()
        if (this::skiaLayer.isInitialized && skiaLayer.nsView.layer == this) {
            skiaLayer.nsView.layer = null
            skiaLayer.nsView.wantsLayer = false
        }
    }

    override fun canDrawInCGLContext(ctx: CGLContextObj?, pixelFormat: CGLPixelFormatObj?, forLayerTime: CFTimeInterval, displayTime: CPointer<CVTimeStamp>?): Boolean {
        return true
    }

    override fun drawInCGLContext(ctx: CGLContextObj?, pixelFormat: CGLPixelFormatObj?, forLayerTime: CFTimeInterval, displayTime: CPointer<CVTimeStamp>?) {
        CGLSetCurrentContext(ctx)
        try {
            redrawer.drawInLayerContext()
        }
        catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }

        super.drawInCGLContext(ctx, pixelFormat, forLayerTime, displayTime)
    }
}