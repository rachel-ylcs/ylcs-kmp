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
        for (line in lines) {
            val entry = LogEntry(nextId++, line, level, timestamp, source)
            entries += entry
            follow.onAppend()
            while (entries.size > capacity) {
                entries.removeAt(0)
                discarded++
            }
        }
        revision++
    }

    fun clear() {
        entries.clear()
        discarded = 0
        revision++
        follow.resume()
    }
}