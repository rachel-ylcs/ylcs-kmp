package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import love.yinlin.annotation.LooseTyped
import love.yinlin.app
import love.yinlin.compose.*
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.ds.DataSourceMusic
import love.yinlin.compose.extension.movableComposable
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.extension.rememberState
import love.yinlin.compose.graphics.PlatformImage
import love.yinlin.compose.graphics.crop
import love.yinlin.compose.graphics.decode
import love.yinlin.compose.graphics.encode
import love.yinlin.compose.image.data.ImageOptions
import love.yinlin.compose.image.input.ImageSource
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.common.UserBar
import love.yinlin.compose.ui.floating.Dialog
import love.yinlin.compose.ui.floating.DialogCrop
import love.yinlin.compose.ui.floating.DialogDownload
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.Menus
import love.yinlin.compose.ui.floating.SheetContent
import love.yinlin.compose.ui.floating.download
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.PrimaryLoadingButton
import love.yinlin.compose.ui.input.PrimaryLoadingTextButton
import love.yinlin.compose.ui.input.PrimaryTextButton
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.layout.HorizontalDivider
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationColumn
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SelectionBox
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.coroutines.Coroutines
import love.yinlin.cs.*
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.rachel.song.Song
import love.yinlin.data.rachel.song.SongComment
import love.yinlin.extension.*
import love.yinlin.foundation.parser.lrc.Lrc
import love.yinlin.fs.*
import love.yinlin.mod.ModFactory
import love.yinlin.startup.StartupMusicPlayer

@Stable
class ScreenMusicDetails(private val sid: String) : Screen() {
    @Stable
    private data class ResourceItem(val type: ModResourceType, val size: Long?)

    private fun Song.clientPath(type: ModResourceType): File = File(app.modPath, this.sid, type.filename)
    private fun Song.remotePath(type: ModResourceType): String = ServerRes.Mod.Song(sid).res(type.filename).url
    private val remoteModPath: String get() = ServerRes.Mod.Song(sid).res(ModResourceType.BASE_RES).url

    private val musicPlayer by derivedStateOf { app.requireClassOrNull<StartupMusicPlayer>() }

    private val clientResources = mutableStateListOf<ResourceItem>()
    private val remoteResources = mutableStateListOf<ResourceItem>()

    private var clientSong: Song? by mutableRefStateOf(null)
    private var remoteSong: Song? by mutableRefStateOf(null)

    private var modifyFlag by mutableLongStateOf(0L)

