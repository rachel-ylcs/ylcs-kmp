package love.yinlin.compose.ui.widget

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.distinctUntilChanged
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.input.TertiaryButton
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.extension.DateEx
import love.yinlin.extension.cleaning
import kotlin.math.roundToInt

@Composable
fun LogOutput(
    state: LogOutputState,
    modifier: Modifier = Modifier,
    filter: (LogEntry) -> Boolean = { true },
    showLineNumbers: Boolean = true,
) {
    val listState = rememberLazyListState()
    val currentFilter by rememberUpdatedState(filter)
    val entries by rememberDerivedState(state) { state.entries.filter(currentFilter) }
    var programmatic by rememberFalse()
    var userMoved by rememberFalse()

    val connection = remember(state, listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    userMoved = true
                    state.follow.pause()
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && userMoved && !programmatic && (consumed.y != 0f || available.y != 0f)) {
                    state.follow.onUserViewport(!listState.canScrollForward)
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(listState, state) {
        snapshotFlow {
            LogViewport(listState.isScrollInProgress, listState.canScrollForward, programmatic, userMoved)
        }.distinctUntilChanged().collect { viewport ->
            if (viewport.userMoved && !viewport.isScrollInProgress && !viewport.programmatic) {
                withFrameNanos { }
                if (!viewport.isScrollInProgress && !viewport.programmatic) {
                    state.follow.onUserViewport(!viewport.canScrollForward)
                    userMoved = false
                }
            }
        }
    }

    val entryChange by rememberDerivedState(state) { state.follow.following && entries.isNotEmpty() }
    LaunchedEffect(state.revision, entryChange) {
        if (entryChange) {
            programmatic = true
            cleaning({ programmatic = false}) {
                withFrameNanos { }
                // 滚动到底部
                listState.scrollToItem(entries.lastIndex)
                listState.scrollBy(listState.layoutInfo.viewportEndOffset.toFloat())
            }
        }
    }

    val minWidth = Theme.size.cell1

    Box(
        modifier = modifier.layout { measurable, constraints ->
            val width = constraints.constrainWidth(minWidth.roundToPx())
            val height = constraints.constrainHeight((width / 3f).roundToInt())
            val placeable = measurable.measure(Constraints.fixed(width, height))
            layout(width, height) {
                placeable.placeRelative(0, 0)
            }
        }
    ) {
        SelectionBox {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().nestedScroll(connection)
            ) {
                items(items = entries, key = { it.id }) { entry ->
                    val spanColor = LocalColorVariant.current
                    val textColor = LocalColor.current
                    val errorColor = Theme.color.error
                    val levelColor = when (entry.level) {
                        LogLevel.Trace, LogLevel.Debug -> spanColor
                        LogLevel.Info -> Theme.color.primary
                        LogLevel.Warn -> Theme.color.warning
                        LogLevel.Error -> errorColor
                    }

                    val resolved = entry.spans.map { span ->
                        span to SpanStyle(
                            color = AnsiColor.resolve(span.style.foreground) ?: if (entry.level == LogLevel.Error) errorColor else textColor,
                            background = AnsiColor.resolve(span.style.background) ?: Color.Unspecified,
                            fontWeight = if (span.style.bold) FontWeight.Bold else null,
                            fontStyle = if (span.style.italic) FontStyle.Italic else null,
                            textDecoration = if (span.style.underline) TextDecoration.Underline else null,
                        )
                    }

                    val text = remember(entry) {
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = levelColor, fontWeight = FontWeight.SemiBold)) {
                                append(entry.level.name.uppercase().padEnd(7))
                            }
                            if (entry.timestamp != null) {
                                withStyle(SpanStyle(color = spanColor)) { append("${DateEx.Formatter.standardDateTime.format(entry.timestamp)}  ") }
                            }
                            if (entry.source.isNotEmpty()) {
                                withStyle(SpanStyle(color = spanColor)) { append("${entry.source}  ") }
                            }
                            for ([span, style] in resolved) {
                                withStyle(style) { append(span.text) }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.h, vertical = Theme.padding.g9)) {
                        if (showLineNumbers) {
                            DisableSelection {
                                SimpleEllipsisText(
                                    text = (entry.id + 1).toString().padStart(4),
                                    color = spanColor.copy(alpha = 0.7f),
                                    style = Theme.typography.v8,
                                    modifier = Modifier.width(Theme.size.input7)
                                )
                            }
                        }
                        Text(
                            text = text,
                            modifier = Modifier.weight(1f),
                            style = Theme.typography.v8,
                            color = textColor
                        )
                    }
                }
            }
        }

        val showFollowing by rememberDerivedState { !state.follow.following && state.follow.unread > 0 }
        if (showFollowing) {
            TertiaryButton(
                text = "New + ${state.follow.unread} / ${state.lineCount}",
                icon = Icons.KeyboardArrowDown,
                style = Theme.typography.v8.bold,
                onClick = { state.follow.resume() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(Theme.padding.value).zIndex(2f)
            )
        }
    }
}
