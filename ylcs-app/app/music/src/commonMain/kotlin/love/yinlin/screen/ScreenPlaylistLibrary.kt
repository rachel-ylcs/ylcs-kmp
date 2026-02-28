package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import love.yinlin.app
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.collection.TreeView
import love.yinlin.compose.ui.floating.DialogChoice
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.Sheet
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.input.PrimaryLoadingButton
import love.yinlin.compose.ui.input.SecondaryLoadingButton
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.layout.Space
import love.yinlin.compose.ui.navigation.TabBar
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.rememberInputState
import love.yinlin.cs.ApiBackupDownloadPlaylist
import love.yinlin.cs.ApiBackupUploadPlaylist
import love.yinlin.cs.request
import love.yinlin.data.Data
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.extension.Object
import love.yinlin.extension.catchingError
import love.yinlin.extension.catchingNull
import love.yinlin.extension.lazyProvider
import love.yinlin.extension.moveItem
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.replaceAll
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.extension.toJsonString
import love.yinlin.startup.StartupMusicPlayer
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Stable
class ScreenPlaylistLibrary : Screen() {
    @Stable
    private data class MusicStatusPreview(
        val id: String,
        val name: String,
        val singer: String,
        val isDeleted: Boolean
    ) {
        constructor(musicInfo: MusicInfo) : this(musicInfo.id, musicInfo.name, musicInfo.singer, false)
        constructor(id: String): this(id, "[$id]", "", true)
    }

    private val mp by lazyProvider { app.startup<StartupMusicPlayer>() }

    private val playlistLibrary = app.config.playlistLibrary
    private val tabs by derivedStateOf { playlistLibrary.keys }
    private var currentPage: Int by mutableIntStateOf(if (playlistLibrary.isEmpty) -1 else 0)
    private val library = mutableStateListOf<MusicStatusPreview>()

    private suspend fun addPlaylist() {
        inputPlaylistNameDialog.open()?.let { name ->
            if (playlistLibrary[name] == null) {
                playlistLibrary[name] = MusicPlaylist(name, emptyList())
                currentPage = tabs.indexOf(name)
            }
            else slot.tip.warning("歌单已存在")
        }
    }

    private suspend fun processPlaylist(index: Int) {
        when (processPlaylistDialog.open()) {
            0 -> {
                val oldName = tabs[index]
                val newName = inputPlaylistNameDialog.open(oldName)
                if (newName != null) {
                    if (playlistLibrary[newName] == null) {
                        playlistLibrary.renameKey(oldName, newName) {
                            it.copy(name = newName)
                        }
                        currentPage = tabs.indexOf(newName)
                    }
                    else slot.tip.warning("歌单已存在")
                }
            }
            1 -> {
                val name = tabs[index]
                if (slot.confirm.open(content = "删除歌单\"$name\"")) {
                    // 若正在播放则停止播放器
                    mp?.let {
                        if (it.playlist?.name == name) it.stop()
                    }

                    playlistLibrary -= name
                    if (currentPage == tabs.size) currentPage = if (tabs.isEmpty()) -1 else currentPage - 1
                }
            }
        }
    }

    private suspend fun playPlaylist() {
        val name = tabs[currentPage]
        val playlist = playlistLibrary[name]
        if (playlist != null) {
            if (playlist.items.isNotEmpty()) {
                mp?.startPlaylist(playlist, null, true)
                pop()
            }
            else slot.tip.warning("歌单中还没有添加歌曲哦")
        }
    }

    private suspend fun deleteMusicFromPlaylist(index: Int) {
        val name = tabs[currentPage]
        val musicInfo = library[index]
        if (slot.confirm.open(title = "删除", content = "从歌单\"$name\"中删除\"${musicInfo.name}\"")) {
            val playlist = playlistLibrary[name]
            if (playlist != null) {
                // 若当前列表中有此歌曲则删除
                mp?.let {
                    val playingIndex = it.musicList.indexOfFirst { info -> info.id == musicInfo.id }
                    if (it.playlist?.name == name && playingIndex != -1) it.removeMedia(playingIndex)
                }

                val newItems = playlist.items.toMutableList()
                newItems -= musicInfo.id
                playlistLibrary[name] = playlist.copy(items = newItems)
                library.removeAt(index)
            }
        }
    }

    private fun moveMusicFromPlaylist(fromIndex: Int, toIndex: Int) {
        val name = tabs[currentPage]
        val playlist = playlistLibrary[name]
        if (playlist != null) {
            val newItems = playlist.items.toMutableList()
            newItems.moveItem(fromIndex, toIndex)
            playlistLibrary[name] = playlist.copy(items = newItems)
        }
    }

