package love.yinlin.compose.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.container.SurfaceContainer
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter

@Stable
class MenuScope(val onClose: () -> Unit) {
    @Composable
    fun Menu(text: String? = null, icon: ImageVector? = null, enabled: Boolean = true, onClick: (() -> Unit)? = null) {
        val contentColor = if (enabled) LocalColor.current else Theme.color.disabledContent
        ThemeContainer(contentColor) {
            TextIconAdapter(
                modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = {
                    onClick?.invoke()
                    onClose()
                }).padding(Theme.padding.value)
            ) { idIcon, idText ->
                if (icon != null) Icon(icon = icon, modifier = Modifier.idIcon())
                else Box(modifier = Modifier.idIcon())
                SimpleEllipsisText(text = text ?: "", modifier = Modifier.idText())
            }
        }
    }
}

@Composable
fun Menus(
    visible: Boolean,
    onClose: () -> Unit,
    menus: @Composable MenuScope.() -> Unit,
    maxFlyoutHeight: Dp = Theme.size.cell1 * 2,
    content: @Composable () -> Unit
) {
    Flyout(
        visible = visible,
        onClickOutside = onClose,
        position = FlyoutPosition.Bottom,
        flyout = {
            Column(modifier = Modifier
                .width(IntrinsicSize.Min)
                .heightIn(max = maxFlyoutHeight)
                .clip(Theme.shape.v7)
                .background(Theme.color.surface)
                .border(Theme.border.v7, Theme.color.outline, Theme.shape.v7)
                .verticalScroll(rememberScrollState()),
            ) {
                SurfaceContainer {
                    MenuScope(onClose).menus()
                }
            }
        },
        content = content
    )
}