package love.yinlin.compose.ui.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.layout.MeasurePolicies

/**
 * @param icon 图标资源
 * @param size 大小
 * @param color 前景色
 * @param background 背景色
 */
@Composable
fun ColorIcon(
    icon: ImageVector,
    size: Dp = Theme.size.icon,
    color: Color = Theme.color.onContainer,
    background: Color = Theme.color.primaryContainer,
    modifier: Modifier = Modifier,
) {
    val colorFilter = remember(color) { if (color == Colors.Unspecified) null else ColorFilter.tint(color) }

    Layout(
        modifier = Modifier
            .size(size)
            .clip(Theme.shape.circle)
            .background(background)
            .padding(Theme.padding.g5)
            .then(modifier)
            .paint(
                painter = rememberVectorPainter(icon),
                colorFilter = colorFilter,
                contentScale = ContentScale.Fit
            ),
        measurePolicy = MeasurePolicies.Empty
    )
}