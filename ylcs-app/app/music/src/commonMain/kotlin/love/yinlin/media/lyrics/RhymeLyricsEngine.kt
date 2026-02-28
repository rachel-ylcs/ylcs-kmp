package love.yinlin.media.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.zIndex
import kotlinx.io.files.Path
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.readText

@Stable
internal data class DynamicLineItem(val ch: String, val end: Long)

@Stable
internal data class DynamicLine(override val position: Long, override val text: String, val items: List<DynamicLineItem>) : TextLine

@Stable
internal class RhymeLyricsEngine : TextLyricsEngine<DynamicLine>() {
    override val interval: Long = 16L
    override val type: LyricsEngineType = LyricsEngineType.Rhyme

    private var progress by mutableFloatStateOf(0f)

    override suspend fun load(rootPath: Path): Boolean = catchingDefault(false) {
        val config = Path(rootPath, type.resType.filename).readText()!!.parseJsonValue<RhymeLyricsConfig>()
        // 偏移校准
        val offset = config.offset
        lines = config.lyrics.map { rhymeLine ->
            val lineStart = rhymeLine.start + offset
            val theme = rhymeLine.theme
            DynamicLine(
                position = lineStart,
                text = theme.fastJoinToString(separator = "") { it.ch },
                items = theme.map { DynamicLineItem(it.ch, lineStart + it.end) }
            )
        }
        currentIndex = -1
        progress = 0f
        lines!!.isNotEmpty()
    }

    override fun reset() {
        super.reset()
        progress = 0f
    }

    override fun update(position: Long) {
        super.update(position)
        val line = lines?.getOrNull(currentIndex)
        val items = line?.items ?: return
        var currentLength = 0f
        val totalLength = line.text.length
        for (i in items.indices) {
            val (ch, end) = items[i]
            val length = ch.length
            if (position > end) currentLength += length
            else {
                val start = items.getOrNull(i - 1)?.end ?: line.position
                currentLength += length * (position - start) / (end - start).toFloat()
                break
            }
        }
        progress = if (totalLength == 0) 0f else (currentLength / totalLength).coerceIn(0f, 1f)
    }

    @Composable
    override fun LinePlaceholder() {
        SimpleEllipsisText(text = "", style = Theme.typography.v6)
    }

    @Composable
    override fun LineItem(item: DynamicLine, isCurrent: Boolean) {
        SimpleEllipsisText(
            text = item.text,
            style = if (isCurrent) Theme.typography.v5.bold else Theme.typography.v6,
            modifier = Modifier.zIndex(1f).graphicsLayer {
                if (isCurrent) {
                    clip = true
                    shape = GenericShape { size, _ ->
                        addRect(Rect(size.width * progress, 0f, size.width, size.height))
                    }
                }
            }
        )
        if (isCurrent) {
            SimpleEllipsisText(
                text = item.text,
                color = Colors.Green5,
                style = Theme.typography.v5.bold,
                modifier = Modifier.semantics().zIndex(2f).graphicsLayer {
                    clip = true
                    shape = GenericShape { size, _ ->
                        addRect(Rect(0f, 0f, size.width * progress, size.height))
                    }
                }
            )
        }
    }

    @Composable
    override fun BoxScope.FloatingLine(modifier: Modifier, config: LyricsEngineConfig, textStyle: TextStyle) {
        val currentText by rememberDerivedState { lines?.getOrNull(currentIndex)?.text ?: "" }
        val style = textStyle.copy(fontSize = textStyle.fontSize * config.textSize)

        Box(modifier = modifier) {
            SimpleEllipsisText(
                text = currentText,
                color = Colors.White,
                style = style,
                modifier = Modifier.zIndex(1f).graphicsLayer {
                    clip = true
                    shape = GenericShape { size, _ ->
                        addRect(Rect(size.width * progress, 0f, size.width, size.height))
                    }
                }
            )

            SimpleEllipsisText(
                text = currentText,
                color = Colors(config.textColor),
                style = style,
                modifier = Modifier.semantics().zIndex(2f).graphicsLayer {
                    clip = true
                    shape = GenericShape { size, _ ->
                        addRect(Rect(0f, 0f, size.width * progress, size.height))
                    }
                }
            )
        }
    }
}