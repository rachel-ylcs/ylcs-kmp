package love.yinlin.compose.ui.text

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import love.yinlin.compose.Theme
import love.yinlin.compose.interaction.collectState
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.shadow

@Composable
internal fun DecorationBox(
    state: InputState,
    hint: String?,
    alignment: Alignment.Vertical,
    enabled: Boolean,
    colorProvider: InputStatusColorProvider,
    leading: InputDecoration?,
    trailing: InputDecoration?,
    innerContent: @Composable () -> Unit
) {
    val minWidth = Theme.size.input2
    val shape = Theme.shape.v8

    val interactionState by state.interactionSource.collectState(
        colorProvider.useFocused, colorProvider.useHovered, colorProvider.usePressed, colorProvider.useDragged
    )

    val lightColor by animateColorAsState(
        targetValue = if (enabled) colorProvider.color(state, interactionState) else Theme.color.disabledContent,
        animationSpec = tween(Theme.animation.duration.v7)
    )
    val backgroundColor = if (enabled) Theme.color.backgroundVariant else Theme.color.disabledContainer

    InputHintLayout(state, hint, shape, lightColor) { border, hintAnchor ->
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .defaultMinSize(minWidth = minWidth, minHeight = minWidth * 0.28f)
                .shadow(shape, Theme.shadow.v7)
                .clip(shape)
                .background(backgroundColor)
                .then(border)
                .pointerIcon(PointerIcon.Default)
                .hoverable(state.interactionSource)
                .padding(
                    horizontal = if (leading != null) Theme.padding.h else Theme.padding.h10,
                    vertical = Theme.padding.v10
                ),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
        ) {
            if (leading != null) {
                Box(modifier = Modifier.align(leading.alignment)) {
                    leading.Content(state)
                }
            }

            Box(modifier = Modifier.weight(1f).align(alignment).then(hintAnchor).pointerIcon(PointerIcon.Text, enabled = enabled)) {
                innerContent()
            }

            if (trailing != null) {
                Box(modifier = Modifier.align(trailing.alignment)) {
                    trailing.Content(state)
                }
            }
        }
    }
}
