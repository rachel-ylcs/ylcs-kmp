package love.yinlin.platform.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import love.yinlin.compose.Colors
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.ui.node.fadingEdges
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.readText
import kotlin.math.abs

@Stable
data class DynamicLineItem(val ch: String, val end: Long)

@Stable
data class DynamicLine(val position: Long, val text: String, val items: List<DynamicLineItem>)

@Composable
private fun LyricsRhymeLine(text: String, progress: Float, offset: Int) {
    val fontSize = MaterialTheme.typography.headlineSmall.fontSize / (offset / 30f + 1f)
    val fontWeight = if (offset == 0) FontWeight.Bold else FontWeight.Light
    val (borderWidth, shadowWidth) = with(LocalDensity.current) {
        val fontSizePx = fontSize.toPx()
        fontSizePx / 16f to fontSizePx / 24f
    }

    Box {
        Text(
            text = text,
            color = Colors.White,
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
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = fontSize,
                    fontWeight = fontWeight
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.zIndex(3f).drawWithContent {
                    clipRect(0f, 0f, size.width * progress, size.height) {
                        this@drawWithContent.drawContent()
                    }
                }
            )
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
internal class RhymeLyricsEngine : LyricsEngine {
    override val interval: Long = 20L
    override val type: LyricsEngineType = LyricsEngineType.Rhyme

    private var lines: List<DynamicLine>? by mutableStateOf(null)
    private var currentIndex by mutableIntStateOf(-1)
    private var progress by mutableFloatStateOf(0f)
    private val currentText by derivedStateOf { lines?.getOrNull(currentIndex)?.text ?: "" }

    private val listState = LazyListState()
    private var isDragging by mutableStateOf(false)

    override suspend fun load(rootPath: Path): Boolean = catchingDefault(false) {
        val config = Path(rootPath, type.resType.filename).readText()!!.parseJsonValue<RhymeLyricsConfig>()
        val offset = config.offset
        val lyrics = config.lyrics
        lines = buildList(capacity = 10 + lyrics.size) {
            val startTime = lyrics.first().start
            repeat(6) { add(DynamicLine(position = startTime / 6 * it, text = "", items = emptyList())) }

            addAll(config.lyrics.map { rhymeLine ->
                val lineStart = rhymeLine.start + offset
                val theme = rhymeLine.theme
                DynamicLine(
                    position = lineStart,
                    text = theme.fastJoinToString(separator = "") { it.ch },
                    items = theme.map { DynamicLineItem(it.ch, lineStart + it.end) }
                )
            })

            repeat(4) { add(DynamicLine(position = Long.MAX_VALUE - it, text = "", items = emptyList())) }
        }
        currentIndex = -1
        progress = 0f
        lines!!.isNotEmpty()
    }

    override fun reset() {
        lines = null
        currentIndex = -1
        progress = 0f
    }

    override fun update(position: Long) {
        val newIndex = lines?.let { items ->
            val index = items.indexOfFirst { it.position > position } - 1
            if (index >= 0) index else -1
        } ?: -1
        currentIndex = newIndex
        val items = lines?.getOrNull(newIndex)?.items ?: return
        var currentLength = 0f
        val totalLength = currentText.length
        for (i in items.indices) {
            val (ch, end) = items[i]
            val length = ch.length
            if (position > end) currentLength += length
            else {
                val start = items.getOrNull(i - 1)?.end ?: 0
                currentLength += length * (position - start) / (end - start).toFloat()
                break
            }
        }
        progress = if (totalLength == 0) 0f else (currentLength / totalLength).coerceIn(0f, 1f)
    }

    @Composable
    override fun LyricsCanvas(modifier: Modifier, config: LyricsEngineConfig, host: LyricsEngineHost) {
        val scope = rememberCoroutineScope()

        LaunchedEffect(currentIndex) {
            if (!isDragging) {
                if (currentIndex >= 3) listState.animateScrollToItem(currentIndex - 3)
                else listState.animateScrollToItem(0)
            }
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
                        left = CustomTheme.padding.zeroSpace,
                        top = maxHeight * 3 / 7,
                        right = CustomTheme.padding.zeroSpace,
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
                                    scope.launch { host.seekTo(item.position) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            LyricsRhymeLine(
                                text = item.text,
                                progress = progress,
                                offset = abs(listState.firstVisibleItemIndex + 3 - index),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun FloatingLyricsCanvas(config: LyricsEngineConfig, textStyle: TextStyle) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
            Box(modifier = Modifier.background(color = Colors(config.backgroundColor)).padding(CustomTheme.padding.value)) {
                Text(
                    text = currentText,
                    color = Colors.White,
                    style = textStyle.copy(fontSize = textStyle.fontSize * config.textSize),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.zIndex(1f)
                )
                Text(
                    text = currentText,
                    color = Colors(config.textColor),
                    style = textStyle.copy(fontSize = textStyle.fontSize * config.textSize),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.zIndex(2f).drawWithContent {
                        clipRect(0f, 0f, size.width * progress, size.height) {
                            this@drawWithContent.drawContent()
                        }
                    }
                )
            }
        }
    }
}