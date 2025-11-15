package love.yinlin.compose.ui.floating

import androidx.compose.runtime.Stable
import kotlinx.io.Sink
import love.yinlin.extension.fileSizeString
import love.yinlin.platform.NetClient
import love.yinlin.platform.download

@Stable
class FloatingDownloadDialog : FloatingDialogProgress() {
    suspend fun openSuspend(url: String, sink: Sink, onSave: suspend () -> Unit): Boolean {
        super.openSuspend()
        val result = sink.use {
            val result = NetClient.download(
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