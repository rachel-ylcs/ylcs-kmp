package love.yinlin.platform.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import love.yinlin.compose.Colors
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.node.fadingEdges
import kotlin.math.abs

@Stable
internal abstract class TextLine : Comparable<TextLine> {
    abstract val position: Long
    abstract val text: String
    override fun compareTo(other: TextLine) = position.compareTo(other.position)
}

@Stable
internal abstract class TextLyricsEngine<E : TextLine> : LyricsEngine {
    protected var lines: List<E>? by mutableRefStateOf(null)
    protected var currentIndex by mutableIntStateOf(-1)
    protected val currentText by derivedStateOf { lines?.getOrNull(currentIndex)?.text ?: "" }

    protected val listState = LazyListState()
    protected var isDragging by mutableStateOf(false)

    override fun reset() {
        lines = null
        currentIndex = -1
    }

    override fun update(position: Long) {
        val newIndex = lines?.let { items ->
            val index = items.indexOfFirst { it.position > position } - 1
            if (index >= 0) index else -1
        } ?: -1
        currentIndex = newIndex
    }

    @Composable
    protected abstract fun LineItem(item: E, offset: Int)

    @Composable
    final override fun LyricsCanvas(modifier: Modifier, config: LyricsEngineConfig, host: LyricsEngineHost) {
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
                            LineItem(item, abs(listState.firstVisibleItemIndex + 3 - index))
                        }
                    }
                }
            }
        }
    }

    @Composable
    protected abstract fun BoxScope.FloatingLine(config: LyricsEngineConfig, textStyle: TextStyle)

    @Composable
    final override fun FloatingLyricsCanvas(config: LyricsEngineConfig, textStyle: TextStyle) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
            Box(modifier = Modifier.background(color = Colors(config.backgroundColor)).padding(CustomTheme.padding.value)) {
                FloatingLine(config, textStyle)
            }
        }
    }
}