package love.yinlin.api

import kotlinx.io.files.Path
import love.yinlin.extension.deleteRecursively
import java.io.File

private class SingleAPIFile(path: String) : APIFile {
    override val files: List<String> = listOf(path)
}

private val APIFile.first: File get() = File(files.first())

operator fun APIFile.get(index: Int): APIFile = SingleAPIFile(files[index])

val APIFile.isEmpty: Boolean get() = files.isEmpty()

val APIFile.num: Int get() = files.size

fun APIFile.copy(other: APIFile): File = first.copyTo(other.first, true)

fun APIFile.delete() = files.map { Path(it) }.forEach { it.deleteRecursively() }

fun APIFile.mkdir() = first.mkdirs()