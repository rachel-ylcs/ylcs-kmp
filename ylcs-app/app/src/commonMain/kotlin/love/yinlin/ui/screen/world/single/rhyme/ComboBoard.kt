package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import love.yinlin.compose.*
import kotlin.math.atan2

// 连击板
@Stable
internal class ComboBoard : RhymeDynamic(), RhymeContainer.Rectangle {
    @Stable
    enum class ActionResult(val score: Int, val title: String, val brush: Brush) {
        MISS(0, "MISS", Brush.verticalGradient(listOf(Colors.Ghost, Colors.Pink4))),
        BAD(1, "BAD", Brush.verticalGradient(listOf(Colors.Gray2, Colors.Red6))),
        GOOD(2, "GOOD", Brush.verticalGradient(listOf(Colors.Gray2, Colors.Orange2))),
        PERFECT(3, "PERFECT", Brush.verticalGradient(listOf(Colors.Yellow2, Colors.Green2)))
    }

    companion object {
        const val FPA = RhymeConfig.FPA / 3
        const val COMBO_COUNT = 20
        val RotateAngle = Track.Start.let { -atan2(it.y, it.x) }
    }

    private val textWidth = 270f
    private val textHeight: Float = 90f
    override val position: Offset = Offset(Size.Game.width / 2 + ProgressBoard.RADIUS / 2 + 50, 100f)
    override val size: Size = Size(textWidth, textHeight)
    override val transform: (DrawTransform.() -> Unit) = {
        rotateRad(RotateAngle, Offset(textWidth, textHeight))
    }

    private var result by mutableStateOf<ActionResult?>(null)
    private var combo by mutableIntStateOf(0)
    private var frame by mutableIntStateOf(0)

    private val actionTextCache = TextCache()
    private val comboTextCache = TextCache(16)

    fun updateAction(newResult: ActionResult): Int {
        // 重置进度
        result = newResult
        frame = 0
        // 计算得分
        if (newResult == ActionResult.MISS || newResult == ActionResult.BAD) combo = 0 // 清空连击
        else ++combo // 增加连击
        return newResult.score + combo / COMBO_COUNT // 连击得分奖励
    }

    override fun onUpdate(position: Long) {
        // Animation: Enter | Wait | Exit
        // Frame:      FPA  | FPA  | FPA
        if (frame == FPA * 3) {
            result = null
            frame = 0
        }
        else ++frame
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        result?.let { currentResult ->
            //         { 1 - ((x - FPA) / FPA) ^ 2  , 0       <= x <= FPA
            // f(x) =  { 1                          , FPA     <= x <= 2 * FPA
            //         { ((x - 3 * FPA) / FPA) ^ 2  , 2 * FPA <= x <= 3 * FPA
            val progress = when (val currentFrame = frame) {
                in 0 .. FPA -> 1 - (currentFrame - FPA) * (currentFrame - FPA) / (FPA * FPA).toFloat()
                in 2 * FPA .. 3 * FPA -> (currentFrame - 3 * FPA) * (currentFrame - 3 * FPA) / (FPA * FPA).toFloat()
                else -> 1f
            }.coerceIn(0f, 1f)
            // 结果
            val content = actionTextCache.measureText(textManager, currentResult.title, textHeight, FontWeight.Bold)
            scale(progress, this@ComboBoard.size.center) {
                textManager.run {
                    text(
                        content = content,
                        position = Offset((textWidth - content.width) / 2, 0f),
                        brush = currentResult.brush,
                        shadow = Shadow(Colors.Dark, Offset(2f, 2f), 2f)
                    )
                }
            }
            // 连击
            if (combo > 1) {
                val content = comboTextCache.measureText(textManager, "+$combo", textHeight / 2, FontWeight.Bold)
                val topLeft = Offset(textWidth - content.width, textHeight / 2)
                scale(progress, topLeft.translate(content.width / 2, textHeight / 4)) {
                    textManager.run {
                        text(
                            content = content,
                            position = topLeft,
                            color = Colors.White.copy(alpha = (progress * 2).coerceIn(0f, 1f)),
                            shadow = Shadow(Colors.Dark, Offset(1f, 1f), 1f)
                        )
                    }
                }
            }
        }
    }
}