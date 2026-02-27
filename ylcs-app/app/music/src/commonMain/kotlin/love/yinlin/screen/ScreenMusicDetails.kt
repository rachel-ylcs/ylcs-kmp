package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.io.files.Path
import love.yinlin.app
import love.yinlin.common.PathMod
import love.yinlin.compose.*
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.UserBar
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.Menus
import love.yinlin.compose.ui.floating.download
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryLoadingTextButton
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationColumn
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.SimpleClipText
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.coroutines.Coroutines
import love.yinlin.cs.*
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.rachel.song.Song
import love.yinlin.data.rachel.song.SongComment
import love.yinlin.extension.*
import love.yinlin.mod.ModFactory
import love.yinlin.platform.Platform
import love.yinlin.platform.UnsupportedPlatformText
import love.yinlin.startup.StartupMusicPlayer
import love.yinlin.tpl.lyrics.LrcParser

@Stable
class ScreenMusicDetails(private val sid: String) : Screen() {
    @Stable
    private data class ResourceItem(val type: ModResourceType, val size: Int?)

    private fun Song.clientPath(type: ModResourceType): Path = Path(PathMod, this.sid, type.filename)
    private fun Song.remotePath(type: ModResourceType): String = ServerRes.Mod.Song(sid).res(type.filename).url
    private val remoteModPath: String get() = ServerRes.Mod.Song(sid).res(ModResourceType.BASE_RES).url

    private val mp by lazyProvider { app.startup<StartupMusicPlayer>() }

    private val clientResources = mutableStateListOf<ResourceItem>()
    private val remoteResources = mutableStateListOf<ResourceItem>()

    private var clientSong: Song? by mutableStateOf(null)
    private var remoteSong: Song? by mutableStateOf(null)
    private var lyrics: String? by mutableRefStateOf(null)

