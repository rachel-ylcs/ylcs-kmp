package love.yinlin.ui.component.lyrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import love.yinlin.common.ThemeColor
import love.yinlin.platform.Coroutines
import kotlin.math.abs

@Stable
@Serializable
private data class LrcLine(
    val position: Long,
    val text: String
) : Comparable<LrcLine> {
    override fun compareTo(other: LrcLine) = position.compareTo(other.position)
}

@Composable
private fun LyricsLrcLine(
    text: String,
    isCurrent: Boolean,
    offset: Int
) {
    val fontSize = MaterialTheme.typography.headlineMedium.fontSize / (offset / 30f + if (isCurrent) 0.9f else 1f)
    val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Light
    val brush = if (isCurrent) Brush.horizontalGradient(ThemeColor.primaryGradient) else null
    val alpha = 3 / (offset + 3f)

    Text(
        text = text,
        color = MaterialTheme.colorScheme.background,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = fontSize,
            fontWeight = fontWeight,
            brush = brush
        ),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.MiddleEllipsis,
        modifier = Modifier.alpha(alpha)
    )
}

@Stable
class LyricsLrc : LyricsEngine {
    private var lines: List<LrcLine>? by mutableStateOf(null)
    private val listState = LazyListState()
    private var currentIndex by mutableIntStateOf(-1)
    private var isDragging by mutableStateOf(false)

    suspend fun reset() {
        lines = null
        currentIndex = -1
        listState.animateScrollToItem(0)
    }

    fun updateIndex(position: Long) {
        currentIndex = lines?.let { items ->
            val index = items.indexOfFirst { it.position > position } - 1
            if (index >= 0) index else -1
        } ?: -1
    }

    suspend fun parseLrcString(source: String) {
        try {
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
            val sortLines = newLines.distinctBy { it.position }.sorted()
            if (sortLines.isNotEmpty()) {
                val startTime = sortLines.first().position
                lines = buildList(capacity = 9 + sortLines.size) {
                    // 插入6个空并均分起始时间
                    repeat(6) { add(LrcLine(startTime / 6 * it, "")) }
                    // 插入原歌词
                    addAll(sortLines)
                    // 插入永久3个空
                    repeat(3) { add(LrcLine(Long.MAX_VALUE - it, "")) }
                }
            }
        }
        catch (_: Throwable) { }
    }

    @Composable
    override fun content(
        modifier: Modifier,
        onLyricsClick: (Long) -> Unit
    ) {
        LaunchedEffect(currentIndex) {
            if (!isDragging) if (currentIndex >= 3) listState.animateScrollToItem(currentIndex - 3)
        }

        LaunchedEffect(listState.interactionSource) {
            val interactions = mutableSetOf<Interaction>()
            listState.interactionSource.interactions.map { interaction ->
                when (interaction) {
                    is DragInteraction.Start -> interactions += interaction
                    is DragInteraction.Stop -> interactions -= interaction.start
                    is DragInteraction.Cancel -> interactions -= interaction.start
                }
                interactions.isNotEmpty()
            }.collect { isDragging = it }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            lines?.let { lines ->
                itemsIndexed(
                    items = lines,
                    key = { _, item -> item.position }
                ) { index, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillParentMaxHeight(0.142857f)
                            .clickable {
                                if (item.text.isNotEmpty()) onLyricsClick(item.position)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        LyricsLrcLine(
                            text = item.text,
                            isCurrent = index == currentIndex,
                            offset = abs(listState.firstVisibleItemIndex + 3 - index),
                        )
                    }
                }
            }
        }
    }
}