@file:JsModule("libpag")
@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("FunctionName", "PropertyName", "ConstPropertyName")
package org.libpag

import org.khronos.webgl.*
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageBitmap
import org.w3c.files.File
import kotlin.js.*

external interface Marker : JsAny {
    var startTime: Double
    var duration: Double
    var comment: String
}

external interface Color : JsAny {
    var red: Double
    var green: Double
    var blue: Double
}

external interface YUVBuffer : JsAny {
    var data: JsArray<JsNumber>
    var lineSize: JsArray<JsNumber>
}

external interface DebugData : JsAny {
    var FPS: Double?
    var flushTime: Double?
}

external interface Rect : JsAny {
    var left: Double
    var top: Double
    var right: Double
    var bottom: Double
}

external interface PAGVideoRange : JsAny {
    var startTime: Double
    var endTime: Double
    var playDuration: Double
    var reversed: Boolean
}

external class Matrix : JsAny {
    companion object {
        fun makeAll(scaleX: Double, skewX: Double, transX: Double, skewY: Double, scaleY: Double, transY: Double, pers0: Double = definedExternally, pers1: Double = definedExternally, pers2: Double = definedExternally): Matrix
        fun makeScale(scaleX: Double, scaleY: Double): Matrix
        fun makeTrans(dx: Double, dy: Double): Matrix
    }

    var a: Double // scaleX
    var b: Double // skewY
    var c: Double // skewX
    var d: Double // scaleY
    var tx: Double // transX
    var ty: Double // transY

    fun set(index: Int, value: Double)
    fun setAffine(a: Double, b: Double, c: Double, d: Double, tx: Double, ty: Double)
}

external class TextDocument : JsAny {
    var applyFill: Boolean
    var applyStroke: Boolean
    var baselineShift: Double
    var boxText: Boolean
    var firstBaseLine: Double
    var fauxBold: Boolean
    var fauxItalic: Boolean
    var fillColor: Color
    var fontFamily: String
    var fontStyle: String
    var fontSize: Double
    var strokeColor: Color
    var strokeOverFill: Boolean
    var strokeWidth: Double
    var text: String
    var leading: Double
    var tracking: Double
    var backgroundColor: Color
    var backgroundAlpha: Double
}

external class SoftwareDecoder : JsAny {
    fun onConfigure(headers: JsArray<Uint8Array>, mimeType: String, width: Int, height: Int): Boolean
    fun onSendBytes(bytes: Uint8Array, timestamp: Double): Double // 0:Success -1:TryAgainLater -2:Error
    fun onDecodeFrame(): Double // 0:Success -1:TryAgainLater -2:Error
    fun onEndOfStream(): Double // 0:Success -1:TryAgainLater -2:Error
    fun onFlush()
    fun onRenderFrame(): YUVBuffer?
    fun onRelease()
}

external class SoftwareDecoderFactory : JsAny {
    fun createSoftwareDecoder(pag: PAG): SoftwareDecoder?
}

external class PAGFont : JsAny {
    companion object {
        fun create(fontFamily: String, fontStyle: String): PAGFont
        fun registerFont(family: String, data: File): Promise<PAGFont>
        fun registerFallbackFontNames(fontNames: JsArray<JsString> = definedExternally)
    }

    val fontFamily: String
    val fontStyle: String
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
    fun matrix(): Matrix
    fun setMatrix(matrix: Matrix)
    fun destroy()
}

open external class PAGLayer : JsAny {
    companion object {

    }

    fun uniqueID(): Int
    fun layerType(): Int
    fun layerName(): String
    fun matrix(): Matrix
    fun setMatrix(matrix: Matrix)
    fun resetMatrix()
    fun getTotalMatrix(): Matrix
    fun alpha(): Double
    fun setAlpha(opacity: Double)
    fun visible(): Boolean
    fun setVisible(visible: Boolean)
    fun editableIndex(): Int
    fun parent(): PAGComposition
    // fun markers()
    fun localTimeToGlobal(localTime: Double): Double
    fun globalToLocalTime(globalTime: Double): Double
    fun duration(): Double
    fun frameRate(): Float
    fun startTime(): Double
    fun setStartTime(time: Double)
    fun currentTime(): Double
    fun setCurrentTime(time: Double)
    fun getProgress(): Double
    fun setProgress(percent: Double)
    fun preFrame()
    fun nextFrame()
    fun getBounds(): Rect
    fun trackMatteLayer(): PAGLayer
    fun excludedFromTimeline(): Boolean
    fun setExcludedFromTimeline(value: Boolean)
    fun isPAGFile(): Boolean
    // fun asTypeLayer()
}

external class PAGSolidLayer : PAGLayer {
    companion object {
        fun make(duration: Double, width: Int, height: Int, solidColor: Color, opacity: Double): PAGSolidLayer
    }

    fun solidColor(): Color
    fun setSolidColor(color: Color)
}

external class PAGImageLayer : PAGLayer {
    companion object {
        fun make(width: Int, height: Int, duration: Double): PAGImageLayer
    }

    fun contentDuration(): Double
    fun getVideoRanges(): JsArray<PAGVideoRange>
    fun replaceImage(image: PAGImage)
    fun setImage(image: PAGImage)
    fun layerTimeToContent(layerTime: Double): Double
    fun contentTimeToLayer(contentTime: Double): Double
    fun imageBytes(): Uint8Array?
}

