package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
enum class ActionResult(
    val score: Int,
    val range: Float,
    val title: String,
    val brush: Brush
) {
    MISS(0, DynamicAction.BODY_RATIO * 6f, "MISS", Brush.verticalGradient(listOf(Colors.Ghost, Colors.Pink4))),
    BAD(1, DynamicAction.BODY_RATIO * 2.25f, "BAD", Brush.verticalGradient(listOf(Colors.Gray2, Colors.Red6))),
    GOOD(2, DynamicAction.BODY_RATIO * 1.5f, "GOOD", Brush.verticalGradient(listOf(Colors.Gray2, Colors.Orange2))),
    PERFECT(3, DynamicAction.BODY_RATIO * 0.75f, "PERFECT", Brush.verticalGradient(listOf(Colors.Yellow2, Colors.Green2)));

    fun startRange(center: Float) = center - range / 2
    fun endRange(center: Float) = center + range / 2
    fun inRange(center: Float, value: Float) = value >= startRange(center) && value <= endRange(center)
    fun viewStartRange(center: Float) = center - (range + DynamicAction.BODY_RATIO) / 2
    fun viewEndRange(center: Float) = center + (range + DynamicAction.BODY_RATIO) / 2

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

    private var result by mutableStateOf<ActionResult?>(null)
    private var combo by mutableIntStateOf(0)
    private var animation = CurveFrameAnimation(manager.fps / 2)

    private val actionTextCache = TextDrawer.Cache()
    private val comboTextCache = TextDrawer.Cache(32)

    override fun onClientUpdate(tick: Long) {
        if (!animation.update()) result = null
    }

    override fun Drawer.onClientDraw() {
        result?.let { currentResult ->
            animation.withProgress { progress ->
                val canvasWidth = size.width
                val textHeight = size.height
                val contentCenter = center
                val content = measureText(actionTextCache, currentResult.title, textHeight, FontWeight.ExtraBold)
                val contentBorder = measureText(actionTextCache, currentResult.title, textHeight, FontWeight.Bold)
                // 判定结果
                transform({
                    scale(progress, contentCenter)
                    translate((canvasWidth - content.width) / 2, 0f)
                }) {
                    text(
                        content = content,
                        brush = currentResult.brush
                    )
                    text(
                        content = contentBorder,
                        color = Colors.Dark.copy(alpha = 0.5f),
                        drawStyle = Stroke(width = 1f, join = StrokeJoin.Round)
                    )
                }
                // 连击数
                if (combo > 1) {
                    val comboText = "+$combo"
                    val comboContent = measureText(comboTextCache, comboText, textHeight / 2, FontWeight.ExtraBold)
                    val comboContentBorder = measureText(comboTextCache, comboText, textHeight / 2, FontWeight.Bold)
                    val topLeft = Offset(canvasWidth - comboContent.width, textHeight / 2)
                    transform({
                        translate(topLeft)
                        scale(progress, Offset(comboContent.width / 2, textHeight / 4))
                    }) {
                        text(
                            content = comboContent,
                            color = Colors.White.copy(alpha = (progress * 2).coerceIn(0f, 1f))
                        )
                        text(
                            content = comboContentBorder,
                            color = Colors.Dark.copy(alpha = 0.5f),
                            drawStyle = Stroke(width = 1f, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }
    }

    fun updateAction(newResult: ActionResult, scoreRatio: Float = 1f): Int {
        // 重置进度
        result = newResult
        // 计算得分
        if (newResult == ActionResult.MISS || newResult == ActionResult.BAD) combo = 0 // 清空连击
        else ++combo // 增加连击
        val reward = (newResult.score * scoreRatio).toInt() + combo / ActionResult.COMBO_REWARD_COUNT // 连击得分奖励
        animation.start()
        return reward
    }
}