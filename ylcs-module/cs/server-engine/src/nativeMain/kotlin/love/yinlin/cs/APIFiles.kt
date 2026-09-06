package love.yinlin.cs

import love.yinlin.fs.File

val EmptyAPIFile = object : APIFile {
    override val files: List<String> = emptyList()

    override fun toString(): String = "[]"
}

private class SingleAPIFile(path: String) : APIFile {
    override val files: List<String> = listOf(path)

    override fun toString(): String = files.joinToString(prefix = "[", postfix = "]")
}

internal class ServerAPIFile(override val files: List<String>) : APIFile {
    override fun toString(): String = files.joinToString(prefix = "[", postfix = "]")
}

private val APIFile.first: File get() = File(files.first())

operator fun APIFile.get(index: Int): APIFile = SingleAPIFile(files[index])

val APIFile.isEmpty: Boolean get() = files.isEmpty()

val APIFile.num: Int get() = files.size

fun APIFile.copy(other: APIFile): File {
    first.writeToSync(other.first)
    return other.first
}

fun APIFile.delete() = files.forEach { File(it).deleteRecursivelySync() }

fun APIFile.mkdir() = first.mkdirSync()