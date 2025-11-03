package love.yinlin.startup

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.extension.catchingNull
import love.yinlin.io.Sources
import love.yinlin.io.safeToSources
import love.yinlin.platform.Coroutines
import love.yinlin.platform.openFileDialog
import love.yinlin.platform.openMultipleFileDialog
import love.yinlin.platform.saveFileDialog
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.SyncStartup
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri

actual class StartupPicker : SyncStartup {
    private lateinit var context: Context
    private val handle: Long get() = context.handle

    actual override fun init(context: Context, args: StartupArgs) {
        this.context = context
    }

    actual suspend fun pickPicture(): Source? = Coroutines.io {
        catchingNull {
            val path = openFileDialog(handle, "选择一张图片", "图片", "*.jpg;*.png;*.webp")
            SystemFileSystem.source(Path(path!!)).buffered()
        }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = Coroutines.io {
        catchingNull {
            require(maxNum > 0)
            val paths = openMultipleFileDialog(handle, maxNum, "最多选择${maxNum}张图片", "图片", "*.jpg;*.png;*.webp")
            val files = paths.map { Path(it) }
            require(files.size in 1 .. maxNum)
            files.safeToSources { SystemFileSystem.source(it).buffered() }
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = Coroutines.io {
        catchingNull {
            val path = openFileDialog(handle, "选择一个文件", "文件", filter.joinToString(";"))
            SystemFileSystem.source(Path(path!!)).buffered()
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = Coroutines.io {
        catchingNull {
            val path = openFileDialog(handle, "选择一个文件", "文件", filter.joinToString(";"))
            RegularUri(path!!)
        }
    }

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? = Coroutines.io {
        catchingNull {
            val path = saveFileDialog(handle, "保存文件", filename, filter, "文件")
            RegularUri(path!!)
        }
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = Coroutines.io {
        catchingNull {
            val path = Path(saveFileDialog(handle, "保存图片", filename, "*.webp", "图片")!!)
            path to SystemFileSystem.sink(path).buffered()
        }
    }

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = Coroutines.io {
        catchingNull {
            val path = Path(saveFileDialog(handle, "保存视频", filename, "*.mp4", "视频")!!)
            path to SystemFileSystem.sink(path).buffered()
        }
    }

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) {}

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) SystemFileSystem.delete(origin as Path, false)
    }
}