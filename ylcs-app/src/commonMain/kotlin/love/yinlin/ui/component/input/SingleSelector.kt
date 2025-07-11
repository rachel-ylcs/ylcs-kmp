package love.yinlin.ui.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import love.yinlin.common.Colors
import love.yinlin.common.ThemeValue

@Stable
class SingleSelectorScope<T>(
    private val current: T,
    private val onSelected: (T) -> Unit,
    private val textStyle: TextStyle,
    private val hasIcon: Boolean
) {
    @Composable
    fun Item(item: T, title: String, enabled: Boolean = true) {
        val selected = current == item
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(enabled = enabled) { if (!selected) onSelected(item) }
                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Colors.Transparent)
                .border(width = ThemeValue.Border.Small, color = MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                .padding(ThemeValue.Padding.Value),
            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace / 2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasIcon && item == current) {
                Icon(
                    imageVector = Icons.Filled.Done,
                    tint = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(ThemeValue.Size.MicroIcon * 0.8f),
                    contentDescription = null
                )
            }
            Text(
                text = title,
                color = if (!enabled) MaterialTheme.colorScheme.surfaceVariant
                    else if (item == current) MaterialTheme.colorScheme.onSecondaryContainer
                    else Colors.Unspecified,
                style = textStyle,
                maxLines = 1,
                overflow = Clip
            )
        }
    }
}

@Composable
fun <T> SingleSelector(
    current: T,
    onSelected: (T) -> Unit,
    style: TextStyle = LocalTextStyle.current,
    hasIcon: Boolean = true,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace),
    maxLines: Int = Int.MAX_VALUE,
    modifier: Modifier = Modifier,
    content: @Composable SingleSelectorScope<T>.() -> Unit
) {
    val scope = remember(current, onSelected, style, hasIcon) {
        SingleSelectorScope(current, onSelected, style, hasIcon)
    }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        itemVerticalAlignment = Alignment.CenterVertically,
        maxLines = maxLines
    ) {
        scope.content()
    }
}