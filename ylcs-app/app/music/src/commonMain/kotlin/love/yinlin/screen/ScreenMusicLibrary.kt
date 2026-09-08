package love.yinlin.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.*
import androidx.compose.ui.zIndex
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.floating.DialogChoice
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.DialogModFilter
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.input.PrimaryLoadingButton
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.node.shadow
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.MimeType
import love.yinlin.data.mod.ModInfo
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.Playlist
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingError
import love.yinlin.extension.replaceAll
import love.yinlin.extension.then
import love.yinlin.fs.File
import love.yinlin.mod.ModFactory
import love.yinlin.startup.StartupMusicPlayer

@Stable
class ScreenMusicLibrary : Screen() {
    @Stable
    private data class MusicInfoPreview(
        val id: String,
        val name: String,
        val singer: String,
        val modification: Int = 0,
        val selected: Boolean = false
    ) {
        constructor(musicInfo: MusicInfo) : this(musicInfo.id, musicInfo.name, musicInfo.singer, modification = musicInfo.modification)
    }

    private fun MusicInfoPreview.path(type: ModResourceType) = File(app.modPath, this.id, type.filename)

    private val mp by derivedStateOf { app.requireClassOrNull<StartupMusicPlayer>() }

    private val playlistLibrary = app.config.playlistLibrary
    private var library = mutableStateListOf<MusicInfoPreview>()

    private val selectIdList: List<String> by derivedStateOf { library.fastFilter { it.selected }.fastMap { it.id } }

    private val isManaging by derivedStateOf { library.any { it.selected } }
    private var isSearching by mutableStateOf(false)

    private val isSelectAll by derivedStateOf { library.fastAll { it.selected } }

    private val gridState = LazyGridState()

    private fun resetLibrary() {
        library.replaceAll(mp?.library?.map { MusicInfoPreview(it.value) } ?: emptyList())
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
        val rawLibrary = mp?.library
        if (rawLibrary.isNullOrEmpty()) {
            slot.tip.warning("曲库没有歌曲哦")
            return
        }

        val result = searchDialog.open()
        if (result != null) {
            library.replaceAll(Coroutines.cpu {
                rawLibrary.asSequence().filter {
                    it.value.name.contains(result, true)
                }.map { MusicInfoPreview(it.value) }.toList()
            })
            isSearching = true
        }
    }

    private suspend fun openSearchFilter() {
        val rawLibrary = mp?.library
        if (rawLibrary.isNullOrEmpty()) {
            slot.tip.warning("曲库没有歌曲哦")
            return
        }

        val result = searchFilterDialog.open(rawLibrary.values)
        if (result != null) {
            library.replaceAll(Coroutines.cpu {
                val newLibrary = mutableListOf<MusicInfoPreview>()
                for ((_, info) in rawLibrary) {
                    if (result.checkSuspend(info)) newLibrary += MusicInfoPreview(info)
                }
                newLibrary
            })
            isSearching = true
        }
    }

    private fun closeSearch() {
        resetLibrary()
        isSearching = false
    }

