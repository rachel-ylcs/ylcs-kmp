package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import love.yinlin.compose.Colors
import love.yinlin.compose.Path
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.TextDrawer
import love.yinlin.compose.game.animation.CurveFrameAnimation
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.translate
import love.yinlin.data.music.RhymeLyricsConfig
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
class ChorusEnvironment(
    rhymeManager: RhymeManager,
    lyricsConfig: RhymeLyricsConfig,
) : Spirit(rhymeManager), BoxBody {
    companion object {
        const val TIP_HEIGHT = 100f
        const val ANIMATION_DURATION = 4000L
    }

    override val size: Size = manager.size

    private val chorusList = lyricsConfig.chorus

    private var chorusIndex = 0
    private val animation = CurveFrameAnimation(manager.fps * (ANIMATION_DURATION / 1000).toInt(), false)
    private val leftArea = Path(arrayOf(
        leftCenter.translate(x = -size.width / 2, y = -TIP_HEIGHT / 2),
        leftCenter.translate(x = -size.width / 2, y = TIP_HEIGHT / 2),
        leftCenter.translate(x = -TIP_HEIGHT, y = TIP_HEIGHT / 2),
        leftCenter.translate(y = -TIP_HEIGHT / 2)
    ))
    private val rightArea = Path(arrayOf(
        rightCenter.translate(x = size.width / 2, y = TIP_HEIGHT / 2),
        rightCenter.translate(x = size.width / 2, y = -TIP_HEIGHT / 2),
        rightCenter.translate(x = TIP_HEIGHT, y = -TIP_HEIGHT / 2),
        rightCenter.translate(y = TIP_HEIGHT / 2)
    ))

    private val leftText = "C   H   O   "
    private val rightText = "   R   U   S"
    private val textCache = TextDrawer.Cache()

    override fun onClientUpdate(tick: Long) {
        animation.update()
        chorusList.getOrNull(chorusIndex)?.let { chorus ->
            // 到达副歌点
            if (tick > chorus.start - ANIMATION_DURATION) {
                ++chorusIndex
                animation.start()
            }
        }
    }

    override fun Drawer.onClientDraw() {
        animation.withProgress { progress ->
            val offset = size.width / 2 * progress
            val textHeight = TIP_HEIGHT * 0.8f
            translate(x = offset, y = 0f) {
                path(Colors.Ghost, leftArea, alpha = 0.25f * progress)
                val content = measureText(textCache, leftText, textHeight, FontWeight.ExtraBold)
                val contentBorder = measureText(textCache, leftText, textHeight, FontWeight.Bold)
                translate(x = -TIP_HEIGHT - content.width, y = (size.height - TIP_HEIGHT * 0.8f) / 2) {
                    text(
                        content = content,
                        color = Colors.Ghost.copy(alpha = 0.75f * progress)
                    )
                    text(
                        content = contentBorder,
                        color = Colors.Steel4.copy(alpha = 0.25f * progress),
                        drawStyle = Stroke(1f * progress)
                    )
                }
            }
            translate(x = -offset, y = 0f) {
                path(Colors.Ghost, rightArea, alpha = 0.25f * progress)
                val content = measureText(textCache, rightText, textHeight, FontWeight.ExtraBold)
                val contentBorder = measureText(textCache, rightText, textHeight, FontWeight.Bold)
                translate(x = size.width + TIP_HEIGHT, y = (size.height - TIP_HEIGHT * 0.8f) / 2) {
                    text(
                        content = content,
                        color = Colors.Ghost.copy(alpha = 0.75f * progress)
                    )
                    text(
                        content = contentBorder,
                        color = Colors.Steel4.copy(alpha = 0.25f * progress),
                        drawStyle = Stroke(1f * progress)
                    )
                }
            }
        }
    }
}