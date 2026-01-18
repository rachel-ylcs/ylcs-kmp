@file:JsModule("libpag")
@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("FunctionName", "PropertyName", "ConstPropertyName")
package org.libpag

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.TexImageSource
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.WebGLFramebuffer
import org.khronos.webgl.WebGLRenderingContext
import org.khronos.webgl.WebGLTexture
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageBitmap
import org.w3c.files.File
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsBoolean
import kotlin.js.JsModule
import kotlin.js.JsName
import kotlin.js.Promise
import kotlin.js.definedExternally

external interface PAGViewOptions : JsAny {
    var useScale: Boolean?
    var useCanvas2D: Boolean?
    var firstFrame: Boolean?
}

external class PAG : JsAny {
    val PAGPlayer: PAGPlayer.Companion
    val PAGFile: PAGFile.Companion
    val PAGView: PAGView.Companion
    val PAGSurface: PAGSurface.Companion
    val PAGComposition: PAGComposition.Companion
    val SDKVersion: () -> String
}

internal external fun PAGInit(moduleOption: JsAny? = definedExternally): Promise<PAG?>

@JsName("Rect")
external class PAGRect : JsAny {
    val left: Double
    val top: Double
    val right: Double
    val bottom: Double
}

@JsName("Matrix")
external class PAGMatrix : JsAny {
    companion object {
        fun makeAll(scaleX: Double, skewX: Double, transX: Double, skewY: Double, scaleY: Double, transY: Double, pers0: Double = definedExternally, pers1: Double = definedExternally, pers2: Double = definedExternally): PAGMatrix
        fun makeScale(scaleX: Double, scaleY: Double): PAGMatrix
        fun makeTrans(dx: Double, dy: Double): PAGMatrix
    }

    // scaleX
    var a: Double
    // skewY
    var b: Double
    // skewX
    var c: Double
    // scaleY
    var d: Double
    // transX
    var tx: Double
    // transY
    var ty: Double

    fun get(index: Int): Double
    fun set(index: Int, value: Double)
    fun setAll(scaleX: Double, skewX: Double, transX: Double, skewY: Double, scaleY: Double, transY: Double)
    fun setAffine(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double)
    fun reset()
    fun setTranslate(dx: Double, dy: Double)
    fun setScale(sx: Double, sy: Double, px: Double = definedExternally, py: Double = definedExternally)
    fun setRotate(degrees: Double, px: Double = definedExternally, py: Double = definedExternally)
    fun setSinCos(sinV: Double, cosV: Double, px: Double = definedExternally, py: Double = definedExternally)
    fun setSkew(kx: Double, ky: Double, px: Double = definedExternally, py: Double = definedExternally)
    fun setConcat(a: PAGMatrix, b: PAGMatrix)
    fun preTranslate(dx: Double, dy: Double)
    fun preScale(sx: Double, sy: Double, px: Double = definedExternally, py: Double = definedExternally)
    fun preRotate(degrees: Double, px: Double = definedExternally, py: Double = definedExternally)
    fun preSkew(kx: Double, ky: Double, px: Double = definedExternally, py: Double = definedExternally)
    fun preConcat(other: PAGMatrix)
    fun postTranslate(dx: Double, dy: Double)
    fun postScale(sx: Double, sy: Double, px: Double = definedExternally, py: Double = definedExternally)
    fun postRotate(degrees: Double, px: Double = definedExternally, py: Double = definedExternally)
    fun postSkew(kx: Double, ky: Double, px: Double = definedExternally, py: Double = definedExternally)
    fun postConcat(other: PAGMatrix)
    fun destroy()
}

external class PAGImage : JsAny {
    companion object {
        fun fromFile(data: File): Promise<PAGImage>
        fun fromSource(source: TexImageSource): PAGImage
        fun fromPixels(pixels: Uint8Array, width: Int, height: Int, colorType: Int, alphaType: Int): PAGImage
        fun fromTexture(textureID: Int, width: Int, height: Int, flipY: Boolean): PAGImage
    }

    fun width(): Int
    fun height(): Int
    fun scaleMode(): Int
    fun setScaleMode(mode: Int)
    fun matrix(): PAGMatrix
    fun setMatrix(matrix: PAGMatrix)
    fun destroy()
}

