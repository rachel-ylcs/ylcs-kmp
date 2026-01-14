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
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.CustomTheme
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
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.*
import love.yinlin.platform.Platform
import love.yinlin.platform.UnsupportedPlatformText
import love.yinlin.platform.platform
import love.yinlin.screen.music.loader.ScreenCreateMusic
import love.yinlin.screen.music.loader.ScreenImportMusic
import love.yinlin.screen.music.loader.ScreenPlatformMusic

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

    private val selectIdList: List<String> by derivedStateOf { library.fastFilter { it.selected }.fastMap { it.id } }

    private val isManaging by derivedStateOf { library.any { it.selected } }
    private var isSearching by mutableStateOf(false)

    private val isSelectAll by derivedStateOf { library.fastAll { it.selected } }

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
            Action(Icons.Outlined.Token, "工坊") {
                pop()
                navigate(::ScreenModCenter)
            }
            Action(Icons.Outlined.Upload, "导入") {
                pop()
                navigate(::ScreenImportMusic, null)
            }
            Action(Icons.Outlined.DesignServices, "创造") {
                pop()
                navigate(::ScreenCreateMusic)
            }
            Action(Icons.Outlined.ChangeCircle, "从旧版迁移") {
                launch {
                    if (slot.confirm.openSuspend(content = "迁移时请耐心等待, 不可重复迁移")) {
                        migrateOldVersion()
                    }
                }
            }
            Action(ExtraIcons.QQMusic, "QQ音乐", useImage = true) {
                pop()
                navigate(::ScreenPlatformMusic, null, PlatformMusicType.QQMusic)
            }
            Action(ExtraIcons.NetEaseCloudMusic, "网易云音乐", useImage = true) {
                pop()
                navigate(::ScreenPlatformMusic, null, PlatformMusicType.NetEaseCloud)
            }
            Action(ExtraIcons.KugouMusic, "酷狗音乐", useImage = true) {
                pop()
                navigate(::ScreenPlatformMusic, null, PlatformMusicType.Kugou)
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

    private val searchDialog = this land FloatingDialogInput(hint = "歌曲名", maxLength = 32)

    private val addMusicDialog = this land FloatingDialogDynamicChoice("添加到歌单")

    private suspend fun migrateOldVersion() {
        val idMap = arrayOf(1052, 1054, 1051, 1060, 1057, 1058, 1055, 1056, 1059, 1061, 1053, 1108, 1105, 1106, 1107, 1102, 1101, 1104, 1103, 1109, 1111, 1110, 2001, 2002, 2003, 1001, 1002, 1003, 1004, 2004, 2006, 2007, 3001, 4001, 4002, 3002, 3003, 1151, 1152, 1153, 1154, 1201, 1203, 1202, 1204, 1205, 1208, 1206, 1209, 1207, 1210, 1211, 2008, 4003, 2009, 3004, 3005, 2005, 2010, 2011, 1251, 1252, 1253, 1254, 1255, 1256, 1257, 3006, 3007, 4004, 2012, 2013, 2014, 2015, 2016, 1305, 1301, 1310, 1302, 1303, 1311, 1306, 1307, 1308, 1304, 3008, 1309, 2017, 2018, 4005, 4006, 2019, 4007, 2020, 4008, 2021, 2022, 2023, 2024, 2025, 4009, 2026, 4010, 2027, 2028, 2029, 2030, 4011, 3009, 2031, 4012, 2032, 2033, 2034, 2035, 4013, 2036, 4014, 4015, 4016, 2037, 2038, 2039, 2040, 2041, 4017, 4018, 4019, 2042, 4020, 2043, 2044, 4021, 4022, 2045, 3010, 4023, 2046, 4024, 4025, 3011, 4026, 4027, 4028, 3012, 3013, 2047, 3014, 3015, 2048, 3016, 3017, 3018, 2049, 2050, 3019, 3020, 3021, 3022, 2051, 3023, 4029, 4030, 3024, 3025, 3026, 4031, 3027, 4032, 4033, 4034, 3028, 4035, 2052, 4036, 2053, 1312, 3029)

        catchingError {
            require(platform in Platform.Web) { UnsupportedPlatformText }
            // 检查是否存在music目录
            val oldMusicPath = Path(app.os.storage.dataPath, "music")
            require(oldMusicPath.exists) { "不存在旧版MOD需要迁移" }

            slot.loading.openSuspend()
            var count = 0
            // 遍历每一个旧版 MOD
            Coroutines.io {
                val moveList = mutableListOf<String>()
                for (modPath in oldMusicPath.list()) {
                    catching {
                        val oldId = modPath.name
                        val newId = idMap[oldId.toInt() - 1].toString()
                        // 重命名所有资源
                        for (resPath in modPath.list()) {
                            val resName = resPath.name
                            when (resName) {
                                "0-config" -> {
                                    val oldConfig = resPath.readText()!!
                                    val newConfig = oldConfig.replace("\"${oldId}\"", "\"${newId}\"")
                                    resPath.writeText(newConfig)
                                    resPath.rename(ModResourceType.Config.filename)
                                }
                                "10-flac" -> resPath.rename(ModResourceType.Audio.filename)
                                "20-record" -> resPath.rename(ModResourceType.Record.filename)
                                "21-background" -> resPath.rename(ModResourceType.Background.filename)
                                "22-animation" -> resPath.rename(ModResourceType.Animation.filename)
                                "30-lrc" -> resPath.rename(ModResourceType.LineLyrics.filename)
                                "40-pv" -> resPath.rename(ModResourceType.Video.filename)
                                "50-rhyme" -> resPath.rename(ModResourceType.Rhyme.filename)
                                else -> resPath.delete() // 未知资源删除
                            }
                        }
                        // 重命名目录
                        modPath.rename(newId)
                        moveList += newId
                    }
                }
                // 目录移动
                for (newId in moveList) {
                    val newModPath = Path(Paths.modPath, newId)
                    if (!newModPath.exists) {
                        if (Path(oldMusicPath, newId).move(newModPath)) ++count
                    }
                }
                // 清空旧版 MOD 目录
                oldMusicPath.deleteRecursively()
                if (moveList.isNotEmpty()) mp.updateMusicLibraryInfo(moveList)
            }
            slot.tip.success("已成功迁移${count}个旧版MOD")
        }?.let {
            slot.tip.error("迁移失败: ${it.message}")
        }
        slot.loading.close()
    }
}