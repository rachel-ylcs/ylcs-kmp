package love.yinlin.compose.ui.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.launch
import love.yinlin.compose.Colors
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.rememberFalse
import love.yinlin.compose.ui.floating.BallonTip
import love.yinlin.compose.ui.node.condition
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MiniIcon(
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = CustomTheme.size.icon,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.padding(CustomTheme.padding.innerIconSpace).size(size),
            imageVector = icon,
            contentDescription = null,
            tint = color,
        )
    }
}

@Stable
data class ColorfulImageVector(
    val icon: ImageVector,
    val color: Color,
    val background: Color
)

@Composable
fun colorfulImageVector(
    icon: ImageVector,
    color: Color = Colors.Ghost,
    background: Color = Colors.Transparent
) = ColorfulImageVector(icon, color, background)

@Composable
fun ColorfulIcon(
    icon: ColorfulImageVector,
    size: Dp = CustomTheme.size.icon,
    gap: Float = 1.5f,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .condition(onClick != null) { clickable { onClick?.invoke() } }
            .background(icon.background.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.padding(CustomTheme.padding.innerIconSpace * gap).size(size),
            imageVector = icon.icon,
            contentDescription = null,
            tint = icon.color,
        )
    }
}

@Composable
fun ClickIcon(
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = CustomTheme.size.icon,
    indication: Boolean = true,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) = MiniIcon(
    icon = icon,
    color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
    size = size,
    modifier = modifier
        .clip(MaterialTheme.shapes.extraSmall)
        .clickable(
            onClick = onClick,
            indication = if (indication) LocalIndication.current else null,
            interactionSource = remember { MutableInteractionSource() },
            enabled = enabled
        )
)

@Composable
fun ClickIcon(
    icon: ImageVector,
    tip: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = CustomTheme.size.icon,
    indication: Boolean = true,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    BallonTip(text = tip) { ClickIcon(icon, color, size, indication, enabled, modifier, onClick) }
}

@Composable
fun LoadingCircle(
    modifier: Modifier = Modifier,
    size: Dp = CustomTheme.size.icon,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(modifier = modifier) {
        CircularProgressIndicator(
            modifier = Modifier.padding(CustomTheme.padding.innerIconSpace).size(size),
            color = color
        )
    }
}

@Composable
fun StaticLoadingIcon(
    isLoading: Boolean,
    icon: ImageVector,
    size: Dp = CustomTheme.size.icon,
    color: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        LoadingCircle(
            size = size,
            color = color,
            modifier = modifier
        )
    }
    else {
        MiniIcon(
            icon = icon,
            color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
            size = size,
            modifier = modifier
        )
    }
}

@Composable
fun LoadingIcon(
    icon: ImageVector,
    size: Dp = CustomTheme.size.icon,
    color: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by rememberFalse()

    if (isLoading) {
        LoadingCircle(
            size = size,
            color = color,
            modifier = modifier
        )
    }
    else {
        ClickIcon(
            icon = icon,
            color = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant,
            size = size,
            enabled = enabled && !isLoading,
            onClick = {
                scope.launch {
                    isLoading = true
                    onClick()
                    isLoading = false
                }
            },
            modifier = modifier
        )
    }
}

@Composable
fun LoadingIcon(
    icon: ImageVector,
    tip: String,
    size: Dp = CustomTheme.size.icon,
    color: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit
) {
    BallonTip(text = tip) { LoadingIcon(icon, size, color, enabled, modifier, onClick) }
}

@Composable
fun MiniIcon(
    res: DrawableResource,
    size: Dp = CustomTheme.size.icon,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.padding(CustomTheme.padding.innerIconSpace).size(size),
            painter = painterResource(res),
            contentDescription = null
        )
    }
}

@Composable
fun ClickIcon(
    res: DrawableResource,
    size: Dp = CustomTheme.size.icon,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) = MiniIcon(
    res = res,
    size = size,
    modifier = modifier
        .clip(MaterialTheme.shapes.extraSmall)
        .clickable(onClick = onClick)
)

@Composable
fun ClickIcon(
    res: DrawableResource,
    tip: String,
    size: Dp = CustomTheme.size.icon,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    BallonTip(text = tip) { ClickIcon(res, size, modifier, onClick) }
}

@Composable
fun IconText(
    icon: ImageVector,
    text: String,
    size: Dp = CustomTheme.size.extraIcon,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(CustomTheme.padding.equalValue),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = null,
            modifier = Modifier.size(size)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun MiniImage(
    icon: ImageVector,
    size: Dp = CustomTheme.size.icon,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberVectorPainter(icon),
            contentDescription = null,
            modifier = Modifier.padding(CustomTheme.padding.innerIconSpace).size(size)
        )
    }
}

@Composable
fun MiniImage(
    res: DrawableResource,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(res),
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale,
            alignment = alignment,
            alpha = alpha,
            contentDescription = null
        )
    }
}

@Composable
fun MiniImage(
    painter: Painter,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale,
            alignment = alignment,
            alpha = alpha,
            contentDescription = null
        )
    }
}

@Composable
fun ClickImage(
    res: DrawableResource,
    contentScale: ContentScale = ContentScale.Fit,
    alignment: Alignment = Alignment.Center,
    alpha: Float = 1f,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) = MiniImage(
    res = res,
    contentScale = contentScale,
    alignment = alignment,
    alpha = alpha,
    modifier = modifier.clickable(onClick = onClick)
)