package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.rotateRad
import love.yinlin.common.Colors
import kotlin.math.atan2

// 分数板
@Stable
internal class ScoreBoard : RhymeDynamic(), RhymeContainer.Rectangle {
    @Stable
    private class ScoreNumber(pos: Offset) : RhymeDynamic(), RhymeContainer.Rectangle {
        // 七段数码管
        //     1
        // 6 ▎ ━  ▎ 2
        //   ▎ 7  ▎
        //   ▎ ━  ▎
        //   ▎    ▎
        // 5 ▎ ━  ▎ 3
        //     4
        companion object {
            const val APF = 600 / RhymeConfig.FPS

            const val RECT_WIDTH = 32f
            const val RECT_HEIGHT = 8f
            const val RECT_RADIUS = RECT_HEIGHT / 2
            const val WIDTH = 2 * RECT_HEIGHT + RECT_WIDTH
            const val HEIGHT = 3 * RECT_HEIGHT + 2 * RECT_WIDTH

            val NumberArray = byteArrayOf(
                (1 + 2 + 4 + 8 + 16 + 32 + 0).toByte(),
                (0 + 2 + 4 + 0 + 0 + 0 + 0).toByte(),
                (1 + 2 + 0 + 8 + 16 + 0 + 64).toByte(),
                (1 + 2 + 4 + 8 + 0 + 0 + 64).toByte(),
                (0 + 2 + 4 + 0 + 0 + 32 + 64).toByte(),
                (1 + 0 + 4 + 8 + 0 + 32 + 64).toByte(),
                (1 + 0 + 4 + 8 + 16 + 32 + 64).toByte(),
                (1 + 2 + 4 + 0 + 0 + 0 + 0).toByte(),
                (1 + 2 + 4 + 8 + 16 + 32 + 64).toByte(),
                (1 + 2 + 4 + 8 + 0 + 32 + 64).toByte()
            )

            val Rects = arrayOf(
                Rect(RECT_HEIGHT, 0f, RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT),
                Rect(RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT, RECT_HEIGHT * 2 + RECT_WIDTH, RECT_HEIGHT + RECT_WIDTH),
                Rect(RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT * 2 + RECT_WIDTH, RECT_HEIGHT * 2 + RECT_WIDTH, RECT_HEIGHT * 2 + RECT_WIDTH * 2),
                Rect(RECT_HEIGHT, RECT_HEIGHT * 2 + RECT_WIDTH * 2, RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT * 3 + RECT_WIDTH * 2),
                Rect(0f, RECT_HEIGHT * 2 + RECT_WIDTH, RECT_HEIGHT, RECT_HEIGHT * 2 + RECT_WIDTH * 2),
                Rect(0f, RECT_HEIGHT, RECT_HEIGHT, RECT_HEIGHT + RECT_WIDTH),
                Rect(RECT_HEIGHT, RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT + RECT_WIDTH, RECT_HEIGHT * 2 + RECT_WIDTH)
            )

            private fun fetchNumber(v: Byte): Int {
                for (i in 0 .. 9) {
                    if (NumberArray[i] == v) return i
                }
                return 0
            }

            private fun encode(v1: Byte, v2: Byte = 0, v3: Byte = 127): Int = ((v1.toInt() and 0xff) shl 16) or ((v2.toInt() and 0xff) shl 8) or (v3.toInt() and 0xff)
        }

        override val position: Offset = pos
        override val size: Size = Size(WIDTH, HEIGHT)

        private var data by mutableIntStateOf(encode(NumberArray[0]))

        val current: Byte get() = ((data shr 16) and 0xff).toByte()
        val target: Byte get() = ((data shr 8) and 0xff).toByte()
        val alpha: Byte get() = (data and 0xff).toByte()
        val isPlaying: Boolean get() = target.toInt() != 0
        val score: Int get() = fetchNumber(if (isPlaying) target else current)
        val isZero: Boolean get() = (if (isPlaying) target else current) == NumberArray[0]

        fun reset(v1: Byte = current, v2: Byte = target, v3: Byte = alpha) {
            data = encode(v1, v2, v3)
        }

        override fun onUpdate(position: Long) {
            val v2 = target
            if (v2.toInt() != 0) {
                val a = alpha
                if (a <= 0) reset(v1 = v2, v2 = 0, v3 = 127)
                else reset(v3 = (a - APF).toByte().coerceIn(0, 127))
            }
        }

        override fun DrawScope.onDraw(textManager: RhymeTextManager) {
            val a = (alpha / 127f).coerceIn(0f, 1f)
            val v1 = current.toInt()
            val v2 = target.toInt()
            repeat(Rects.size) {
                val mask = 1 shl it
                val rect = Rects[it]
                val b1 = (v1 and mask) == mask
                val b2 = (v2 and mask) == mask
                if (b1) {
                    if (b2) roundRect(Colors.Red4, RECT_RADIUS, rect.topLeft, rect.size, 1f)
                    else roundRect(Colors.Red4, RECT_RADIUS, rect.topLeft, rect.size, a)
                }
                else if (b2) roundRect(Colors.Red4, RECT_RADIUS, rect.topLeft, rect.size, 1 - a)
            }
        }
    }

    companion object {
        const val GAP = ScoreNumber.RECT_HEIGHT
        const val WIDTH = ScoreNumber.WIDTH * 4 + GAP * 3
        val RotateAngle = NoteBoard.Track.Center.let { atan2(it.y, it.x) }
    }

    override val position: Offset = Offset(Size.Game.width / 2 - ProgressBoard.RADIUS / 2 - 50 - WIDTH, 100f)
    override val size: Size = Size(WIDTH, ScoreNumber.HEIGHT)
    override val transform: (DrawTransform.() -> Unit) = {
        rotateRad(RotateAngle, Offset.Zero)
    }

    private val numbers = Array(4) {
        ScoreNumber(Offset(it * (ScoreNumber.WIDTH + GAP), 0f))
    }

    // 组合四个数位得分
    val score: Int get() {
        var factor = 10000
        return numbers.sumOf {
            factor /= 10
            it.score * factor
        }
    }

    // 增加得分
    fun addScore(value: Int) {
        var newScore = score + value
        if (value < 1 || newScore > 9999) return
        for (index in 3 downTo 0) {
            val number = numbers[index]
            val data = ScoreNumber.NumberArray[newScore % 10]
            if (number.isPlaying) {
                if (data != number.target) number.reset(v2 = data, v3 = number.alpha)
            }
            else {
                if (data != number.current) number.reset(v2 = data, v3 = 127)
            }
            newScore /= 10
        }
    }

    override fun onUpdate(position: Long) {
        for (number in numbers) number.onUpdate(position)
    }

    override fun DrawScope.onDraw(textManager: RhymeTextManager) {
        if (numbers.any { !it.isZero }) {
            for (number in numbers) number.run { draw(textManager) }
        }
    }
}