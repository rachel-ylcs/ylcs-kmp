package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import love.yinlin.app
import love.yinlin.common.DataBin
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.catchingError
import love.yinlin.extension.toJsonString
import love.yinlin.foundation.NetClient
import love.yinlin.fs.*
import love.yinlin.startup.StartupMusicPlayer
import love.yinlin.tpl.PlatformMusicAPI
import love.yinlin.uri.Uri

@Stable
class ScreenPlatformMusic(private val deeplink: Uri?, type: PlatformMusicType) : Screen() {
    private val mp by derivedStateOf { app.requireClassOrNull<StartupMusicPlayer>() }

    private var platformType by mutableStateOf(type)
    private var items by mutableRefStateOf(emptyList<PlatformMusicInfo>())

    private val gridState = LazyGridState()

    private suspend fun searchMusic(result: String) {
        slot.loading.open {
            val api = PlatformMusicAPI.build(platformType)
            val result = api.search(result)
            gridState.requestScrollToItem(0)
            if (result != null) items = result
            else slot.tip.warning("搜索失败")
        }
    }

    private suspend fun parseMusic(result: String) {
        slot.loading.open {
            val api = PlatformMusicAPI.build(platformType)
            val result = api.parseLink(result)
            gridState.requestScrollToItem(0)
            if (result != null) items = result
            else slot.tip.warning("解析失败")
        }
    }

    private suspend fun downloadMusic(platformMusicInfo: PlatformMusicInfo) {
        slot.loading.open {
            catchingError {
                Coroutines.io {
                    // 1. 下载音频
                    val audioFile = app.createTempFile { NetClient.File.download(platformMusicInfo.audioUrl, it) }
                    // 2. 下载封面
                    val recordFile = app.createTempFile { NetClient.File.download(platformMusicInfo.pic, it) }
                    require(audioFile != null && audioFile.fileSize() > 1024 * 1024L) { "解析音频失败" }
                    require(recordFile != null && recordFile.fileSize() > 1024 * 10L) { "解析封面失败" }
                    // 3. 生成目录
                    val id = "${platformType.prefix}${platformMusicInfo.id}"
                    val modPath = app.modPath
                    val musicPath = File(modPath, id)
                    musicPath.mkdir()
                    // 4. 写入配置
                    val info = MusicInfo(
                        version = "1.0",
                        author = platformType.description,
                        id = id,
                        name = platformMusicInfo.name,
                        singer = platformMusicInfo.singer,
                        lyricist = "未知",
                        composer = "未知",
                        album = "未知",
                        chorus = null
                    )
                    info.path(modPath, ModResourceType.Config).writeText(info.toJsonString())
                    // 5. 写入音频
                    audioFile.writeTo(info.path(modPath, ModResourceType.Audio))
                    // 6. 写入封面
                    recordFile.writeTo(info.path(modPath, ModResourceType.Record))
                    // 7. 写入壁纸
                    info.path(modPath, ModResourceType.Background).writeByteArray(DataBin.BlackBackgroundPicture)
                    // 8. 写入歌词
                    info.path(modPath, ModResourceType.LineLyrics).writeText(platformMusicInfo.lyrics)
                    // 9. 更新曲库
                    mp?.updateMusicLibraryInfo(listOf(id))
                    slot.tip.success("导入 ${platformMusicInfo.name} 成功")
                }
            }?.let { slot.tip.warning("下载失败 ${it.message}") }
        }
    }

    override val title: String get() = platformType.description

    override suspend fun initialize() {
        deeplink?.let {
            val result = parseDialog.open(it.toString(), "${platformType.description}解析")
            if (result != null) parseMusic(result)
        }
    }

    @Composable
    private fun PlatformMusicInfoCard(info: PlatformMusicInfo, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            shape = Theme.shape.v3,
            contentPadding = Theme.padding.value9,
            shadowElevation = Theme.shadow.v3
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                ) {
                    WebImage(
                        uri = info.pic,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(Theme.shape.v7)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v7)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon = Icons.MusicNote)
                            SimpleEllipsisText(text = info.name, style = Theme.typography.v6.bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon = Icons.Artist)
                            SimpleEllipsisText(text = info.singer)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.Badge, modifier = Modifier.idIcon())
                        SimpleEllipsisText(text = info.id, modifier = Modifier.idText())
                    }
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.Timer, modifier = Modifier.idIcon())
                        SimpleEllipsisText(text = info.time, modifier = Modifier.idText())
                    }
                    Box(modifier = Modifier.weight(1f))
                    LoadingIcon(icon = Icons.Download, onClick = { downloadMusic(info) })
                }
            }
        }
    }

    @Composable
    override fun RowScope.LeftActions() {
        if (items.isNotEmpty()) {
            Icon(icon = Icons.Clear, tip = "刷新", onClick = {
                items = emptyList()
            })
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        LoadingIcon(icon = Icons.Search, tip = "搜索", onClick = {
            val result = searchDialog.open(title = "${platformType.description}搜索")
            if (result != null) launch { searchMusic(result) }
        })
        LoadingIcon(icon = Icons.Preview, tip = "解析", onClick = {
            val result = parseDialog.open(title = "${platformType.description}解析")
            if (result != null) launch { parseMusic(result) }
        })
    }

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize().padding(Theme.padding.eValue),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            val types = PlatformMusicType.entries
            Filter(
                size = types.size,
                selectedProvider = { platformType == types[it] },
                titleProvider = { types[it].description },
                iconProvider = { types[it].icon },
                iconColor = Colors.Unspecified,
                onClick = { index, selected -> if (selected) platformType = types[index] },
                modifier = Modifier.fillMaxWidth()
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.cell1),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(Theme.padding.e),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.e),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(
                    items = items,
                    key = { it.id }
                ) {
                    PlatformMusicInfoCard(info = it, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    private val parseDialog = this land DialogInput(hint = "ID/链接/歌单", maxLength = 128)
    private val searchDialog = this land DialogInput(hint = "歌曲关键词", maxLength = 16)
}