package love.yinlin.media.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import kotlinx.io.files.Path
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.readText
import love.yinlin.tpl.lyrics.LrcParser

@Stable
internal data class StaticLine(override val position: Long, override val text: String) : TextLine

@Stable
internal class LineLyricsEngine : TextLyricsEngine<StaticLine>() {
    override val interval: Long = 150L
    override val type: LyricsEngineType = LyricsEngineType.Line

    override suspend fun load(rootPath: Path): Boolean = catchingDefault(false) {
        val source = Path(rootPath, type.resType.filename).readText()
        lines = source?.let { LrcParser(it).lines }?.map { StaticLine(it.position, it.text) }
        currentIndex = -1
        lines!!.isNotEmpty()
    }

    @Composable
    override fun LinePlaceholder() {
        SimpleEllipsisText(text = "", style = Theme.typography.v6)
    }

    @Composable
    override fun LineItem(item: StaticLine, isCurrent: Boolean) {
        val color by animateColorAsState(targetValue = if (isCurrent) Colors.Green5 else LocalColor.current, animationSpec = tween(durationMillis = Theme.animation.duration.v3))
        SimpleEllipsisText(text = item.text, color = color, style = if (isCurrent) Theme.typography.v5.bold else Theme.typography.v6)
    }

    @Composable
    override fun BoxScope.FloatingLine(modifier: Modifier, config: LyricsEngineConfig, textStyle: TextStyle) {
        val currentText by rememberDerivedState { lines?.getOrNull(currentIndex)?.text ?: "" }
        SimpleEllipsisText(
            text = currentText,
            color = Colors(config.textColor),
            style = textStyle.copy(fontSize = textStyle.fontSize * config.textSize),
            textAlign = TextAlign.Center,
            modifier = modifier
        )
    }
}