package love.yinlin.compose.game.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.util.fastForEach
import love.yinlin.compose.game.data.RhymeDifficulty

internal val StarPaths by lazy {
    listOf(
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
}

@Composable
internal fun DifficultyStar(
    difficulty: RhymeDifficulty,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.drawWithContent {
        val h = size.height
        repeat(difficulty.ordinal + 1) { index ->
            withTransform({
                translate(index * h)
                scale(h / 1024f, Offset.Zero)
            }) {
                StarPaths.fastForEach { (path, color) -> drawPath(path, color) }
            }
        }
    })
}