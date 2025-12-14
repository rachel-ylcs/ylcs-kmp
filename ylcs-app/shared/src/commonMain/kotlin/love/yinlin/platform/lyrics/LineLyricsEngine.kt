package love.yinlin.platform.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.zIndex
import kotlinx.io.files.Path
import love.yinlin.compose.Colors
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.readText

@Stable
internal data class StaticLine(override val position: Long, override val text: String) : TextLine()

@Stable
internal class LineLyricsEngine : TextLyricsEngine<StaticLine>() {
    override val interval: Long = 150L
    override val type: LyricsEngineType = LyricsEngineType.Line

    override suspend fun load(rootPath: Path): Boolean = catchingDefault(false) {
        val source = Path(rootPath, type.resType.filename).readText()
        lines = source?.let { LrcParser(it).paddingLyrics }?.map { StaticLine(it.position, it.text) }
        currentIndex = -1
        return lines != null
    }

    @Composable
    override fun LineItem(item: StaticLine, offset: Int) {
        val fontSize = MaterialTheme.typography.headlineSmall.fontSize / (offset / 30f + 1f)
        val fontWeight = if (offset == 0) FontWeight.Bold else FontWeight.Light
        val color = if (offset == 0) MaterialTheme.colorScheme.primary else Colors.White
        val (borderWidth, shadowWidth) = with(LocalDensity.current) {
            val fontSizePx = fontSize.toPx()
            fontSizePx / 16f to fontSizePx / 24f
        }
        val text = item.text

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

    @Composable
    override fun BoxScope.FloatingLine(config: LyricsEngineConfig, textStyle: TextStyle) {
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