    private data class PlaylistPreviewItem(val id: String, val name: String)

    private fun decodePlaylist(map: Map<String, MusicPlaylist>): Map<String, List<PlaylistPreviewItem>> {
        return map.mapValues { (_, playlist) ->
            playlist.items.fastMap { id -> PlaylistPreviewItem(id, mp?.library[id]?.name ?: "未知[id=$id]") }
        }
    }

    private suspend fun downloadCloudPlaylist(): Map<String, List<PlaylistPreviewItem>>? {
        if (app.config.userToken.isEmpty()) slot.tip.warning("请先登录")
        else when (val result = ApiBackupDownloadPlaylist.request(app.config.userToken)) {
            is Data.Success -> {
                val playlist = catchingNull { decodePlaylist(result.data.o1.to()) }
                if (playlist == null) slot.tip.error("云端歌单存在异常")
                return playlist
            }
            is Data.Failure -> slot.tip.error(result.throwable.message)
        }
        return null
    }

    override val title: String = "歌单"

    @Composable
    private fun PlaylistTab(modifier: Modifier = Modifier) {
        TabBar(
            size = tabs.size,
            index = currentPage,
            onNavigate = { currentPage = it },
            titleProvider = { tabs[it] },
            onLongClick = { launch { processPlaylist(it) } },
            modifier = modifier
        )
    }

    @Composable
    private fun PlaylistGrid(modifier: Modifier = Modifier) {
        var dragStartIndex = remember { -1 }
        var dragEndIndex = remember { -1 }
        val gridState = rememberLazyGridState()
        val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
            if (dragStartIndex == -1) dragStartIndex = from.index
            dragEndIndex = to.index
            library.moveItem(from.index, to.index)
        }

        LaunchedEffect(currentPage, tabs) {
            if (currentPage == -1) library.clear()
            else {
                val playlist = playlistLibrary[tabs[currentPage]]
                if (playlist != null) {
                    library.replaceAll(playlist.items.fastMap {
                        val musicInfo = mp?.library[it]
                        if (musicInfo != null) MusicStatusPreview(musicInfo) else MusicStatusPreview(it)
                    })
                }
                else library.clear()
            }
        }

