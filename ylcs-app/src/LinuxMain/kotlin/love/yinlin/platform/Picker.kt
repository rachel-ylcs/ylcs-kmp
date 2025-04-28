package love.yinlin.platform

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private fun JFileChooser.setFileChooserInfo(multiple: Boolean, title: String, filterName: String, filter: String) {
    dialogTitle = title
    isMultiSelectionEnabled = multiple
    val extFilters = filter.split(";").map {
        if (it.startsWith("*.")) it.removePrefix("*.")
        else if (it.startsWith(".")) it.removePrefix(".")
        else it
    }
    fileFilter = FileNameExtensionFilter(filterName, *extFilters.toTypedArray())
}

internal fun openFileDialog(title: String, filterName: String, filter: String): String? {
    val chooser = JFileChooser()
    chooser.setFileChooserInfo(false, title, filterName, filter)
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile.absolutePath else null
}

internal fun openMultipleFileDialog(maxNum: Int, title: String, filterName: String, filter: String): Array<String> {
    val chooser = JFileChooser()
    chooser.setFileChooserInfo(true, title, filterName, filter)
    val result = chooser.showOpenDialog(null)
    val files = if (result == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFiles?.map { it.absolutePath }?.toTypedArray()
    } else null
    return if (files != null && files.size in 1 .. maxNum) files else emptyArray()
}

internal fun saveFileDialog(title: String, filename: String, ext: String, filterName: String): String? {
    val chooser = JFileChooser()
    chooser.setFileChooserInfo(false, title, filterName, ext)
    chooser.selectedFile = File(chooser.currentDirectory, filename)
    val result = chooser.showSaveDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile.absolutePath else null
}