    private val pageComments = object : Pagination<SongComment, Long, Long>(
        default = 0L,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: SongComment): Long = item.cid
        override fun offset(item: SongComment): Long = item.cid
    }

    private val listState = LazyListState()

    private suspend fun requestClientSong(): Song? {
        val musicInfo = DataSourceMusic.library[sid]
        return if (musicInfo != null) {
            val items = enumAssociateWith { type: ModResourceType -> musicInfo.path(app.modPath, type).fileSize() }
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
            if (data.accompaniment) remoteResources.add(ResourceItem(ModResourceType.Accompaniment, null))
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
        val player = musicPlayer ?: return
        val checkData = player.checkReloadPlaylistByAdd(sid)
        if (checkData == StartupMusicPlayer.ReloadAddData.Playing) {
            slot.tip.warning("\"${player.currentMusic?.name}\"正在播放, 请先停止播放器")
            return
        }

        // 下载MOD
        val path = Coroutines.io {
            app.createTempFile { sink ->
                downloadDialog.download(remoteModPath, sink) { }
            }
        }
        // 安装MOD
        if (path != null) {
            catchingError {
                slot.loading.open {
                    // 解压
                    path.read { source ->
                        val result = ModFactory.Release(source, app.modPath).process { _, _, _ -> }
                        require(result.metadata.version == ModFactory.VERSION) { "不匹配的MOD版本" }
                    }
                    path.delete() // 删除临时文件
                }
                // 更新曲库
                DataSourceMusic.reloadMusicInfo(sid)?.let { DataSourceMusic.library[sid] = it }
                player.reloadPlaylistByAdd(sid, checkData)
                // 更新状态
                clientSong = requestClientSong()
                slot.tip.success("安装成功")
            }?.then { slot.tip.error("安装失败: ${it.message}") }
        }
        else slot.tip.error("下载失败")
    }

    private suspend fun downloadModResource(item: ResourceItem, song: Song) {
        val player = musicPlayer ?: return
        if (slot.confirm.open(content = "下载资源: ${item.type.description}?")) {
            val checkData = player.checkReloadPlaylistByAdd(sid)
            if (checkData == StartupMusicPlayer.ReloadAddData.Playing) {
                slot.tip.warning("\"${player.currentMusic?.name}\"正在播放, 请先停止播放器")
                return
            }
            // 下载资源
            catchingError {
                File(app.modPath, sid, item.type.filename).write { sink ->
                    require(downloadDialog.download(song.remotePath(item.type), sink) { })
                }
                // 更新曲库
                DataSourceMusic.reloadMusicInfo(sid)?.let { DataSourceMusic.library[sid] = it }
                player.reloadPlaylistByAdd(sid, checkData)
                // 更新状态
                clientSong = requestClientSong()
                slot.tip.success("下载成功")
            }?.then { slot.tip.error("下载失败: ${it.message}") }
        }
    }

    private suspend fun onSendComment(content: String): Boolean = app.config.userProfile?.let { (val uid, val name, val label, val exp) ->
        val err = ApiSongSendSongComment.request(app.config.userToken, sid, content) {
            pageComments.items += SongComment(
                cid = it,
                uid = uid,
                ts = DateEx.CurrentString,
                content = content,
                name = name,
                label = label,
                exp = exp
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

    override fun initialize() {
        launch {
            val currentClientSong = requestClientSong()
            clientSong = currentClientSong

            supervisorScope {
                this.launch { remoteSong = requestRemoteSong() }
                this.launch { requestNewSongComments() }
            }
        }
    }

    val resIcon = enumAssociateWith { type: ModResourceType ->
        when (type) {
            ModResourceType.Config -> Icons.Construction
            ModResourceType.Audio -> Icons.AudioFile
            ModResourceType.Record -> Icons.Album
            ModResourceType.Background -> Icons.Image
            ModResourceType.LineLyrics -> Icons.Lyrics
            ModResourceType.Animation -> Icons.GifBox
            ModResourceType.Video -> Icons.Movie
            ModResourceType.Rhyme -> Icons.Lyrics
            ModResourceType.Accompaniment -> Icons.MusicNote
        }
    }

    @Stable
    private data class ResourceButton(val text: String, val icon: ImageVector, val destructive: Boolean = false, val action: (ResourceItem) -> Unit)

    @Stable
    private data class ResourceAction(val buttons: List<ResourceButton> = [], val content: (@Composable (ResourceItem) -> Unit)? = null)

    private val resActionDelete = ResourceButton("删除", Icons.Delete, destructive = true) { item ->
        launch {
            if (slot.confirm.open(content = "删除${item.type.description}资源")) {
                clientResources.remove(item)
                clientSong?.clientPath(item.type)?.delete()
                ++modifyFlag
            }
        }
    }

    private fun resActionReplaceImage(aspectRatio: Float) = ResourceButton("替换", Icons.Replace) { item ->
        clientSong?.then { song ->
            launch {
                app.picker.pickPicture()?.use { source ->
                    app.createTempFile { sink -> source.transferTo(sink) > 0L }
                }?.then { src ->
                    cropDialog.open(source = ImageSource.LocalFile(src), aspectRatio = aspectRatio)?.then { region ->
                        catchingError {
                            Coroutines.io {
                                val image = PlatformImage.decode(src.readByteArray()!!)!!
                                image.crop(region)
                                song.clientPath(item.type).writeByteArray(image.encode(quality = ImageQuality.Full)!!)
                            }
                            DataSourceMusic.library.findAssign(sid) { it.copy(modification = it.modification + 1) }
                            clientResources.findAssign(item) {
                                it.copy(type = it.type, size = song.clientPath(it.type).fileSize())
                            }
                            ++modifyFlag
                        }?.then { slot.tip.error("图片载入或裁剪失败") }
                    }
                }
            }
        }
    }

    private val resActionConfigEditor = ResourceButton("修改", Icons.Edit) {
        DataSourceMusic.library[sid]?.let(configSheet::open)
    }

    private val resActionLyricsEditor = ResourceButton("编辑", Icons.Edit) {
        DataSourceMusic.library[sid]?.then { info ->
            launch { musicPlayer?.pause() }
            navigate(::ScreenLyricsEditor, info)
        }
    }

    @Composable
    private fun ResourceImagePreview(item: ResourceItem) {
        LocalFileImage(
            source = clientSong?.clientPath(item.type),
            version = modifyFlag.toString(),
            modifier = Modifier.fillMaxWidth().height(Theme.size.cell1 * 1.5f),
            contentScale = ContentScale.Fit,
            contentDescription = item.type.description,
            options = ImageOptions.Zoom
        )
    }

    @Composable
    private fun ResourceLyricsPreview(item: ResourceItem) {
        var text by rememberState(item, modifyFlag) { "正在读取歌词..." }

        LaunchedEffect(item, modifyFlag) {
            text = catchingNull {
                Coroutines.io {
                    val source = clientSong?.clientPath(item.type)?.readText() ?: error("歌词资源不存在")
                    Lrc.parse(source).plainText
                }
            }?.ifBlank { "暂无歌词" } ?: "歌词读取失败"
        }

        SelectionBox {
            Text(
                text = text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            )
        }
    }

    private val resAction = enumAssociateWith { type: ModResourceType ->
        when (type) {
            ModResourceType.Config -> ResourceAction(buttons = [resActionConfigEditor])
            ModResourceType.Audio -> ResourceAction()
            ModResourceType.Record -> ResourceAction(buttons = [resActionReplaceImage(1f)], content = { ResourceImagePreview(it) })
            ModResourceType.Background -> ResourceAction(buttons = [resActionReplaceImage(0.5625f)], content = { ResourceImagePreview(it) })
            ModResourceType.LineLyrics -> ResourceAction(buttons = [resActionLyricsEditor], content = { ResourceLyricsPreview(it) })
            ModResourceType.Animation -> ResourceAction(buttons = [resActionDelete], content = { ResourceImagePreview(it) })
            ModResourceType.Video -> ResourceAction(buttons = [resActionDelete])
            ModResourceType.Rhyme -> ResourceAction(buttons = [resActionDelete])
            ModResourceType.Accompaniment -> ResourceAction(buttons = [resActionDelete])
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        Icon(icon = Icons.Share, tip = "分享", onClick = {
            slot.tip.info("敬请期待")
        })
    }

    private val detailsLayout = movableComposable { modifier: Modifier ->
        val song = clientSong ?: remoteSong

        Column(modifier = modifier.background(Theme.color.surface)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f).background(Theme.color.backgroundVariant)) {
                clientSong?.let {
                    LocalFileImage(
                        source = it.clientPath(ModResourceType.Record),
                        version = modifyFlag.toString(),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } ?: remoteSong?.let {
                    WebImage(
                        source = it.remotePath(ModResourceType.Record),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SimpleEllipsisText(
                            text = "ID: ${song?.sid ?: sid}",
                            color = Theme.color.onContainer,
                            modifier = Modifier.weight(1f, false).background(Theme.color.primaryContainer, Theme.shape.v7).padding(Theme.padding.value)
                        )
                        SimpleEllipsisText(
                            text = song?.version ?: "1.0",
                            color = Theme.color.onContainer,
                            modifier = Modifier.weight(1f, false).background(Theme.color.secondaryContainer, Theme.shape.v7).padding(Theme.padding.value)
                        )
                    }

                    if (song != null) {
                        val modeColor = if (clientSong != null) Theme.color.tertiaryContainer else Theme.color.warning

                        SimpleEllipsisText(
                            text = if (clientSong != null) "本地 MOD" else "远程 MOD",
                            style = Theme.typography.v7.bold,
                            color = if (clientSong != null) Theme.color.onContainer else Theme.color.onWarning,
                            modifier = Modifier.background(modeColor, Theme.shape.v7).padding(Theme.padding.value)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                ) {
                    SimpleEllipsisText(
                        text = "专辑: ${song?.album ?: "未知"}",
                        style = Theme.typography.v8,
                        color = Theme.color.onSurfaceVariant,
                        modifier = Modifier.weight(1f, false).background(Theme.color.backgroundVariant, Theme.shape.v7).padding(Theme.padding.value)
                    )
                    SimpleEllipsisText(
                        text = "演唱: ${song?.singer ?: "未知"}",
                        style = Theme.typography.v8,
                        color = Theme.color.onSurfaceVariant,
                        modifier = Modifier.weight(1f, false).background(Theme.color.backgroundVariant, Theme.shape.v7).padding(Theme.padding.value)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                ) {
                    SimpleEllipsisText(
                        text = "作词：${song?.lyricist ?: "未知"}",
                        style = Theme.typography.v8,
                        color = Theme.color.onSurfaceVariant,
                        modifier = Modifier.weight(1f, false).background(Theme.color.backgroundVariant, Theme.shape.v7).padding(Theme.padding.value)
                    )
                    SimpleEllipsisText(
                        text = "作曲：${song?.composer ?: "未知"}",
                        style = Theme.typography.v8,
                        color = Theme.color.onSurfaceVariant,
                        modifier = Modifier.weight(1f, false).background(Theme.color.backgroundVariant, Theme.shape.v7).padding(Theme.padding.value)
                    )
                }
            }
        }
    }

    @Composable
    private fun ResourceItemLayout(item: ResourceItem, remote: Boolean) {
        val (buttons, content) = resAction[item.type]!!

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .condition(!remote && content != null) {
                    clickable {
                        launch { resourceDialog.open(item) }
                    }
                }.padding(Theme.padding.value),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon = resIcon[item.type]!!, color = Theme.color.onSurfaceVariant)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.g7)
            ) {
                SimpleEllipsisText(text = item.type.description, style = Theme.typography.v7.bold, color = Theme.color.onSurface)
                item.size?.let {
                    SimpleEllipsisText(text = it.fileSizeString, style = Theme.typography.v8, color = Theme.color.onSurfaceVariant)
                }
            }

            if (remote) {
                LoadingIcon(icon = Icons.Download, color = Theme.color.primary, onClick = {
                    if (app.config.userProfile == null) slot.tip.warning("请先登录")
                    else launch { remoteSong?.then { downloadModResource(item, it) } }
                })
            }
            else {
                ActionScope.Right.Container {
                    for (action in buttons) {
                        key(action.text) {
                            Icon(
                                icon = action.icon,
                                color = if (action.destructive) Theme.color.error else Theme.color.secondary,
                                onClick = { action.action(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    private val resourceLayout = movableComposable { modifier: Modifier ->
        Column(modifier = modifier.background(Theme.color.surface)) {
            if (clientResources.isNotEmpty()) {
                val downloadResources by rememberDerivedState {
                    remoteResources.filter { item -> clientResources.find { it.type == item.type } == null }
                }

                for (item in clientResources) {
                    key(item.type) {
                        ResourceItemLayout(item = item, remote = false)
                        HorizontalDivider()
                    }
                }

                for (item in downloadResources) {
                    key(item.type) {
                        ResourceItemLayout(item = item, remote = true)
                        HorizontalDivider()
                    }
                }
            }
            else if (clientSong == null && remoteSong != null) {
                PrimaryLoadingButton(
                    text = "下载 MOD",
                    icon = Icons.Download,
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                    onClick = {
                        if (app.config.userProfile == null) slot.tip.warning("请先登录")
                        else if (slot.confirm.open(content = "下载该MOD?")) launch { downloadMod() }
                    }
                )
            }
        }
    }

    private val songCommentHeader = movableComposable { modifier: Modifier ->
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon = Icons.VerifiedUser)
            SimpleEllipsisText(text = "歌评", style = Theme.typography.v6.bold, color = Theme.color.onSurface, modifier = Modifier.weight(1f))
            PrimaryLoadingTextButton(
                text = "写歌评",
                icon = Icons.Comment,
                enabled = app.config.userProfile != null,
                onClick = {
                    commentDialog.open()?.then { onSendComment(it) }
                }
            )
        }
    }

    @Composable
    private fun SongCommentLayout(comment: SongComment, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    UserBar(
                        avatar = comment.avatarPath.url,
                        name = comment.name,
                        time = comment.ts,
                        label = comment.label,
                        level = comment.level,
                        onAvatarClick = {
                            @OptIn(LooseTyped::class)
                            navigate("ScreenUserCard", comment.uid)
                        }
                    )
                }

                var menuVisible by rememberFalse()

                Menus(
                    visible = menuVisible,
                    onClose = { menuVisible = false },
                    menus = {
                        app.config.userProfile?.then { user ->
                            if (user.uid == comment.uid || user.hasPrivilegeVIPTopic) {
                                Menu(text = "删除", icon = Icons.Delete, onClick = {
                                    launch { onDeleteComment(comment.cid) }
                                })
                            }
                        }
                    }
                ) {
                    Icon(icon = Icons.MoreHorizontal, color = Theme.color.onSurfaceVariant, tip = "更多", onClick = { menuVisible = true })
                }
            }

            SelectionBox {
                Text(text = comment.content, color = Theme.color.onSurface, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    private val commentListLayout = movableComposable { modifier: Modifier, header: @Composable LazyItemScope.() -> Unit ->
        PaginationColumn(
            items = pageComments.items,
            key = { it.cid },
            state = listState,
            canRefresh = false,
            canLoading = pageComments.canLoading,
            onLoading = ::requestMoreSongComments,
            modifier = modifier,
            header = header,
            itemDivider = { HorizontalDivider() }
        ) {
            SongCommentLayout(comment = it, modifier = Modifier.fillMaxWidth().padding(Theme.padding.value9))
        }
    }

    @Composable
    private fun Portrait() {
        commentListLayout(Modifier.padding(LocalImmersivePadding.current).fillMaxSize().background(Theme.color.surface)) {
            detailsLayout(Modifier.fillMaxWidth())
            HorizontalDivider()
            resourceLayout(Modifier.fillMaxWidth())
            songCommentHeader(Modifier.fillMaxWidth().padding(Theme.padding.value9))
        }
    }

    @Composable
    private fun Landscape() {
        Row(modifier = Modifier.fillMaxSize().background(Theme.color.surface)) {
            val immersivePadding = LocalImmersivePadding.current

            if (clientSong != null) {
                detailsLayout(Modifier
                    .padding(immersivePadding.withoutEnd)
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                )
                Divider()
                resourceLayout(Modifier
                    .padding(immersivePadding.withoutHorizontal)
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                )
            }
            else {
                Column(modifier = Modifier
                    .padding(immersivePadding.withoutEnd)
                    .width(Theme.size.cell1 * 1.5f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                ) {
                    detailsLayout(Modifier.fillMaxWidth())
                    resourceLayout(Modifier.fillMaxWidth())
                }
            }
            Divider()
            commentListLayout(Modifier.padding(immersivePadding.withoutStart).weight(1f).fillMaxHeight()) {
                songCommentHeader(Modifier.fillMaxWidth().padding(Theme.padding.value9))
            }
        }
    }

    @Composable
    override fun Content() {
        val deviceType by rememberDeviceType()
        when (deviceType) {
            Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait()
            Device.Type.LANDSCAPE -> Landscape()
        }
    }

    private val resourceDialog = this land object : Dialog<Unit>() {
        private var item: ResourceItem? by mutableRefStateOf(null)

        suspend fun open(item: ResourceItem) {
            this.item = item
            awaitResult()
        }

        @Composable
        override fun Land() {
            LandDialog {
                item?.let { resource ->
                    Column(
                        modifier = Modifier.width(Theme.size.cell1 * 2f).heightIn(max = Theme.size.cell1 * 2f).padding(Theme.padding.eValue7),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            resIcon[resource.type]?.then { Icon(icon = it) }
                            SimpleEllipsisText(text = resource.type.description, style = Theme.typography.v6.bold, modifier = Modifier.weight(1f))
                            Icon(icon = Icons.Clear, onClick = ::close)
                        }

                        Box(modifier = Modifier.fillMaxWidth().weight(1f, false)) {
                            key(resource.type, modifyFlag) {
                                resAction[resource.type]?.content?.invoke(resource)
                            }
                        }
                    }
                }
            }
        }
    }

    private val configSheet = this land object : SheetContent<MusicInfo>() {
        private val name = InputState(maxLength = 32)
        private val singer = InputState(maxLength = 32)
        private val lyricist = InputState(maxLength = 32)
        private val composer = InputState(maxLength = 32)
        private val album = InputState(maxLength = 32)

        private val canSubmit by derivedStateOf {
            name.isSafe && singer.isSafe && lyricist.isSafe && composer.isSafe && album.isSafe
        }

        override suspend fun initialize(args: MusicInfo) {
            name.text = args.name
            singer.text = args.singer
            lyricist.text = args.lyricist
            composer.text = args.composer
            album.text = args.album
        }

        @Composable
        override fun Content(args: MusicInfo) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue9),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText(text = "MOD配置", style = Theme.typography.v6.bold)
                    PrimaryTextButton(text = "保存", icon = Icons.Check, enabled = canSubmit, onClick = {
                        // 更新 library
                        val newInfo = args.copy(
                            name = name.text,
                            singer = singer.text,
                            lyricist = lyricist.text,
                            composer = composer.text,
                            album = album.text,
                            modification = args.modification + 1
                        )
                        DataSourceMusic.library.findAssign(sid) { newInfo }
                        ++modifyFlag
                        // 写入文件
                        launch {
                            catching { newInfo.path(app.modPath, ModResourceType.Config).writeText(newInfo.toJsonString()) }
                        }
                        // 关闭 sheet
                        close()
                    })
                }
                Input(state = name, hint = "歌曲名", modifier = Modifier.fillMaxWidth())
                Input(state = singer, hint = "歌手", modifier = Modifier.fillMaxWidth())
                Input(state = lyricist, hint = "作词", modifier = Modifier.fillMaxWidth())
                Input(state = composer, hint = "作曲", modifier = Modifier.fillMaxWidth())
                Input(state = album, hint = "专辑", modifier = Modifier.fillMaxWidth())
            }
        }
    }

    private val commentDialog = this land DialogInput(hint = "留下你的足迹...", maxLength = 1024, maxLines = 5, minLines = 1)

    private val downloadDialog = this land DialogDownload()

    private val cropDialog = this land DialogCrop()
}
