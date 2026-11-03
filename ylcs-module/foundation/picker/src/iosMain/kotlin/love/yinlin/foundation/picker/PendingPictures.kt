package love.yinlin.foundation.picker

import love.yinlin.concurrent.Lock
import love.yinlin.extension.catching
import love.yinlin.fs.File
internal class PendingPictures {
    private val lock = Lock()
    private var files: MutableList<File?>? = null
    private var processed = 0
    private var cancelled = false

    fun start(size: Int) {
        lock.synchronized {
            if (!cancelled) files = MutableList(size) { null }
        }
    }

    fun complete(index: Int, file: File?): List<File>? {
        var orphan: File? = null
        val completed = lock.synchronized {
            val entries = files
            if (cancelled || entries == null) {
                orphan = file
                null
            }
            else {
                entries[index] = file
                processed++
                if (processed == entries.size) entries.filterNotNull() else null
            }
        }
        orphan?.let { catching { it.deleteSync() } }
        return completed
    }

    fun cancel() {
        val toDelete = lock.synchronized {
            cancelled = true
            val remaining = files?.filterNotNull() ?: []
            files = null
            remaining
        }
        toDelete.forEach { file -> catching { file.deleteSync() } }
    }
}