external class PAGTextLayer : PAGLayer {
    companion object {
        fun make(duration: Double, text: String, fontSize: Double, fontFamily: String, fontStyle: String): PAGTextLayer
        fun make(duration: Double, textDocument: TextDocument): PAGTextLayer
    }

    fun fillColor(): Color
    fun setFillColor(color: Color)
    fun font(): PAGFont
    fun setFont(font: PAGFont)
    fun fontSize(): Double
    fun setFontSize(size: Double)
    fun strokeColor(): Color
    fun setStrokeColor(color: Color)
    fun text(): String
    fun setText(text: String)
    fun reset()
}

open external class PAGComposition : PAGLayer {
    companion object {
        fun make(width: Int, height: Int): PAGComposition
    }

    fun width(): Int
    fun height(): Int
    fun setContentSize(width: Int, height: Int)
    fun numChildren(): Int
    fun getLayerAt(index: Int): PAGLayer?
    fun getLayerIndex(pagLayer: PAGLayer): Int
    fun setLayerIndex(pagLayer: PAGLayer, index: Int): Int
    fun addLayer(pagLayer: PAGLayer): Boolean
    fun addLayerAt(pagLayer: PAGLayer, index: Int): Boolean
    fun contains(pagLayer: PAGLayer): Boolean
    fun removeLayer(pagLayer: PAGLayer): PAGLayer?
    fun removeLayerAt(index: Int): PAGLayer?
    fun removeAllLayers()
    fun swapLayer(pagLayer1: PAGLayer, pagLayer2: PAGLayer)
    fun swapLayerAt(index1: Int, index2: Int)
    fun audioBytes(): Uint8Array?
    // fun audioMarkers()
    fun audioStartTime(): Double
    fun getLayersByName(layerName: String): PAGLayer
    fun getLayersUnderPoint(localX: Double, localY: Double): PAGLayer
}

external class PAGFile : PAGComposition {
    companion object {
        fun loadFromBuffer(data: ArrayBuffer): Promise<PAGFile>
        fun maxSupportedTagLevel(): Int
    }

    fun tagLevel(): Int
    fun numTexts(): Int
    fun numImages(): Int
    fun numVideos(): Int
    fun getTextData(): TextDocument
    fun replaceText(editableTextIndex: Int, textData: TextDocument)
    fun replaceImage(editableImageIndex: Int, pagImage: PAGImage)
    // fun getLayersByEditableIndex()
    fun getEditableIndices(layerType: Int): JsArray<JsNumber>
    fun timeStretchMode(): Int
    fun setTimeStretchMode(mode: Int)
    fun setDuration(duration: Double)
    fun copyOriginal(): PAGFile
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
    fun destroy()
}

external class PAGPlayer : JsAny {
    companion object {
        fun create(): PAGPlayer
    }

    fun setProgress(progress: Double)
    fun flush(): Promise<JsBoolean>
    fun duration(): Double
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
    fun getComposition(): PAGComposition?
    fun setComposition(pagComposition: PAGComposition?)
    fun getSurface(): PAGSurface?
    fun matrix(): Matrix
    fun setMatrix(matrix: Matrix)
    fun nextFrame()
    fun preFrame()
    fun autoClear(): Boolean
    fun setAutoClear(value: Boolean)
    fun getBounds(): Rect
    // fun getLayersUnderPoint()
    fun hitTestPoint(layer: PAGLayer, x: Double, y: Double, pixelHitTest: Boolean = definedExternally): Boolean
    fun renderingTime(): Double
    fun imageDecodingTime(): Double
    fun presentingTime(): Double
    fun graphicsMemory(): Double
    fun prepare(): Promise<JsAny?>
    fun destroy()
    // fun linkVideoReader()
    // fun unlinkVideoReader()
}

external class PAG : JsAny {
    var PAGPlayer: PAGPlayer.Companion
    var PAGFile: PAGFile.Companion
    var PAGView: PAGView.Companion
    var PAGFont: PAGFont.Companion
    var PAGImage: PAGImage.Companion
    var PAGLayer: PAGLayer.Companion
    var PAGComposition: PAGComposition.Companion
    var PAGSurface: PAGSurface.Companion
    var PAGTextLayer: PAGTextLayer.Companion
    var PAGImageLayer: PAGImage.Companion
    var PAGSolidLayer: PAGSolidLayer.Companion
    var Matrix: Matrix.Companion
    var SDKVersion: () -> String
    var currentPlayer: PAGPlayer?
}

internal external fun PAGInit(moduleOption: JsAny? = definedExternally): Promise<PAG?>

external interface PAGViewOptions : JsAny {
    var useScale: Boolean?
    var useCanvas2D: Boolean?
    var firstFrame: Boolean?
}

external class PAGView(pagLayer: PAGLayer, canvasElement: HTMLCanvasElement) : JsAny {
    companion object {
        fun init(composition: PAGComposition, canvas: HTMLCanvasElement, initOptions: PAGViewOptions? = definedExternally): Promise<PAGView?>
    }

    var repeatCount: Int
    var isPlaying: Boolean

    fun duration(): Double
    fun addListener(eventName: String, listener: (event: JsAny) -> Unit)
    fun removeListener(eventName: String, listener: ((event: JsAny) -> Unit)?): Boolean
    fun play(): Promise<JsAny?>
    fun pause()
    fun stop(notification: Boolean = definedExternally): Promise<JsAny?>
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
    fun matrix(): Matrix
    fun setMatrix(matrix: Matrix)
    // fun getLayersUnderPoint()
    fun updateSize()
    fun prepare(): Promise<JsAny?>
    fun makeSnapshot(): Promise<ImageBitmap>
    fun destroy()
    fun getDebugData(): DebugData
    fun setDebugData(debugData: DebugData)
}