@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.ui.widget

import org.w3c.dom.HTMLVideoElement
import kotlin.js.Promise

internal fun openQrCamera(video: HTMLVideoElement, isCurrent: () -> Boolean): Promise<JsBoolean> = js("""
    Promise.resolve().then(function () {
        return navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: 'environment' } } });
    }).then(function (stream) {
        if (!isCurrent()) {
            stream.getTracks().forEach(function (track) { track.stop(); });
            return false;
        }
        video.srcObject = stream;
        return video.play().then(function () { return true; });
    })
""")

internal fun closeQrCamera(video: HTMLVideoElement): Unit = js("""
    {
        const stream = video.srcObject;
        video.srcObject = null;
        if (stream) stream.getTracks().forEach(function (track) { track.stop(); });
    }
""")

internal fun hasQrTorch(video: HTMLVideoElement): Boolean = js("""
    {
        const track = video.srcObject && video.srcObject.getVideoTracks()[0];
        return !!(track && track.getCapabilities && track.getCapabilities().torch);
    }
""")

internal fun setQrTorch(video: HTMLVideoElement, enabled: Boolean): Promise<JsAny?> = js("video.srcObject.getVideoTracks()[0].applyConstraints({ advanced: [{ torch: enabled }] })")
