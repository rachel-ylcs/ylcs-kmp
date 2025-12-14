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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.io.files.Path
import love.yinlin.compose.Colors
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.mutableRefStateOf
import love.yinlin.compose.ui.node.fadingEdges
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.readText
import kotlin.math.abs

@Composable
private fun LyricsLrcLine(text: String, offset: Int) {
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
internal class LineLyricsEngine : LyricsEngine {
    override val type: LyricsEngineType = LyricsEngineType.Line

    private var lines: List<LrcLine>? by mutableRefStateOf(null)
    private var currentIndex by mutableIntStateOf(-1)
    private val currentText by derivedStateOf { lines?.getOrNull(currentIndex)?.text ?: "" }

    override suspend fun load(rootPath: Path): Boolean = catchingDefault(false) {
        val source = Path(rootPath, type.resType.filename).readText()
        lines = source?.let { LrcParser(it).paddingLyrics }
        return lines != null
    }

    override suspend fun reset() {
        lines = null
        currentIndex = -1
        listState.scrollToItem(0)
    }

    override fun update(position: Long) {
        val newIndex = lines?.let { items ->
            val index = items.indexOfFirst { it.position > position } - 1
            if (index >= 0) index else -1
        } ?: -1
        currentIndex = newIndex
    }

    private val listState = LazyListState()
    private var isDragging by mutableStateOf(false)

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

    @Composable
    override fun FloatingLyricsCanvas(config: LyricsEngineConfig, textStyle: TextStyle) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
            Box(modifier = Modifier.background(color = Colors(config.backgroundColor)).padding(CustomTheme.padding.value)) {
                Text(
                    text = currentText,
                    color = Colors(config.textColor),
                    style = textStyle.copy(fontSize = textStyle.fontSize * config.textSize),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}