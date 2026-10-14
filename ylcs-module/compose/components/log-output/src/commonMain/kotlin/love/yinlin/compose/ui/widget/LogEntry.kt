package love.yinlin.compose.ui.widget

import androidx.compose.runtime.Stable
import kotlinx.datetime.LocalDateTime

@Stable
data class LogEntry(
    val id: Long,
    val spans: List<LogSpan>,
    val level: LogLevel = LogLevel.Info,
    val timestamp: LocalDateTime? = null,
    val source: String = "",
) {
    val text: String get() = spans.joinToString("") { it.text }
}