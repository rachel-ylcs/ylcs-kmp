package love.yinlin.screen.world.single.rhyme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.common.Paths
import love.yinlin.compose.*
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.data.mod.ModResourceType

@Composable
internal fun RhymeButton(
    icon: ImageVector,
    transparent: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val acrylicColor = Colors(0x99f2f2f2)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    acrylicColor.copy(alpha = 0.6f),
                                    acrylicColor.copy(alpha = 0.3f)
                                )
                            ),
                            blendMode = BlendMode.Overlay
                        )
                    }
                }
                .border(
                    width = CustomTheme.border.small,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Colors.White.copy(alpha = 0.5f),
                            Colors.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
                .background(acrylicColor.copy(alpha = if (transparent) 0.2f else 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.padding(CustomTheme.padding.innerIconSpace * 2f).size(CustomTheme.size.icon * 1.5f),
                imageVector = icon,
                contentDescription = null,
                tint = Colors.Ghost,
            )
        }
    }
}

@Composable
internal fun RhymeOverlayLayout(
    title: String,
    onBack: () -> Unit,
    action: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue),
            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RhymeButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    transparent = false,
                    onClick = onBack
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Colors.White
                )
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
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .shadow(CustomTheme.shadow.surface, MaterialTheme.shapes.extraLarge)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable(onClick = onClick)
                .background(Colors.Gray8),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LocalFileImage(
                path = { entry.musicInfo.path(Paths.modPath, ModResourceType.Record) },
                entry,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
            Text(
                text = entry.musicInfo.name,
                color = if (entry.enabled) Colors.Steel4 else Colors.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.extraValue)
            )
        }
    }
}