package love.yinlin.compose.ui.input

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import love.yinlin.compose.LocalAnimationSpeed
import love.yinlin.compose.ui.CustomTheme
import love.yinlin.compose.ui.node.clickableNoRipple

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {},
    enabled: Boolean = true,
    duration: Int = LocalAnimationSpeed.current / 2
) {
    val width = CustomTheme.size.smallInput
    Box(modifier = Modifier
        .size(width, width / 2)
        .clip(RoundedCornerShape(50))
        .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        .alpha(if (enabled) 1f else 0.7f)
        .clickableNoRipple(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        val offsetX by animateDpAsState(
            targetValue = if (checked) width / 2 else width / 24,
            animationSpec = tween(durationMillis = duration),
        )
        Box(modifier = Modifier.offset(x = offsetX)
            .size(width / 2 - width / 12)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.background)
        )
    }
}