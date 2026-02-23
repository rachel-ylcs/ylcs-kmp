package love.yinlin.compose.ui.floating

import kotlinx.io.Sink
import love.yinlin.app
import love.yinlin.cs.NetClient
import love.yinlin.extension.fileSizeString
import love.yinlin.extension.filenameOrRandom

typealias DialogDownload = DialogProgress<Boolean>

suspend fun DialogDownload.download(url: String, sink: Sink, onSave: suspend (Boolean) -> Unit): Boolean {
    return open("下载中...") {
        sink.use {
            NetClient.download(
                url = url,
                sink = it,
                isCancel = { !this.isOpen },
                onGetSize = { total -> this.total = total.fileSizeString },
                onTick = { current, total ->
                    this.current = current.fileSizeString
                    if (total != 0L) this.progress = current / total.toFloat()
                }
            ).also { result -> onSave(result) }
        }
    } ?: false
}

suspend fun DialogDownload.downloadPhoto(url: String) {
    val picker = app.picker
    val filename = url.filenameOrRandom(".webp")
    picker.prepareSavePicture(filename)?.let { (origin, sink) ->
        val result = download(url, sink) {
            if (it) picker.actualSave(filename, origin, sink)
        }
        picker.cleanSave(origin, result)
    }
}

suspend fun DialogDownload.downloadVideo(url: String) {
    val picker = app.picker
    val filename = url.filenameOrRandom(".mp4")
    picker.prepareSaveVideo(filename)?.let { (origin, sink) ->
        val result = download(url, sink) {
            if (it) picker.actualSave(filename, origin, sink)
        }
        picker.cleanSave(origin, result)
    }
}

suspend fun DialogDownload.downloadPhotos(pics: List<String>) {
    if (pics.isEmpty()) return
    open("下载中...") {
        val picker = app.picker
        this.total = pics.size.toString()
        for (index in pics.indices) {
            val pic = pics[index]
            val filename = pic.filenameOrRandom(".webp")
            picker.prepareSavePicture(filename)?.let { (origin, sink) ->
                sink.use {
                    NetClient.download(
                        url = pic,
                        sink = it,
                        isCancel = { !this.isOpen },
                        onGetSize = { },
                        onTick = { _, _ -> }
                    ).also { result ->
                        if (result) picker.actualSave(filename, origin, it)
                    }
                }.also { picker.cleanSave(origin, it) }
            }
            this.current = (index + 1).toString()
            this.progress = (index + 1) / pics.size.toFloat()
        }
        true
    }
}