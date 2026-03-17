package love.yinlin.compose.ui.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.semantics.Role
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.node.fastAnimateOffsetX
import love.yinlin.compose.ui.node.fastClipCircle
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.semantics
import love.yinlin.compose.ui.node.shadow
import love.yinlin.compose.ui.node.silentClick

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val width = Theme.size.input8
    val shape = Theme.shape.circle

    val trackColor = when {
        !enabled -> Theme.color.disabledContent
        checked -> Theme.color.onContainer
        else -> Theme.color.onBackgroundVariant
    }

    val backgroundColor = when {
        !enabled -> Theme.color.disabledContainer
        checked -> Theme.color.primaryContainer
        else -> Theme.color.backgroundVariant
    }

    Box(modifier = Modifier
        .size(width, width / 2)
        .shadow(shape, Theme.shadow.v8)
        .clip(shape)
        .background(backgroundColor)
        .pointerIcon(PointerIcon.Hand, enabled = enabled)
        .semantics(Role.Switch)
        .silentClick(enabled = enabled) { onCheckedChange(!checked) }
        .then(modifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(modifier = Modifier
            .fastAnimateOffsetX(if (checked) width / 2 else width / 24)
            .size(width * 5 / 12)
            .fastClipCircle()
            .background(trackColor)
        )
    }
}