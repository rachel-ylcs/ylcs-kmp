package love.yinlin.compose.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.semantics.Role
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.TextIconAdapter

@Composable
fun Breadcrumb(
    size: Int,
    onNavigate: (Int) -> Unit,
    titleProvider: (Int) -> String,
    modifier: Modifier = Modifier,
    iconProvider: ((Int) -> ImageVector?)? = null,
    key: ((Int) -> Any)? = null,
    padding: PaddingValues = Theme.padding.value,
    activeColor: Color = Theme.color.primary,
) {
    LazyRow(
        modifier = modifier.semantics(Role.Tab),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(count = size, key = key) { index ->
            Row(
                modifier = Modifier.animateItem(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val title = titleProvider(index)
                val icon = iconProvider?.invoke(index)

                val isLast = index == size - 1
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                val contentColor by animateColorAsState(
                    targetValue = when {
                        isHovered -> activeColor
                        isLast -> LocalColor.current
                        else -> LocalColorVariant.current
                    },
                    animationSpec = tween(Theme.animation.duration.default)
                )

                TextIconAdapter(
                    modifier = Modifier.clickable(
                        enabled = !isLast,
                        indication = null,
                        interactionSource = interactionSource,
                        onClick = { onNavigate(index) }
                    ).pointerIcon(PointerIcon.Hand).padding(padding)
                ) { iconId, textId ->
                    icon?.let { Icon(icon = it, color = contentColor, modifier = Modifier.iconId()) }
                    SimpleClipText(text = title, modifier = Modifier.textId(), color = contentColor, style = LocalStyle.current.bold)
                }
                if (!isLast) Icon(icon = Icons.KeyboardArrowRight)
            }
        }
    }
}