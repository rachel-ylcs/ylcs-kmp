package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.util.fastJoinToString
import love.yinlin.common.Colors
import love.yinlin.data.music.RhymeLyricsConfig
import kotlin.text.ifEmpty

// 歌词板
@Stable
internal class LyricsBoard(
    private val lyrics: RhymeLyricsConfig
) : RhymeDynamic(), RhymeContainer.Rectangle {
    private val textHeight: Float = 72f
    override val position: Offset = Offset.Zero
    override val size: Size = Size(Size.Game.width, textHeight)

    private var currentIndex = -1
    private var text by mutableStateOf("")
    private var progress by mutableFloatStateOf(0f)

    private val textCache = TextCache(16)

    override fun onUpdate(position: Long) {
        val lines = lyrics.lyrics
        val nextLine = lines.getOrNull(currentIndex + 1)
        if (nextLine != null && position >= nextLine.start) {
            ++currentIndex
            // 合并字符显示 (因为 plain text 内可能包含不是 Action 的空白字符)
            text = nextLine.theme.fastJoinToString("") { it.ch }
        } else if (progress >= 1f) return // 优化句间停顿

        val line = lines.getOrNull(currentIndex) ?: return
        val theme = line.theme
        var currentLength = 0f
        val totalLength = text.length
        if (theme.size != totalLength) return

        for (i in theme.indices) {
            val action = theme[i]
            val length = action.ch.length
            if (position > line.start + action.end) currentLength += length
            else {
                val start = theme.getOrNull(i - 1)?.end ?: 0
                currentLength += length * (position - line.start - start) / (action.end - start).toFloat()
                break
            }
        }
        progress = (currentLength / totalLength).coerceIn(0f, 1f)
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        val line = text.ifEmpty { null } ?: return
        val content = textCache.measureText(textManager, line, textHeight)
        val textWidth = content.width
        val start = Offset((this@LyricsBoard.size.width - textWidth) / 2, 0f)
        textManager.run {
            text(
                content = content,
                position = start,
                color = Colors.White,
                shadow = Shadow(Colors.Dark, Offset(3f, 3f), 3f)
            )
            clip(start, Size(textWidth * progress, textHeight)) {
                text(
                    content = content,
                    position = start,
                    color = Colors.Green4
                )
            }
        }
    }
}