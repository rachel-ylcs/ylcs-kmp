package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
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
    val colors: List<Color>,
) {
    MISS(
        score = 0,
        range = DynamicAction.BODY_RATIO * 6f,
        title = "MISS",
        colors = listOf(Colors(0xFFFF0844), Colors(0xFFFFB199))
    ),
    BAD(
        score = 1,
        range = DynamicAction.BODY_RATIO * 2.25f,
        title = "BAD",
        colors = listOf(Colors(0xFF9FA5D5), Colors(0xFFE8F5C8))
    ),
    GOOD(
        score = 2,
        range = DynamicAction.BODY_RATIO * 1.5f,
        title = "GOOD",
        colors = listOf(Colors(0xFF43E97B), Colors(0xFF38F9D7))
    ),
    PERFECT(
        score = 3,
        range = DynamicAction.BODY_RATIO * 0.75f,
        title = "PERFECT",
        colors = listOf(Colors(0xFFF6D365), Colors(0xFFFDA085))
    );

    fun startRange(center: Float) = center - range / 2
    fun endRange(center: Float) = center + range / 2
    fun inRange(center: Float, value: Float) = value >= startRange(center) && value <= endRange(center)
    fun viewStartRange(center: Float) = center - (range + DynamicAction.BODY_RATIO) / 2
    fun viewEndRange(center: Float) = center + (range + DynamicAction.BODY_RATIO) / 2

    companion object {
        const val COMBO_REWARD_COUNT = 30
    }
}

@Stable
class ComboBoard(
    rhymeManager: RhymeManager,
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(1400f, 150f))
    override val size: Size = Size(450f, 250f)

    private var result by mutableStateOf<ActionResult?>(null)
    private var combo by mutableIntStateOf(0)
    private var animation = CurveFrameAnimation((manager.fps * 0.7f).toInt())

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