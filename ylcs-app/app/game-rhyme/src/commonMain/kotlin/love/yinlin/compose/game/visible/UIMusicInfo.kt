package love.yinlin.compose.game.visible

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.util.fastForEach
import love.yinlin.app.global.resources.Res
import love.yinlin.app.global.resources.xwwk
import love.yinlin.compose.Colors
import love.yinlin.compose.extension.scale
import love.yinlin.compose.game.data.RhymePlayInfo
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.compose.game.drawer.PrepareDrawer
import love.yinlin.compose.game.drawer.TextGraph
import love.yinlin.compose.game.layer.UILayer
import love.yinlin.compose.game.traits.Visible

class UIMusicInfo(private val info: RhymePlayInfo) : Visible(position = DefaultPosition, size = DefaultSize) {
    companion object {
        private const val STAR_SIZE = UILayer.DEFAULT_HEIGHT * 0.35f
        private const val STAR_SCALE = STAR_SIZE / 1024f
        private val DefaultPosition = Offset(UILayer.DEFAULT_PADDING + UICover.DefaultSize.width + 30, UILayer.DEFAULT_PADDING)
        private val DefaultSize = Size(500f, UILayer.DEFAULT_HEIGHT)
        private val TextSize = Size(500f, STAR_SIZE)
        private val TextStroke = Stroke(1f)
    }

    private val difficulty = info.playConfig.difficulty

    private val starPaths = listOf(
        Path().apply {
            moveTo(539.457f, 110.815f)
            lineTo(418.568f, 355.852f)
            lineTo(539.457f, 483.457f)
            lineTo(539.457f, 110.815f)
            close()
        } to Color(0xFF60C9C3),
        Path().apply {
            moveTo(418.568f, 355.852f)
            lineTo(148.05f, 395.16f)
            lineTo(539.457f, 483.457f)
            lineTo(418.568f, 355.852f)
            close()
        } to Color(0xFF6ADDD6),
        Path().apply {
            moveTo(660.444f, 355.852f)
            lineTo(539.457f, 110.815f)
            lineTo(539.457f, 483.457f)
            lineTo(660.444f, 355.852f)
            close()
        } to Color(0xFF6ADDD6),
        Path().apply {
            moveTo(930.864f, 395.16f)
            lineTo(660.444f, 355.852f)
            lineTo(539.457f, 483.457f)
            lineTo(930.864f, 395.16f)
            close()
        } to Color(0xFFA9ECEB),
        Path().apply {
            moveTo(735.111f, 585.975f)
            lineTo(930.864f, 395.16f)
            lineTo(539.457f, 483.457f)
            lineTo(735.111f, 585.975f)
            close()
        } to Color(0xFF00A298),
        Path().apply {
            moveTo(539.457f, 483.457f)
            lineTo(781.333f, 855.309f)
            lineTo(735.111f, 585.975f)
            lineTo(539.457f, 483.457f)
            close()
        } to Color(0xFFA9ECEB),
        Path().apply {
            moveTo(148.049f, 395.16f)
            lineTo(343.802f, 585.975f)
            lineTo(539.457f, 483.457f)
            lineTo(148.049f, 395.16f)
            close()
        } to Color(0xFF00A298),
        Path().apply {
            moveTo(343.802f, 585.975f)
            lineTo(297.58f, 855.309f)
            lineTo(539.457f, 483.457f)
            lineTo(343.802f, 585.975f)
            close()
        } to Color(0xFF6ADDD6),
        Path().apply {
            moveTo(297.58f, 855.309f)
            lineTo(539.457f, 728.1f)
            lineTo(539.457f, 483.457f)
            lineTo(297.58f, 855.309f)
            close()
        } to Color(0xFF00C4B8),
        Path().apply {
            moveTo(539.457f, 483.457f)
            lineTo(539.457f, 728.1f)
            lineTo(781.333f, 855.309f)
            lineTo(539.457f, 483.457f)
            close()
        } to Color(0xFF6ADDD6)
    )

    private var title: TextGraph? = null

    override fun PrepareDrawer.prepareDraw(viewportSize: Size, viewportBounds: Rect) {
        if (title == null) title = measureText(info.musicInfo.name, Res.font.xwwk, FontWeight.Bold)
    }

    override fun Drawer.onDraw() {
        // 标题
        title?.let { graph ->
            text(graph, Offset.Zero, TextSize, Colors.Dark)
            text(graph, Offset.Zero, TextSize, Colors.White, drawStyle = TextStroke)
        }
        // 难度星级
        repeat(difficulty.ordinal + 1) { index ->
            transform({
                translate((index - 0.1f) * STAR_SIZE, STAR_SIZE + 10)
                scale(STAR_SCALE, Offset.Zero)
            }) {
                starPaths.fastForEach { (path, color) -> path(color, path) }
            }
        }
    }
}