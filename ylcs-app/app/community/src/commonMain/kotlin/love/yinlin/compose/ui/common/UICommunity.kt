package love.yinlin.compose.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.app
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.cs.url
import love.yinlin.data.rachel.profile.UserPublicProfile
import love.yinlin.extension.DateEx

@Composable
fun BoxText(text: String, color: Color) {
    SimpleEllipsisText(
        text = text,
        style = Theme.typography.v7.bold,
        color = color,
        modifier = Modifier.wrapContentSize().border(Theme.border.v7, color = color).padding(Theme.padding.g9),
    )
}

@Composable
fun PortraitValue(
    value: String,
    title: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.condition(onClick != null) { silentClick(onClick = onClick).pointerIcon(PointerIcon.Hand) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
    ) {
        SimpleClipText(text = value, style = Theme.typography.v6.bold)
        SimpleClipText(text = title)
    }
}

@Composable
fun UserProfileInfoColumn(profile: UserPublicProfile, onLevelClick: (() -> Unit)? = null) {
    val isOwner = app.config.userProfile?.uid == profile.uid

    // 背景墙
    WebImage(
        uri = profile.wallPath.url,
        key = if (isOwner) app.config.cacheUserWall else remember { DateEx.TodayString },
        modifier = Modifier.fillMaxWidth().aspectRatio(1.77777f)
    )
    // 头像和信息
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(Theme.padding.value),
        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
    ) {
        WebImage(
            uri = profile.avatarPath.url,
            key = if (isOwner) app.config.cacheUserAvatar else remember { DateEx.TodayString },
            contentScale = ContentScale.Crop,
            circle = true,
            modifier = Modifier.fillMaxHeight().aspectRatio(1f)
        )
        Column(modifier = Modifier.weight(1f)) {
            SimpleEllipsisText(text = profile.name, style = Theme.typography.v6.bold, modifier = Modifier.fillMaxWidth())
            UserLabel(label = profile.label, level = profile.level, onClick = onLevelClick)
        }
    }
    // 个性签名
    SelectionBox {
        Text(
            text = profile.signature,
            style = Theme.typography.v8,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(Theme.padding.value)
        )
    }
}