package love.yinlin.startup

internal external fun openFileDialog(parent: Long, title: String, filterName: String, filter: String): String?
internal external fun openMultipleFileDialog(parent: Long, maxNum: Int, title: String, filterName: String, filter: String): Array<String>
internal external fun saveFileDialog(parent: Long, title: String, filename: String, ext: String, filterName: String): String?