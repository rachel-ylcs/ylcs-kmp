package love.yinlin.ui.screen.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import io.github.alexzhirkevich.qrose.options.*
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.*
import love.yinlin.common.uri.UriGenerator
import love.yinlin.compose.*
import love.yinlin.data.Data
import love.yinlin.data.rachel.song.Song
import love.yinlin.data.rachel.song.SongComment
import love.yinlin.extension.DateEx
import love.yinlin.platform.ImageQuality
import love.yinlin.platform.OS
import love.yinlin.platform.Platform
import love.yinlin.platform.app
import love.yinlin.ui.component.image.*
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.layout.Pagination
import love.yinlin.ui.component.layout.PaginationColumn
import love.yinlin.ui.component.screen.FloatingSheet
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.community.UserBar

@Stable
private enum class ModQQGroup(
    val id: String,
    val k: String,
    val authKey: String
) {
    Album("1048965901", "k6XfoZ7qC9uWUfdKufTSgqQokpsg6m7U", "KtUvt3KL%2FSavxwgbXhU562BiUvZiCLPEX4Vbx4O7vicWQ7wC9Nq9UN4hbMAUKOJ%2F"),
    Single("836289670", "GMg4nlFsnNslW_ZVZE7I1XcKy9d0_CA6", "Aanfl0VufijRqTaoVFOfizQMjJwLZSYSND9jXpXvIsR1p0qa97Lcu8GpOgjaVuDR"),
    Video("971218639", "Hiv2kwEpxJeEYddfVC0IzmMonqGxSWev", "Q47DA4cNASFrinbcT3%2BMZXU6G%2FS%2Bi03fUy4lz2KDHuhSMX6nLJHYwV1m%2B%2BWWzjvo"),
    Game("942459444", "XvTbFryfRqO1h5L9FN9VvxYmmpsEROhr", "ah3tB5Ef9Ki4cqhTmBDa2MR%2FnvEIvWT4ZBaj%2FuKSHqt6YohsaQ%2Bf1qo%2FoeeIU2qi")
}

@Stable
class ScreenSongDetails(model: AppModel, val args: Args) : SubScreen<ScreenSongDetails.Args>(model) {
    @Stable
    @Serializable
    data class Args(val song: Song)

    private val pageComments = object : Pagination<SongComment, Long, Long>(0L) {
        override fun distinctValue(item: SongComment): Long = item.cid
        override fun offset(item: SongComment): Long = item.cid
    }

    private val listState = LazyListState()

    private val inputState = TextInputState()

    private suspend fun requestNewComments() {
        val result = ClientAPI.request(
            route = API.User.Song.GetSongComments,
            data = API.User.Song.GetSongComments.Request(
                sid = args.song.sid,
                num = pageComments.pageNum
            )
        )
        when (result) {
            is Data.Success -> pageComments.newData(result.data)
            is Data.Failure -> slot.tip.error(result.message)
        }
    }

    private suspend fun requestMoreComments() {
        val result = ClientAPI.request(
            route = API.User.Song.GetSongComments,
            data = API.User.Song.GetSongComments.Request(
                sid = args.song.sid,
                cid = pageComments.offset,
                num = pageComments.pageNum
            )
        )
        if (result is Data.Success) pageComments.moreData(result.data)
    }

    private suspend fun onSendComment(content: String): Boolean {
        app.config.userProfile?.let { user ->
            val result = ClientAPI.request(
                route = API.User.Song.SendSongComment,
                data = API.User.Song.SendSongComment.Request(
                    token = app.config.userToken,
                    sid = args.song.sid,
                    content = content
                )
            )
            when (result) {
                is Data.Success -> {
                    pageComments.items += SongComment(
                        cid = result.data,
                        uid = user.uid,
                        ts = DateEx.CurrentString,
                        content = content,
                        name = user.name,
                        label = user.label,
                        exp = user.exp
                    )
                    listState.animateScrollToItem(pageComments.items.size - 1)
                    return true
                }
                is Data.Failure -> slot.tip.error(result.message)
            }
        }
        return false
    }

    private suspend fun onDeleteComment(cid: Long) {
        if (slot.confirm.openSuspend(content = "删除评论")) {
            val result = ClientAPI.request(
                route = API.User.Song.DeleteSongComment,
                data = API.User.Song.DeleteSongComment.Request(
                    token = app.config.userToken,
                    cid = cid
                )
            )
            when (result) {
                is Data.Success -> pageComments.items.removeAll { it.cid == cid }
                is Data.Failure -> slot.tip.error(result.message)
            }
        }
    }

