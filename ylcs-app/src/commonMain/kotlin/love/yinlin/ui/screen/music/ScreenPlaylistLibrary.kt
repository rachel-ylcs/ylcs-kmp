package love.yinlin.ui.screen.music

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.ThemeColor
import love.yinlin.data.Data
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.extension.Object
import love.yinlin.extension.moveItem
import love.yinlin.extension.replaceAll
import love.yinlin.extension.to
import love.yinlin.extension.toJson
import love.yinlin.platform.app
import love.yinlin.ui.component.container.Tree
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.input.LoadingRachelButton
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.TabBar
import love.yinlin.ui.component.screen.BottomSheet
import love.yinlin.ui.component.screen.CommonSheetState
import love.yinlin.ui.component.screen.DialogChoice
import love.yinlin.ui.component.screen.DialogInput
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

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
        ).padding(horizontal = 15.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = musicInfo.name,
            style = MaterialTheme.typography.titleMedium,
            color = if (musicInfo.isDeleted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (musicInfo.isDeleted) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = musicInfo.singer,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
            color = ThemeColor.fade
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
class ScreenPlaylistLibrary(model: AppModel) : Screen<ScreenPlaylistLibrary.Args>(model) {
    @Stable
    @Serializable
    data object Args : Screen.Args

    private val playlistLibrary = app.config.playlistLibrary
    private val tabs by derivedStateOf { playlistLibrary.map { key, _ -> key } }
    private var currentPage: Int by mutableStateOf(if (tabs.isEmpty()) -1 else 0)
    private val library = mutableStateListOf<MusicStatusPreview>()

    private val cloudBackupSheet = CommonSheetState()

    private val inputPlaylistNameDialog = DialogInput(
        hint = "歌单名",
        maxLength = 16
    )

    private val processPlaylistDialog = DialogChoice.fromIconItems(
        items = listOf(
            "重命名" to Icons.Outlined.Edit,
            "删除" to Icons.Outlined.Delete
        )
    )

    private suspend fun addPlaylist() {
        val name = inputPlaylistNameDialog.open()
        if (name != null) {
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
                    val musicFactory = app.musicFactory
                    if (musicFactory.currentPlaylist?.name == name) musicFactory.stop()
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
                app.musicFactory.startPlaylist(playlist)
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
                val musicFactory = app.musicFactory
                val playingIndex = musicFactory.musicList.indexOfFirst { it.id == musicInfo.id }
                if (musicFactory.currentPlaylist?.name == name && playingIndex != -1) musicFactory.removeMedia(playingIndex)

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
                    val musicLibrary = app.musicFactory.musicLibrary
                    library.replaceAll(playlist.items.map {
                        val musicInfo = musicLibrary[it]
                        if (musicInfo != null) MusicStatusPreview(musicInfo) else MusicStatusPreview(it)
                    })
                }
                else library.clear()
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(300.dp),
            state = gridState,
            modifier = modifier,
        ) {
            itemsIndexed(
                items = library,
                key = { index, item -> item.id }
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
                        enabledDrag = !app.musicFactory.isReady,
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
        val musicLibrary = app.musicFactory.musicLibrary
        return map.mapValues { (_, playlist) ->
            playlist.items.map { id -> PlaylistPreviewItem(id, musicLibrary[id]?.name ?: "未知[id=$id]") }
        }
    }

    private suspend fun downloadCloudPlaylist(): Data<Map<String, List<PlaylistPreviewItem>>> {
        val result = ClientAPI.request(
            route = API.User.Backup.DownloadPlaylist,
            data = app.config.userToken
        )
        return when (result) {
            is Data.Success -> {
                try {
                    val playlists: Map<String, MusicPlaylist> = result.data.to()
                    Data.Success(decodePlaylist(playlists))
                }
                catch (e: Throwable) {
                    Data.Error(message = "云端歌单存在异常", throwable = e)
                }
            }
            is Data.Error -> result
        }
    }

    @Composable
    private fun CloudBackupLayout() {
        var playlists: Map<String, List<PlaylistPreviewItem>> by mutableStateOf(emptyMap())

        BottomSheet(state = cloudBackupSheet) {
            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction = 0.6f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LoadingRachelButton(
                        text = "云备份",
                        icon = Icons.Outlined.CloudUpload,
                        onClick = {
                            if (slot.confirm.open(content = "云备份会用本地歌单覆盖整个云端歌单且无法撤销!")) {
                                val result = ClientAPI.request(
                                    route = API.User.Backup.UploadPlaylist,
                                    data = API.User.Backup.UploadPlaylist.Request(
                                        token = app.config.userToken,
                                        playlist = playlistLibrary.toMap().toJson().Object
                                    )
                                )
                                when (result) {
                                    is Data.Success -> {
                                        playlists = decodePlaylist(playlistLibrary.toMap())
                                        slot.tip.success(result.message)
                                    }
                                    is Data.Error -> slot.tip.error(result.message)
                                }
                            }
                        }
                    )
                    LoadingRachelButton(
                        text = "云恢复",
                        icon = Icons.Outlined.CloudDownload,
                        onClick = {
                            if (playlists.isNotEmpty()) {
                                if (!app.musicFactory.isReady) {
                                    if (slot.confirm.open(content = "云恢复会用云端歌单覆盖整个本地歌单且无法撤销!")) {
                                        playlistLibrary.replaceAll(playlists.mapValues { (name, value) ->
                                            MusicPlaylist(name, value.map { it.id })
                                        })
                                        slot.tip.success("云恢复成功")
                                    }
                                }
                                else slot.tip.warning("恢复歌单需要先停止播放器")
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

        LaunchedEffect(Unit) {
            val result = downloadCloudPlaylist()
            when (result) {
                is Data.Success -> playlists = result.data
                is Data.Error -> slot.tip.error(result.message)
            }
        }
    }

    @Composable
    override fun content() {
        SubScreen(
            modifier = Modifier.fillMaxSize(),
            title = "歌单",
            onBack = { pop() },
            leftActions = {
                if (currentPage != -1) {
                    ActionSuspend(Icons.Outlined.PlayArrow) {
                        playPlaylist()
                    }
                }
            },
            actions = {
                Action(Icons.Outlined.CloudUpload) {
                    if (app.config.userProfile?.hasPrivilegeBackup == true) {
                        cloudBackupSheet.open()
                    }
                }
                ActionSuspend(Icons.Outlined.Add) {
                    addPlaylist()
                }
            },
            slot = slot
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PlaylistTab(modifier = Modifier.fillMaxWidth())
                PlaylistGrid(modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }

        cloudBackupSheet.withOpen {
            CloudBackupLayout()
        }

        inputPlaylistNameDialog.withOpen()
        processPlaylistDialog.withOpen()
    }
}