package love.yinlin.compose.interaction

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import love.yinlin.compose.extension.rememberState

@Stable
data class InteractionState(
    val isFocused: Boolean = false,
    val isHovered: Boolean = false,
    val isPressed: Boolean = false,
    val isDragged: Boolean = false,
)

@Composable
fun InteractionSource.collectState(
    useFocused: Boolean = true,
    useHovered: Boolean = true,
    usePressed: Boolean = true,
    useDragged: Boolean = true,
): State<InteractionState> {
    val state = rememberState { InteractionState() }

    LaunchedEffect(useFocused, useHovered, usePressed, useDragged) {
        val focusInteractions = mutableListOf<FocusInteraction.Focus>()
        val hoverInteractions = mutableListOf<HoverInteraction.Enter>()
        val pressInteractions = mutableListOf<PressInteraction.Press>()
        val dragInteractions = mutableListOf<DragInteraction.Start>()

        interactions.collect { interaction ->
            when (interaction) {
                is FocusInteraction if useFocused -> {
                    when (interaction) {
                        is FocusInteraction.Focus -> focusInteractions.add(interaction)
                        is FocusInteraction.Unfocus -> focusInteractions.remove(interaction.focus)
                    }
                    state.value = state.value.copy(isFocused = focusInteractions.isNotEmpty())
                }
                is HoverInteraction if useHovered -> {
                    when (interaction) {
                        is HoverInteraction.Enter -> hoverInteractions.add(interaction)
                        is HoverInteraction.Exit -> hoverInteractions.remove(interaction.enter)
                    }
                    state.value = state.value.copy(isHovered = hoverInteractions.isNotEmpty())
                }
                is PressInteraction if usePressed -> {
                    when (interaction) {
                        is PressInteraction.Press -> pressInteractions.add(interaction)
                        is PressInteraction.Release -> pressInteractions.remove(interaction.press)
                        is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
                    }
                    state.value = state.value.copy(isPressed = pressInteractions.isNotEmpty())
                }
                is DragInteraction if useDragged -> {
                    when (interaction) {
                        is DragInteraction.Start -> dragInteractions.add(interaction)
                        is DragInteraction.Stop -> dragInteractions.remove(interaction.start)
                        is DragInteraction.Cancel -> dragInteractions.remove(interaction.start)
                    }
                    state.value = state.value.copy(isDragged = dragInteractions.isNotEmpty())
                }
            }
        }
    }

    return state
}