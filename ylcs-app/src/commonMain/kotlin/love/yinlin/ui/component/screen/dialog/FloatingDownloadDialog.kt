package love.yinlin.ui.component.screen.dialog

import androidx.compose.runtime.Stable
import kotlinx.io.Sink
import love.yinlin.extension.fileSizeString
import love.yinlin.platform.app
import love.yinlin.platform.safeDownload
import love.yinlin.ui.component.screen.FloatingDialogProgress

@Stable
class FloatingDownloadDialog : FloatingDialogProgress() {
    suspend fun openSuspend(url: String, sink: Sink, onSave: suspend () -> Unit): Boolean {
        super.openSuspend()
        val result = sink.use {
            val result = app.fileClient.safeDownload(
                url = url,
                sink = it,
                isCancel = { !this.isOpen },
                onGetSize = { total -> this.total = total.fileSizeString },
                onTick = { current, total ->
                    this.current = current.fileSizeString
                    if (total != 0L) this.progress = current / total.toFloat()
                }
            )
            if (result) onSave()
            result
        }
        super.close()
        return result
    }
}