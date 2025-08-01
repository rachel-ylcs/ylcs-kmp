package love.yinlin.platform

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.extension.Sources
import love.yinlin.extension.catchingNull
import love.yinlin.extension.safeToSources
import java.io.File

actual object Picker {
    var windowHandle: Long = 0L

    private external fun openFileDialog(parent: Long, title: String, filterName: String, filter: String): String?
    private external fun openMultipleFileDialog(parent: Long, maxNum: Int, title: String, filterName: String, filter: String): Array<String>
    private external fun saveFileDialog(parent: Long, title: String, filename: String, ext: String, filterName: String): String?

    actual suspend fun pickPicture(): Source? = Coroutines.io {
        catchingNull {
            val path = openFileDialog(windowHandle, "选择一张图片", "图片", "*.jpg;*.png;*.webp")
            File(path!!).inputStream().asSource().buffered()
        }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = Coroutines.io {
        catchingNull {
            require(maxNum > 0)
            val paths = openMultipleFileDialog(windowHandle, maxNum, "最多选择${maxNum}张图片", "图片", "*.jpg;*.png;*.webp")
            val files = paths.map { File(it) }
            require(files.size in 1 .. maxNum)
            files.safeToSources { it.inputStream().asSource().buffered() }
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = Coroutines.io {
        catchingNull {
            val path = openFileDialog(windowHandle, "选择一个文件", "文件", filter.joinToString(";"))
            File(path!!).inputStream().asSource().buffered()
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitPath? = Coroutines.io {
        catchingNull {
            val path = openFileDialog(windowHandle, "选择一个文件", "文件", filter.joinToString(";"))
            NormalPath(path!!)
        }
    }

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitPath? = Coroutines.io {
        catchingNull {
            val path = saveFileDialog(windowHandle, "保存文件", filename, filter, "文件")
            NormalPath(path!!)
        }
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = Coroutines.io {
        catchingNull {
            val path = Path(saveFileDialog(windowHandle, "保存图片", filename, "*.webp", "图片")!!)
            path to SystemFileSystem.sink(path).buffered()
        }
    }

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = Coroutines.io {
        catchingNull {
            val path = Path(saveFileDialog(windowHandle, "保存视频", filename, "*.mp4", "视频")!!)
            path to SystemFileSystem.sink(path).buffered()
        }
    }

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = Unit

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) SystemFileSystem.delete(origin as Path, false)
    }
}