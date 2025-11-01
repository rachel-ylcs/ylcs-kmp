package love.yinlin.platform.lyrics

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastJoinToString
import kotlinx.serialization.Serializable
import love.yinlin.extension.catching
import love.yinlin.extension.timeString
import kotlin.collections.ifEmpty

@Stable
@Serializable
data class LrcLine(val position: Long, val text: String) : Comparable<LrcLine> {
    override fun compareTo(other: LrcLine) = position.compareTo(other.position)
}

class LrcParser(source: String) {
    private var lines: List<LrcLine>? = null

    init {
        val newLines = mutableListOf<LrcLine>()
        val pattern = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)".toRegex()
        val items = source.split("\\r?\\n".toRegex())
        for (item in items) {
            val line = item.trim()
            if (line.isEmpty()) continue
            catching {
                val result = pattern.find(line)!!.groups
                val minutes = result[1]!!.value.toLong()
                val seconds = result[2]!!.value.toLong()
                val millisecondsString = result[3]!!.value
                var milliseconds = millisecondsString.toLong()
                if (millisecondsString.length == 2) milliseconds *= 10L
                val position = (minutes * 60 + seconds) * 1000 + milliseconds
                val text = result[4]!!.value.trim()
                if (text.isNotEmpty()) newLines += LrcLine(position, text)
            }
        }
        lines = newLines.fastFilter { it.position > 100L }.fastDistinctBy { it.position }.sorted().ifEmpty { null }
    }

    val ok: Boolean get() = lines != null

    val paddingLyrics: List<LrcLine>? get() = lines?.let { items ->
        val startTime = items.first().position
        buildList(capacity = 10 + items.size) {
            // 插入6个空并均分起始时间
            repeat(6) { add(LrcLine(startTime / 6 * it, "")) }
            // 插入原歌词
            this += items
            // 插入永久4个空
            repeat(4) { add(LrcLine(Long.MAX_VALUE - it, "")) }
        }
    }

    val plainText: String get() = lines?.let { items ->
        items.fastJoinToString("\n") { it.text }
    } ?: ""

    override fun toString(): String = lines?.let { items ->
        items.fastJoinToString("\n") {
            val milliseconds = (it.position % 1000) / 10
            "[${it.position.timeString}.${if (milliseconds < 10) "0" else ""}$milliseconds]${it.text}"
        }
    } ?: ""
}