external class PAGSurface : JsAny {
    companion object {
        fun fromCanvas(canvasID: String): PAGSurface
        fun fromTexture(textureID: Int, width: Int, height: Int, flipY: Boolean): PAGSurface
        fun fromRenderTarget(frameBufferID: Int, width: Int, height: Int, flipY: Boolean): PAGSurface
    }

    fun width(): Int
    fun height(): Int
    fun updateSize()
    fun clearAll(): Boolean
    fun freeCache()
    fun readPixels(colorType: Int, alphaType: Int): Uint8Array?
}

open external class PAGLayer : JsAny {
    fun uniqueID(): Int
    fun layerType(): Int
    fun layerName(): String
    fun matrix(): PAGMatrix
    fun setMatrix(matrix: PAGMatrix)
    fun resetMatrix()
    fun getTotalMatrix(): PAGMatrix
    fun alpha(): Double
    fun setAlpha(opacity: Double)
    fun visible(): Boolean
    fun setVisible(visible: Boolean)
    fun editableIndex(): Int
    fun parent(): PAGComposition
    // fun markers()
    fun localTimeToGlobal(localTime: Long): Long
    fun globalToLocalTime(globalTime: Long): Long
    fun duration(): Long
    fun frameRate(): Float
    fun startTime(): Long
    fun setStartTime(time: Long)
    fun currentTime(): Long
    fun setCurrentTime(time: Long)
    fun getProgress(): Double
    fun setProgress(percent: Double)
    fun preFrame()
    fun nextFrame()
    fun getBounds(): PAGRect
    fun trackMatteLayer(): PAGLayer
    fun excludedFromTimeline(): Boolean
    fun setExcludedFromTimeline(value: Boolean)
    fun isPAGFile(): Boolean
    // fun asTypeLayer()
}

open external class PAGComposition : PAGLayer {
    companion object {
        fun make(width: Int, height: Int): PAGComposition
    }

    fun width(): Int
    fun height(): Int
    fun setContentSize(width: Int, height: Int)
    fun numChildren(): Int
    fun getLayerAt(index: Int): PAGLayer
    fun getLayerIndex(pagLayer: PAGLayer): Int
    fun setLayerIndex(pagLayer: PAGLayer, index: Int): Int
    fun addLayer(pagLayer: PAGLayer): Boolean
    fun addLayerAt(pagLayer: PAGLayer, index: Int): Boolean
    fun contains(pagLayer: PAGLayer): Boolean
    fun removeLayer(pagLayer: PAGLayer): PAGLayer
    fun removeLayerAt(index: Int): PAGLayer
    fun removeAllLayers()
    fun swapLayer(pagLayer1: PAGLayer, pagLayer2: PAGLayer)
    fun swapLayerAt(index1: Int, index2: Int)
    fun audioBytes(): Uint8Array?
    // fun audioMarkers()
    fun audioStartTime(): Long
    fun getLayersByName(layerName: String): PAGLayer
    fun getLayersUnderPoint(localX: Double, localY: Double): PAGLayer
}

external class PAGFile : PAGComposition {
    companion object {
        fun loadFromBuffer(data: ArrayBuffer): Promise<PAGFile>
        fun maxSupportedTagLevel(): Int
        fun tagLevel(): Int
        fun numTexts(): Int
        fun numImages(): Int
        fun numVideos(): Int
        // fun getTextData()
        // fun replaceText()
        // fun replaceImage()
        // fun getLayersByEditableIndex()
        // fun getEditableIndices()
        // fun timeStretchMode()
        // fun setTimeStretchMode()
        fun setDuration(duration: Long)
        fun copyOriginal(): PAGFile
    }
}

