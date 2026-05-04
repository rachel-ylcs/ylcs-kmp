package love.yinlin.compose.game.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.game.data.RhymeIllustration
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.node.BlurState
import love.yinlin.compose.ui.node.acrylicTarget
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo

@Composable
internal fun RhymeCommonButton(
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
internal fun RhymeMusicCard(
    info: MusicInfo,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shadowElevation = Theme.shadow.v5,
        tonalLevel = 1,
        shape = Theme.shape.v7
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LocalFileImage(
                uri = info.path(app.modPath, ModResourceType.Record).path,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(Theme.size.image7)
            )
            Column(
                modifier = Modifier.weight(1f).padding(Theme.padding.value),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
            ) {
                SimpleEllipsisText(text = info.name, style = Theme.typography.v6.bold)
                Box(modifier = Modifier.fillMaxWidth()) { content() }
            }
        }
    }
}

@Composable
internal fun RhymeIllustrationLayout(
    illustration: RhymeIllustration,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shadowElevation = Theme.shadow.v5,
        tonalLevel = 1,
        shape = Theme.shape.v5,
        onClick = onClick
    ) {
        val info = illustration.info

        WebImage(
            uri = illustration.url,
            key = info.id,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Colors.Dark.copy(alpha = 0.8f))
                .padding(Theme.padding.value)
                .align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            val unlocked = illustration.unlocked

            SimpleEllipsisText(text = info.title, color = Colors.White, style = Theme.typography.v7.bold)
            SimpleEllipsisText(
                text = if (unlocked) "已解锁" else "银币: ${info.cost}",
                color = if (unlocked) Colors.Green4 else Colors.Purple4
            )
        }
    }
}

@Composable
internal fun RhymeIllustrationSelector(
    illustration: RhymeIllustration,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shadowElevation = Theme.shadow.v5,
        tonalLevel = 1,
        shape = Theme.shape.v5,
        border = if (checked) BorderStroke(Theme.border.v4, Theme.color.primary) else null,
        onClick = onClick
    ) {
        val info = illustration.info

        WebImage(
            uri = illustration.url,
            key = info.id,
            modifier = Modifier.fillMaxSize()
        )
        SimpleEllipsisText(
            text = info.title,
            color = Colors.White,
            style = Theme.typography.v8.bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().background(Colors.Dark.copy(alpha = 0.8f)).padding(Theme.padding.value).align(Alignment.BottomCenter)
        )
    }
}

@Composable
internal fun RhymeBlurSurface(
    blurState: BlurState,
    shape: Shape = Theme.shape.rectangle,
    border: Dp = Theme.border.v7,
    contentAlignment: Alignment = Alignment.Center,
    contentPadding: PaddingValues = PaddingValues.Zero,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .border(border, Theme.color.outline, shape)
            .acrylicTarget(state = blurState)
            .padding(contentPadding),
        contentAlignment = contentAlignment
    ) {
        content()
    }
}