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
import kotlinx.datetime.LocalDateTime
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

@Stable
sealed interface AnsiColor {
    @Stable
    data class Indexed(val index: Int) : AnsiColor
    @Stable
    data class Rgb(val red: Int, val green: Int, val blue: Int) : AnsiColor
}

@Stable
data class LogStyle(
    val foreground: AnsiColor? = null,
    val background: AnsiColor? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
)

@Stable
data class LogSpan(
    val text: String,
    val style: LogStyle = LogStyle()
)

private class AnsiDecoder {
    private var style = LogStyle()

    private fun applySgr(sequence: String) {
        val codes = if (sequence.isEmpty()) [0] else sequence.split(';').map { it.toIntOrNull() ?: 0 }
        var i = 0
        while (i < codes.size) {
            when (val code = codes[i]) {
                0 -> style = LogStyle()
                1 -> style = style.copy(bold = true)
                3 -> style = style.copy(italic = true)
                4 -> style = style.copy(underline = true)
                22 -> style = style.copy(bold = false)
                23 -> style = style.copy(italic = false)
                24 -> style = style.copy(underline = false)
                39 -> style = style.copy(foreground = null)
                49 -> style = style.copy(background = null)
                in 30 .. 37 -> style = style.copy(foreground = AnsiColor.Indexed(code - 30))
                in 90 .. 97 -> style = style.copy(foreground = AnsiColor.Indexed(code - 90 + 8))
                in 40 .. 47 -> style = style.copy(background = AnsiColor.Indexed(code - 40))
                in 100 .. 107 -> style = style.copy(background = AnsiColor.Indexed(code - 100 + 8))
                38, 48 -> {
                    var color: AnsiColor? = null
                    if (codes.getOrNull(i + 1) == 5 && codes.getOrNull(i + 2) in 0 .. 255) {
                        color = AnsiColor.Indexed(codes[i + 2])
                        i += 2
                    }
                    else if (codes.getOrNull(i + 1) == 2 && i + 4 < codes.size && codes.subList(i + 2, i + 5).all { it in 0 .. 255 }) {
                        color = AnsiColor.Rgb(codes[i + 2], codes[i + 3], codes[i + 4])
                        i += 4
                    }
                    if (color != null) style = if (code == 38) style.copy(foreground = color) else style.copy(background = color)
                }
            }
            i++
        }
    }

    fun feed(input: String): List<LogSpan> {
        val result: MutableList<LogSpan> = []
        val text = buildString {
            var i = 0
            while (i < input.length) {
                if (input[i] != '\u001b') {
                    append(input[i++])
                    continue
                }
                if (this.isNotEmpty()) {
                    result += LogSpan(this.toString(), style)
                    clear()
                }
                if (i + 1 == input.length) break
                when (input[i + 1]) {
                    '[' -> {
                        var end = i + 2
                        while (end < input.length && input[end].code !in 0x40 .. 0x7e) end++
                        if (end == input.length) break
                        if (input[end] == 'm') applySgr(input.substring(i + 2, end))
                        i = end + 1
                    }
                    ']' -> {
                        var end = i + 2
                        while (end < input.length && input[end] != '\u0007' && !(input[end] == '\u001b' && end + 1 < input.length && input[end + 1] == '\\')) end++
                        if (end == input.length) break
                        i = end + if (input[end] == '\u0007') 1 else 2
                    }
                    else -> i += 2
                }
            }
        }
        if (text.isNotEmpty()) result += LogSpan(text, style)
        style = LogStyle()
        return result
    }
}

@Stable
enum class LogLevel { Trace, Debug, Info, Warn, Error }

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

@Stable
internal class TailFollowState {
    var following by mutableStateOf(true)
        private set

    var unread by mutableIntStateOf(0)
        private set

    fun pause() {
        following = false
    }

    fun onUserViewport(atBottom: Boolean) {
        if (atBottom) resume() else pause()
    }

    fun onAppend(count: Int = 1) {
        if (!following) unread += count
    }

    fun resume() {
        following = true
        unread = 0
    }
}

@Stable
class LogOutputState(val capacity: Int = DEFAULT_CAPACITY) {
    companion object {
        const val DEFAULT_CAPACITY = 10000
    }

    internal val follow = TailFollowState()
    private val decoder = AnsiDecoder()

    val entries: List<LogEntry> field = mutableStateListOf<LogEntry>()
    val lineCount: Int get() = entries.size

    var revision by mutableLongStateOf(0L)
        private set
    var discarded by mutableLongStateOf(0L)
        private set
    private var nextId = 0L

    val plainText: String get() = entries.joinToString("\n") { entry ->
        buildString {
            append(entry.level.name.uppercase())
            append("  ")
            if (entry.timestamp != null) {
                append(entry.timestamp)
                append("  ")
            }
            if (entry.source.isNotEmpty()) {
                append(entry.source)
                append("  ")
            }
            if (entry.text.isNotEmpty()) {
                append(entry.text)
                append("  ")
            }
        }
    }

    fun append(text: String, level: LogLevel = LogLevel.Info, timestamp: LocalDateTime? = null, source: String = "") {
        val lines: MutableList<List<LogSpan>> = []
        val current: MutableList<LogSpan> = []
        var cr = false
        for (span in decoder.feed(text)) {
            val run = buildString {
                for (ch in span.text) {
                    if (cr && ch != '\n') current.clear()
                    cr = false
                    when (ch) {
                        '\n' -> {
                            if (this.isNotEmpty()) {
                                current += span.copy(text = this.toString())
                                clear()
                            }
                            lines += current.toList()
                            current.clear()
                        }
                        '\r' -> {
                            if (this.isNotEmpty()) {
                                current += span.copy(text = this.toString())
                                clear()
                            }
                            cr = true
                        }
                        else -> append(ch)
                    }
                }
            }
            if (run.isNotEmpty()) current += span.copy(text = run)
        }
        if (current.isNotEmpty() || lines.isEmpty()) lines += current.toList()
        for (line in lines) {
            val entry = LogEntry(nextId++, line, level, timestamp, source)
            entries += entry
            follow.onAppend()
            while (entries.size > capacity) {
                entries.removeAt(0)
                discarded++
            }
        }
        revision++
    }

    fun clear() {
        entries.clear()
        discarded = 0
        revision++
        follow.resume()
    }
}

private val AnsiColorResolveSteps: IntArray = [0, 95, 135, 175, 215, 255]

@Composable
private fun AnsiColor.resolve(): Color = when (this) {
    is AnsiColor.Rgb -> Color(red, green, blue)
    is AnsiColor.Indexed -> when {
        index < 16 -> [
            LocalColor.current, Theme.color.error, Theme.color.secondary, Theme.color.warning,
            Theme.color.primary, Theme.color.tertiary, Theme.color.secondary, LocalColorVariant.current
        ][index % 8]
        index < 232 -> {
            val v = index - 16
            Color(AnsiColorResolveSteps[v / 36], AnsiColorResolveSteps[(v / 6) % 6], AnsiColorResolveSteps[v % 6])
        }
        else -> (8 + (index - 232) * 10).let { Color(it, it, it) }
    }
}

@Stable
private data class LogViewport(
    val isScrollInProgress: Boolean,
    val canScrollForward: Boolean,
    val programmatic: Boolean,
    val userMoved: Boolean
)

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
                            color = span.style.foreground?.resolve() ?: if (entry.level == LogLevel.Error) errorColor else textColor,
                            background = span.style.background?.resolve() ?: Color.Unspecified,
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
