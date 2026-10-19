package love.yinlin.compose.ui.widget

import androidx.compose.runtime.*
import kotlinx.datetime.LocalDateTime
import kotlin.collections.plusAssign
import kotlin.text.iterator

@Stable
class LogOutputState(val capacity: Int = DEFAULT_CAPACITY) {
    companion object {
        const val DEFAULT_CAPACITY = 10000
    }

    init {
        require(capacity >= 0) { "capacity must be non-negative" }
    }

    internal val follow = TailFollowState()
    private val decoder = AnsiDecoder()

    val entries: List<LogEntry> field = mutableStateListOf<LogEntry>()
    val lineCount: Int get() = entries.size

    var revision by mutableLongStateOf(0L)
        private set
    var discarded by mutableLongStateOf(0L)
        private set
    private var nextId = 0L

    val plainText: String get() = entries.joinToString("\n") { entry ->
        buildString {
            append(entry.level.name.uppercase())
            append("  ")
            if (entry.timestamp != null) {
                append(entry.timestamp)
                append("  ")
            }
            if (entry.source.isNotEmpty()) {
                append(entry.source)
                append("  ")
            }
            if (entry.text.isNotEmpty()) {
                append(entry.text)
                append("  ")
            }
        }
    }

    fun append(text: String, level: LogLevel = LogLevel.Info, timestamp: LocalDateTime? = null, source: String = "") {
        val lines: MutableList<List<LogSpan>> = []
        val current: MutableList<LogSpan> = []
        var cr = false
        for (span in decoder.feed(text)) {
            val run = buildString {
                for (ch in span.text) {
                    if (cr && ch != '\n') current.clear()
                    cr = false
                    when (ch) {
                        '\n' -> {
                            if (this.isNotEmpty()) {
                                current += span.copy(text = this.toString())
                                clear()
                            }
                            lines += current.toList()
                            current.clear()
                        }
                        '\r' -> {
                            if (this.isNotEmpty()) {
                                current += span.copy(text = this.toString())
                                clear()
                            }
                            cr = true
                        }
                        else -> append(ch)
                    }
                }
            }
            if (run.isNotEmpty()) current += span.copy(text = run)
        }

        if (current.isNotEmpty() || lines.isEmpty()) lines += current.toList()

        if (lines.size == 1 && capacity > 0) {
            entries += LogEntry(nextId++, lines[0], level, timestamp, source)
            follow.onAppend()
            if (entries.size > capacity) {
                entries.removeAt(0)
                discarded++
            }
            revision++
            return
        }

        val skipped = (lines.size - capacity).coerceAtLeast(0)
        val added = lines.size - skipped
        val removed = (entries.size - (capacity - added)).coerceAtLeast(0)
        if (removed == entries.size) entries.clear()
        else if (removed == 1) entries.removeAt(0)
        else if (removed > 1) {
            // 持久化列表的范围删除可能逐项移动；保留只读快照的尾部后一次重建。
            val retained = entries.toList().subList(removed, entries.size)
            entries.clear()
            entries.addAll(retained)
        }
        discarded += removed.toLong() + skipped
        nextId += skipped
        if (added == 1) entries += LogEntry(nextId++, lines[skipped], level, timestamp, source)
        else if (added > 1) entries.addAll(List(added) { index ->
            LogEntry(nextId++, lines[skipped + index], level, timestamp, source)
        })
        follow.onAppend(lines.size)
        revision++
    }

    fun clear() {
        entries.clear()
        discarded = 0
        revision++
        follow.resume()
    }
}