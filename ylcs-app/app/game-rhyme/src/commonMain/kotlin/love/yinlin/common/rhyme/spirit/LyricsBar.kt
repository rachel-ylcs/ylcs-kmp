package love.yinlin.common.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.util.fastJoinToString
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.TextDrawer
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Transform
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.common.rhyme.RhymeManager

@Stable
class LyricsBar(
    rhymeManager: RhymeManager,
    lyricsConfig: RhymeLyricsConfig
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(50f, 25f))
    override val size: Size = Size(620f, 44f)

    private val lines = lyricsConfig.lyrics
    private var currentIndex = -1
    private var text by mutableStateOf("")
    private var progress by mutableFloatStateOf(0f)

    private val textCache = TextDrawer.Cache(16)

    override fun onClientUpdate(tick: Long) {
        val nextLine = lines.getOrNull(currentIndex + 1)
        if (nextLine != null && tick >= nextLine.start) {
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
            if (tick > line.start + action.end) currentLength += length
            else {
                val start = theme.getOrNull(i - 1)?.end ?: 0
                currentLength += length * (tick - line.start - start) / (action.end - start).toFloat()
                break
            }
        }
        progress = (currentLength / totalLength).coerceIn(0f, 1f)
    }

    override fun Drawer.onClientDraw() {
        val line = text.ifEmpty { null } ?: return
        val content = measureText(textCache, line, size.height)
        val textWidth = content.width
        // 保持文本居中
        translate((size.width - textWidth) / 2, 0f) {
            val startWidth = textWidth * progress
            clip(Offset(startWidth, 0f), Size(textWidth - startWidth, size.height)) {
                text(
                    content = content,
                    color = Colors(0xFFE2E6FF),
                    shadow = Shadow(Colors.Dark, Offset(3f, 3f), 3f)
                )
            }
            clip(Offset.Zero, Size(startWidth, size.height)) {
                text(
                    content = content,
                    color = Colors.Green4
                )
            }
        }
    }
}