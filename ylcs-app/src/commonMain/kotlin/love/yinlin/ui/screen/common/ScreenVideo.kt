package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.common.Scheme
import love.yinlin.common.Uri
import love.yinlin.extension.fileSizeString
import love.yinlin.platform.Picker
import love.yinlin.platform.app
import love.yinlin.platform.safeDownload
import love.yinlin.ui.component.platform.VideoPlayer
import love.yinlin.ui.component.screen.FloatingDialogProgress
import love.yinlin.ui.component.screen.SubScreen

@Stable
class ScreenVideo(model: AppModel, val args: Args) : SubScreen<ScreenVideo.Args>(model) {
    @Stable
    @Serializable
    data class Args(val url: String)

    private val downloadDialog = FloatingDialogProgress()

    private fun downloadVideo() {
        val uri = Uri.parse(args.url)
        if (uri?.scheme == Scheme.Https || uri?.scheme == Scheme.Http) {
            val filename = args.url.substringAfterLast('/').substringBefore('?')
            launch {
                Picker.prepareSaveVideo(filename)?.let { (origin, sink) ->
                    downloadDialog.openSuspend()
                    val result = sink.use {
                        val result = app.fileClient.safeDownload(
                            url = args.url,
                            sink = it,
                            isCancel = { !downloadDialog.isOpen },
                            onGetSize = { total -> downloadDialog.total = total.fileSizeString },
                            onTick = { current, total ->
                                downloadDialog.current = current.fileSizeString
                                if (total != 0L) downloadDialog.progress = current / total.toFloat()
                            }
                        )
                        if (result) Picker.actualSave(filename, origin, sink)
                        result
                    }
                    Picker.cleanSave(origin, result)
                    downloadDialog.close()
                }
            }
        }
        else slot.tip.warning("此视频不支持下载")
    }

    override val title: String? = null

    @Composable
    override fun SubContent(device: Device) {
        VideoPlayer(
            url = args.url,
            modifier = Modifier.fillMaxSize(),
            onBack = { onBack() },
            onDownload = { downloadVideo() }
        )
    }

    @Composable
    override fun Floating() {
        downloadDialog.Land()
    }
}