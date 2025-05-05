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
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.common.ExtraIcons
import love.yinlin.common.ThemeValue
import love.yinlin.common.UriGenerator
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicResourceType
import love.yinlin.extension.deleteRecursively
import love.yinlin.extension.replaceAll
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.ui.component.image.LocalFileImage
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.screen.ActionScope
import love.yinlin.ui.component.screen.CommonSubScreen
import love.yinlin.ui.component.screen.FloatingDialogChoice
import love.yinlin.ui.component.screen.FloatingDialogDynamicChoice
import love.yinlin.ui.component.screen.FloatingDialogInput
import love.yinlin.ui.screen.music.loader.ScreenCreateMusic
import love.yinlin.ui.screen.music.loader.ScreenImportMusic
import love.yinlin.ui.screen.music.loader.ScreenKugouMusic
import love.yinlin.ui.screen.music.loader.ScreenNetEaseCloudMusic
import love.yinlin.ui.screen.music.loader.ScreenQQMusic

@Stable
data class MusicInfoPreview(
    val id: String,
    val name: String,
    val singer: String,
    val modification: Int = 0,
    val selected: Boolean = false
) {
    constructor(musicInfo: MusicInfo) : this(musicInfo.id, musicInfo.name, musicInfo.singer, modification = musicInfo.modification)

    @Stable
    val recordPath: Path get() = Path(OS.Storage.musicPath, this.id, MusicResourceType.Record.default.toString())
}

