package love.yinlin.compose.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.TextIconAdapter

@Composable
fun TabBar(
    size: Int,
    index: Int,
    onNavigate: (Int) -> Unit,
    titleProvider: (Int) -> String,
    modifier: Modifier = Modifier,
    iconProvider: ((Int) -> ImageVector?)? = null,
    enabledProvider: ((Int) -> Boolean)? = null,
    key: ((Int) -> Any)? = null,
    padding: PaddingValues = Theme.padding.value,
    style: TextStyle = LocalStyle.current.bold,
    activeColor: Color = Theme.color.primary,
    onLongClick: ((Int) -> Unit)? = null,
) {
    LazyRow(
        modifier = modifier.semantics(Role.Tab),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(count = size, key = key) { i ->
            val selected = index == i
            val title = titleProvider(i)
            val icon = iconProvider?.invoke(i)
            val enabled = enabledProvider?.invoke(i) ?: true

            val contentColor = when {
                !enabled -> Theme.color.disabledContent
                selected -> activeColor
                else -> LocalColor.current
            }

            val indicatorRatio by animateFloatAsState(
                targetValue = if (selected) 1f else 0f,
                animationSpec = tween(Theme.animation.duration.default)
            )

            TextIconAdapter(
                modifier = Modifier.combinedClickable(
                    enabled = enabled,
                    onClick = { if (!selected) onNavigate(i) },
                    onLongClick = { onLongClick?.invoke(i) }
                ).drawBehind {
                    val (boxWidth, boxHeight) = this.size
                    val indicatorHeight = boxHeight * 0.05f
                    val startRatio = (1 - indicatorRatio) / 2
                    drawRect(
                        color = activeColor,
                        topLeft = Offset(startRatio * boxWidth, boxHeight - indicatorHeight),
                        size = Size(indicatorRatio * boxWidth, indicatorHeight)
                    )
                }.padding(padding)
            ) { iconId, textId ->
                icon?.let { Icon(icon = it, color = contentColor, modifier = Modifier.iconId()) }
                SimpleClipText(text = title, modifier = Modifier.textId(), color = contentColor, style = style)
            }
        }
    }
}