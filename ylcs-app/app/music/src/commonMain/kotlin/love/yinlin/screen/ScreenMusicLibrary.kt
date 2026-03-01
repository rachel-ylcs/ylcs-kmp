package love.yinlin.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import kotlinx.io.files.Path
import love.yinlin.app
import love.yinlin.common.PathMod
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.ActionScope
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.floating.DialogChoice
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABAction
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.icon.Icons2
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.input.PrimaryLoadingButton
import love.yinlin.compose.ui.layout.Divider
import love.yinlin.compose.ui.node.shadow
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.MimeType
import love.yinlin.data.mod.ModInfo
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingError
import love.yinlin.extension.deleteRecursively
import love.yinlin.extension.lazyProvider
import love.yinlin.extension.replaceAll
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

    private fun MusicInfoPreview.path(type: ModResourceType) = Path(PathMod, this.id, type.filename)

    private val mp by lazyProvider { app.startup<StartupMusicPlayer>() }

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
        val result = searchDialog.open()
        if (result != null) {
            library.replaceAll(mp?.library?.asSequence()?.filter {
                it.value.name.contains(result, true)
            }?.map { MusicInfoPreview(it.value) }?.toList() ?: emptyList())
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
                        playlistLibrary[name] = playlist.copy(items = oldItems + newItems)
                        // 添加到当前播放的列表
                        mp?.let { player ->
                            if (player.playlist?.name == name) {
                                player.addMedias(newItems.asSequence().filter { it !in player.musicList }.toList())
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
        mp?.let { player ->
            if (player.isReady) slot.tip.warning("请先停止播放器")
            else if (slot.confirm.open(content = "彻底删除曲库中这些歌曲吗")) {
                val deleteItems = selectIdList
                for (item in deleteItems) {
                    val removeItem = player.library.remove(item)
                    removeItem?.path(PathMod)?.deleteRecursively()
                }
                resetLibrary()
            }
        }
    }

    private suspend fun onMusicPackage() {
        mp?.let { player ->
            if (player.isReady) slot.tip.warning("请先停止播放器")
            else {
                catchingError {
                    Coroutines.io {
                        app.picker.savePath("${DateEx.CurrentLong}.rachel", MimeType.BINARY, "*.rachel")?.write { sink ->
                            slot.loading.open {
                                val packageItems = selectIdList
                                ModFactory.Merge(
                                    mediaPaths = packageItems.fastMapNotNull { player.library[it]?.path(PathMod) },
                                    sink = sink,
                                    info = ModInfo(author = app.config.userProfile?.name ?: "无名")
                                ).process(filters = ModResourceType.ALL) { _, _, _ -> }
                                exitManagement()
                                slot.tip.success("导出MOD成功")
                            }
                        }
                    }
                }?.let { slot.tip.warning("导出MOD失败") }
            }
        }
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
            else LoadingIcon(icon = Icons.Search, tip = "搜索", onClick = ::openSearch)
        }
    }

    @Composable
    override fun ColumnScope.SecondTitleBar() {
        Divider()
        ActionScope.Left.Container(modifier = Modifier.fillMaxWidth().padding(Theme.padding.value)) {
            Icon(icon = Icons.Token, tip = "工坊", onClick = {
                pop()
                navigate(::ScreenModCenter)
            })
            Icon(icon = Icons.Upload, tip = "导入", onClick = {
                pop()
                navigate(::ScreenImportMusic, null)
            })
            Icon(icon = Icons.DesignServices, tip = "创造", onClick = {
                pop()
                navigate(::ScreenCreateMusic)
            })
            Icon(icon = Icons2.QQMusic, color = Colors.Unspecified, tip = "QQ音乐", onClick = {
                pop()
                navigate(::ScreenPlatformMusic, null, PlatformMusicType.QQMusic)
            })
            Icon(icon = Icons2.NetEaseCloudMusic, color = Colors.Unspecified, tip = "网易云音乐", onClick = {
                pop()
                navigate(::ScreenPlatformMusic, null, PlatformMusicType.NetEaseCloud)
            })
            Icon(icon = Icons2.KugouMusic, color = Colors.Unspecified, tip = "酷狗音乐", onClick = {
                pop()
                navigate(::ScreenPlatformMusic, null, PlatformMusicType.Kugou)
            })
        }
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
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .shadow(Theme.shape.v5, Theme.shadow.v7)
            .clip(Theme.shape.v5)
            .background(if (musicInfo.selected) Theme.color.primaryContainer else Theme.color.surface)
            .combinedClickable(onClick = onClick, onLongClick = if (enableLongClick) onLongClick else null)
        ) {
            ThemeContainer(
                color = if (musicInfo.selected) Theme.color.onContainer else Theme.color.onSurface,
                variantColor = if (musicInfo.selected) Theme.color.onContainerVariant else Theme.color.onSurfaceVariant
            ) {
                LocalFileImage(
                    uri = musicInfo.path(ModResourceType.Record).toString(),
                    musicInfo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.aspectRatio(1f).fillMaxHeight()
                )
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(Theme.padding.eValue),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
                ) {
                    SimpleEllipsisText(text = musicInfo.name, style = Theme.typography.v7.bold)
                    SimpleEllipsisText(text = musicInfo.singer, style = Theme.typography.v8)
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

    private val searchDialog = this land DialogInput(hint = "歌曲名", maxLength = 32)

    private val addMusicDialog = this land DialogChoice.ByDynamicList()
}