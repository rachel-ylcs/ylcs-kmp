package love.yinlin.startup

import love.yinlin.annotation.NativeLibApi
import love.yinlin.platform.NativeLibLoader

@NativeLibApi
internal object NativePicker {
    init {
        NativeLibLoader.resource("picker")
    }

    @JvmStatic
    external fun openFileDialog(parent: Long, title: String, filterName: String, filter: String): String?
    @JvmStatic
    external fun openMultipleFileDialog(parent: Long, maxNum: Int, title: String, filterName: String, filter: String): Array<String>
    @JvmStatic
    external fun saveFileDialog(parent: Long, title: String, filename: String, ext: String, filterName: String): String?
}