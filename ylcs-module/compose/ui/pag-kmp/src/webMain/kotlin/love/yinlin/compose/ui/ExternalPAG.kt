@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.ui

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLCanvasElement
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsBoolean
import kotlin.js.Promise
import kotlin.js.definedExternally

open external class PAGLayer : JsAny

open external class PAGComposition : PAGLayer

external class PAGFile : PAGComposition {
    companion object {
        fun load(data: ArrayBuffer): Promise<PAGFile>
    }
}

external class PAGPlayer {
    companion object {
        fun create(): PAGPlayer
    }

    val isDestroyed: Boolean

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
    fun nextFrame()
    fun preFrame()
    fun autoClear(): Boolean
    fun setAutoClear(value: Boolean)
    fun prepare(): Promise<JsAny?>
    fun destroy()
}

external class PAGSurface {
    companion object {
        fun fromCanvas(canvasID: String): PAGSurface;
        fun fromTexture(textureID: Int, width: Int, height: Int, flipY: Boolean): PAGSurface;
        fun fromRenderTarget(frameBufferID: Int, width: Int, height: Int, flipY: Boolean): PAGSurface;
    }

    val isDestroyed: Boolean

    fun width(): Int
    fun height(): Int
    fun updateSize()
    fun clearAll(): Boolean
    fun freeCache()
    fun readPixels(colorType: Int, alphaType: Int): Uint8Array?
    fun destroy()
}

external interface PAGViewOptions {
    var useScale: Boolean?
    var useCanvas2D: Boolean?
    var firstFrame: Boolean?
}

external class PAGView : JsAny {
    companion object {
        fun init(file: PAGComposition, canvas: HTMLCanvasElement, initOptions: PAGViewOptions? = definedExternally): Promise<PAGView?>
    }

    var repeatCount: Int
    var isPlaying: Boolean
    val isDestroyed: Boolean

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
    fun getComposition(): PAGComposition
    fun setComposition(pagComposition: PAGComposition)
    fun updateSize()
    fun prepare(): Promise<JsAny?>
    fun destroy()
}

external class PAG : JsAny {
    val PAGPlayer: PAGPlayer.Companion
    val PAGFile: PAGFile.Companion
    val PAGView: PAGView.Companion
    val PAGSurface: PAGSurface.Companion
}

external fun PAGInit(moduleOption: JsAny? = definedExternally): Promise<PAG?>