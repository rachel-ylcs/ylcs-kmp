package love.yinlin.screen.music

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastMap
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.compose.*
import love.yinlin.compose.screen.CommonScreen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.Data
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.extension.*
import love.yinlin.ui.component.container.TabBar
import love.yinlin.ui.component.container.Tree
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.rememberTextInputState
import love.yinlin.compose.ui.input.LoadingClickText
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.floating.FloatingDialogChoice
import love.yinlin.compose.ui.floating.FloatingDialogInput
import love.yinlin.compose.ui.floating.FloatingSheet
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.service
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

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

@Composable
private fun ReorderableCollectionItemScope.MusicStatusCard(
    musicInfo: MusicStatusPreview,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    enabledDrag: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ).padding(CustomTheme.padding.value),
        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = musicInfo.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (musicInfo.isDeleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            textDecoration = if (musicInfo.isDeleted) TextDecoration.LineThrough else null,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.weight(2f)
        )
        Text(
            text = musicInfo.singer,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        ClickIcon(
            icon = Icons.Outlined.DragHandle,
            modifier = Modifier.draggableHandle(
                enabled = enabledDrag,
                onDragStarted = { onDragStart() },
                onDragStopped = onDragEnd
            ),
            onClick = {}
        )
    }
}

@Stable
class ScreenPlaylistLibrary(manager: ScreenManager) : CommonScreen(manager) {
    private val factory = service.musicFactory.instance
    private val playlistLibrary = service.config.playlistLibrary
    private val tabs by derivedStateOf { playlistLibrary.map { key, _ -> key } }
    private var currentPage: Int by mutableIntStateOf(if (tabs.isEmpty()) -1 else 0)
    private val library = mutableStateListOf<MusicStatusPreview>()

    private suspend fun addPlaylist() {
        val name = inputPlaylistNameDialog.openSuspend()
        if (name != null) {
            if (playlistLibrary[name] == null) {
                playlistLibrary[name] = MusicPlaylist(name, emptyList())
                currentPage = tabs.indexOf(name)
            }
            else slot.tip.warning("歌单已存在")
        }
    }

