package love.yinlin.ui.component.lyrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.extension.rememberDerivedState
import love.yinlin.platform.Coroutines
import kotlin.compareTo

@Stable
@Serializable
data class LrcLine(
    val position: Long,
    val text: String
) : Comparable<LrcLine> {
    override fun compareTo(other: LrcLine) = position.compareTo(other.position)
}

@Stable
class LyricsLrcState {
    var lines: List<LrcLine>? by mutableStateOf(null)
        private set

    fun indexOf(position: Long): Int? = lines?.let { items ->
        var low = 0
        var high = items.lastIndex
        var result: Int? = null
        while (low <= high) {
            val mid = (low + high) ushr 1
            val currentValue = items[mid].position
            if (currentValue < position) {
                result = mid
                low = mid + 1
            } else high = mid - 1
        }
        return result
    }

    suspend fun parseLrcString(source: String) = try {
        val newLines = mutableListOf<LrcLine>()
        Coroutines.cpu {
            val pattern = "\\[(\\d{2}):(\\d{2}).(\\d{2,3})](.*)".toRegex()
            val items = source.split("\\r?\\n".toRegex())
            for (item in items) {
                val line = item.trim()
                if (line.isEmpty()) continue
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
        lines = newLines.distinctBy { it.position }.sorted()
    }
    catch (_: Throwable) { }
}

@Composable
fun LyricsLrc(
    position: Long,
    state: LyricsLrcState,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val currentIndex by rememberDerivedState { state.indexOf(position) }

    LaunchedEffect(currentIndex) {
        currentIndex?.let {
            listState.animateScrollToItem(it)
        }
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        state.lines?.let { lines ->
            items(
                items = lines,
                key = { it.position }
            ) {
                Text(
                    text = it.text,
                    color = MaterialTheme.colorScheme.background,
                    style = MaterialTheme.typography.displayMedium,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )
            }
        }
    }
}