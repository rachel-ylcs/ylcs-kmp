package love.yinlin.compose.ui.input

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.layout.MeasurePolicies
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.Text

private val ToggleableState.next: ToggleableState get() = when (this) {
    ToggleableState.On -> ToggleableState.Indeterminate
    ToggleableState.Indeterminate -> ToggleableState.Off
    ToggleableState.Off -> ToggleableState.On
}

@Composable
fun TriStateCheckBox(
    state: ToggleableState,
    onStateChange: (ToggleableState) -> Unit,
    text: String,
    enabled: Boolean = true,
    color: Color = Theme.color.primaryContainer,
    contentColor: Color = Theme.color.onContainer,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.pointerIcon(PointerIcon.Hand, enabled = enabled),
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val backgroundColor = if (enabled) Theme.color.backgroundVariant else Theme.color.disabledContainer
        val iconColor = if (enabled) contentColor else Theme.color.disabledContent
        val checkedPainter = rememberVectorPainter(Icons.Check)
        val indeterminatePainter = rememberVectorPainter(Icons.Remove)

        val alpha by animateFloatAsState(if (state != ToggleableState.Off) 1f else 0f, spring(dampingRatio = 0.9f, stiffness = 1400.0f))

        val onClick = { onStateChange(state.next) }

        val size = Theme.size.input10
        val cornerRadius = size / 5
        val borderColor = Theme.color.outline

        Layout(modifier = Modifier.size(size).clip(RoundedCornerShape(cornerRadius)).drawBehind {
            val boxSize = this.size
            val corner = CornerRadius(cornerRadius.toPx())
            drawRoundRect(backgroundColor, Offset.Zero, boxSize, corner, style = Fill)
            drawRoundRect(borderColor, Offset.Zero, boxSize, corner, style = Stroke(boxSize.width / 16))
            drawRoundRect(if (enabled) color else backgroundColor, Offset.Zero, boxSize, corner, style = Fill, alpha = alpha)
            val painter = if (state == ToggleableState.On) checkedPainter else indeterminatePainter
            painter.run { draw(boxSize, alpha, ColorFilter.tint(iconColor)) }
        }.triStateToggleable(
            state = state,
            onClick = onClick,
            enabled = enabled,
            role = Role.Checkbox
        ), measurePolicy = MeasurePolicies.Space)

        Text(text = text, modifier = Modifier.silentClick(enabled = enabled, onClick = onClick))
    }
}