external class PAGPlayer : JsAny {
    companion object {
        fun create(): PAGPlayer
    }

    fun setProgress(progress: Double)
    fun flush(): Promise<JsBoolean>
    fun duration(): Long
    fun getProgress(): Double
    fun currentFrame(): Int
    fun videoEnabled(): Boolean
    fun setVideoEnabled(enabled: Boolean)
    fun cacheEnabled(): Boolean;
    fun setCacheEnabled(enabled: Boolean)
    fun cacheScale(): Float
    fun setCacheScale(value: Float)
    fun maxFrameRate(): Float
    fun setMaxFrameRate(value: Float)
    fun scaleMode(): Int
    fun setScaleMode(value: Int)
    fun setSurface(pagSurface: PAGSurface?)
    fun getComposition(): PAGComposition
    fun setComposition(pagComposition: PAGComposition?)
    fun getSurface(): PAGSurface
    fun matrix(): PAGMatrix
    fun setMatrix(matrix: PAGMatrix)
    fun nextFrame()
    fun preFrame()
    fun autoClear(): Boolean
    fun setAutoClear(value: Boolean)
    fun getBounds(): PAGRect
    // fun getLayersUnderPoint()
    // fun hitTestPoint()
    fun renderingTime(): Long
    fun imageDecodingTime(): Long
    fun presentingTime(): Long
    fun graphicsMemory(): Double
    fun prepare(): Promise<JsAny?>
    // fun linkVideoReader()
    // fun unlinkVideoReader()
}

external class BackendContext(handle: Int, externallyOwned: Boolean = definedExternally) : JsAny {
    companion object {
        fun from(gl: WebGLRenderingContext): BackendContext
        fun from(gl: BackendContext): BackendContext
    }

    val handle: Int

    fun getContext(): WebGLRenderingContext?
    fun makeCurrent(): Boolean
    fun clearCurrent()
    fun registerTexture(texture: WebGLTexture)
    fun getTexture(handle: Int): WebGLTexture?
    fun unregisterTexture(handle: Int)
    fun registerRenderTarget(frameBuffer: WebGLFramebuffer)
    fun getRenderTarget(handle: Int): WebGLFramebuffer?
    fun unregisterRenderTarget(handle: Int)
    fun destroy()
}

external class PAGView(pagLayer: PAGLayer, canvasElement: HTMLCanvasElement) : JsAny {
    companion object {
        fun init(composition: PAGComposition, canvas: HTMLCanvasElement, initOptions: PAGViewOptions? = definedExternally): Promise<PAGView?>
        fun makePAGSurface(pagGlContext: BackendContext, width: Int, height: Int): PAGSurface
    }

    var repeatCount: Int
    var isPlaying: Boolean

    fun duration(): Long
    fun addListener(eventName: String, listener: (event: JsAny) -> Unit)
    fun removeListener(eventName: String, listener: ((event: JsAny) -> Unit)?): Boolean
    fun play(): Promise<JsAny?>
    fun pause()
    fun stop(notification: Boolean?): Promise<JsAny?>
    fun setRepeatCount(repeatCount: Int)
    fun getProgress(): Double
    fun currentFrame(): Int
    fun setProgress(progress: Double): Double
    fun videoEnabled(): Boolean
    fun setVideoEnabled(enable: Boolean)
    fun cacheEnabled(): Boolean
    fun setCacheEnabled(enable: Boolean)
    fun cacheScale(): Float
    fun setCacheScale(value: Float)
    fun maxFrameRate(): Float
    fun setMaxFrameRate(value: Float)
    fun scaleMode(): Int
    fun setScaleMode(value: Int)
    fun flush(): Promise<JsBoolean>
    fun freeCache()
    fun getComposition(): PAGComposition?
    fun setComposition(pagComposition: PAGComposition?)
    fun matrix(): PAGMatrix
    fun setMatrix(matrix: PAGMatrix)
    // fun getLayersUnderPoint()
    fun updateSize()
    fun prepare(): Promise<JsAny?>
    fun makeSnapshot(): Promise<ImageBitmap>
    fun destroy()
}