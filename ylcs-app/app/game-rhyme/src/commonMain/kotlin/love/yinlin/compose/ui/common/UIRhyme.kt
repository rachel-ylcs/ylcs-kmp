package love.yinlin.compose.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import love.yinlin.common.PathMod
import love.yinlin.common.rhyme.RhymeMusic
import love.yinlin.compose.*
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.data.mod.ModResourceType

private val AcrylicColor = Colors(0x99f2f2f2)
private val AcrylicBrush by lazy { Brush.verticalGradient(colors = listOf(AcrylicColor.copy(alpha = 0.6f), AcrylicColor.copy(alpha = 0.3f))) }
private val AcrylicBorderBrush by lazy { Brush.verticalGradient(colors = listOf(Colors.White.copy(alpha = 0.5f), Colors.Transparent)) }

internal val ResumeNumberBrush by lazy { Brush.linearGradient(listOf(Colors.Steel4, Colors.Blue4, Colors.Purple4)) }

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
                drawContent()
                drawRect(brush = AcrylicBrush, blendMode = BlendMode.Overlay)
            }.border(border, AcrylicBorderBrush, shape)
            .background(AcrylicColor.copy(alpha = 0.2f))
            .condition(onClick != null) { clickable(onClick = onClick) }
            .padding(contentPadding),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}

@Composable
internal fun RhymeAcrylicButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    RhymeAcrylicSurface(
        modifier = modifier,
        shape = Theme.shape.circle,
        contentPadding = Theme.padding.eValue,
        onClick = onClick
    ) {
        Icon(icon = icon, color = Colors.Ghost, modifier = Modifier.size(Theme.size.image9))
    }
}

@Composable
internal fun RhymeButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = Theme.shape.circle,
        contentPadding = Theme.padding.eValue,
        border = BorderStroke(Theme.border.v10, Theme.color.primary),
        shadowElevation = Theme.shadow.v3,
        onClick = onClick
    ) {
        Icon(icon = icon, modifier = Modifier.size(Theme.size.image9))
    }
}

@Composable
internal fun RhymeOverlayLayout(
    title: String,
    onBack: () -> Unit,
    action: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RhymeButton(icon = Icons.ArrowBack, onClick = onBack)
                Text(text = title, style = Theme.typography.v6.bold)
            }
            Box(contentAlignment = Alignment.CenterEnd) { action() }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            content()
        }
    }
}

@Composable
internal fun RhymeMusicCard(
    entry: RhymeMusic,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = Theme.shape.v5,
        shadowElevation = Theme.shadow.v3,
        onClick = onClick
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LocalFileImage(
                uri = entry.musicInfo.path(PathMod, ModResourceType.Record).toString(),
                entry,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
            SimpleEllipsisText(
                text = entry.musicInfo.name,
                color = if (entry.enabled) Colors.Steel4 else LocalColor.current,
                textAlign = TextAlign.Center,
                style = Theme.typography.v6.bold,
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9)
            )
        }
    }
}