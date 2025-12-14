package love.yinlin.platform.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.util.fastJoinToString
import androidx.compose.ui.zIndex
import kotlinx.io.files.Path
import love.yinlin.compose.Colors
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.readText

@Stable
internal data class DynamicLineItem(val ch: String, val end: Long)

@Stable
internal data class DynamicLine(override val position: Long, override val text: String, val items: List<DynamicLineItem>) : TextLine()

@Stable
internal class RhymeLyricsEngine : TextLyricsEngine<DynamicLine>() {
    override val interval: Long = 20L
    override val type: LyricsEngineType = LyricsEngineType.Rhyme

    private var progress by mutableFloatStateOf(0f)

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
        super.reset()
        progress = 0f
    }

    override fun update(position: Long) {
        super.update(position)
        val items = lines?.getOrNull(currentIndex)?.items ?: return
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
    override fun LineItem(item: DynamicLine, offset: Int) {
        val fontSize = MaterialTheme.typography.headlineSmall.fontSize / (offset / 30f + 1f)
        val fontWeight = if (offset == 0) FontWeight.Bold else FontWeight.Light
        val (borderWidth, shadowWidth) = with(LocalDensity.current) {
            val fontSizePx = fontSize.toPx()
            fontSizePx / 16f to fontSizePx / 24f
        }
        val text = item.text

        Box {
            Text(
                text = text,
                color = Colors.White,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = fontSize, fontWeight = fontWeight),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.zIndex(2f)
            )
            if (offset == 0) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = fontSize, fontWeight = fontWeight),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.semantics { hideFromAccessibility() }.zIndex(3f).drawWithContent {
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
                        shadow = Shadow(color = Colors.Black, offset = Offset(shadowWidth, shadowWidth), blurRadius = shadowWidth),
                        drawStyle = Stroke(width = borderWidth, join = StrokeJoin.Round)
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

    @Composable
    override fun BoxScope.FloatingLine(config: LyricsEngineConfig, textStyle: TextStyle) {
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