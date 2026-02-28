package love.yinlin.media.lyrics

import androidx.annotation.CallSuper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.fadingEdge

@Stable
internal interface TextLine {
    val position: Long
    val text: String
}

@Stable
internal abstract class TextLyricsEngine<E : TextLine> : LyricsEngine {
    protected var lines: List<E>? by mutableRefStateOf(null)
    protected var currentIndex by mutableIntStateOf(-1)
    private var lastPosition: Long = 0L

    protected val listState = LazyListState()

    @Composable
    protected abstract fun LinePlaceholder()

    @Composable
    protected abstract fun LineItem(item: E, isCurrent: Boolean)

    @Composable
    protected abstract fun BoxScope.FloatingLine(modifier: Modifier = Modifier, config: LyricsEngineConfig, textStyle: TextStyle)

    @CallSuper
    override fun reset() {
        lines = null
        currentIndex = -1
    }

    @CallSuper
    override fun update(position: Long) {
        val items = lines
        currentIndex = if (items != null) {
            // 先检查是否位于当前句和下一句之间, 此分支命中率最高
            if (position >= lastPosition) {
                val nextPosition1 = items.getOrNull(currentIndex + 1)?.position ?: Long.MAX_VALUE
                val nextPosition2 = items.getOrNull(currentIndex + 2)?.position ?: Long.MAX_VALUE
                lastPosition = position
                if (position < nextPosition1) return // 不需要更新
                else if (position < nextPosition2) { // 前移
                    ++currentIndex
                    return
                }
            }
            else lastPosition = position
            val targetIndex = items.indexOfFirst { it.position > position }
            if (targetIndex == -1) {
                if (currentIndex != items.lastIndex) items.lastIndex
                else return // 到达最后一句不需要更新
            }
            else if (targetIndex > 0) targetIndex - 1
            else -1
        }
        else -1
    }

    @Composable
    private fun LineItemWrapper(onClick: (() -> Unit)? = null, content: @Composable BoxScope.() -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.padding.h)
                .clip(Theme.shape.v7)
                .condition(onClick != null) { clickable(onClick = onClick) }
                .padding(Theme.padding.value),
            contentAlignment = Alignment.Center,
            content = content
        )
    }

    @Composable
    final override fun LyricsCanvas(config: LyricsEngineConfig, host: LyricsEngineHost) {
        val scope = rememberCoroutineScope()
        var containerSize by rememberState { IntSize.Zero }

        LaunchedEffect(currentIndex) {
            if (!listState.isScrollInProgress && lines != null) {
                val targetIndex = currentIndex.coerceAtLeast(0)
                listState.animateScrollToItem(targetIndex)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
                .onSizeChanged { containerSize = it }
                .fadingEdge(padding = PaddingValues(top = 75.dp, bottom = 75.dp))
        ) {
            item(key = "Placeholder1") {
                LineItemWrapper { LinePlaceholder() }
            }
            item(key = "Placeholder2") {
                LineItemWrapper { LinePlaceholder() }
            }
            lines?.let { lines ->
                itemsIndexed(
                    items = lines,
                    key = { _, item -> item.position }
                ) { index, item ->
                    LineItemWrapper(onClick = {
                        scope.launch { host.seekTo(item.position) }
                    }) {
                        LineItem(item, index == currentIndex)
                    }
                }
            }
            item(key = ItemKey("Placeholder0")) {
                Space(containerSize.width, containerSize.height)
            }
        }
    }

    @Composable
    final override fun FloatingLyricsCanvas(modifier: Modifier, config: LyricsEngineConfig, textStyle: TextStyle) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.BottomCenter
            ) {
                FloatingLine(
                    modifier = Modifier.background(color = Colors(config.backgroundColor)).padding(Theme.padding.value),
                    config = config,
                    textStyle = textStyle
                )
            }
        }
    }
}