    private val pageComments = object : Pagination<SongComment, Long, Long>(
        default = 0L,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: SongComment): Long = item.cid
        override fun offset(item: SongComment): Long = item.cid
    }

    private val listState = LazyListState()

    private fun requestClientSong(): Song? {
        val musicInfo = mp?.library[sid]
        return if (musicInfo != null) {
            val items = ModResourceType.entries.associateWith { musicInfo.path(PathMod, it).fileSize.toInt() }
            clientResources.replaceAll(items.asSequence().filter { it.value > 0 }.map { ResourceItem(it.key, it.value) }.toList())
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

    private suspend fun downloadMod() {
        // 下载MOD
        val path = Coroutines.io {
            app.os.storage.createTempFile { sink ->
                downloadDialog.download(remoteModPath, sink) { }
            }
        }
        // 安装MOD
        if (path != null) {
            catchingError {
                slot.loading.open {
                    Coroutines.io {
                        // 解压
                        path.read { source ->
                            val result = ModFactory.Release(source, PathMod).process { _, _, _ ->  }
                            require(result.metadata.version == ModFactory.VERSION) { "不匹配的MOD版本" }
                        }
                        path.delete() // 删除临时文件
                    }
                }
                // 通知
                mp?.updateMusicLibraryInfo(listOf(sid))
                // 更新状态
                clientSong = requestClientSong()
                slot.tip.success("安装成功")
            }?.let { slot.tip.error("安装失败: ${it.message}") }
        }
        else slot.tip.error("下载失败")
    }

    private suspend fun downloadModResource(item: ResourceItem, song: Song) {
        if (slot.confirm.open(content = "下载资源: ${item.type.description}?")) {
            // 下载资源
            catchingError {
                Coroutines.io {
                    Path(PathMod, sid, item.type.filename).write { sink ->
                        require(downloadDialog.download(song.remotePath(item.type), sink) { })
                    }
                }
                // 通知
                mp?.updateMusicLibraryInfo(listOf(sid))
                // 更新状态
                clientSong = requestClientSong()
                slot.tip.success("下载成功")
            }?.let { slot.tip.error("下载失败: ${it.message}") }
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
        listState.requestScrollToItem(pageComments.items.size - 1)
        err == null
    } ?: false

    private suspend fun onDeleteComment(cid: Long) {
        if (slot.confirm.open(content = "删除评论")) {
            ApiSongDeleteSongComment.request(app.config.userToken, cid) {
                pageComments.items.removeAll { it.cid == cid }
            }.errorTip
        }
    }

    override val title: String get() = clientSong?.name ?: remoteSong?.name ?: "未知歌曲"

    override suspend fun initialize() {
        val currentClientSong = requestClientSong()
        clientSong = currentClientSong

        supervisorScope {
            this.launch { remoteSong = requestRemoteSong() }
            this.launch { requestNewSongComments() }
            this.launch {
                lyrics = catchingNull {
                    Coroutines.io {
                        LrcParser(currentClientSong!!.clientPath(ModResourceType.LineLyrics).readText()!!).plainText
                    }
                }
            }
        }
    }

    val resBrush = ModResourceType.entries.associateWith {
        Brush.linearGradient(when (it) {
            ModResourceType.Config -> listOf(Colors.Yellow5, Colors.Yellow6)
            ModResourceType.Audio -> listOf(Colors.Pink4, Colors.Pink5)
            ModResourceType.Record -> listOf(Colors.Purple4, Colors.Purple5)
            ModResourceType.Background -> listOf(Colors.Blue5, Colors.Blue6)
            ModResourceType.LineLyrics -> listOf(Colors.Green7, Colors.Green8)
            ModResourceType.Animation -> listOf(Colors.Orange5, Colors.Orange6)
            ModResourceType.Video -> listOf(Colors.Green5, Colors.Green6)
            ModResourceType.Rhyme -> listOf(Colors.Cyan5, Colors.Cyan6)
        })
    }

    val resIcon = ModResourceType.entries.associateWith {
        when (it) {
            ModResourceType.Config -> Icons.Construction
            ModResourceType.Audio -> Icons.AudioFile
            ModResourceType.Record -> Icons.Album
            ModResourceType.Background -> Icons.Image
            ModResourceType.LineLyrics -> Icons.Lyrics
            ModResourceType.Animation -> Icons.GifBox
            ModResourceType.Video -> Icons.Movie
            ModResourceType.Rhyme -> Icons.MusicNote
        }
    }

    @Stable
    private data class ResourceAction(val text: String, val icon: ImageVector, val action: (ResourceItem) -> Unit)

    private val resActionDelete = ResourceAction("删除", Icons.Delete) { item ->
        launch {
            if (slot.confirm.open(content = "删除${item.type.description}资源")) {
                clientResources.remove(item)
                clientSong?.clientPath(item.type)?.delete()
            }
        }
    }

    private val resAction = ModResourceType.entries.associateWith {
        when (it) {
            ModResourceType.Config -> emptyList()
            ModResourceType.Audio -> emptyList()
            ModResourceType.Record -> emptyList()
            ModResourceType.Background -> emptyList()
            ModResourceType.LineLyrics -> emptyList()
            ModResourceType.Animation -> emptyList()
            ModResourceType.Video -> listOf(resActionDelete)
            ModResourceType.Rhyme -> listOf(resActionDelete)
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        if (clientResources.isEmpty() && remoteSong != null) {
            LoadingIcon(icon = Icons.Download, tip = "下载", onClick = {
                if (app.config.userProfile == null) slot.tip.warning("请先登录")
                else Platform.use(*Platform.Web,
                    ifTrue = { slot.tip.warning(UnsupportedPlatformText) },
                    ifFalse = {
                        if (slot.confirm.open(content = "下载该MOD?")) launch { downloadMod() }
                    }
                )
            })
        }
        Icon(icon = Icons.Share, tip = "分享", onClick = {
            slot.tip.info("敬请期待")
        })
    }

    @Composable
    private fun DetailsLayout(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            val song = clientSong ?: remoteSong

            clientSong?.let {
                LocalFileImage(
                    uri = it.clientPath(ModResourceType.Background).toString(),
                    modifier = Modifier.matchParentSize().zIndex(1f),
                    contentScale = ContentScale.Crop
                )
            } ?: remoteSong?.let {
                WebImage(
                    uri = it.remotePath(ModResourceType.Background),
                    modifier = Modifier.matchParentSize().zIndex(1f),
                    contentScale = ContentScale.Crop
                )
            } ?: Box(modifier = Modifier.matchParentSize().background(Colors.Black).zIndex(1f))

            Box(
                modifier = Modifier.zIndex(2f),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(Modifier
                    .size(Theme.size.image5)
                    .clip(Theme.shape.circle)
                    .border(Theme.border.v5, Theme.color.secondary, Theme.shape.circle)
                    .zIndex(2f)
                ) {
                    clientSong?.let {
                        LocalFileImage(
                            uri = it.clientPath(ModResourceType.Record).toString(),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } ?: remoteSong?.let {
                        WebImage(
                            uri = it.remotePath(ModResourceType.Background),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } ?: Box(modifier = Modifier.fillMaxSize().background(Colors.Black))
                }

                Surface(
                    modifier = Modifier.padding(
                        horizontal = Theme.padding.h9,
                        vertical = Theme.size.image5 / 2
                    ).fillMaxWidth().zIndex(1f),
                    shape = Theme.shape.v3,
                    contentPadding = Theme.padding.eValue9,
                    shadowElevation = Theme.shadow.v3
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
                    ) {
                        Row(
                            modifier = Modifier.padding(bottom = Theme.size.image5 / 4).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextIconAdapter { idIcon, idText ->
                                Icon(icon = Icons.Loyalty, modifier = Modifier.idIcon())
                                SimpleClipText(text = song?.sid ?: "0", style = Theme.typography.v7.bold, modifier = Modifier.idText())
                            }
                            TextIconAdapter { idIcon, idText ->
                                Icon(icon = Icons.Commit, modifier = Modifier.idIcon())
                                SimpleClipText(text = "v${song?.version ?: "?.?"}", style = Theme.typography.v7.bold, modifier = Modifier.idText())
                            }
                        }

                        SimpleEllipsisText(
                            text = song?.name ?: "未知歌曲",
                            style = Theme.typography.v6.bold,
                            color = Theme.color.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        TextIconAdapter { idIcon, idText ->
                            Icon(icon = Icons.Artist, modifier = Modifier.idIcon())
                            SimpleEllipsisText(text = "演唱: ${song?.singer ?: "未知"}", modifier = Modifier.idText())
                        }
                        TextIconAdapter { idIcon, idText ->
                            Icon(icon = Icons.Lyrics, modifier = Modifier.idIcon())
                            SimpleEllipsisText(text = "作词: ${song?.lyricist ?: "未知"}", modifier = Modifier.idText())
                        }
                        TextIconAdapter { idIcon, idText ->
                            Icon(icon = Icons.Lyrics, modifier = Modifier.idIcon())
                            SimpleEllipsisText(text = "作曲: ${song?.composer ?: "未知"}", modifier = Modifier.idText())
                        }
                        TextIconAdapter { idIcon, idText ->
                            Icon(icon = Icons.Album, modifier = Modifier.idIcon())
                            SimpleEllipsisText(text = "专辑: ${song?.album ?: "未知"}", modifier = Modifier.idText())
                        }

                        Divider()

                        SelectionBox {
                            Text(
                                text = lyrics ?: "",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1.5f).verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ResourceItemLayout(item: ResourceItem, remote: Boolean) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(resBrush[item.type]!!, alpha = if (remote) 0.5f else 1f)
                .clickable {
                    if (remote) {
                        if (app.config.userProfile == null) slot.tip.warning("请先登录")
                        else launch { remoteSong?.let { downloadModResource(item, it) } }
                    }
                }.padding(Theme.padding.eValue),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeContainer(
                color = if (remote) Theme.color.disabledContent else Theme.color.onContainer,
                variantColor = if (remote) Theme.color.disabledContent else Theme.color.onContainerVariant
            ) {
                val resSize = item.size?.toLong()?.fileSizeString?.let { "($it)" } ?: ""

                Icon(icon = if (remote) Icons.Download else resIcon[item.type]!!)
                SimpleEllipsisText(text = "${item.type.description}$resSize", style = Theme.typography.v7.bold, modifier = Modifier.weight(1f))
                if (remote) SimpleEllipsisText(text = "下载", style = Theme.typography.v7.bold)
                else {
                    ActionScope.Right.Container {
                        for (action in resAction[item.type]!!) {
                            Icon(icon = action.icon, tip = action.text, onClick = { action.action(item) })
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ResourceLayout(modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            val downloadResources by rememberDerivedState {
                remoteResources.filter { item -> clientResources.find { it.type == item.type } == null }
            }

            for (item in clientResources) ResourceItemLayout(item = item, remote = false)
            for (item in downloadResources) ResourceItemLayout(item = item, remote = true)
        }
    }

    @Composable
    private fun SongCommentHeader(modifier: Modifier = Modifier) {
        Row(modifier = modifier) {
            PrimaryLoadingTextButton(
                text = "写歌评",
                icon = Icons.Comment,
                enabled = app.config.userProfile != null,
                onClick = { commentDialog.open()?.let { onSendComment(it) } }
            )
        }
    }

    @Composable
    private fun SongCommentLayout(comment: SongComment, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            UserBar(
                avatar = comment.avatarPath.url,
                name = comment.name,
                time = comment.ts,
                label = comment.label,
                level = comment.level,
                onAvatarClick = { navigate(::ScreenUserCard, comment.uid) }
            )
            SelectionBox {
                Text(text = comment.content, modifier = Modifier.fillMaxWidth())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                var menuVisible by rememberFalse()

                Menus(
                    visible = menuVisible,
                    onClose = { menuVisible = false },
                    menus = {
                        app.config.userProfile?.let { user ->
                            if (user.uid == comment.uid || user.hasPrivilegeVIPTopic) {
                                Menu(text = "删除", icon = Icons.Send, onClick = {
                                    launch { onDeleteComment(comment.cid) }
                                })
                            }
                        }
                    }
                ) {
                    Icon(icon = Icons.MoreHorizontal, onClick = { menuVisible = true })
                }
            }
        }
    }

    @Composable
    private fun Portrait() {
        PaginationColumn(
            items = pageComments.items,
            key = { it.cid },
            state = listState,
            canRefresh = false,
            canLoading = pageComments.canLoading,
            onLoading = ::requestMoreSongComments,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            header = {
                DetailsLayout(modifier = Modifier.fillMaxWidth())
                ResourceLayout(modifier = Modifier.fillMaxWidth())
                SongCommentHeader(modifier = Modifier.fillMaxWidth().padding(Theme.padding.value))
            }
        ) {
            SongCommentLayout(comment = it, modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.h))
        }
    }

    @Composable
    private fun Landscape() {
        Row(modifier = Modifier.fillMaxSize()) {
            val immersivePadding = LocalImmersivePadding.current

            DetailsLayout(modifier = Modifier
                .padding(immersivePadding.withoutEnd)
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
            )
            Divider()
            ResourceLayout(modifier = Modifier
                .padding(immersivePadding.withoutHorizontal)
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
            )
            Divider()
            PaginationColumn(
                items = pageComments.items,
                key = { it.cid },
                state = listState,
                canRefresh = false,
                canLoading = pageComments.canLoading,
                onLoading = ::requestMoreSongComments,
                modifier = Modifier
                    .padding(immersivePadding.withoutStart)
                    .weight(1f)
                    .fillMaxHeight(),
                header = { SongCommentHeader(modifier = Modifier.fillMaxWidth().padding(Theme.padding.value)) }
            ) {
                SongCommentLayout(comment = it, modifier = Modifier.fillMaxWidth().padding(horizontal = Theme.padding.h))
            }
        }
    }

    @Composable
    override fun Content() {
        when (LocalDevice.current.type) {
            Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait()
            Device.Type.LANDSCAPE -> Landscape()
        }
    }

    private val commentDialog = this land DialogInput(
        hint = "留下你的足迹...",
        maxLength = 1024,
        maxLines = 5,
        minLines = 1
    )

    private val downloadDialog = this land DialogDownload()
}