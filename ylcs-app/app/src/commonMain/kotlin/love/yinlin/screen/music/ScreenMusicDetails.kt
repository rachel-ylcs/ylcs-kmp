package love.yinlin.screen.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.io.files.Path
import love.yinlin.api.*
import love.yinlin.app
import love.yinlin.common.ExtraIcons
import love.yinlin.common.Paths
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.floating.FloatingDialogInput
import love.yinlin.compose.ui.floating.FloatingDownloadDialog
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.LoadingClickText
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.OffsetLayout
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationColumn
import love.yinlin.compose.ui.layout.SimpleEmptyBox
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.rachel.song.Song
import love.yinlin.data.rachel.song.SongComment
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingError
import love.yinlin.extension.delete
import love.yinlin.extension.fileSizeString
import love.yinlin.extension.read
import love.yinlin.extension.readText
import love.yinlin.extension.replaceAll
import love.yinlin.extension.size
import love.yinlin.extension.write
import love.yinlin.mod.ModFactory
import love.yinlin.platform.Coroutines
import love.yinlin.platform.Platform
import love.yinlin.platform.UnsupportedPlatformText
import love.yinlin.platform.lyrics.LrcParser
import love.yinlin.platform.platform
import love.yinlin.screen.community.ScreenUserCard
import love.yinlin.screen.community.UserBar

@Stable
class ScreenMusicDetails(manager: ScreenManager, private val sid: String) : Screen(manager) {
    private val clientResources = mutableStateListOf<ResourceItem>()
    private val remoteResources = mutableStateListOf<ResourceItem>()

    private var clientSong: Song? by mutableStateOf(null)
    private var remoteSong: Song? by mutableStateOf(null)

