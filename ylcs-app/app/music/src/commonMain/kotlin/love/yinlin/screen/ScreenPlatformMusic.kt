package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import kotlinx.io.files.Path
import love.yinlin.app
import love.yinlin.common.DataBin
import love.yinlin.common.PathMod
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.Surface
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.input.Filter
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputDecoration
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.TextIconAdapter
import love.yinlin.coroutines.Coroutines
import love.yinlin.cs.NetClient
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.catchingError
import love.yinlin.extension.fileSize
import love.yinlin.extension.lazyProvider
import love.yinlin.extension.mkdir
import love.yinlin.extension.toJsonString
import love.yinlin.extension.writeByteArray
import love.yinlin.extension.writeText
import love.yinlin.extension.writeTo
import love.yinlin.startup.StartupMusicPlayer
import love.yinlin.tpl.PlatformMusicParser
import love.yinlin.uri.Uri

@Stable
class ScreenPlatformMusic(deeplink: Uri?, type: PlatformMusicType) : Screen() {
    private val mp by lazyProvider { app.startup<StartupMusicPlayer>() }

    private var platformType by mutableStateOf(type)
    private var linkState = InputState(deeplink?.toString() ?: "")
    private var items by mutableRefStateOf(emptyList<PlatformMusicInfo>())

    private val gridState = LazyGridState()

    private suspend fun downloadMusic() {
        slot.loading.open {
            catchingError {
                val ids = mutableListOf<String>()
                Coroutines.io {
                    for (item in items) {
                        // 1. 下载音频
                        val audioFile = app.os.storage.createTempFile { NetClient.simpleDownload(item.audioUrl, it) }
                        // 2. 下载封面
                        val recordFile = app.os.storage.createTempFile { NetClient.simpleDownload(item.pic, it) }
                        if (audioFile == null || recordFile == null) continue
                        if (audioFile.fileSize <= 1024 * 1024L) continue
                        if (recordFile.fileSize <= 1024 * 10L) continue
                        // 3. 生成目录
                        val id = "${platformType.prefix}${item.id}"
                        val musicPath = Path(PathMod, id)
                        musicPath.mkdir()
                        // 4. 写入配置
                        val info = MusicInfo(
                            version = "1.0",
                            author = platformType.description,
                            id = id,
                            name = item.name,
                            singer = item.singer,
                            lyricist = "未知",
                            composer = "未知",
                            album = "未知",
                            chorus = null
                        )
                        info.path(PathMod, ModResourceType.Config).writeText(info.toJsonString())
                        // 5. 写入音频
                        audioFile.writeTo(info.path(PathMod, ModResourceType.Audio))
                        // 6. 写入封面
                        recordFile.writeTo(info.path(PathMod, ModResourceType.Record))
                        // 7. 写入壁纸
                        info.path(PathMod, ModResourceType.Background).writeByteArray(DataBin.BlackBackgroundPicture)
                        // 8. 写入歌词
                        info.path(PathMod, ModResourceType.LineLyrics).writeText(item.lyrics)
                        // 9. 更新曲库
                        ids += id
                    }
                }
                require(ids.isNotEmpty()) { "未检测到任何资源" }
                mp?.updateMusicLibraryInfo(ids)
                slot.tip.success("已成功导入${ids.size}首歌曲")
            }?.let { slot.tip.warning("下载失败 ${it.message}") }
        }
    }

    override val title: String get() = platformType.description

    @Composable
    private fun PlatformMusicInfoCard(info: PlatformMusicInfo, modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            shape = Theme.shape.v3,
            shadowElevation = Theme.shadow.v3
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                WebImage(uri = info.pic, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(1f))

                Column(
                    modifier = Modifier.fillMaxWidth().padding(Theme.padding.eValue),
                    verticalArrangement = Arrangement.spacedBy(Theme.padding.e),
                ) {
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.MusicNote, modifier = Modifier.idIcon())
                        SimpleEllipsisText(text = info.name, modifier = Modifier.idText())
                    }
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.Badge, modifier = Modifier.idIcon())
                        SimpleEllipsisText(text = info.id, modifier = Modifier.idText())
                    }
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.Artist, modifier = Modifier.idIcon())
                        SimpleEllipsisText(text = "演唱: ${info.singer}", modifier = Modifier.idText())
                    }
                    TextIconAdapter { idIcon, idText ->
                        Icon(icon = Icons.Timer, modifier = Modifier.idIcon())
                        SimpleEllipsisText(text = "时长: ${info.time}", modifier = Modifier.idText())
                    }
                }
            }
        }
    }

    @Composable
    override fun RowScope.LeftActions() {
        if (linkState.text.isNotEmpty() || items.isNotEmpty()) {
            Icon(icon = Icons.Refresh, tip = "刷新", onClick = {
                linkState.text = ""
                items = emptyList()
            })
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        LoadingIcon(icon = Icons.Preview, tip = "预览", enabled = linkState.text.isNotEmpty(), onClick = {
            val parser = PlatformMusicParser.build(platformType)
            val result = parser.parseLink(linkState.text)
            gridState.requestScrollToItem(0)
            if (result != null) items = result
            else slot.tip.warning("解析失败")
        })
        LoadingIcon(icon = Icons.Download, tip = "下载", onClick = ::downloadMusic)
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
            Input(state = linkState, hint = "ID/链接/歌单", modifier = Modifier.fillMaxWidth(), trailing = InputDecoration.Icon.Clear)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(Theme.size.cell4),
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
}