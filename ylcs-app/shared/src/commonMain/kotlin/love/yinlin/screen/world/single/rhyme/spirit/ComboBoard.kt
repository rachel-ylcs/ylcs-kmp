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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.TextDrawer
import love.yinlin.compose.game.animation.ReverseCurveFrameAnimation
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Transform
import love.yinlin.screen.world.single.rhyme.RhymeManager
import love.yinlin.screen.world.single.rhyme.data.ActionResult

@Stable
class ComboBoard(
    rhymeManager: RhymeManager,
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(1450f, 150f))
    override val size: Size = Size(400f, 200f)

    private val statistics = IntArray(ActionResult.entries.size) { 0 }

    private var result by mutableStateOf<ActionResult?>(null)
    private var combo by mutableIntStateOf(0)
    private var animation = ReverseCurveFrameAnimation((manager.fps * 0.75f).toInt())

    private val actionTextHeight = size.height * 0.6f
    private val comboTextHeight = size.height - actionTextHeight

    private val actionTextCache = TextDrawer.Cache()
    private val comboTextCache = TextDrawer.Cache(32)

    private val textBrush = ActionResult.entries.map { Brush.linearGradient(
        it.colors,
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    ) }

    override fun onClientUpdate(tick: Long) {
        if (!animation.update()) result = null
    }

    override fun Drawer.onClientDraw() {
        result?.let { currentResult ->
            animation.withProgress { progress ->
                val canvasWidth = size.width
                val canvasCenter = center
                val content = measureText(actionTextCache, currentResult.title, actionTextHeight, FontWeight.Black, FontStyle.Italic, fontIndex = 1)
                val contentStroke = measureText(actionTextCache, currentResult.title, actionTextHeight, FontWeight.Light, FontStyle.Italic, fontIndex = 1)
                // 判定结果
                transform({
                    scale(progress, canvasCenter)
                }) {
                    translate((canvasWidth - content.width) / 2, 0f) {
                        text(
                            content = content,
                            brush = textBrush[currentResult.ordinal],
                            shadow = Shadow(Colors.Dark, Offset(5f, 5f), 5f)
                        )
                        text(
                            content = contentStroke,
                            color = Colors.White,
                            drawStyle = Stroke(5f)
                        )
                    }
                }
                // 连击数
                if (combo > 1) {
                    val comboText = "+$combo"
                    val comboContent = measureText(comboTextCache, comboText, comboTextHeight, FontWeight.Black, FontStyle.Italic, fontIndex = 1)
                    val comboContentStroke = measureText(comboTextCache, comboText, comboTextHeight, FontWeight.Light, FontStyle.Italic, fontIndex = 1)
                    transform({
                        translate(0f, actionTextHeight)
                        scale(progress, Offset(canvasWidth / 2, comboTextHeight / 2))
                        translate((canvasWidth - comboContent.width) / 2, 0f)
                    }) {
                        text(
                            content = comboContent,
                            color = Colors.White.copy(alpha = (progress * 2).coerceIn(0f, 1f)),
                            shadow = Shadow(Colors.Dark, Offset(3f, 3f), 3f)
                        )
                        text(
                            content = comboContentStroke,
                            color = Colors.Dark.copy(alpha = progress),
                            drawStyle = Stroke(3f)
                        )
                    }
                }
            }
        }
    }

    fun updateAction(newResult: ActionResult, scoreRatio: Float = 1f): Int {
        // 统计
        ++statistics[newResult.ordinal]
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