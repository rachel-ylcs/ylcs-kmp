package love.yinlin.ui.screen.music

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.AppModel
import love.yinlin.common.ExtraIcons
import love.yinlin.compose.*
import love.yinlin.data.MimeType
import love.yinlin.data.mod.ModInfo
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicResourceType
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.DateEx
import love.yinlin.extension.deleteRecursively
import love.yinlin.extension.replaceAll
import love.yinlin.mod.ModFactory
import love.yinlin.platform.OS
import love.yinlin.platform.Picker
import love.yinlin.platform.app
import love.yinlin.ui.component.image.LocalFileImage
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.layout.ActionScope
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.screen.*
import love.yinlin.ui.screen.music.loader.ScreenCreateMusic
import love.yinlin.ui.screen.music.loader.ScreenImportMusic
import love.yinlin.ui.screen.music.loader.ScreenPlatformMusic

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
        color = if (musicInfo.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shadowElevation = CustomTheme.shadow.miniSurface,
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .combinedClickable(
                onLongClick = if (enableLongClick) onLongClick else null,
                onClick = onClick
            ),
        ) {
            Box(modifier = Modifier.aspectRatio(1f).fillMaxHeight()) {
                LocalFileImage(
                    path = { musicInfo.recordPath },
                    musicInfo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(CustomTheme.padding.extraValue),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalExtraSpace)
            ) {
                Text(
                    text = musicInfo.name,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = musicInfo.singer,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
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

    private val selectIdList: List<String> get() = library.fastFilter { it.selected }.fastMap { it.id }

    private val isManaging by derivedStateOf { library.any { it.selected } }
    private var isSearching by mutableStateOf(false)

    @Stable
    private enum class ImportMusicItem(val text: String, val icon: ImageVector, val isImage: Boolean) {
        FromFactory("MOD工坊", Icons.Outlined.Factory, false),
        FromMod("导入MOD", Icons.Outlined.Extension, false),
        FromLocal("本地制作", Icons.Outlined.Unarchive, false),
        FromQQMusic("QQ音乐", ExtraIcons.QQMusic, true),
        FromNetEaseCloudMusic("网易云音乐", ExtraIcons.NetEaseCloudMusic, true),
        FromKugouMusic("酷狗音乐", ExtraIcons.KugouMusic, true),
    }

    private fun resetLibrary() {
        library.replaceAll(app.musicFactory.musicLibrary.map {
            MusicInfoPreview(it.value)
        })
    }

    private fun selectAll() {
        library.fastForEachIndexed { index, musicInfo ->
            if (!musicInfo.selected) library[index] = musicInfo.copy(selected = true)
        }
    }

    private fun exitManagement() {
        library.fastForEachIndexed { index, musicInfo ->
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
                        if (item !in oldItems) newItems += item
                    }
                    if (newItems.isNotEmpty()) {
                        playlistLibrary[name] = playlist.copy(items = oldItems + newItems)
                        // 添加到当前播放的列表
                        val musicFactory = app.musicFactory
                        if (musicFactory.currentPlaylist?.name == name) {
                            musicFactory.addMedias(newItems
                                .fastFilter { id -> musicFactory.musicList.find { it.id == id } == null }
                                .fastMapNotNull { musicFactory.musicLibrary[it] })
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
            resetLibrary()
        }
    }

    private suspend fun onMusicPackage() {
        val musicFactory = app.musicFactory
        if (musicFactory.isReady) slot.tip.warning("请先停止播放器")
        else Picker.savePath("${DateEx.CurrentLong}.rachel", MimeType.BINARY, "*.rachel")?.let { path ->
            try {
                slot.loading.openSuspend()
                path.sink.use { sink ->
                    val packageItems = selectIdList
                    ModFactory.Merge(
                        mediaPaths = packageItems.fastMapNotNull { musicFactory.musicLibrary[it]?.path },
                        sink = sink,
                        info = ModInfo(author = app.config.userProfile?.name ?: "无名")
                    ).process { _, _, _ -> }
                }
                slot.loading.close()
                exitManagement()
                slot.tip.success("导出成功")
            }
            catch (_: Throwable) {
                slot.tip.warning("无法导出MOD")
            }
        }
    }

    override suspend fun initialize() {
        resetLibrary()
        monitor(state = { app.musicFactory.musicLibrary }) {
            if (isManaging) exitManagement()
            if (!isSearching) resetLibrary()
        }
    }

    override val title: String by derivedStateOf {
        "${if (isSearching) "搜索" else "曲库"} (${if (selectIdList.isNotEmpty()) "${selectIdList.size}/" else ""}${library.size})"
    }

    override fun onBack() {
        if (isManaging) exitManagement()
        else if (isSearching) closeSearch()
        else pop()
    }

    @Composable
    override fun ActionScope.LeftActions() {
        if (isManaging) {
            val isSelectAll = library.fastAll { it.selected }
            Action(Icons.Outlined.SelectAll, if (isSelectAll) "取消全选" else "全选") {
                if (isSelectAll) exitManagement()
                else selectAll()
            }
        }
    }

    @Composable
    override fun ActionScope.RightActions() {
        if (!isManaging) {
            if (isSearching) {
                Action(Icons.Outlined.Home, "返回曲库") {
                    closeSearch()
                }
            }
            else {
                ActionSuspend(Icons.Outlined.Search, "搜索") {
                    openSearch()
                }
            }
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        if (library.isEmpty()) EmptyBox()
        else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(CustomTheme.size.cellWidth),
                contentPadding = CustomTheme.padding.equalValue,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
            ) {
                itemsIndexed(
                    items = library,
                    key = { _, item -> item.id }
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
    }

    override val fabCanExpand: Boolean by derivedStateOf { isManaging }

    override val fabIcon: ImageVector by derivedStateOf { if (fabCanExpand) Icons.Outlined.Add else Icons.Outlined.Token }

    override val fabMenus: Array<FABAction> = arrayOf(
        FABAction(Icons.AutoMirrored.Outlined.PlaylistAdd, "添加到歌单") {
            launch { onMusicAdd() }
        },
        FABAction(Icons.Outlined.Delete, "删除") {
            launch { onMusicDelete() }
        },
        FABAction(Icons.Outlined.Archive, "导出MOD") {
            launch { onMusicPackage() }
        }
    )

    override suspend fun onFabClick() {
        val result = importDialog.openSuspend() ?: return
        if (result == ImportMusicItem.FromFactory.ordinal) {
            pop()
            navigate<ScreenMusicModFactory>()
        }
        else if (app.musicFactory.isReady) slot.tip.warning("请先停止播放器")
        else {
            pop()
            when (result) {
                ImportMusicItem.FromMod.ordinal -> navigate(ScreenImportMusic.Args(null))
                ImportMusicItem.FromLocal.ordinal -> navigate<ScreenCreateMusic>()
                ImportMusicItem.FromQQMusic.ordinal -> navigate(ScreenPlatformMusic.Args(null, PlatformMusicType.QQMusic))
                ImportMusicItem.FromNetEaseCloudMusic.ordinal -> navigate(ScreenPlatformMusic.Args(null, PlatformMusicType.NetEaseCloud))
                ImportMusicItem.FromKugouMusic.ordinal -> navigate(ScreenPlatformMusic.Args(null, PlatformMusicType.Kugou))
            }
        }
    }

    private val importDialog = object : FloatingDialogChoice() {
        override val num: Int = ImportMusicItem.entries.size

        @Composable
        override fun Name(index: Int) = Text(text = ImportMusicItem.entries[index].text, modifier = Modifier.fillMaxWidth())

        @Composable
        override fun Icon(index: Int) {
            val item = ImportMusicItem.entries[index]
            if (item.isImage) MiniImage(icon = item.icon, size = CustomTheme.size.mediumIcon)
            else MiniIcon(icon = item.icon, size = CustomTheme.size.mediumIcon)
        }
    }

    private val searchDialog = FloatingDialogInput(hint = "歌曲名", maxLength = 32)

    private val addMusicDialog = FloatingDialogDynamicChoice("添加到歌单")

    @Composable
    override fun Floating() {
        searchDialog.Land()
        addMusicDialog.Land()
        importDialog.Land()
    }
}