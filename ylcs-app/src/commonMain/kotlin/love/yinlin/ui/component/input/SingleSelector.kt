package love.yinlin.ui.component.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.common.ThemeValue
import love.yinlin.ui.component.image.MiniIcon

@Stable
class SingleSelectorScope<T>(
    private val current: T,
    private val onSelected: (T) -> Unit,
    private val textStyle: TextStyle,
    private val hasIcon: Boolean
) {
    @Composable
    fun Item(item: T, title: String, enabled: Boolean = true) {
        FilterChip(
            selected = item == current,
            onClick = { if (current != item) onSelected(item) },
            enabled = enabled,
            label = {
                Text(
                    text = title,
                    style = textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = if (hasIcon && item == current) {
                {
                    MiniIcon(
                        icon = Icons.Filled.Done,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else null,
            elevation = FilterChipDefaults.filterChipElevation(hoveredElevation = ThemeValue.Padding.ZeroSpace)
        )
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