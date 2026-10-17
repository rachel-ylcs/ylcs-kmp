package love.yinlin.compose.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberDpState
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberTrue
import love.yinlin.compose.ui.floating.Flyout
import love.yinlin.compose.ui.floating.FlyoutPosition
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.text.SimpleEllipsisText

@Stable
open class DataTableSelectionCell(options: List<String>, initialIndex: Int = 0) : DataTableCell(), DataTableSortableCell {
    val options: List<String> = options.toList()

    init {
        require(this.options.isNotEmpty() && this.options.distinct().size == this.options.size) { "DataTable selection options must be non-empty and unique" }
        require(initialIndex in this.options.indices) { "DataTable selection index is out of bounds" }
    }

    private val optionOrder = this.options.withIndex().associate { [index, option] -> option to index }
    private var value by mutableIntStateOf(initialIndex)

    override val text: String get() = options[value]
    override val sortType = DataTableSelectionCell::class

    override fun update(value: String): Boolean {
        this.value = optionOrder[value] ?: return false
        return true
    }

    override fun compareTo(other: DataTableSortableCell): Int = value.compareTo((other as DataTableSelectionCell).value)

    override fun captureValue(): () -> Unit {
        val initial = value
        return { value = initial }
    }

    @Composable
    override fun Display(modifier: Modifier) {
        Row(
            modifier = modifier.padding(horizontal = Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText(text, modifier = Modifier.weight(1f), style = Theme.typography.v8)
            Icon(icon = Icons.KeyboardArrowDown, color = Theme.color.onSurfaceVariant, modifier = Modifier.size(Theme.size.smallIcon))
        }
    }

    @Composable
    override fun Editor(state: DataTableEditState, modifier: Modifier) {
        val options = options
        val focus = LocalFocusManager.current
        val density = LocalDensity.current
        val requester = remember(state) { FocusRequester() }
        val listState = rememberLazyListState(value)
        var hadFocus by rememberFalse(state)
        var menuWidth by rememberDpState(state) { 0.dp }
        var expanded by rememberTrue(state)

        Flyout(
            visible = expanded && menuWidth > 0.dp,
            onClickOutside = {
                expanded = false
                state.commit()
            },
            position = FlyoutPosition.Bottom,
            focusable = false,
            space = 0.dp,
            flyout = {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .width(menuWidth)
                        .heightIn(max = Theme.size.cell2)
                        .clip(Theme.shape.v7)
                        .background(Theme.color.surface)
                        .border(Theme.border.v7, Theme.color.outline, Theme.shape.v7)
                ) {
                    itemsIndexed(items = options, key = { _, option -> option }) { index, option ->
                        SimpleEllipsisText(
                            option,
                            color = if (index == value) Theme.color.primary else Theme.color.onSurface,
                            style = Theme.typography.v8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index == value) Theme.color.primary.copy(alpha = 0.12f) else Colors.Transparent)
                                .clickable {
                                    state.updateText(option)
                                    expanded = false; state.commit()
                                    focus.clearFocus()
                                }.padding(Theme.padding.value10)
                        )
                    }
                }
            }
        ) {
            Row(
                modifier = modifier.onSizeChanged {
                    menuWidth = with(density) { it.width.toDp() }
                }.focusRequester(requester).onFocusChanged {
                    if (it.isFocused) hadFocus = true
                    else if (hadFocus) { expanded = false; state.commit() }
                }.onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) false else when (it.key) {
                        Key.Escape -> {
                            state.cancel()
                            focus.clearFocus()
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            state.commit()
                            focus.clearFocus();
                            true
                        }
                        Key.DirectionDown, Key.DirectionUp -> {
                            val delta = if (it.key == Key.DirectionDown) 1 else -1
                            state.updateText(options[(value + delta).coerceIn(options.indices)])
                            true
                        }
                        else -> false
                    }
                }.clickable { expanded = !expanded }.padding(horizontal = Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleEllipsisText(text = state.text, modifier = Modifier.weight(1f), style = Theme.typography.v8)
                Icon(icon = Icons.KeyboardArrowDown, color = Theme.color.primary, modifier = Modifier.size(Theme.size.smallIcon))
            }
        }

        LaunchedEffect(state) { requester.requestFocus() }
        LaunchedEffect(value) { listState.scrollToItem(value) }
    }
}