    private fun onCardClick(index: Int) {
        val item = library[index]
        if (isManaging) library[index] = item.copy(selected = !item.selected)
        else navigate(::ScreenMusicDetails, item.id)
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
                        val totalItems = oldItems + newItems
                        playlistLibrary[name] = playlist.copy(items = totalItems)
                        mp?.then { player ->
                            // 添加到当前播放的列表
                            val currentPlaylist = player.playlist
                            if (currentPlaylist is Playlist.User && currentPlaylist.name == name) {
                                player.updateNewMedias(totalItems)
                            }
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
        // 检查待删除歌曲是否已经在当前播放列表中
        val deleteItems = selectIdList
        val existItem = mp?.checkMusicIsInCurrentPlaylist(deleteItems)
        if (existItem != null) slot.tip.warning("\"$existItem\"在播放列表中, 请先停止播放器")
        else if (slot.confirm.open(content = "彻底删除曲库中这些歌曲吗")) {
            val source = mp?.library
            if (source == null) slot.tip.error("播放器初始化失败")
            else {
                for (item in deleteItems) {
                    val removeItem = source.remove(item)
                    removeItem?.path(app.modPath)?.deleteRecursively()
                }
                resetLibrary()
            }
        }
    }

    private suspend fun onMusicPackage() {
        val packageItems = selectIdList
        catchingError {
            slot.loading.open {
                Coroutines.io {
                    app.picker.savePath("${DateEx.CurrentLong}.rachel", MimeType.BINARY, "*.rachel")?.write { sink ->
                        val player = mp!!
                        ModFactory.Merge(
                            mediaPaths = packageItems.fastMapNotNull { player.library[it]?.path(app.modPath) },
                            sink = sink,
                            info = ModInfo(author = app.config.userProfile?.name ?: "无名")
                        ).process(filters = ModResourceType.ALL) { _, _, _ -> }
                    }
                }
                exitManagement()
                slot.tip.success("导出MOD成功")
            }
        }?.then { slot.tip.warning("导出MOD失败") }
    }

    override val title: String get() = if (isSearching) "搜索" else "曲库"

    override suspend fun initialize() {
        resetLibrary()
        monitor(state = { mp?.library }) {
            if (isManaging) exitManagement()
            if (!isSearching) resetLibrary()
        }
    }

    override fun onBack() {
        if (isManaging) exitManagement()
        else if (isSearching) closeSearch()
        else pop()
    }

    @Composable
    override fun RowScope.LeftActions() {
        if (isManaging) {
            Icon(icon = Icons.SelectAll, tip = if (isSelectAll) "取消全选" else "全选", onClick = {
                if (isSelectAll) exitManagement()
                else selectAll()
            })
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        if (isManaging) {
            LoadingIcon(icon = Icons.PlaylistAdd, tip = "添加到歌单", onClick = ::onMusicAdd)
            LoadingIcon(icon = Icons.Delete, tip = "删除", onClick = ::onMusicDelete)
            LoadingIcon(icon = Icons.Archive, tip = "导出MOD", onClick = ::onMusicPackage)
        }
        else {
            if (isSearching) Icon(icon = Icons.Home, tip = "返回曲库", onClick = ::closeSearch)
            else {
                LoadingIcon(icon = Icons.Search, tip = "搜索", onClick = ::openSearch)
                LoadingIcon(icon = Icons.Filter, tip = "筛选", onClick = ::openSearchFilter)
            }
        }
    }

    @Composable
    override fun ColumnScope.SecondTitleBar() {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Theme.padding.value),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleEllipsisText(text = "已安装 / ${library.size}", color = Theme.color.secondary)
            SimpleEllipsisText(text = selectIdList.size.let { if (it > 0) "已选择 / $it" else "" }, color = Theme.color.tertiary)
        }
    }

    @Composable
    private fun MusicCard(
        musicInfo: MusicInfoPreview,
        enableLongClick: Boolean,
        onLongClick: () -> Unit,
        onClick: () -> Unit
    ) {
        ThemeContainer(
            color = if (musicInfo.selected) Theme.color.onContainer else Theme.color.onSurface,
            variantColor = if (musicInfo.selected) Theme.color.onContainerVariant else Theme.color.onSurfaceVariant
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .condition(musicInfo.selected) {
                        border(Theme.border.v4, Colors.Yellow4, Theme.shape.v5)
                    }
                    .shadow(Theme.shape.v5, Theme.shadow.v7)
                    .clip(Theme.shape.v5)
                    .background(Theme.color.surface)
                    .combinedClickable(onClick = onClick, onLongClick = if (enableLongClick) onLongClick else null)
            ) {
                LocalFileImage(
                    uri = musicInfo.path(ModResourceType.Record).path,
                    musicInfo.id,
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f,
                    modifier = Modifier.matchParentSize().zIndex(1f)
                )
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue).zIndex(2f),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
                ) {
                    SimpleEllipsisText(text = musicInfo.name, style = Theme.typography.v7.bold)
                    SimpleEllipsisText(text = musicInfo.singer, style = Theme.typography.v8)
                    LoadingIcon(icon = Icons.PlayArrow, tip = "试听", onClick = {
                        mp?.startPlaylist(Playlist.Default, musicInfo.id, true)
                        pop()
                    })
                }
            }
        }
    }

    @Composable
    override fun Content() {
        if (library.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v7, Alignment.CenterVertically)
            ) {
                SimpleEllipsisText(text = "曲库空空的,快去工坊里下载吧!", style = Theme.typography.v6)
                PrimaryLoadingButton(text = "工坊", icon = Icons.Token, onClick = {
                    pop()
                    navigate(::ScreenModCenter)
                })
            }
        }
        else {
            Theme.ThemeModeWrapper(true) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Theme.size.cell4),
                    state = gridState,
                    contentPadding = Theme.padding.eValue,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.e),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
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
                            onClick = { onCardClick(index) }
                        )
                    }
                }
            }
        }
    }

    override val fab: FAB = object : FAB() {
        private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

        override val action: FABAction? by derivedStateOf {
            if (isScrollTop) null else FABAction(
                iconProvider = { Icons.ArrowUpward },
                onClick = {
                    if (!isScrollTop) gridState.animateScrollToItem(0)
                }
            )
        }
    }

    private val addMusicDialog = this land DialogChoice.ByDynamicList()

    private val searchDialog = this land DialogInput(hint = "歌曲名", maxLength = 32)

    private val searchFilterDialog = this land DialogModFilter()
}
