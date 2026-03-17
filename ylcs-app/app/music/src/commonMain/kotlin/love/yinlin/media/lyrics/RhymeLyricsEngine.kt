package love.yinlin.media.lyrics

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.util.fastJoinToString
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.text.FastCenterText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.parseJsonValue
import love.yinlin.fs.File

@Stable
internal data class DynamicLineItem(val ch: String, val end: Long)

@Stable
internal data class DynamicLine(override val position: Long, override val text: String, val items: List<DynamicLineItem>) : TextLine

@Stable
internal class RhymeLyricsEngine : TextLyricsEngine<DynamicLine>() {
    override val interval: Long = 16L
    override val type: LyricsEngineType = LyricsEngineType.Rhyme

    private var progress by mutableFloatStateOf(0f)

    override suspend fun load(rootPath: File): Boolean = catchingDefault(false) {
        val config = File(rootPath, type.resType.filename).readText()!!.parseJsonValue<RhymeLyricsConfig>()
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
    override fun LineItem(item: DynamicLine, isCurrent: Boolean, measurer: TextMeasurer) {
        val normalStyle = Theme.typography.v6
        val currentStyle = Theme.typography.v5.bold
        val normalColor = LocalColor.current

        FastCenterText(
            layoutAction = { layout(measurer, "T", if (isCurrent) currentStyle else normalStyle) },
            drawAction = {
                draw(measure(measurer, item.text, if (isCurrent) currentStyle else normalStyle)) {
                    clipRect(if (isCurrent) it.width * progress else 0f, 0f, it.width, it.height) {
                        drawText(it, normalColor)
                    }
                }
                if (isCurrent) {
                    draw(result = measure(measurer, item.text, currentStyle)) {
                        clipRect(0f, 0f, it.width * progress, it.height) {
                            drawText(it, Colors.Green5)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    override fun BoxScope.FloatingLine(config: LyricsEngineConfig, textStyle: TextStyle) {
        val measurer = rememberTextMeasurer(16)

        FastCenterText(
            layoutAction = { layout(measurer, "T", textStyle.copy(fontSize = textStyle.fontSize * config.textSize)) },
            drawAction = {
                val style = textStyle.copy(fontSize = textStyle.fontSize * config.textSize)
                val result = measure(measurer, lines?.getOrNull(currentIndex)?.text ?: "", style)
                draw(result) {
                    val offset = it.width * progress
                    drawBackground(result, Colors(config.backgroundColor))
                    clipRect(offset, 0f, it.width, it.height) {
                        drawText(it, Colors.White)
                    }
                    clipRect(0f, 0f, offset, it.height) {
                        drawText(it, Colors(config.textColor))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}