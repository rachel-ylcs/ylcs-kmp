package love.yinlin.ui.screen.music

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
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
import love.yinlin.extension.rememberDerivedState
import love.yinlin.extension.rememberState
import love.yinlin.extension.replaceAll
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.ui.component.image.WebImage
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
@Serializable
data object ScreenMusicLibrary : Screen<ScreenMusicLibrary.Model> {
    class Model(model: AppModel) : Screen.Model(model) {
        var library = mutableStateListOf<MusicInfoPreview>()

        val isManaging by derivedStateOf { library.any { it.selected } }
        var isSearching by mutableStateOf(false)
            private set

        val searchDialog = DialogInput(
            hint = "歌曲名",
            maxLength = 32
        )

        fun resetLibrary() {
            library.replaceAll(app.musicFactory.musicLibrary.map {
                MusicInfoPreview(it.value)
            })
        }

        fun exitManagement() {
            library.forEachIndexed { index, musicInfo ->
                if (musicInfo.selected) library[index] = musicInfo.copy(selected = false)
            }
        }

        suspend fun openSearch() {
            val result = searchDialog.open()
            if (result != null) {
                library.replaceAll(app.musicFactory.musicLibrary
                    .filter { it.value.name.contains(result, true) }
                    .map { MusicInfoPreview(it.value) })
                isSearching = true
            }
        }

        fun closeSearch() {
            resetLibrary()
            isSearching = false
        }

        fun onCardClick(index: Int) {
            if (isManaging) {
                val item = library[index]
                library[index] = item.copy(selected = !item.selected)
            }
            else {
                println("openCard $index")
            }
        }

        fun onCardLongClick(index: Int) {
            library[index] = library[index].copy(selected = true)
        }

        suspend fun onMusicAdd() {

        }

        suspend fun onMusicDelete() {

        }
    }

    override fun model(model: AppModel): Model = Model(model).apply {
        resetLibrary()
    }

    @Composable
    override fun content(model: Model) {
        val title by rememberDerivedState {
            if (model.isManaging) "选择歌曲"
            else if (!model.isSearching) "曲库"
            else if (model.library.isEmpty()) "搜索为空"
            else "搜索结果"
        }

        SubScreen(
            modifier = Modifier.fillMaxSize(),
            title = title,
            onBack = {
                if (model.isManaging) model.exitManagement()
                else if (model.isSearching) model.closeSearch()
                else model.pop()
            },
            actions = {
                if (model.isManaging) {
                    ActionSuspend(Icons.AutoMirrored.Outlined.PlaylistAdd) {
                        model.onMusicAdd()
                    }
                    ActionSuspend(Icons.Outlined.Delete) {
                        model.onMusicDelete()
                    }
                }
                else {
                    ActionSuspend(if (model.isSearching) Icons.Outlined.Close else Icons.Outlined.Search) {
                        if (model.isSearching) model.closeSearch()
                        else model.openSearch()
                    }
                }
            },
            slot = model.slot
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (app.isPortrait) 150.dp else 200.dp),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = model.library,
                    key = { index, item -> item.id }
                ) { index, item ->
                    MusicCard(
                        musicInfo = item,
                        enableLongClick = !model.isManaging,
                        onLongClick = { model.onCardLongClick(index) },
                        onClick = { model.onCardClick(index) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        model.searchDialog.withOpen()
    }
}