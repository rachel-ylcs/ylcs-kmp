package love.yinlin.compose.ui.input

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import love.yinlin.compose.LocalStyle
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.ui.floating.Flyout
import love.yinlin.compose.ui.floating.FlyoutPosition
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.measureTextWidth

@Suppress("AssignedValueIsNeverRead")
@Composable
fun ComboBox(
    items: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    index: Int = -1,
    hint: String = "",
    enabled: Boolean = true,
    padding: PaddingValues = Theme.padding.value,
    style: TextStyle = LocalStyle.current,
    shape: Shape = Theme.shape.v7,
    border: Dp = Theme.border.v7,
    maxFlyoutHeight: Dp = Theme.size.cell1,
) {
    var isOpen by rememberFalse()
    var layoutWidth by rememberState { 0.dp }

    Flyout(
        visible = isOpen,
        onClickOutside = { isOpen = false },
        position = FlyoutPosition.Bottom,
        flyout = {
            LazyColumn(
                modifier = Modifier
                    .width(layoutWidth)
                    .heightIn(max = maxFlyoutHeight)
                    .clip(shape)
                    .background(Theme.color.surface)
                    .border(border, Theme.color.outline, shape),
                state = rememberLazyListState(initialFirstVisibleItemIndex = index.coerceAtLeast(0))
            ) {
                itemsIndexed(items) { i, title ->
                    val isCurrent = i == index
                    Text(
                        text = title,
                        color = if (isCurrent) Theme.color.primary else Theme.color.onSurface,
                        style = style,
                        modifier = Modifier.fillMaxWidth().clickable {
                            onSelect(i)
                            isOpen = false
                        }.condition(isCurrent) {
                            border(border, Theme.color.primary)
                        }.padding(Theme.padding.value)
                    )
                }
            }
        }
    ) {
        val density = LocalDensity.current

        Row(
            modifier = modifier
                .onSizeChanged { layoutWidth = with(density) { it.width.toDp() } }
                .height(IntrinsicSize.Min)
                .clip(shape)
                .semantics(Role.DropdownList)
                .background(if (enabled) Theme.color.backgroundVariant else Theme.color.disabledContainer)
                .border(border, Theme.color.outline, shape)
                .clickable(enabled = enabled) { isOpen = true }
                .padding(padding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val maxTextWidth = measureTextWidth(items, style, hint)
            val title = items.getOrNull(index)
            val contentColor = when {
                !enabled -> Theme.color.disabledContent
                title == null -> Theme.color.onBackgroundVariant.copy(alpha = 0.5f)
                else -> Theme.color.onBackground
            }

            SimpleClipText(
                text = title ?: hint,
                color = contentColor,
                style = style,
                modifier = Modifier.padding(end = Theme.padding.h / 2).width(maxTextWidth)
            )

            val angle by animateFloatAsState(
                targetValue = if (isOpen) 180f else 0f,
                animationSpec = tween(Theme.animation.duration.default)
            )

            Icon(icon = Icons.KeyboardArrowDown, color = contentColor, modifier = Modifier.rotate(angle))
        }
    }
}