package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.TextDrawer
import love.yinlin.compose.game.animation.CurveFrameAnimation
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Transform
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
enum class ComboActionResult(val score: Int, val title: String, val brush: Brush) {
    MISS(0, "MISS", Brush.verticalGradient(listOf(Colors.Ghost, Colors.Pink4))),
    BAD(1, "BAD", Brush.verticalGradient(listOf(Colors.Gray2, Colors.Red6))),
    GOOD(2, "GOOD", Brush.verticalGradient(listOf(Colors.Gray2, Colors.Orange2))),
    PERFECT(3, "PERFECT", Brush.verticalGradient(listOf(Colors.Yellow2, Colors.Green2)));

    companion object {
        const val COMBO_REWARD_COUNT = 20
    }
}

@Stable
class ComboBoard(
    rhymeManager: RhymeManager,
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(1400f, 150f))
    override val size: Size = Size(450f, 110f)

    private var result by mutableStateOf<ComboActionResult?>(null)
    private var combo by mutableIntStateOf(0)
    private var animation = CurveFrameAnimation(manager.fps / 2)

    private val actionTextCache = TextDrawer.Cache()
    private val comboTextCache = TextDrawer.Cache(16)

    override fun onClientUpdate(tick: Long) {
        if (!animation.update()) result = null
    }

    override fun Drawer.onClientDraw() {
        result?.let { currentResult ->
            val progress = animation.progress
            val canvasWidth = this@ComboBoard.size.width
            val textHeight = this@ComboBoard.size.height
            val content = measureText(actionTextCache, currentResult.title, textHeight, FontWeight.ExtraBold)
            transform({
                scale(progress, this@ComboBoard.center)
                translate((canvasWidth - content.width) / 2, 0f)
            }) {
                text(
                    content = content,
                    brush = currentResult.brush,
                    shadow = Shadow(Colors.Dark, Offset(5f, 5f), 5f)
                )
            }
            // 连击
            if (combo > 1) {
                val comboContent = measureText(comboTextCache, "+$combo", textHeight / 2, FontWeight.ExtraBold)
                val topLeft = Offset(canvasWidth - comboContent.width, textHeight / 2)
                transform({
                    translate(topLeft)
                    scale(progress, Offset(comboContent.width / 2, textHeight / 4))
                }) {
                    text(
                        content = comboContent,
                        color = Colors.White.copy(alpha = (progress * 2).coerceIn(0f, 1f)),
                        shadow = Shadow(Colors.Dark, Offset(3f, 3f), 3f)
                    )
                }
            }
        }
    }

    fun updateAction(newResult: ComboActionResult): Int {
        // 重置进度
        result = newResult
        // 计算得分
        if (newResult == ComboActionResult.MISS || newResult == ComboActionResult.BAD) combo = 0 // 清空连击
        else ++combo // 增加连击
        val reward = newResult.score + combo / ComboActionResult.COMBO_REWARD_COUNT // 连击得分奖励
        animation.start()
        return reward
    }
}