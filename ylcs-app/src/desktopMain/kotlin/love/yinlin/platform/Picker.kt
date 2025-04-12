package love.yinlin.platform

import javafx.application.Platform
import javafx.stage.FileChooser
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.extension.Sources
import love.yinlin.extension.safeToSources
import java.io.File
import javax.swing.filechooser.FileSystemView
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private var lastChoosePath: File? = FileSystemView.getFileSystemView()?.homeDirectory
private val pictureFilter = FileChooser.ExtensionFilter("图片", "*.jpg", "*.png", "*.webp")

actual object PicturePicker {
    actual suspend fun pick(): Source? = suspendCoroutine { continuation ->
        Platform.runLater {
            continuation.safeResume {
                val file = FileChooser().run {
                    title = "选择一张图片"
                    initialDirectory = lastChoosePath
                    extensionFilters += pictureFilter
                    showOpenDialog(null)
                }
                lastChoosePath = file.parentFile
                continuation.resume(file.inputStream().asSource().buffered())
            }
        }
    }

    actual suspend fun pick(maxNum: Int): Sources<Source>? = suspendCoroutine { continuation ->
        Platform.runLater {
            continuation.safeResume {
                require(maxNum > 0)
                val files = FileChooser().run {
                    title = "最多选择${maxNum}张图片"
                    initialDirectory = lastChoosePath
                    extensionFilters += pictureFilter
                    showOpenMultipleDialog(null)
                }
                require(files != null && files.size in 1 .. maxNum)
                continuation.resume(files.safeToSources { it.inputStream().asSource().buffered() })
            }
        }
    }

    actual suspend fun prepareSave(filename: String): Pair<Any, Sink>? = suspendCoroutine { continuation ->
        Platform.runLater {
            continuation.safeResume {
                val file = FileChooser().run {
                    title = "保存图片"
                    initialDirectory = lastChoosePath
                    initialFileName = filename
                    extensionFilters += pictureFilter
                    showSaveDialog(null)
                }
                val path = Path(file.absolutePath)
                continuation.resume(path to SystemFileSystem.sink(path).buffered())
            }
        }
    }

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = Unit

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) SystemFileSystem.delete(origin as Path, false)
    }
}