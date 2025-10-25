package love.yinlin.extension

import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem





class Sources<S : RawSource>(
    private val sources: MutableList<S> = mutableListOf()
) : AutoCloseable, MutableList<S> by sources {
    override fun close() {
        for (source in sources) source.close()
    }
}

inline fun <T, S : RawSource> Collection<T>.safeToSources(crossinline block: (T) -> S?): Sources<S>? {
    val sources = Sources<S>()
    return try {
        for (item in this) block(item)?.let { sources += it }
        sources
    }
    catch (_: Throwable) {
        sources.close()
        null
    }
}

fun FileSystem.deleteRecursively(path: Path, mustExist: Boolean = true) {
    if (mustExist && !SystemFileSystem.exists(path)) throw FileNotFoundException("File does not exist: $path")
    val queue = ArrayDeque<Path>()
    queue.add(path)
    while (queue.isNotEmpty()) {
        val currentPath = queue.first()
        val metadata = SystemFileSystem.metadataOrNull(currentPath)
        when {
            metadata == null -> throw IOException("Path is neither a file nor a directory: $path")
            metadata.isRegularFile -> {
                delete(currentPath)
                queue.removeFirst()
            }
            metadata.isDirectory -> {
                val list = list(currentPath)
                if (list.isEmpty()) {
                    delete(currentPath)
                    queue.removeFirst()
                } else queue.addAll(0, list)
            }
        }
    }
}