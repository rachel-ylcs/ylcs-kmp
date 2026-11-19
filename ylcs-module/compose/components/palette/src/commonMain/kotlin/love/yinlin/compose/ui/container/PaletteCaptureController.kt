package love.yinlin.compose.ui.container

import androidx.compose.ui.graphics.layer.GraphicsLayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 捕获任务只持有最新采样层。绘制失效会合并为待处理标记，避免动画帧触发并行分析。
 */
internal class PaletteCaptureController(
    private val coroutineScope: CoroutineScope,
    private val state: PaletteState,
) {
    private var latestLayer: GraphicsLayer? = null
    private var captureJob: Job? = null
    private var capturePending = false
    private var hasCaptured = false
    private var disposed = false

    fun onBackdropDraw(layer: GraphicsLayer) {
        if (disposed || layer.isReleased) return
        latestLayer = layer
        capturePending = true
        startCapture()
    }

    fun refresh() {
        val layer = latestLayer
        if (layer != null && !layer.isReleased) onBackdropDraw(layer)
        else capturePending = true
    }

    private fun startCapture() {
        if (disposed || captureJob?.isActive == true) return

        val job = coroutineScope.launch(start = CoroutineStart.LAZY) {
            captureLoop()
        }
        captureJob = job
        job.start()
    }

    private suspend fun captureLoop() {
        try {
            while (capturePending && !disposed) {
                capturePending = false
                if (hasCaptured) delay(MIN_CAPTURE_INTERVAL_MILLIS)
                if (disposed) break

                val layer = latestLayer?.takeUnless { it.isReleased } ?: break
                state.beginCapture()

                // 图层转成小尺寸 ImageBitmap 只发生在采样任务中；量化在 Default dispatcher 执行。
                val bitmap = layer.toImageBitmap()
                val previousFingerprint = state.fingerprint
                val result = withContext(Dispatchers.Default) {
                    Palette.from(bitmap).generateIfChanged(previousFingerprint)
                }

                if (result.palette != null) {
                    state.fingerprint = result.fingerprint
                    state.publish(result.palette)
                } else {
                    state.finishCapture()
                }
                hasCaptured = true
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            state.fail(error)
        } finally {
            captureJob = null
            if (capturePending && !disposed) startCapture()
        }
    }

    fun dispose() {
        disposed = true
        capturePending = false
        latestLayer = null
        captureJob?.cancel()
        captureJob = null
    }

    private companion object {
        const val MIN_CAPTURE_INTERVAL_MILLIS = 400L
    }
}
