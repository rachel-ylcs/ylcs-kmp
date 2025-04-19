package love.yinlin.ui.screen.music

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicResourceType
import love.yinlin.extension.deleteRecursively
import love.yinlin.extension.rememberDerivedState
import love.yinlin.extension.rememberState
import love.yinlin.extension.replaceAll
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.platform.path
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.screen.DialogDynamicChoice
import love.yinlin.ui.component.screen.DialogInput
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen

@Stable
data class MusicInfoPreview(
    val id: String,
    val name: String,
    val singer: String,
    val selected: Boolean = false
) {
    constructor(musicInfo: MusicInfo) : this(musicInfo.id, musicInfo.name, musicInfo.singer)

    @Stable
    val recordPath get(): Path = Path(OS.Storage.musicPath, this.id, MusicResourceType.Record.defaultFilename)
}

@Composable
private fun MusicCard(
    musicInfo: MusicInfoPreview,
    enableLongClick: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recordUri by rememberState(musicInfo) {
        // 因为 KMP 文件系统太烂了, 并没有支持到文件修改时间的读取, 这里临时使用文件大小来标识修改标记
        val metadata = SystemFileSystem.metadataOrNull(musicInfo.recordPath)
        "${musicInfo.recordPath}?recordKey=${metadata?.size}"
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (musicInfo.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .combinedClickable(
                onLongClick = if (enableLongClick) onLongClick else null,
                onClick = onClick
            ),
        ) {
            WebImage(
                uri = recordUri,
                contentScale = ContentScale.Fit,
                modifier = Modifier.weight(3f).aspectRatio(1f)
            )
            Column(
                modifier = Modifier.weight(4f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = musicInfo.name,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = musicInfo.singer,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Stable
class ScreenMusicLibrary(model: AppModel) : Screen<ScreenMusicLibrary.Args>(model) {
    @Stable
    @Serializable
    data object Args : Screen.Args

    private val playlistLibrary = app.config.playlistLibrary
    private var library = mutableStateListOf<MusicInfoPreview>()

    private val selectIdList: List<String> get() = library.filter { it.selected }.map { it.id }

    private val isManaging by derivedStateOf { library.any { it.selected } }
    private var isSearching by mutableStateOf(false)
        private set

    private val searchDialog = DialogInput(
        hint = "歌曲名",
        maxLength = 32
    )

    private val addMusicDialog = DialogDynamicChoice("添加到歌单")

    private fun resetLibrary() {
        library.replaceAll(app.musicFactory.musicLibrary.map {
            MusicInfoPreview(it.value)
        })
    }

    private fun exitManagement() {
        library.forEachIndexed { index, musicInfo ->
            if (musicInfo.selected) library[index] = musicInfo.copy(selected = false)
        }
    }

    private suspend fun openSearch() {
        val result = searchDialog.open()
        if (result != null) {
            library.replaceAll(app.musicFactory.musicLibrary
                .filter { it.value.name.contains(result, true) }
                .map { MusicInfoPreview(it.value) })
            isSearching = true
        }
    }

    private fun closeSearch() {
        resetLibrary()
        isSearching = false
    }

    private fun onCardClick(index: Int) {
        if (isManaging) {
            val item = library[index]
            library[index] = item.copy(selected = !item.selected)
        }
        else {

        }
    }

    private fun onCardLongClick(index: Int) {
        library[index] = library[index].copy(selected = true)
    }

    private suspend fun onMusicAdd() {
        val names = playlistLibrary.map { key, _ -> key }
        if (names.isNotEmpty()) {
            val result = addMusicDialog.open(names)
            if (result != null) {
                val name = names[result]
                val playlist = playlistLibrary[name]
                if (playlist != null) {
                    val addItems = selectIdList
                    exitManagement()
                    val oldItems = playlist.items
                    val newItems = mutableListOf<String>()
                    for (item in addItems) {
                        if (!oldItems.contains(item)) {
                            newItems += item
                        }
                    }
                    if (newItems.isNotEmpty()) {
                        playlistLibrary[name] = playlist.copy(items = oldItems + newItems)
                        // TODO: 检查是否在播放列表中
                        slot.tip.success("已添加${newItems.size}首歌曲")
                    }
                    else slot.tip.warning("歌曲均已存在于歌单中")
                }
            }
        }
        else slot.tip.warning("还没有创建任何歌单哦")
    }

    private suspend fun onMusicDelete() {
        if (slot.confirm.open(content = "彻底删除曲库中这些歌曲吗")) {
            val deleteItems = selectIdList
            exitManagement()
            // TODO: 检查是否在播放列表中
            for (item in deleteItems) {
                val removeItem = app.musicFactory.musicLibrary.remove(item)
                if (removeItem != null) {
                    SystemFileSystem.deleteRecursively(removeItem.path)
                }
            }
        }
    }

    override suspend fun initialize() {
        resetLibrary()
    }

    @Composable
    override fun content() {
        val title by rememberDerivedState {
            if (isManaging) "选择歌曲"
            else if (!isSearching) "曲库"
            else if (library.isEmpty()) "搜索为空"
            else "搜索结果"
        }

        SubScreen(
            modifier = Modifier.fillMaxSize(),
            title = title,
            onBack = {
                if (isManaging) exitManagement()
                else if (isSearching) closeSearch()
                else pop()
            },
            actions = {
                if (isManaging) {
                    ActionSuspend(Icons.AutoMirrored.Outlined.PlaylistAdd) {
                        onMusicAdd()
                    }
                    ActionSuspend(Icons.Outlined.Delete) {
                        onMusicDelete()
                    }
                }
                else {
                    Action(Icons.Outlined.Add) {
                        navigate(ScreenImportMusic.Args(null))
                    }
                    ActionSuspend(if (isSearching) Icons.Outlined.Close else Icons.Outlined.Search) {
                        if (isSearching) closeSearch()
                        else openSearch()
                    }
                }
            },
            slot = slot
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (app.isPortrait) 150.dp else 200.dp),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = library,
                    key = { index, item -> item.id }
                ) { index, item ->
                    MusicCard(
                        musicInfo = item,
                        enableLongClick = !isManaging,
                        onLongClick = { onCardLongClick(index) },
                        onClick = { onCardClick(index) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        searchDialog.withOpen()
        addMusicDialog.withOpen()
    }
}