package love.yinlin.foundation.picker

import love.yinlin.foundation.LibLoader
import love.yinlin.foundation.NativeLibApi

@NativeLibApi
internal object NativePicker {
    init {
        LibLoader.resource("ylcs-module.foundation.picker")
    }

    @JvmStatic
    external fun openFileDialog(parent: Long, title: String, filterName: String, filter: String): String?
    @JvmStatic
    external fun openMultipleFileDialog(parent: Long, maxNum: Int, title: String, filterName: String, filter: String): Array<String>
    @JvmStatic
    external fun saveFileDialog(parent: Long, title: String, filename: String, ext: String, filterName: String): String?
}