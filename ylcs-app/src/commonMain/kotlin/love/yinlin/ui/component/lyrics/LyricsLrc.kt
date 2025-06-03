package love.yinlin.ui.component.lyrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import love.yinlin.common.Colors
import love.yinlin.common.ThemeValue
import love.yinlin.extension.timeString
import love.yinlin.ui.component.layout.fadingEdges
import kotlin.math.abs

@Stable
@Serializable
data class LrcLine(
    val position: Long,
    val text: String
) : Comparable<LrcLine> {
    override fun compareTo(other: LrcLine) = position.compareTo(other.position)
}

@Composable
private fun LyricsLrcLine(
    text: String,
    offset: Int
) {
    val fontSize = MaterialTheme.typography.headlineSmall.fontSize / (offset / 30f + 1f)
    val fontWeight = if (offset == 0) FontWeight.Bold else FontWeight.Light
    val color = if (offset == 0) MaterialTheme.colorScheme.primary else Colors.White
    val (borderWidth, shadowWidth) = with(LocalDensity.current) {
        val fontSizePx = fontSize.toPx()
        fontSizePx / 16f to fontSizePx / 24f
    }

    Box {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = fontSize,
                fontWeight = fontWeight
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.zIndex(2f)
        )
        if (offset == 0) {
            Text(
                text = text,
                color = Colors.Dark,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    shadow = Shadow(
                        color = Colors.Black,
                        offset = Offset(shadowWidth, shadowWidth),
                        blurRadius = shadowWidth
                    ),
                    drawStyle = Stroke(
                        width = borderWidth,
                        join = StrokeJoin.Round
                    )
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                textDecoration = null,
                modifier = Modifier.semantics { hideFromAccessibility() }.alpha(0.7f).zIndex(1f)
            )
        }
    }
}

@Stable
class LyricsLrc : LyricsEngine {
    class Parser(source: String) {
        private var lines: List<LrcLine>? = null

        init {
            val newLines = mutableListOf<LrcLine>()
            val pattern = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)".toRegex()
            val items = source.split("\\r?\\n".toRegex())
            for (item in items) {
                val line = item.trim()
                if (line.isEmpty()) continue
                try {
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
                catch (_: Throwable) { }
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

    private var lines: List<LrcLine>? by mutableStateOf(null)
    private val listState = LazyListState()
    private var currentIndex by mutableIntStateOf(-1)
    private var isDragging by mutableStateOf(false)

    suspend fun reset() {
        lines = null
        currentIndex = -1
        listState.scrollToItem(0)
    }

    override fun updateIndex(position: Long): String? {
        val newIndex = lines?.let { items ->
            val index = items.indexOfFirst { it.position > position } - 1
            if (index >= 0) index else -1
        } ?: -1
        currentIndex = newIndex
        return lines?.getOrNull(newIndex)?.text
    }

    fun parseLrcString(source: String) {
        lines = Parser(source).paddingLyrics
    }

    @Composable
    override fun Content(
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

        BoxWithConstraints(modifier = modifier) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().fadingEdges(
                    edges = DpRect(
                        left = ThemeValue.Padding.ZeroSpace,
                        top = maxHeight * 3 / 7,
                        right = ThemeValue.Padding.ZeroSpace,
                        bottom = maxHeight * 3 / 7
                    )
                )
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
                                .clickable(enabled = item.text.isNotEmpty()) {
                                    onLyricsClick(item.position)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LyricsLrcLine(
                                text = item.text,
                                offset = abs(listState.firstVisibleItemIndex + 3 - index),
                            )
                        }
                    }
                }
            }
        }
    }
}