    private fun onAvatarClick(uid: Int) {
        discoveryPart.onUserAvatarClick(uid)
    }

    @Composable
    private fun SongLayout(
        song: Song,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier,
            shadowElevation = CustomTheme.shadow.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(5f),
                        verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                    ) {
                        RachelText(
                            text = song.name,
                            style = MaterialTheme.typography.labelLarge,
                            icon = Icons.Outlined.MusicNote,
                            color = MaterialTheme.colorScheme.primary
                        )
                        RachelText(
                            text = "编号: ${song.sid}",
                            icon = Icons.Outlined.Loyalty
                        )
                        RachelText(
                            text = "ID: ${song.id}",
                            icon = Icons.Outlined.Badge
                        )
                        RachelText(
                            text = "版本: ${song.version}",
                            icon = ExtraIcons.Artist
                        )
                    }
                    WebImage(
                        uri = remember(song) { song.recordPath },
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(3f).aspectRatio(1f).clip(MaterialTheme.shapes.large)
                    )
                }
                RachelText(
                    text = "演唱: ${song.singer}",
                    icon = ExtraIcons.Artist
                )
                RachelText(
                    text = "作词: ${song.lyricist}",
                    icon = Icons.Outlined.Lyrics
                )
                RachelText(
                    text = "作曲: ${song.composer}",
                    icon = Icons.Outlined.Lyrics
                )
                RachelText(
                    text = "专辑: ${song.album}",
                    icon = Icons.Outlined.Album
                )
                RachelText(
                    text = "动画: ${if (song.bgd) "已收录" else "未收录"}",
                    color = if (song.bgd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Outlined.GifBox
                )
                RachelText(
                    text = "视频: ${if (song.video) "已收录" else "未收录"}",
                    color = if (song.video) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    icon = Icons.Outlined.MusicVideo
                )
            }
        }
    }

    @Composable
    private fun SongCommentLayout(
        comment: SongComment,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            UserBar(
                avatar = comment.avatarPath,
                name = comment.name,
                time = comment.ts,
                label = comment.label,
                level = comment.level,
                onAvatarClick = { onAvatarClick(comment.uid) }
            )
            SelectionContainer {
                Text(
                    text = comment.content,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = CustomTheme.padding.verticalSpace),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                app.config.userProfile?.let { user ->
                    if (user.uid == comment.uid || user.hasPrivilegeVIPTopic) {
                        Text(
                            text = "删除",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.clickable {
                                launch { onDeleteComment(comment.cid) }
                            }.padding(CustomTheme.padding.littleValue)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BottomLayout(modifier: Modifier = Modifier) {
        TextInput(
            state = inputState,
            hint = "评论",
            maxLength = 256,
            maxLines = 5,
            minLines = 1,
            trailingIcon = {
                LoadingIcon(
                    icon = Icons.AutoMirrored.Filled.Send,
                    enabled = inputState.ok,
                    onClick = {
                        if (onSendComment(inputState.text)) inputState.text = ""
                    }
                )
            },
            onImeClick = {
                if (inputState.ok) launch {
                    if (onSendComment(inputState.text)) inputState.text = ""
                }
            },
            modifier = modifier
        )
    }

    @Composable
    private fun Portrait(song: Song) {
        PaginationColumn(
            items = pageComments.items,
            key = { it.cid },
            state = listState,
            canRefresh = false,
            canLoading = pageComments.canLoading,
            onLoading = { requestMoreComments() },
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            header = {
                SongLayout(
                    song = song,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(modifier = Modifier.padding(bottom = CustomTheme.padding.verticalSpace))
            },
            itemDivider = PaddingValues(vertical = CustomTheme.padding.verticalSpace)
        ) {
            SongCommentLayout(
                comment = it,
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalSpace)
            )
        }
    }

    @Composable
    private fun Landscape(song: Song) {
        Row(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            SongLayout(
                song = song,
                modifier = Modifier
                    .padding(immersivePadding.withoutEnd)
                    .width(CustomTheme.size.panelWidth)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            )
            VerticalDivider()

            PaginationColumn(
                items = pageComments.items,
                key = { it.cid },
                state = listState,
                canRefresh = false,
                canLoading = pageComments.canLoading,
                onLoading = { requestMoreComments() },
                itemDivider = PaddingValues(vertical = CustomTheme.padding.verticalSpace),
                modifier = Modifier
                    .padding(immersivePadding.withoutStart)
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(CustomTheme.padding.value)
            ) {
                SongCommentLayout(
                    comment = it,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    override val title: String = args.song.name

    override suspend fun initialize() {
        requestNewComments()
    }

    @Composable
    override fun ActionScope.RightActions() {
        Action(Icons.Outlined.Share, "分享") {
            shareSheet.open()
        }
        Action(Icons.Outlined.Download, "下载") {
            val group = when (args.song.album) {
                "腐草为萤", "蚍蜉渡海", "琉璃", "山色有无中", "风花雪月", "离地十公分·A面", "离地十公分·B面", "银临" -> ModQQGroup.Album
                "单曲集" -> ModQQGroup.Single
                "影视剧OST" -> ModQQGroup.Video
                "游戏OST" -> ModQQGroup.Game
                else -> null
            }
            launch {
                if (group == null) slot.tip.warning("未找到此歌曲的下载源")
                else {
                    val result = Platform.use(*Platform.Phone,
                        ifTrue = { OS.Application.startAppIntent(UriGenerator.qqGroup(group.id)) },
                        ifFalse = { OS.Application.startAppIntent(UriGenerator.qqGroup(group.k, group.authKey)) }
                    )
                    if (!result) slot.tip.warning("未安装QQ")
                }
            }
        }
    }

    @Composable
    override fun BottomBar() {
        if (app.config.userProfile != null) {
            BottomLayout(modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxWidth()
                .padding(CustomTheme.padding.equalValue))
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        when (device.type) {
            Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait(song = args.song)
            Device.Type.LANDSCAPE -> Landscape(song = args.song)
        }
    }

    private val shareSheet = object : FloatingSheet() {
        override val maxHeightRatio: Float = 1f
        override val initFullScreen: Boolean = true

        @Composable
        override fun Content() {
            val gradientColors = remember {
                listOf(Colors.Steel3, Colors.Steel4, Colors.Steel5, Colors.Steel6, Colors.Steel7, Colors.Steel8)
            }

            Box(modifier = Modifier.padding(
                horizontal = CustomTheme.padding.horizontalExtraSpace * 2,
                vertical = CustomTheme.padding.verticalExtraSpace * 2
            ).fillMaxSize().shadow(
                elevation = CustomTheme.shadow.surface * 2,
                shape = MaterialTheme.shapes.extraLarge,
                ambientColor = Colors.Steel5,
                spotColor = Colors.Steel6
            ).background(
                brush = Brush.linearGradient(colors = gradientColors),
                shape = MaterialTheme.shapes.extraLarge
            )) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace, Alignment.Bottom)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(CustomTheme.padding.extraValue),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ColorfulIcon(
                            icon = colorfulImageVector(
                                icon = Icons.AutoMirrored.Outlined.Send,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                background = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            gap = 2f,
                            onClick = {

                            }
                        )
                    }
                    Text(
                        text = args.song.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = Colors.Ghost,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
                    )
                    Text(
                        text = args.song.singer,
                        style = MaterialTheme.typography.titleLarge,
                        color = Colors.Ghost,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalExtraSpace)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        WebImage(
                            uri = args.song.recordPath,
                            key = Local.VERSION,
                            quality = ImageQuality.Full,
                            modifier = Modifier.fillMaxWidth(fraction = 0.65f)
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.extraLarge.copy(topStart = CornerSize(0), bottomEnd = CornerSize(0)))
                        )
                        Box(modifier = Modifier.background(Colors.Ghost).weight(1f).aspectRatio(1f)) {
                            MiniImage(
                                painter = rememberQrCodePainter(data = "rachel://yinlin.love/openSong?id=${args.song.id}") {
                                    shapes {
                                        ball = QrBallShape.circle()
                                        frame = QrFrameShape.roundCorners(0.25f)
                                    }
                                    colors {
                                        dark = QrBrush.solid(Colors.Dark)
                                        frame = QrBrush.solid(Colors.Dark)
                                    }
                                },
                                modifier = Modifier.padding(CustomTheme.padding.equalExtraSpace)
                                    .fillMaxSize().clip(MaterialTheme.shapes.small)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun Floating() {
        shareSheet.Land()
    }
}