    private val pageComments = object : Pagination<SongComment, Long, Long>(
        default = 0L,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: SongComment): Long = item.cid
        override fun offset(item: SongComment): Long = item.cid
    }

    private val listState = LazyListState()

    override val title: String by derivedStateOf { clientSong?.name ?: remoteSong?.name ?: "未知歌曲" }

    private fun Song.clientPath(type: ModResourceType): Path = Path(Paths.modPath, this.sid, type.filename)
    private fun Song.remotePath(type: ModResourceType): String = ServerRes.Mod.Song(sid).res(type.filename).url
    private val remoteModPath: String get() = ServerRes.Mod.Song(sid).res(ModResourceType.BASE_RES).url

    @Stable
    private data class ResourceItem(
        val type: ModResourceType,
        val size: Int?
    ) {
        fun brush(alpha: Float): Brush = Brush.linearGradient(when (type) {
            ModResourceType.Config -> listOf(Colors.Yellow5, Colors.Yellow3, Colors.Yellow5)
            ModResourceType.Audio -> listOf(Colors.Pink4, Colors.Pink2, Colors.Pink4)
            ModResourceType.Record -> listOf(Colors.Purple4, Colors.Purple2, Colors.Purple4)
            ModResourceType.Background -> listOf(Colors.Blue5, Colors.Blue3, Colors.Blue5)
            ModResourceType.LineLyrics -> listOf(Colors.Green7, Colors.Green5, Colors.Green7)
            ModResourceType.Animation -> listOf(Colors.Orange5, Colors.Orange3, Colors.Orange5)
            ModResourceType.Video -> listOf(Colors.Green5, Colors.Green3, Colors.Green5)
            ModResourceType.Rhyme -> listOf(Colors.Cyan5, Colors.Cyan3, Colors.Cyan5)
        }.map { it.copy(alpha = alpha) })

        val icon: ImageVector get() = when (type) {
            ModResourceType.Config -> Icons.Outlined.Construction
            ModResourceType.Audio -> Icons.Outlined.AudioFile
            ModResourceType.Record -> Icons.Outlined.Album
            ModResourceType.Background -> Icons.Outlined.Image
            ModResourceType.LineLyrics -> Icons.Outlined.Lyrics
            ModResourceType.Animation -> Icons.Outlined.GifBox
            ModResourceType.Video -> Icons.Outlined.Movie
            ModResourceType.Rhyme -> Icons.Outlined.MusicNote
        }
    }

    private fun requestClientSong(): Song? {
        val musicInfo = app.mp.library[sid]
        return if (musicInfo != null) {
            val items = ModResourceType.entries.associateWith { musicInfo.path(Paths.modPath, it).size.toInt() }
            clientResources.replaceAll(items.filter { it.value > 0 }.map { ResourceItem(it.key, it.value) })
            Song(
                sid = musicInfo.id,
                version = musicInfo.version,
                name = musicInfo.name,
                singer = musicInfo.singer,
                lyricist = musicInfo.lyricist,
                composer = musicInfo.composer,
                album = musicInfo.album,
                animation = (items[ModResourceType.Animation] ?: 0) > 0,
                video = (items[ModResourceType.Video] ?: 0) > 0,
                rhyme = (items[ModResourceType.Rhyme] ?: 0) > 0
            )
        } else null
    }

    private suspend fun requestRemoteSong(): Song? {
        var song: Song? = null
        ApiSongGetSong.request(sid) { data ->
            remoteResources.replaceAll(ModResourceType.BASE.map { ResourceItem(it, null) })
            if (data.animation) remoteResources.add(ResourceItem(ModResourceType.Animation, null))
            if (data.video) remoteResources.add(ResourceItem(ModResourceType.Video, null))
            if (data.rhyme) remoteResources.add(ResourceItem(ModResourceType.Rhyme, null))
            song = data
        }
        return song
    }

    private suspend fun requestNewSongComments() {
        ApiSongGetSongComments.request(sid, pageComments.default, pageComments.pageNum) {
            pageComments.newData(it)
        }.errorTip
    }

    private suspend fun requestMoreSongComments() {
        ApiSongGetSongComments.request(sid, pageComments.offset, pageComments.pageNum) {
            pageComments.moreData(it)
        }
    }

    private fun downloadMod() {
        if (app.config.userProfile == null) {
            slot.tip.warning("请先登录")
            return
        }
        launch {
            // 下载MOD
            val path = Coroutines.io {
                app.os.storage.createTempFile { sink ->
                    downloadDialog.openSuspend(remoteModPath, sink) { }
                }
            }
            // 安装MOD
            if (path != null) {
                slot.loading.openSuspend()
                catchingError {
                    Coroutines.io {
                        // 解压
                        path.read { source ->
                            val result = ModFactory.Release(source, Paths.modPath).process { _, _, _ ->  }
                            require(result.metadata.version == ModFactory.VERSION) { "不匹配的MOD版本" }
                        }
                        // 删除临时文件
                        path.delete()
                    }
                    slot.loading.close()
                    // 通知
                    app.mp.updateMusicLibraryInfo(listOf(sid))
                    // 更新状态
                    clientSong = requestClientSong()
                    slot.tip.success("安装成功")
                }?.let { slot.tip.error("安装失败: ${it.message}") }
            }
            else slot.tip.error("下载失败")
        }
    }

    private fun downloadModResource(item: ResourceItem, song: Song) {
        if (app.config.userProfile == null) {
            slot.tip.warning("请先登录")
            return
        }
        launch {
            if (slot.confirm.openSuspend(content = "下载资源: ${item.type.description}?")) {
                // 下载资源
                catchingError {
                    Coroutines.io {
                        Path(Paths.modPath, sid, item.type.filename).write { sink ->
                            require(downloadDialog.openSuspend(song.remotePath(item.type), sink) {})
                        }
                    }
                    // 通知
                    app.mp.updateMusicLibraryInfo(listOf(sid))
                    // 更新状态
                    clientSong = requestClientSong()
                    slot.tip.success("下载成功")
                }?.let { slot.tip.error("下载失败: ${it.message}") }
            }
        }
    }

    private suspend fun onSendComment(content: String): Boolean = app.config.userProfile?.let { user ->
        val err = ApiSongSendSongComment.request(app.config.userToken, sid, content) {
            pageComments.items += SongComment(
                cid = it,
                uid = user.uid,
                ts = DateEx.CurrentString,
                content = content,
                name = user.name,
                label = user.label,
                exp = user.exp
            )
        }.errorTip
        listState.animateScrollToItem(pageComments.items.size - 1)
        err == null
    } ?: false

    private suspend fun onDeleteComment(cid: Long) {
        if (slot.confirm.openSuspend(content = "删除评论")) {
            ApiSongDeleteSongComment.request(app.config.userToken, cid) {
                pageComments.items.removeAll { it.cid == cid }
            }.errorTip
        }
    }

    override suspend fun initialize() {
        clientSong = requestClientSong()
        launch { remoteSong = requestRemoteSong() }
        launch { requestNewSongComments() }
    }

    @Composable
    private fun DetailsLayout(modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
            val song by rememberDerivedState { clientSong ?: remoteSong }
            val deviceType = LocalDevice.current.type

            Box(
                modifier = Modifier
                    .padding(bottom = if (deviceType == Device.Type.PORTRAIT) 48.dp else 0.dp)
                    .matchParentSize()
                    .shadow(CustomTheme.shadow.surface)
                    .background(Colors.Black)
                    .zIndex(1f)
            ) {
                clientSong?.let {
                    LocalFileImage(
                        path = { it.clientPath(ModResourceType.Background) },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: remoteSong?.let {
                    WebImage(
                        uri = remember(song) { it.remotePath(ModResourceType.Background) },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: Box(modifier = Modifier.fillMaxSize())
            }

            Column(modifier = Modifier.fillMaxWidth().zIndex(2f)) {
                if (deviceType == Device.Type.PORTRAIT) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f))
                }
                else {
                    Box(modifier = Modifier.fillMaxWidth().height(96.dp))
                }
                Column(
                    modifier = Modifier.padding(
                        horizontal = CustomTheme.padding.horizontalExtraSpace,
                        vertical = CustomTheme.padding.verticalExtraSpace
                    ).fillMaxWidth().shadow(
                        elevation = CustomTheme.shadow.surface,
                        shape = MaterialTheme.shapes.extraLarge.copy(topStart = CornerSize(0), topEnd = CornerSize(0)),
                        clip = false
                    ).background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.extraLarge.copy(topStart = CornerSize(0), topEnd = CornerSize(0))
                    ).padding(CustomTheme.padding.equalValue),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace),
                ) {
                    var showLyrics by rememberFalse()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            NormalText(
                                text = song?.sid ?: "0",
                                icon = Icons.Outlined.Loyalty,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        OffsetLayout(y = (-48).dp) {
                            Box(
                                modifier = Modifier.size(96.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = CustomTheme.border.medium,
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface
                                    ).clickable(enabled = clientSong != null) {
                                        showLyrics = !showLyrics
                                    }
                            ) {
                                clientSong?.let {
                                    LocalFileImage(
                                        path = { it.clientPath(ModResourceType.Record) },
                                        modifier = Modifier.fillMaxSize(),
                                        circle = true,
                                        contentScale = ContentScale.Crop
                                    )
                                } ?: remoteSong?.let {
                                    WebImage(
                                        uri = remember(song) { it.remotePath(ModResourceType.Record) },
                                        modifier = Modifier.fillMaxSize(),
                                        circle = true,
                                        contentScale = ContentScale.Crop
                                    )
                                } ?: Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Colors.Black))
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            NormalText(
                                text = "v${song?.version ?: "?.?"}",
                                icon = ExtraIcons.Artist,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Text(
                        text = if (showLyrics) "歌词" else song?.name ?: "未知歌曲",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(CustomTheme.padding.extraValue).align(Alignment.CenterHorizontally)
                    )

                    if (showLyrics) {
                        var lyrics by rememberState { "" }

                        LaunchedEffect(Unit) {
                            lyrics = Coroutines.io {
                                catchingDefault("") { LrcParser(clientSong!!.clientPath(ModResourceType.LineLyrics).readText()!!).plainText }
                            }
                        }

                        SelectionContainer {
                            Text(
                                text = lyrics,
                                modifier = Modifier.fillMaxWidth().aspectRatio(2f).verticalScroll(rememberScrollState())
                            )
                        }
                    }
                    else {
                        NormalText(
                            text = "演唱: ${song?.singer ?: "未知"}",
                            icon = ExtraIcons.Artist
                        )
                        NormalText(
                            text = "作词: ${song?.lyricist ?: "未知"}",
                            icon = Icons.Outlined.Lyrics
                        )
                        NormalText(
                            text = "作曲: ${song?.composer ?: "未知"}",
                            icon = Icons.Outlined.Lyrics
                        )
                        NormalText(
                            text = "专辑: ${song?.album ?: "未知"}",
                            icon = Icons.Outlined.Album
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ActionScope.ResourceItemActionLayout(type: ModResourceType) {
        // TODO: 等待重做
        when (type) {
            ModResourceType.Config -> {

            }
            ModResourceType.Audio -> {

            }
            ModResourceType.Record -> {

            }
            ModResourceType.Background -> {

            }
            ModResourceType.LineLyrics -> {

            }
            ModResourceType.Animation -> {

            }
            ModResourceType.Video -> {

            }
            ModResourceType.Rhyme -> {

            }
        }
    }

    @Composable
    private fun ResourceItemLayout(item: ResourceItem, remote: Boolean) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .background(brush = remember(item, remote) { item.brush(if (remote) 0.5f else 1f) })
            .clickable {
                if (remote) remoteSong?.let { downloadModResource(item, it) }
            }
            .padding(CustomTheme.padding.equalValue)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniIcon(
                    icon = if (remote) Icons.Outlined.Download else item.icon,
                    color = if (remote) Colors.Gray4 else Colors.White
                )
                Text(
                    text = item.type.description,
                    color = if (remote) Colors.Gray4 else Colors.White,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = remember(item, remote) {
                        if (remote) "下载"
                        else item.size?.toLong()?.fileSizeString ?: ""
                    },
                    color = if (remote) Colors.Gray4 else Colors.White,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!remote) {
                ActionScope.Right.ActionLayout(modifier = Modifier.fillMaxWidth()) {
                    ResourceItemActionLayout(item.type)
                }
            }
        }
    }

    @Composable
    private fun ResourceLayout(modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
            if (clientResources.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) {
                    SimpleEmptyBox()
                }
            }
            else {
                Column(modifier = Modifier.fillMaxWidth().background(Colors.Black)) {
                    val downloadResources by rememberDerivedState {
                        remoteResources.filter { item -> clientResources.find { it.type == item.type } == null }
                    }

                    for (item in clientResources) {
                        ResourceItemLayout(item = item, remote = false)
                    }

                    for (item in downloadResources) {
                        ResourceItemLayout(item = item, remote = true)
                    }
                }
            }
        }
    }

    @Composable
    override fun ActionScope.RightActions() {
        if (clientResources.isEmpty() && remoteSong != null) {
            ActionSuspend(Icons.Outlined.Download, "下载") {
                if (platform == Platform.WebWasm) slot.tip.warning(UnsupportedPlatformText)
                else if (slot.confirm.openSuspend(content = "下载该MOD?")) downloadMod()
            }
        }
        Action(Icons.Outlined.Share, "分享") {
            slot.tip.info("敬请期待")
        }
    }

    @Composable
    private fun SongCommentHeader() {
        LoadingClickText(
            text = "写歌评",
            icon = Icons.AutoMirrored.Outlined.Comment,
            enabled = app.config.userProfile != null,
            onClick = {
                commentDialog.openSuspend()?.let { input ->
                    onSendComment(input)
                }
            },
            modifier = Modifier.padding(bottom = CustomTheme.padding.verticalSpace)
        )
        if (pageComments.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) { SimpleEmptyBox() }
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
                avatar = remember(comment) { comment.avatarPath.url },
                name = comment.name,
                time = comment.ts,
                label = comment.label,
                level = comment.level,
                onAvatarClick = { navigate(::ScreenUserCard, comment.uid) }
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
    private fun Portrait() {
        PauseLoading(listState)

        PaginationColumn(
            items = pageComments.items,
            key = { it.cid },
            state = listState,
            canRefresh = false,
            canLoading = pageComments.canLoading,
            onLoading = { requestMoreSongComments() },
            itemDivider = PaddingValues(vertical = CustomTheme.padding.verticalSpace),
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            header = {
                DetailsLayout(modifier = Modifier.fillMaxWidth())
                HorizontalDivider(modifier = Modifier.padding(vertical = CustomTheme.padding.verticalExtraSpace * 2))
                ResourceLayout(modifier = Modifier.fillMaxWidth())
                HorizontalDivider(modifier = Modifier.padding(top = CustomTheme.padding.verticalExtraSpace * 2))
                SongCommentHeader()
            }
        ) {
            SongCommentLayout(
                comment = it,
                modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalSpace)
            )
        }
    }

    @Composable
    private fun Landscape() {
        Row(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            DetailsLayout(
                modifier = Modifier
                    .padding(immersivePadding.withoutEnd)
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            )
            VerticalDivider()
            ResourceLayout(
                modifier = Modifier
                    .padding(immersivePadding.withoutHorizontal)
                    .weight(1f)
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
                onLoading = { requestMoreSongComments() },
                itemDivider = PaddingValues(vertical = CustomTheme.padding.verticalSpace),
                modifier = Modifier
                    .padding(immersivePadding.withoutStart)
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(CustomTheme.padding.value),
                header = {
                    SongCommentHeader()
                }
            ) {
                SongCommentLayout(
                    comment = it,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    override fun Content(device: Device) {
        when (device.type) {
            Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait()
            Device.Type.LANDSCAPE -> Landscape()
        }
    }

    private val commentDialog = this land FloatingDialogInput(
        hint = "留下你的足迹...",
        maxLength = 1024,
        maxLines = 5,
        minLines = 1,
        clearButton = false
    )

    private val downloadDialog = this land FloatingDownloadDialog()
}