    private suspend fun processPlaylist(index: Int) {
        when (processPlaylistDialog.openSuspend()) {
            0 -> {
                val oldName = tabs[index]
                val newName = inputPlaylistNameDialog.openSuspend(oldName)
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
                if (slot.confirm.openSuspend(content = "删除歌单\"$name\"")) {
                    // 若正在播放则停止播放器
                    if (factory.currentPlaylist?.name == name) factory.stop()
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
                factory.startPlaylist(playlist, null, true)
                pop()
            }
            else slot.tip.warning("歌单中还没有添加歌曲哦")
        }
    }

    private suspend fun deleteMusicFromPlaylist(index: Int) {
        val name = tabs[currentPage]
        val musicInfo = library[index]
        if (slot.confirm.openSuspend(title = "删除", content = "从歌单\"$name\"中删除\"${musicInfo.name}\"")) {
            val playlist = playlistLibrary[name]
            if (playlist != null) {
                // 若当前列表中有此歌曲则删除
                val playingIndex = factory.musicList.indexOfFirst { it.id == musicInfo.id }
                if (factory.currentPlaylist?.name == name && playingIndex != -1) factory.removeMedia(playingIndex)

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

    @Composable
    private fun PlaylistTab(modifier: Modifier = Modifier) {
        if (currentPage != -1) {
            TabBar(
                currentPage = currentPage,
                onNavigate = { currentPage = it },
                onLongClick = {
                    launch { processPlaylist(it) }
                },
                items = tabs,
                modifier = modifier
            )
        }
        else EmptyBox()
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
                    val musicLibrary = factory.musicLibrary
                    library.replaceAll(playlist.items.fastMap {
                        val musicInfo = musicLibrary[it]
                        if (musicInfo != null) MusicStatusPreview(musicInfo) else MusicStatusPreview(it)
                    })
                }
                else library.clear()
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(CustomTheme.size.cardWidth),
            state = gridState,
            modifier = modifier,
        ) {
            itemsIndexed(
                items = library,
                key = { _, item -> item.id }
            ) { index, item ->
                ReorderableItem(
                    state = reorderState,
                    key = item.id
                ) {
                    MusicStatusCard(
                        musicInfo = item,
                        onClick = { },
                        onLongClick = {
                            launch { deleteMusicFromPlaylist(index) }
                        },
                        enabledDrag = !factory.isReady,
                        onDragStart = {
                            dragStartIndex = -1
                            dragEndIndex = -1
                        },
                        onDragEnd = {
                            if (dragStartIndex != -1 && dragEndIndex != -1 && dragStartIndex != dragEndIndex) {
                                moveMusicFromPlaylist(dragStartIndex, dragEndIndex)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    private data class PlaylistPreviewItem(val id: String, val name: String)

    private fun decodePlaylist(map: Map<String, MusicPlaylist>): Map<String, List<PlaylistPreviewItem>> {
        val musicLibrary = factory.musicLibrary
        return map.mapValues { (_, playlist) ->
            playlist.items.fastMap { id -> PlaylistPreviewItem(id, musicLibrary[id]?.name ?: "未知[id=$id]") }
        }
    }

    private suspend fun downloadCloudPlaylist(): Data<Map<String, List<PlaylistPreviewItem>>> {
        val result = ClientAPI.request(
            route = API.User.Backup.DownloadPlaylist,
            data = service.config.userToken
        )
        return when (result) {
            is Data.Success -> {
                try {
                    val playlists: Map<String, MusicPlaylist> = result.data.to()
                    Data.Success(decodePlaylist(playlists))
                }
                catch (e: Throwable) {
                    Data.Failure(message = "云端歌单存在异常", throwable = e)
                }
            }
            is Data.Failure -> result
        }
    }

    override val title: String = "歌单"

    @Composable
    override fun ActionScope.LeftActions() {
        if (currentPage != -1) {
            ActionSuspend(Icons.Outlined.PlayArrow, "播放") {
                playPlaylist()
            }
        }
    }

    @Composable
    override fun ActionScope.RightActions() {
        Action(Icons.Outlined.CloudUpload, "云备份") {
            cloudBackupSheet.open()
        }
        ActionSuspend(Icons.Outlined.Add, "创建歌单") {
            addPlaylist()
        }
    }

    @Composable
    override fun Content(device: Device) {
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            PlaylistTab(modifier = Modifier.fillMaxWidth())
            PlaylistGrid(modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }

    private val cloudBackupSheet = object : FloatingSheet() {
        var playlists: Map<String, List<PlaylistPreviewItem>> by mutableRefStateOf(emptyMap())

        override suspend fun initialize() {
            playlists = emptyMap()
            val result = downloadCloudPlaylist()
            if (result is Data.Success) playlists = result.data
        }

        @Composable
        override fun Content() {
            val state = rememberTextInputState()

            Column(
                modifier = Modifier.fillMaxSize().padding(CustomTheme.padding.sheetValue),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "本地歌单", modifier = Modifier.padding(vertical = CustomTheme.padding.verticalSpace))
                TextInput(
                    state = state,
                    hint = "本地歌单(JSON格式)",
                    maxLines = 6,
                    clearButton = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LoadingClickText(
                        text = "导出",
                        icon = Icons.Outlined.Upload,
                        onClick = {
                            try {
                                state.text = playlistLibrary.items.toJsonString()
                            }
                            catch (e: Throwable) {
                                slot.tip.error(e.message ?: "导出失败")
                            }
                        }
                    )
                    LoadingClickText(
                        text = "导入",
                        icon = Icons.Outlined.Download,
                        enabled = state.ok,
                        onClick = {
                            if (!factory.isReady) {
                                if (slot.confirm.openSuspend(content = "导入会覆盖整个本地歌单且无法撤销!")) {
                                    try {
                                        val items = state.text.parseJsonValue<Map<String, MusicPlaylist>>()!!
                                        playlistLibrary.replaceAll(items)
                                        if (items.isNotEmpty()) currentPage = 0
                                        slot.tip.success("导入成功")
                                    }
                                    catch (_: Throwable) {
                                        slot.tip.error("导入格式错误")
                                    }
                                }
                            }
                            else slot.tip.warning("请先停止播放器")
                        }
                    )
                }
                HorizontalDivider()
                Text(text = "云歌单", modifier = Modifier.padding(vertical = CustomTheme.padding.verticalSpace))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LoadingClickText(
                        text = "云备份",
                        icon = Icons.Outlined.CloudUpload,
                        onClick = {
                            if (slot.confirm.openSuspend(content = "云备份会用本地歌单覆盖整个云端歌单且无法撤销!")) {
                                val result = ClientAPI.request(
                                    route = API.User.Backup.UploadPlaylist,
                                    data = API.User.Backup.UploadPlaylist.Request(
                                        token = service.config.userToken,
                                        playlist = playlistLibrary.items.toJson().Object
                                    )
                                )
                                when (result) {
                                    is Data.Success -> {
                                        playlists = decodePlaylist(playlistLibrary.items)
                                        slot.tip.success(result.message)
                                    }
                                    is Data.Failure -> slot.tip.error(result.message)
                                }
                            }
                        }
                    )
                    LoadingClickText(
                        text = "云恢复",
                        icon = Icons.Outlined.CloudDownload,
                        onClick = {
                            if (playlists.isNotEmpty()) {
                                if (!factory.isReady) {
                                    if (slot.confirm.openSuspend(content = "云恢复会用云端歌单覆盖整个本地歌单且无法撤销!")) {
                                        val items = playlists.mapValues { (name, value) ->
                                            MusicPlaylist(name, value.fastMap { it.id })
                                        }
                                        playlistLibrary.replaceAll(items)
                                        if (items.isNotEmpty()) currentPage = 0
                                        slot.tip.success("云恢复成功")
                                    }
                                }
                                else slot.tip.warning("请先停止播放器")
                            }
                        }
                    )
                }
                if (playlists.isNotEmpty()) {
                    Tree(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .horizontalScroll(rememberScrollState())
                            .verticalScroll(rememberScrollState())
                    ) {
                        Node(text = "云备份歌单") {
                            for ((name, playlist) in playlists) {
                                Node(text = name) {
                                    for (id in playlist) {
                                        Node(text = id.name, icon = Icons.Outlined.MusicNote)
                                    }
                                }
                            }
                        }
                    }
                }
                else {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        EmptyBox()
                    }
                }
            }
        }
    }

    private val inputPlaylistNameDialog = FloatingDialogInput(hint = "歌单名", maxLength = 16)

    private val processPlaylistDialog = FloatingDialogChoice.fromIconItems(
        items = listOf(
            "重命名" to Icons.Outlined.Edit,
            "删除" to Icons.Outlined.Delete
        )
    )

    @Composable
    override fun Floating() {
        cloudBackupSheet.Land()
        inputPlaylistNameDialog.Land()
        processPlaylistDialog.Land()
    }
}