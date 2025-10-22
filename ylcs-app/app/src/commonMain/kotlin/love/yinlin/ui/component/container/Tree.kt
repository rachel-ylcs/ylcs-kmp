package love.yinlin.ui.component.container

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.*
import love.yinlin.compose.ui.node.clickableNoRipple
import love.yinlin.compose.ui.image.MiniIcon

@Stable
data class TreeScope(
    val color: Color,
    val style: TextStyle,
    val horizontalPadding: Dp,
    val verticalPadding: Dp
) {
    @Composable
    fun Node(
        text: String,
        icon: ImageVector? = null,
        onClick: (() -> Unit)? = null,
        children: @Composable (TreeScope.() -> Unit)? = null
    ) {
        val expandable = children != null
        var expended by rememberFalse()

        Column {
            Row(
                modifier = Modifier.clickableNoRipple {
                    if (expandable) expended = !expended
                    else onClick?.invoke()
                }.padding(verticalPadding),
                horizontalArrangement = Arrangement.spacedBy(horizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (expandable) {
                    val animation by animateFloatAsState(targetValue = if (expended) 90f else 0f)
                    MiniIcon(
                        icon = Icons.AutoMirrored.Filled.ArrowRight,
                        modifier = Modifier.rotate(animation)
                    )
                    MiniIcon(icon = if (expended) Icons.Outlined.FolderOpen else Icons.Outlined.Folder)
                }
                else if (icon != null) MiniIcon(icon = icon)
                Text(text = text, color = color, style = style)
            }

            if (children != null) {
                AnimatedVisibility(visible = expended) {
                    Column(modifier = Modifier.padding(start = CustomTheme.padding.horizontalExtraSpace + horizontalPadding)) {
                        children()
                    }
                }
            }
        }
    }
}

@Composable
fun Tree(
    color: Color = Colors.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    horizontalPadding: Dp = CustomTheme.padding.littleSpace,
    verticalPadding: Dp = CustomTheme.padding.littleSpace,
    modifier: Modifier = Modifier,
    children: @Composable TreeScope.() -> Unit = {},
) {
    val scope = remember(color, style, horizontalPadding, verticalPadding) {
        TreeScope(color, style, horizontalPadding, verticalPadding)
    }
    Box(modifier = modifier) {
        scope.children()
    }
}