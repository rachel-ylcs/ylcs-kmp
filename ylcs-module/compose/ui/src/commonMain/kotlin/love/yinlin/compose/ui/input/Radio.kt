package love.yinlin.compose.ui.input

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.layout.MeasurePolicies
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.Text

@Stable
class RadioGroup(initIndex: Int? = null) {
    var index: Int? by mutableStateOf(initIndex)
}

@Composable
fun rememberRadioGroup(initIndex: Int? = null): RadioGroup = remember(initIndex) { RadioGroup(initIndex) }

@Composable
fun Radio(
    group: RadioGroup,
    index: Int,
    text: String,
    enabled: Boolean = true,
    color: Color = Theme.color.primaryContainer,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.pointerIcon(PointerIcon.Hand, enabled = enabled),
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val checked = index == group.index
        val radius = Theme.size.input10 / 2
        val dotRadius by animateDpAsState(
            targetValue = if (checked) radius * 0.75f else 0.dp,
            animationSpec = spring(dampingRatio = 0.9f, stiffness = 1400.0f)
        )
        val backgroundColor = if (enabled) Theme.color.backgroundVariant else Theme.color.disabledContainer
        val contentColor = if (enabled) color else Theme.color.disabledContent
        val borderColor = Theme.color.outline

        val onClick = { group.index = if (index == group.index) null else index }

        Layout(modifier = Modifier.size(radius * 2).clip(Theme.shape.circle).drawBehind {
            val radiusPx = radius.toPx()
            drawCircle(backgroundColor, radiusPx, center, style = Fill)
            drawCircle(contentColor, dotRadius.toPx(), center, style = Fill)
            drawCircle(borderColor, radiusPx, center, style = Stroke(radiusPx / 8))
        }.selectable(
            selected = checked,
            onClick = onClick,
            enabled = enabled,
            role = Role.RadioButton
        ), measurePolicy = MeasurePolicies.Space)

        Text(text = text, modifier = Modifier.silentClick(enabled = enabled, onClick = onClick))
    }
}