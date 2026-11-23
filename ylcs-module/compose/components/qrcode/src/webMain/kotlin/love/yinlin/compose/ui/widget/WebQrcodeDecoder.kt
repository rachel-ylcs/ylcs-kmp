@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.ui.widget

import kotlinx.coroutines.await
import love.yinlin.extension.cast
import love.yinlin.extension.catchingNull
import love.yinlin.extension.cleaning
import love.yinlin.extension.createElement
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8ClampedArray
import org.khronos.webgl.toInt8Array
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.ImageBitmap
import kotlin.js.Promise
import kotlin.math.min

@JsModule("jsqr")
@JsName("default")
private external fun decodeQrPixels(data: Uint8ClampedArray, width: Int, height: Int): JsAny?

private fun decodedQrText(result: JsAny?): String? = js("result == null ? null : result.data")

private fun createQrDetector(): Promise<JsAny?> = js("""
    Promise.resolve().then(function () {
        const detector = globalThis.BarcodeDetector;
        if (!detector) return null;
        return detector.getSupportedFormats().then(function (formats) {
            return formats.includes('qr_code') ? new detector({ formats: ['qr_code'] }) : null;
        });
    })
""")

private fun detectQr(detector: JsAny, canvas: HTMLCanvasElement): Promise<JsString?> =
    js("detector.detect(canvas).then(function (codes) { return codes.length ? codes[0].rawValue : null; })")

private fun loadQrImage(bytes: Int8Array): Promise<ImageBitmap> = js("createImageBitmap(new Blob([bytes]))")

internal class WebQrcodeDecoder {
    private val nativeDetector by lazy { createQrDetector() }

    internal suspend fun decodeVideo(canvas: HTMLCanvasElement, video: HTMLVideoElement): String? {
        val width = video.videoWidth
        val height = video.videoHeight
        if (width <= 0 || height <= 0 || video.clientWidth <= 0 || video.clientHeight <= 0 || video.readyState < 2) return null

        // 将取景框映射回 object-fit: cover 裁切前的视频坐标。
        val scale = min(width.toDouble() / video.clientWidth, height.toDouble() / video.clientHeight)
        val side = min(video.clientWidth, video.clientHeight) * scale * 0.8
        val output = side.toInt().coerceIn(1, 640)
        val context = prepare(canvas, output, output)
        context.drawImage(video, (width - side) / 2, (height - side) / 2, side, side, 0.0, 0.0, output.toDouble(), output.toDouble())
        return decode(canvas, context)
    }

    internal suspend fun decodeImage(data: ByteArray): String? {
        val image = loadQrImage(data.toInt8Array()).await()
        val canvas = createElement<HTMLCanvasElement>()
        val context = cleaning({
            val scale = min(1.0, 1600.0 / maxOf(image.width, image.height))
            val width = (image.width * scale).toInt().coerceAtLeast(1)
            val height = (image.height * scale).toInt().coerceAtLeast(1)
            prepare(canvas, width, height).also { it.drawImage(image, 0.0, 0.0, width.toDouble(), height.toDouble()) }
        }, image::close)
        return decode(canvas, context)
    }

    private fun prepare(canvas: HTMLCanvasElement, width: Int, height: Int): CanvasRenderingContext2D {
        if (canvas.width != width) canvas.width = width
        if (canvas.height != height) canvas.height = height
        return canvas.getContext("2d") as CanvasRenderingContext2D
    }

    private suspend fun decode(canvas: HTMLCanvasElement, context: CanvasRenderingContext2D): String? {
        val detector = catchingNull { nativeDetector.await() }
        if (detector != null) catchingNull { return detectQr(detector, canvas).await()?.cast }

        val pixels = context.getImageData(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
        return decodedQrText(decodeQrPixels(pixels.data, pixels.width, pixels.height))
    }
}
