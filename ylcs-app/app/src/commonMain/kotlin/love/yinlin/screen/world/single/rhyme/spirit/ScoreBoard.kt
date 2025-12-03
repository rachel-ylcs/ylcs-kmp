package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.util.fastJoinToString
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.TextDrawer
import love.yinlin.compose.game.animation.LineFrameAnimation
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Transform
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
class ScoreBoard(
    rhymeManager: RhymeManager,
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(275f, 14f))
    override val size: Size = Size(350f, 70f)

    // 游戏得分
    private var score: Int by mutableIntStateOf(0)
    private val scoreText: String by derivedStateOf { score.toString().padStart(4, '0').toList().fastJoinToString(" ") }

    private val textCache = TextDrawer.Cache()
    private val animation = LineFrameAnimation(manager.fps / 2)

    override fun onClientUpdate(tick: Long) {
        animation.update()
    }

    override fun Drawer.onClientDraw() {
        animation.withProgress({ isCompleted, progress ->
            if (isCompleted) 1f else (progress + 0.3f).coerceAtMost(1f)
        }) { progress ->
            val content = measureText(textCache, scoreText, this@ScoreBoard.size.height, FontWeight.ExtraBold)
            translate((size.width - content.width) / 2, 0f) {
                text(
                    content = content,
                    color = Colors(0xffe2e6ff).copy(alpha = progress),
                    shadow = Shadow(Colors.Dark, Offset(3f, 3f), 3f)
                )
            }
        }
    }

    fun addScore(value: Int) {
        if (value != 0) {
            score += value
            animation.start()
        }
    }
}