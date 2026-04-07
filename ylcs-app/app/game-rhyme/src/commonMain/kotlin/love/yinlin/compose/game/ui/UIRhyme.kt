package love.yinlin.compose.game.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo

private val AcrylicColor = Colors(0x99f2f2f2)
private val AcrylicBrush by lazy { Brush.verticalGradient(colors = listOf(AcrylicColor.copy(alpha = 0.6f), AcrylicColor.copy(alpha = 0.3f))) }
private val AcrylicBorderBrush by lazy { Brush.verticalGradient(colors = listOf(Colors.White.copy(alpha = 0.5f), Colors.Transparent)) }

@Composable
fun RhymeCommonButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = Theme.shape.v5,
        shadowElevation = Theme.shadow.v5,
        tonalLevel = 1,
        contentPadding = Theme.padding.value,
        onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon = icon)
            SimpleClipText(text = text)
        }
    }
}

@Composable
fun RhymeMusicCard(
    info: MusicInfo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shadowElevation = Theme.shadow.v5,
        tonalLevel = 1,
        shape = Theme.shape.v7,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
        ) {
            LocalFileImage(
                uri = info.path(app.modPath, ModResourceType.Record).path,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(Theme.size.image7)
            )
            Column(modifier = Modifier.weight(1f).padding(Theme.padding.value)) {
                SimpleEllipsisText(text = info.name, style = Theme.typography.v6.bold)
            }
        }
    }
}

@Composable
internal fun RhymeAcrylicSurface(
    shape: Shape = Theme.shape.rectangle,
    border: Dp = Theme.border.v7,
    contentAlignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues.Zero,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .drawWithContent {
                drawRect(AcrylicColor.copy(alpha = 0.2f))
                drawContent()
                drawRect(brush = AcrylicBrush, blendMode = BlendMode.Overlay)
            }.border(border, AcrylicBorderBrush, shape)
            .condition(onClick != null) { clickable(onClick = onClick) }
            .padding(contentPadding),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}