@Composable
private fun MusicCard(
    musicInfo: MusicInfoPreview,
    enableLongClick: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (musicInfo.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shadowElevation = ThemeValue.Shadow.Surface,
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .combinedClickable(
                onLongClick = if (enableLongClick) onLongClick else null,
                onClick = onClick
            ),
        ) {
            LocalFileImage(
                path = { musicInfo.recordPath },
                musicInfo,
                contentScale = ContentScale.Crop,
                modifier = Modifier.weight(3f).aspectRatio(1f)
            )
            Column(
                modifier = Modifier.weight(4f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = musicInfo.name,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = musicInfo.singer,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Stable
class ScreenMusicLibrary(model: AppModel) : CommonSubScreen(model) {
    private val playlistLibrary = app.config.playlistLibrary
    private var library = mutableStateListOf<MusicInfoPreview>()

    private val selectIdList: List<String> get() = library.filter { it.selected }.map { it.id }

    private val isManaging by derivedStateOf { library.any { it.selected } }
    private var isSearching by mutableStateOf(false)

    private enum class ImportMusicItem(val text: String, val icon: ImageVector, val isImage: Boolean) {
        FromMod("从MOD导入", Icons.Outlined.Token, false),
        FromLocal("本地制作", Icons.Outlined.Unarchive, false),
        FromQQMusic("QQ音乐", ExtraIcons.QQMusic, true),
        FromNetEaseCloudMusic("网易云音乐", ExtraIcons.NetEaseCloudMusic, true),
        FromKugouMusic("酷狗音乐", ExtraIcons.KugouMusic, true),
        FromGroup0("MOD工坊0群", Icons.Outlined.Extension, false),
        FromGroup1("MOD工坊1群", Icons.Outlined.Extension, false),
        FromGroup2("MOD工坊2群", Icons.Outlined.Extension, false),
    }

    private val importDialog = object : FloatingDialogChoice() {
        override val num: Int = ImportMusicItem.entries.size

        @Composable
        override fun Name(index: Int) = Text(text = ImportMusicItem.entries[index].text, modifier = Modifier.fillMaxWidth())

        @Composable
        override fun Icon(index: Int) {
            val item = ImportMusicItem.entries[index]
            if (item.isImage) MiniImage(icon = item.icon, size = ThemeValue.Size.ExtraIcon)
            else MiniIcon(icon = item.icon, size = ThemeValue.Size.ExtraIcon)
        }
    }

    private val searchDialog = FloatingDialogInput(
        hint = "歌曲名",
        maxLength = 32
    )

    private val addMusicDialog = FloatingDialogDynamicChoice("添加到歌单")

    private fun resetLibrary() {
        library.replaceAll(app.musicFactory.musicLibrary.map {
            MusicInfoPreview(it.value)
        })
    }

    private fun selectAll() {
        library.forEachIndexed { index, musicInfo ->
            if (!musicInfo.selected) library[index] = musicInfo.copy(selected = true)
        }
    }

    private fun exitManagement() {
        library.forEachIndexed { index, musicInfo ->
            if (musicInfo.selected) library[index] = musicInfo.copy(selected = false)
        }
    }

    private suspend fun openSearch() {
        val result = searchDialog.openSuspend()
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
        val item = library[index]
        if (isManaging) {
            library[index] = item.copy(selected = !item.selected)
        }
        else navigate(ScreenMusicDetails.Args(item.id))
    }

    private fun onCardLongClick(index: Int) {
        library[index] = library[index].copy(selected = true)
    }

    private suspend fun onMusicAdd() {
        val names = playlistLibrary.map { key, _ -> key }
        if (names.isNotEmpty()) {
            val result = addMusicDialog.openSuspend(names)
            if (result != null) {
                val name = names[result]
                val playlist = playlistLibrary[name]
                if (playlist != null) {
                    val addItems = selectIdList
                    val oldItems = playlist.items
                    val newItems = mutableListOf<String>()
                    for (item in addItems) {
                        if (!oldItems.contains(item)) newItems += item
                    }
                    if (newItems.isNotEmpty()) {
                        playlistLibrary[name] = playlist.copy(items = oldItems + newItems)
                        // 添加到当前播放的列表
                        val musicFactory = app.musicFactory
                        if (musicFactory.currentPlaylist?.name == name) {
                            musicFactory.addMedias(newItems
                                .filter { id -> musicFactory.musicList.find { it.id == id } == null }
                                .mapNotNull { musicFactory.musicLibrary[it] })
                        }
                        slot.tip.success("已添加${newItems.size}首歌曲")
                    }
                    else slot.tip.warning("歌曲均已存在于歌单中")
                    exitManagement()
                }
            }
        }
        else slot.tip.warning("还没有创建任何歌单哦")
    }

    private suspend fun onMusicDelete() {
        val musicFactory = app.musicFactory
        if (musicFactory.isReady) slot.tip.warning("请先停止播放器")
        else if (slot.confirm.openSuspend(content = "彻底删除曲库中这些歌曲吗")) {
            val deleteItems = selectIdList
            for (item in deleteItems) {
                val removeItem = app.musicFactory.musicLibrary.remove(item)
                if (removeItem != null) SystemFileSystem.deleteRecursively(removeItem.path)
            }
            exitManagement()
        }
    }

    override suspend fun initialize() {
        resetLibrary()
    }

    override val title: String by derivedStateOf {
        if (isManaging) "选择歌曲"
        else if (!isSearching) "曲库"
        else if (library.isEmpty()) "搜索为空"
        else "搜索结果"
    }

    override fun onBack() {
        if (isManaging) exitManagement()
        else if (isSearching) closeSearch()
        else pop()
    }

    @Composable
    override fun ActionScope.LeftActions() {
        if (isSearching) {
            Action(Icons.Outlined.Close) {
                closeSearch()
            }
        }
        if (isManaging) {
            Action(Icons.Outlined.SelectAll) {
                if (library.all { it.selected }) exitManagement()
                else selectAll()
            }
        }
    }

    @Composable
    override fun ActionScope.RightActions() {
        if (isManaging) {
            ActionSuspend(Icons.AutoMirrored.Outlined.PlaylistAdd) {
                onMusicAdd()
            }
            ActionSuspend(Icons.Outlined.Delete) {
                onMusicDelete()
            }
        }
        else {
            ActionSuspend(Icons.Outlined.Add) {
                if (app.musicFactory.isReady) slot.tip.warning("请先停止播放器")
                else {
                    when (importDialog.openSuspend()) {
                        ImportMusicItem.FromMod.ordinal -> {
                            pop()
                            navigate(ScreenImportMusic.Args(null))
                        }
                        ImportMusicItem.FromLocal.ordinal -> {
                            pop()
                            navigate<ScreenCreateMusic>()
                        }
                        ImportMusicItem.FromQQMusic.ordinal -> {
                            pop()
                            navigate(ScreenQQMusic.Args(null))
                        }
                        ImportMusicItem.FromNetEaseCloudMusic.ordinal -> {
                            pop()
                            navigate(ScreenNetEaseCloudMusic.Args(null))
                        }
                        ImportMusicItem.FromKugouMusic.ordinal -> {
                            pop()
                            navigate(ScreenKugouMusic.Args(null))
                        }
                        ImportMusicItem.FromGroup0.ordinal -> {
                            if (!OS.Application.startAppIntent(UriGenerator.qqGroup("836289670"))) slot.tip.warning("未安装QQ")
                        }
                        ImportMusicItem.FromGroup1.ordinal -> {
                            if (!OS.Application.startAppIntent(UriGenerator.qqGroup("971218639"))) slot.tip.warning("未安装QQ")
                        }
                        ImportMusicItem.FromGroup2.ordinal -> {
                            if (!OS.Application.startAppIntent(UriGenerator.qqGroup("942459444"))) slot.tip.warning("未安装QQ")
                        }
                        else -> {}
                    }
                }
            }
            ActionSuspend(Icons.Outlined.Search) {
                openSearch()
            }
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        if (library.isEmpty()) EmptyBox()
        else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(ThemeValue.Size.CellWidth),
                contentPadding = ThemeValue.Padding.EqualValue,
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
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

        LaunchedEffect(app.musicFactory.musicLibrary) {
            if (isManaging) exitManagement()
            if (!isSearching) resetLibrary()
        }
    }

    @Composable
    override fun Floating() {
        searchDialog.Land()
        addMusicDialog.Land()
        importDialog.Land()
    }
}