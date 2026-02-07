package love.yinlin.startup

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.files.Path
import love.yinlin.annotation.NativeLibApi
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catchingNull
import love.yinlin.io.Sources
import love.yinlin.foundation.Context
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.StartupNative
import love.yinlin.foundation.SyncStartup
import love.yinlin.extension.bufferedSink
import love.yinlin.extension.bufferedSource
import love.yinlin.extension.delete
import love.yinlin.extension.safeSources
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri

@StartupNative
@NativeLibApi
actual class StartupPicker : SyncStartup() {
    private lateinit var context: Context
    private val handle: Long get() = context.handle

    actual override fun init(context: Context, args: StartupArgs) {
        this.context = context
    }

    actual suspend fun pickPicture(): Source? = catchingNull {
        Coroutines.io {
            val path = NativePicker.openFileDialog(handle, "选择一张图片", "图片", "*.jpg;*.png;*.webp")
            Path(path!!).bufferedSource
        }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = catchingNull {
        Coroutines.io {
            require(maxNum > 0)
            val paths = NativePicker.openMultipleFileDialog(handle, maxNum, "最多选择${maxNum}张图片", "图片", "*.jpg;*.png;*.webp")
            val files = paths.map { Path(it) }
            require(files.size in 1 .. maxNum)
            files.safeSources()
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = catchingNull {
        Coroutines.io {
            val path = NativePicker.openFileDialog(handle, "选择一个文件", "文件", filter.joinToString(";"))
            Path(path!!).bufferedSource
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = catchingNull {
        Coroutines.io {
            val path = NativePicker.openFileDialog(handle, "选择一个文件", "文件", filter.joinToString(";"))
            RegularUri(path!!)
        }
    }

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? = catchingNull {
        Coroutines.io {
            val path = NativePicker.saveFileDialog(handle, "保存文件", filename, filter, "文件")
            RegularUri(path!!)
        }
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = catchingNull {
        Coroutines.io {
            val path = Path(NativePicker.saveFileDialog(handle, "保存图片", filename, "*.webp", "图片")!!)
            path to path.bufferedSink
        }
    }

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = catchingNull {
        Coroutines.io {
            val path = Path(NativePicker.saveFileDialog(handle, "保存视频", filename, "*.mp4", "视频")!!)
            path to path.bufferedSink
        }
    }

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) {}

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) (origin as Path).delete()
    }
}