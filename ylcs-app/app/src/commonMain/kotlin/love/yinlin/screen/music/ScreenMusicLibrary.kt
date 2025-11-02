package love.yinlin.screen.music

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
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
import love.yinlin.app
import love.yinlin.common.ExtraIcons
import love.yinlin.common.Paths
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.floating.FloatingDialogDynamicChoice
import love.yinlin.compose.ui.floating.FloatingDialogInput
import love.yinlin.data.MimeType
import love.yinlin.data.mod.ModInfo
import love.yinlin.data.music.MusicInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.deleteRecursively
import love.yinlin.extension.replaceAll
import love.yinlin.mod.ModFactory
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.SplitLayout
import love.yinlin.data.mod.ModResourceType
import love.yinlin.extension.catchingError
import love.yinlin.platform.Coroutines
import love.yinlin.screen.music.loader.ScreenCreateMusic
import love.yinlin.screen.music.loader.ScreenImportMusic

@Stable
data class MusicInfoPreview(
    val id: String,
    val name: String,
    val singer: String,
    val modification: Int = 0,
    val selected: Boolean = false
) {
    constructor(musicInfo: MusicInfo) : this(musicInfo.id, musicInfo.name, musicInfo.singer, modification = musicInfo.modification)

    fun path(rootPath: Path, type: ModResourceType) = Path(rootPath, id, type.filename)
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
                    path = { musicInfo.path(Paths.modPath, ModResourceType.Record) },
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
class ScreenMusicLibrary(manager: ScreenManager) : Screen(manager) {
    private val mp = app.mp
    private val playlistLibrary = app.config.playlistLibrary
    private var library = mutableStateListOf<MusicInfoPreview>()

    private val selectIdList: List<String> get() = library.fastFilter { it.selected }.fastMap { it.id }

    private val isManaging by derivedStateOf { library.any { it.selected } }
    private var isSearching by mutableStateOf(false)

    private val gridState = LazyGridState()

    private fun resetLibrary() {
        library.replaceAll(mp.library.map {
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
            library.replaceAll(mp.library
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
        else {
            // TODO:
            // navigate(ScreenMusicDetails.Args(item.id))
        }
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
                        if (mp.playlist?.name == name) {
                            mp.addMedias(newItems
                                .fastFilter { id -> mp.musicList.find { it.id == id } == null }
                                .fastMapNotNull { mp.library[it] })
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
        if (mp.isReady) slot.tip.warning("请先停止播放器")
        else if (slot.confirm.openSuspend(content = "彻底删除曲库中这些歌曲吗")) {
            val deleteItems = selectIdList
            for (item in deleteItems) {
                val removeItem = mp.library.remove(item)
                removeItem?.path(Paths.modPath)?.deleteRecursively()
            }
            resetLibrary()
        }
    }

    private suspend fun onMusicPackage() {
        if (mp.isReady) slot.tip.warning("请先停止播放器")
        else catchingError {
            Coroutines.io {
                app.picker.savePath("${DateEx.CurrentLong}.rachel", MimeType.BINARY, "*.rachel")?.write { sink ->
                    slot.loading.openSuspend()
                    val packageItems = selectIdList
                    ModFactory.Merge(
                        mediaPaths = packageItems.fastMapNotNull { mp.library[it]?.path(Paths.modPath) },
                        sink = sink,
                        info = ModInfo(author = app.config.userProfile?.name ?: "无名")
                    ).process(filters = ModResourceType.ALL) { _, _, _ -> }
                    slot.loading.close()
                    exitManagement()
                    slot.tip.success("导出成功")
                }
            }
        }?.let { slot.tip.warning("无法导出MOD") }
    }

    override suspend fun initialize() {
        resetLibrary()
        monitor(state = { mp.library }) {
            if (isManaging) exitManagement()
            if (!isSearching) resetLibrary()
        }
    }

    override val title: String by derivedStateOf { if (isSearching) "搜索" else "曲库" }

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
        if (isManaging) {
            ActionSuspend(Icons.AutoMirrored.Outlined.PlaylistAdd, "添加到歌单") {
                onMusicAdd()
            }

            ActionSuspend(Icons.Outlined.Delete, "删除") {
                onMusicDelete()
            }

            ActionSuspend(Icons.Outlined.Archive, "导出MOD") {
                onMusicPackage()
            }
        }
        else {
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
    override fun ColumnScope.SecondTitleBar() {
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = CustomTheme.border.small)

        val librarySize by rememberDerivedState { "已安装 - ${library.size}" }
        val selectedSize by rememberDerivedState { selectIdList.size.let { if (it > 0) "已选择 - $it" else "" } }

        ActionScope.Left.ActionLayout(modifier = Modifier.fillMaxWidth().padding(vertical = CustomTheme.padding.verticalSpace)) {
            // TODO:
            Action(Icons.Outlined.Token, "工坊") {
                // navigate<ScreenMusicModFactory>()
            }
            Action(Icons.Outlined.Upload, "导入") {
                pop()
                navigate(::ScreenImportMusic, null)
            }
            Action(Icons.Outlined.DesignServices, "创造") {
                pop()
                navigate(::ScreenCreateMusic)
            }
            Action(ExtraIcons.QQMusic, "QQ音乐", useImage = true) {
                pop()
                // navigate(ScreenPlatformMusic.Args(null, PlatformMusicType.QQMusic))
            }
            Action(ExtraIcons.NetEaseCloudMusic, "网易云音乐", useImage = true) {
                pop()
                // navigate(ScreenPlatformMusic.Args(null, PlatformMusicType.NetEaseCloud))
            }
            Action(ExtraIcons.KugouMusic, "酷狗音乐", useImage = true) {
                pop()
                // navigate(ScreenPlatformMusic.Args(null, PlatformMusicType.Kugou))
            }
        }

        SplitLayout(
            modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.value),
            left = {
                Text(
                    text = librarySize,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            right = {
                Text(
                    text = selectedSize,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    @Composable
    override fun Content(device: Device) {
        if (library.isEmpty()) EmptyBox()
        else {
            PauseLoading(gridState)

            LazyVerticalGrid(
                columns = GridCells.Adaptive(CustomTheme.size.cellWidth),
                state = gridState,
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

    private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector? by derivedStateOf { if (isScrollTop) null else Icons.Outlined.ArrowUpward }

    override suspend fun onFabClick() {
        if (!isScrollTop) gridState.animateScrollToItem(0)
    }

    private val searchDialog = FloatingDialogInput(hint = "歌曲名", maxLength = 32)

    private val addMusicDialog = FloatingDialogDynamicChoice("添加到歌单")

    @Composable
    override fun Floating() {
        searchDialog.Land()
        addMusicDialog.Land()
    }
}