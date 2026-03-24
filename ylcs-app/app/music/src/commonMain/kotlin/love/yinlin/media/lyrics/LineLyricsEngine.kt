package love.yinlin.media.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.text.FastCenterText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.extension.catchingDefault
import love.yinlin.fs.File
import love.yinlin.tpl.lyrics.LrcParser

@Stable
internal data class StaticLine(override val position: Long, override val text: String) : TextLine

@Stable
internal class LineLyricsEngine : TextLyricsEngine<StaticLine>() {
    override val interval: Long = 150L
    override val type: LyricsEngineType = LyricsEngineType.Line

    override suspend fun load(rootPath: File): Boolean = catchingDefault(false) {
        val source = File(rootPath, type.resType.filename).readText()
        lines = source?.let { LrcParser(it).lines }?.map { StaticLine(it.position, it.text) }
        currentIndex = -1
        lines!!.isNotEmpty()
    }

    @Composable
    override fun LinePlaceholder() {
        SimpleEllipsisText(text = "", style = Theme.typography.v6)
    }

    @Composable
    override fun LineItem(item: StaticLine, config: LyricsEngineConfig, isCurrent: Boolean, measurer: TextMeasurer) {
        val color by animateColorAsState(
            targetValue = if (isCurrent) Colors(config.textColor) else Colors(config.textBackgroundColor),
            animationSpec = tween(durationMillis = Theme.animation.duration.v3)
        )
        val normalStyle = Theme.typography.v6
        val currentStyle = Theme.typography.v5.bold

        FastCenterText(
            layoutAction = { layout(measurer, "T", if (isCurrent) currentStyle else normalStyle) },
            drawAction = {
                draw(measure(measurer, item.text, if (isCurrent) currentStyle else normalStyle)) {
                    drawText(it, color)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    override fun BoxScope.FloatingLine(config: LyricsEngineConfig, textStyle: TextStyle) {
        val measurer = rememberTextMeasurer(32)

        FastCenterText(
            layoutAction = { layout(measurer, "T", textStyle.copy(fontSize = textStyle.fontSize * config.textSize)) },
            drawAction = {
                draw(measure(measurer, lines?.getOrNull(currentIndex)?.text ?: "", textStyle.copy(fontSize = textStyle.fontSize * config.textSize))) {
                    drawBackground(it, Colors(config.backgroundColor))
                    drawText(it, Colors(config.textColor))
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}