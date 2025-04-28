package love.yinlin.platform

internal external fun openFileDialog(title: String, filterName: String, filter: String): String?
internal external fun openMultipleFileDialog(maxNum: Int, title: String, filterName: String, filter: String): Array<String>
internal external fun saveFileDialog(title: String, filename: String, ext: String, filterName: String): String?