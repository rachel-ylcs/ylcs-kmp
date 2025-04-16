package love.yinlin.ui.screen.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import love.yinlin.common.ThemeColor
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.extension.rememberValueState
import love.yinlin.extension.replaceAll
import love.yinlin.platform.app
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.layout.TabBar
import love.yinlin.ui.component.screen.DialogChoice
import love.yinlin.ui.component.screen.DialogInput
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Stable
data class MusicStatusPreview(
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
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 10.dp),
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
                onDragStarted = { onDragStart() },
                onDragStopped = onDragEnd
            ),
            onClick = {}
        )
    }
}

@Stable
@Serializable
data object ScreenPlaylistLibrary : Screen<ScreenPlaylistLibrary.Model> {
    class Model(model: AppModel) : Screen.Model(model) {
        val playlistLibrary = app.config.playlistLibrary
        val tabs by derivedStateOf { playlistLibrary.map { key, _ -> key } }
        var currentPage: Int by mutableStateOf(if (tabs.isEmpty()) -1 else 0)
        val library = mutableStateListOf<MusicStatusPreview>()

        val inputPlaylistNameDialog = DialogInput(
            hint = "歌单名",
            maxLength = 16
        )

        val processPlaylistDialog = DialogChoice.fromIconItems(
            items = listOf(
                "重命名" to Icons.Outlined.Edit,
                "删除" to Icons.Outlined.Delete
            )
        )

        suspend fun addPlaylist() {
            val name = inputPlaylistNameDialog.open()
            if (name != null) {
                if (app.config.playlistLibrary[name] == null) {
                    app.config.playlistLibrary[name] = MusicPlaylist(name, emptyList())
                    currentPage = tabs.indexOf(name)
                }
                else slot.tip.warning("歌单已存在")
            }
        }

        suspend fun processPlaylist(index: Int) {
            when (processPlaylistDialog.open()) {
                0 -> {
                    val oldName = tabs[index]
                    val newName = inputPlaylistNameDialog.open(oldName)
                    if (newName != null) {
                        if (app.config.playlistLibrary[newName] == null) {
                            app.config.playlistLibrary.renameKey(oldName, newName) {
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
                        app.config.playlistLibrary -= name
                        // TODO: Music需要检查当前歌单是否在播放
                    }
                }
            }
        }

        suspend fun playPlaylist() {
            val name = tabs[currentPage]
            val playlist = app.config.playlistLibrary[name]
            if (playlist != null) app.musicFactory.startPlaylist(playlist)
        }

        private fun deleteMusicFromPlaylist(index: Int) {
            launch {
                if (slot.confirm.open(title = "删除", content = "从歌单\"${tabs[currentPage]}\"中删除\"${library[index].name}\"")) {

                }
            }
        }

        private fun moveMusicFromPlaylist(fromIndex: Int, toIndex: Int) {

        }

        @Composable
        fun PlaylistTab(modifier: Modifier = Modifier) {
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
        }

        @Composable
        fun PlaylistGrid(modifier: Modifier = Modifier) {
            var dragStartIndex by rememberValueState(-1)
            var dragEndIndex by rememberValueState(-1)
            val gridState = rememberLazyGridState()
            val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
                if (dragStartIndex == -1) dragStartIndex = from.index
                dragEndIndex = to.index
                library.add(to.index, library.removeAt(from.index))
            }

            LaunchedEffect(currentPage, tabs) {
                if (currentPage == -1) library.clear()
                else {
                    val playlist = app.config.playlistLibrary[tabs[currentPage]]
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
                            onClick = { deleteMusicFromPlaylist(index) },
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
    }

    override fun model(model: AppModel): Model = Model(model)

    @Composable
    override fun content(model: Model) {
        SubScreen(
            modifier = Modifier.fillMaxSize(),
            title = "歌单",
            onBack = { model.pop() },
            actions = {
                Action(Icons.Outlined.Add) {
                    model.launch {
                        model.addPlaylist()
                    }
                }
                if (model.currentPage != -1) {
                    ActionSuspend(Icons.Outlined.PlayArrow) {
                        model.playPlaylist()
                    }
                }
            },
            slot = model.slot
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                model.PlaylistTab(modifier = Modifier.fillMaxWidth())
                model.PlaylistGrid(modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }

        model.inputPlaylistNameDialog.withOpen()
        model.processPlaylistDialog.withOpen()
    }
}