        if (library.isEmpty()) {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9, Alignment.CenterVertically),
            ) {
                SimpleEllipsisText(text = "快去曲库添加歌曲吧~", style = Theme.typography.v6)
                PrimaryLoadingButton(text = "曲库", icon = Icons.LibraryMusic, onClick = {
                    pop()
                    navigate(::ScreenMusicLibrary)
                })
            }
        }
        else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.cell1),
                state = gridState,
                modifier = modifier,
            ) {
                itemsIndexed(
                    items = library,
                    key = { _, item -> item.id }
                ) { index, item ->
                    ReorderableItem(state = reorderState, key = item.id) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (mp?.isReady == true) slot.tip.warning("调整歌曲顺序需要停止播放器")
                            }.padding(Theme.padding.value),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.g5),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SimpleEllipsisText(
                                text = item.name,
                                style = Theme.typography.v7.bold,
                                color = if (item.isDeleted) Theme.color.error else LocalColor.current,
                                textDecoration = if (item.isDeleted) TextDecoration.LineThrough else null,
                                textAlign = TextAlign.Start,
                                overflow = TextOverflow.MiddleEllipsis,
                                modifier = Modifier.weight(2f)
                            )
                            SimpleEllipsisText(
                                text = item.singer,
                                style = Theme.typography.v8,
                                textAlign = TextAlign.End,
                                overflow = TextOverflow.MiddleEllipsis,
                                color = LocalColorVariant.current,
                                modifier = Modifier.weight(1f)
                            )
                            LoadingIcon(icon = Icons.Delete, tip = "删除", onClick = { deleteMusicFromPlaylist(index) })
                            Icon(icon = Icons.DragHandle, modifier = Modifier.draggableHandle(
                                enabled = mp?.isReady != true,
                                onDragStarted = {
                                    dragStartIndex = -1
                                    dragEndIndex = -1
                                },
                                onDragStopped = {
                                    if (dragStartIndex != -1 && dragEndIndex != -1 && dragStartIndex != dragEndIndex) {
                                        moveMusicFromPlaylist(dragStartIndex, dragEndIndex)
                                    }
                                }
                            ))
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun RowScope.LeftActions() {
        if (currentPage != -1) LoadingIcon(icon = Icons.PlayArrow, tip = "播放", onClick = ::playPlaylist)
    }

    @Composable
    override fun RowScope.RightActions() {
        Icon(icon = Icons.CloudUpload, tip = "云备份", onClick = cloudBackupSheet::open)
        LoadingIcon(icon = Icons.Add, tip = "创建歌单", onClick = ::addPlaylist)
    }

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (currentPage != -1) {
                PlaylistTab(modifier = Modifier.fillMaxWidth())
                PlaylistGrid(modifier = Modifier.fillMaxWidth().weight(1f))
            }
            else {
                SimpleEllipsisText(text = "你还未创建歌单呢~", style = Theme.typography.v6)
                Space(Theme.padding.v9)
                PrimaryLoadingButton(text = "创建", icon = Icons.Add, onClick = ::addPlaylist)
            }
        }
    }

    private val cloudBackupSheet = this land object : Sheet() {
        override val scrollable: Boolean = false

        var playlists: Map<String, List<PlaylistPreviewItem>> by mutableRefStateOf(emptyMap())

        override suspend fun initialize() {
            playlists = emptyMap()
            playlists = downloadCloudPlaylist() ?: return
        }

        @Composable
        override fun Content() {
            val state = rememberInputState()

            Column(
                modifier = Modifier.fillMaxSize().padding(Theme.padding.eValue9),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
            ) {
                SimpleEllipsisText(text = "本地歌单", style = Theme.typography.v6.bold)
                Input(
                    state = state,
                    hint = "本地歌单(JSON格式)",
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PrimaryLoadingButton(text = "导出", icon = Icons.Upload, onClick = {
                        catchingError { state.text = playlistLibrary.items.toJsonString() }?.let {
                            slot.tip.error(it.message ?: "导出失败")
                        }
                    })
                    PrimaryLoadingButton(text = "导入", icon = Icons.Download, enabled = state.isSafe, onClick = {
                        if (mp?.isReady == true) slot.tip.warning("导入歌单需要先停止播放器")
                        else {
                            if (slot.confirm.open(content = "导入会覆盖整个本地歌单且无法撤销!")) {
                                catchingError {
                                    val items = state.text.parseJsonValue<Map<String, MusicPlaylist>>()
                                    playlistLibrary.replaceAll(items)
                                    if (items.isNotEmpty()) currentPage = 0
                                    slot.tip.success("导入成功")
                                }?.let {
                                    slot.tip.error("导入格式错误")
                                }
                            }
                        }
                    })
                }
                Divider()
                SimpleEllipsisText(text = "云歌单", style = Theme.typography.v6.bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SecondaryLoadingButton(text = "云备份", icon = Icons.CloudUpload, onClick = {
                        if (app.config.userToken.isEmpty()) slot.tip.warning("请先登录")
                        else if (slot.confirm.open(content = "云备份会用本地歌单覆盖整个云端歌单且无法撤销!")) {
                            val localPlaylists = catchingNull { playlistLibrary.items.toJson().Object }
                            if (localPlaylists != null) {
                                ApiBackupUploadPlaylist.request(app.config.userToken, localPlaylists) {
                                    playlists = decodePlaylist(playlistLibrary.items)
                                    slot.tip.success("备份成功")
                                }.errorTip
                            }
                            else slot.tip.error("本地歌单格式异常")
                        }
                    })
                    SecondaryLoadingButton(text = "云恢复", icon = Icons.CloudDownload, onClick = {
                        if (playlists.isNotEmpty()) {
                            if (mp?.isReady == true) slot.tip.warning("导入歌单需要先停止播放器")
                            else {
                                if (slot.confirm.open(content = "云恢复会用云端歌单覆盖整个本地歌单且无法撤销!")) {
                                    val items = playlists.mapValues { (name, value) ->
                                        MusicPlaylist(name, value.fastMap { it.id })
                                    }
                                    playlistLibrary.replaceAll(items)
                                    if (items.isNotEmpty()) currentPage = 0
                                    slot.tip.success("云恢复成功")
                                }
                            }
                        }
                    })
                }
                if (playlists.isNotEmpty()) {
                    TreeView(
                        modifier = Modifier.fillMaxWidth().weight(1f).horizontalScroll(rememberScrollState()).verticalScroll(rememberScrollState()),
                        indent = 0.dp
                    ) {
                        for ((name, playlist) in playlists) {
                            TreeNode(text = name) {
                                for (id in playlist) TreeNode(text = id.name, icon = Icons.MusicNote)
                            }
                        }
                    }
                }
                else {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        SimpleEllipsisText(text = "云端未存储歌单~", style = Theme.typography.v6)
                    }
                }
            }
        }
    }

    private val inputPlaylistNameDialog = this land DialogInput(hint = "歌单名", maxLength = 16)

    private val processPlaylistDialog = this land DialogChoice.fromIconItems(
        items = listOf("重命名" to Icons.Edit, "删除" to Icons.Delete)
    )
}