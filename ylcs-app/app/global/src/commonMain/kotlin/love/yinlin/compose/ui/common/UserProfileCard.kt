package love.yinlin.compose.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.node.verticalFade
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.cs.url
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.extension.DateEx

private val FadeWallImage = [
    0.0f to 1f,
    0.6f to 0.9f,
    0.7f to 0.8f,
    0.8f to 0.65f,
    0.9f to 0.2f,
    0.95f to 0.05f,
    1.0f to 0f
]

private val FadeWallImageWithTopBar = [
    0.0f to 0f,
    0.05f to 0.4f,
    0.1f to 0.5f,
    0.2f to 0.7f,
    0.3f to 0.8f,
    0.6f to 0.9f,
    0.7f to 0.8f,
    0.8f to 0.65f,
    0.9f to 0.2f,
    0.95f to 0.05f,
    1.0f to 0f
]

@Composable
fun UserProfileCard(
    profile: UserPublicProfile,
    modifier: Modifier = Modifier,
    onAvatarClick: (() -> Unit)? = null,
    onLevelClick: (() -> Unit)? = null,
    topBar: (@Composable () -> Unit)? = null
) {
    val isOwner = app.config.userProfile?.uid == profile.uid

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 背景墙
        WebImage(
            uri = profile.wallPath.url,
            key = if (isOwner) app.config.cacheUserWall else DateEx.TodayLong,
            modifier = Modifier.fillMaxWidth()
                .aspectRatio(1.77777f)
                .verticalFade(if (topBar != null) FadeWallImageWithTopBar else FadeWallImage)
                .zIndex(1f)
        )
        // TopBar
        if (topBar != null) {
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).zIndex(2f)) {
                topBar()
            }
        }

        // 信息区
        Row(
            modifier = Modifier.fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(Theme.padding.value9).align(Alignment.BottomCenter)
                .zIndex(2f),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h9)
        ) {
            WebImage(
                uri = profile.avatarPath.url,
                key = if (isOwner) app.config.cacheUserAvatar else DateEx.TodayLong,
                contentScale = ContentScale.Crop,
                circle = true,
                modifier = Modifier.fillMaxHeight().aspectRatio(1f).border(Theme.border.v5, Colors.White, Theme.shape.circle),
                onClick = onAvatarClick
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.Bottom
                ) {
                    SimpleEllipsisText(text = profile.name, style = Theme.typography.v6.bold, modifier = Modifier.weight(1f, fill = false).padding(bottom = Theme.padding.v))
                    UserLabel(label = profile.label, level = profile.level, onClick = onLevelClick)
                }
                SelectionBox {
                    Text(
                        text = profile.signature,
                        style = Theme.typography.v8,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(bottom = Theme.padding.v)
                    )
                }
            }
        }
    }
}