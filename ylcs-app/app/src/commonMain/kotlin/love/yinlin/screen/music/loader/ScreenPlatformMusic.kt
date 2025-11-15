package love.yinlin.screen.music.loader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import kotlinx.io.files.Path
import love.yinlin.api.NetEaseCloudAPI
import love.yinlin.api.QQMusicAPI
import love.yinlin.app
import love.yinlin.common.*
import love.yinlin.uri.Uri
import love.yinlin.compose.*
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.toJsonString
import love.yinlin.platform.Coroutines
import love.yinlin.platform.NetClient
import love.yinlin.resources.Res
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.compose.ui.input.Radio
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.data.mod.ModResourceType
import love.yinlin.extension.*
import love.yinlin.platform.download
import love.yinlin.platform.lyrics.LrcParser

@Composable
private fun PlatformMusicInfoCard(
    info: PlatformMusicInfo,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        shadowElevation = CustomTheme.shadow.surface
    ) {
        WebImage(
            uri = info.pic,
            contentScale = ContentScale.Crop,
            alpha = 0.1f,
            modifier = Modifier.fillMaxSize().zIndex(1f)
        )
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(CustomTheme.padding.equalValue)
                .zIndex(2f),
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            NormalText(
                text = info.name,
                style = MaterialTheme.typography.labelLarge,
                icon = Icons.Outlined.MusicNote,
                color = MaterialTheme.colorScheme.primary
            )
            NormalText(
                text = "ID: ${info.id}",
                icon = Icons.Outlined.Badge
            )
            NormalText(
                text = "演唱: ${info.singer}",
                icon = ExtraIcons.Artist
            )
            NormalText(
                text = "时长: ${info.time}",
                icon = Icons.Outlined.Schedule
            )
            Text(
                text = remember(info) { LrcParser(info.lyrics).plainText },
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private interface PlatformMusicParser {
    suspend fun parseLink(link: String): List<PlatformMusicInfo>?

    companion object {
        fun build(type: PlatformMusicType): PlatformMusicParser = when (type) {
            PlatformMusicType.QQMusic -> QQMusicParser()
            PlatformMusicType.NetEaseCloud -> NetEaseCloudParser()
            PlatformMusicType.Kugou -> KugouParser()
        }
    }
}

private class QQMusicParser : PlatformMusicParser {
    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? = when {
        // 歌曲 https://c6.y.qq.com/base/fcgi-bin/u?__=8e1SWwxbKv0F
        link.contains("c6.y.qq.com") -> Coroutines.io {
            QQMusicAPI.requestMusicId(link)?.let {
                QQMusicAPI.requestMusic(it)
            }
        }?.let { listOf(it) }
        // 歌曲 https://y.qq.com/n/ryqq/songDetail/003yJ3Ba1bDVJc
        link.contains("y.qq.com") && link.contains("songDetail") -> Coroutines.io {
            QQMusicAPI.requestMusic(link.substringAfterLast("/"))
        }?.let { listOf(it) }
        // 歌单 https://i2.y.qq.com/n3/other/pages/share/personalized_playlist_v2/index.html?id=9094549201
        link.contains("i2.y.qq.com") && link.contains("playlist") -> Coroutines.io {
            val id = Uri.parse(link)?.params["id"]
            if (id != null) QQMusicAPI.requestPlaylist(id) else null
        }
        // 歌单 https://i.y.qq.com/n2/m/share/details/taoge.html?id=9094549201
        link.contains("i.y.qq.com") && link.contains("taoge") -> Coroutines.io {
            val id = Uri.parse(link)?.params["id"]
            if (id != null) QQMusicAPI.requestPlaylist(id) else null
        }
        // 歌单 https://y.qq.com/n/ryqq/playlist/9094549201
        link.contains("y.qq.com") && link.contains("playlist") -> Coroutines.io {
            QQMusicAPI.requestPlaylist(link.substringAfterLast("/"))
        }
        // 歌曲 003yJ3Ba1bDVJc
        else -> Coroutines.io {
            QQMusicAPI.requestMusic(link)
        }?.let { listOf(it) }
    }
}

private class NetEaseCloudParser : PlatformMusicParser {
    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? = when {
        // 歌曲 http://163cn.tv/EElG0jr
        link.contains("163cn.tv") -> Coroutines.io {
            NetEaseCloudAPI.requestMusicId(link)?.let {
                NetEaseCloudAPI.requestMusic(it)
            }
        }?.let { listOf(it) }
        // 歌单 https://y.music.163.com/m/playlist?id=13674538430&userid=10015279209&creatorId=10015279209
        link.contains("music.163.com") && link.contains("playlist") -> Coroutines.io {
            val id = Uri.parse(link)?.params["id"]
            if (id != null) NetEaseCloudAPI.requestPlaylist(id) else null
        }
        // 歌曲 https://music.163.com/#/song?textid=1064008&id=504686858
        link.contains("music.163.com") && link.contains("song") -> Coroutines.io {
            val id = Uri.parse(link)?.params["id"]
            if (id != null) NetEaseCloudAPI.requestMusic(id) else null
        }?.let { listOf(it) }
        // 歌曲 504686858
        else -> Coroutines.io {
            NetEaseCloudAPI.requestMusic(link)
        }?.let { listOf(it) }
    }
}

private class KugouParser : PlatformMusicParser {
    override suspend fun parseLink(link: String): List<PlatformMusicInfo>? = null
}

@Stable
class ScreenPlatformMusic(manager: ScreenManager, deeplink: Uri?, type: PlatformMusicType) : Screen(manager) {
    private var platformType by mutableStateOf(type)
    private var linkState = TextInputState(deeplink?.toString() ?: "")
    private var items by mutableRefStateOf(emptyList<PlatformMusicInfo>())

    private suspend fun downloadMusic() {
        catchingError {
            slot.loading.openSuspend()
            val ids = mutableListOf<String>()
            Coroutines.io {
                for (item in items) {
                    // 1. 下载音频
                    val audioFile = app.os.storage.createTempFile { sink ->
                        NetClient.download(
                            url = item.audioUrl,
                            sink = sink,
                            isCancel = { false },
                            onGetSize = { },
                            onTick = { _, _ -> }
                        )
                    }
                    // 2. 下载封面
                    val recordFile = app.os.storage.createTempFile { sink ->
                        NetClient.download(
                            url = item.pic,
                            sink = sink,
                            isCancel = { false },
                            onGetSize = { },
                            onTick = { _, _ -> }
                        )
                    }
                    if (audioFile == null || recordFile == null) continue
                    if (audioFile.size <= 1024 * 1024L) continue
                    if (recordFile.size <= 1024 * 10L) continue
                    // 3. 生成目录
                    val id = "${platformType.prefix}${item.id}"
                    val musicPath = Path(Paths.modPath, id)
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
                    info.path(Paths.modPath, ModResourceType.Config).writeText(info.toJsonString())
                    // 5. 写入音频
                    audioFile.writeTo(info.path(Paths.modPath, ModResourceType.Audio))
                    // 6. 写入封面
                    recordFile.writeTo(info.path(Paths.modPath, ModResourceType.Record))
                    // 7. 写入壁纸
                    info.path(Paths.modPath, ModResourceType.Background).writeByteArray(Res.readBytes("files/black_background.webp"))
                    // 8. 写入歌词
                    info.path(Paths.modPath, ModResourceType.LineLyrics).writeText(item.lyrics)
                    // 9. 更新曲库
                    ids += id
                }
            }
            require(ids.isNotEmpty())
            app.mp.updateMusicLibraryInfo(ids)
            slot.tip.success("已成功导入${ids.size}首歌曲")
        }?.let {
            slot.tip.warning("下载失败")
        }
        slot.loading.close()
    }

    override val title: String by derivedStateOf { platformType.description }

    @Composable
    override fun ActionScope.LeftActions() {
        if (linkState.text.isNotEmpty() || items.isNotEmpty()) {
            Action(Icons.Outlined.Refresh, "刷新") {
                linkState.text = ""
                items = emptyList()
            }
        }
    }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(
            icon = Icons.Outlined.Preview,
            tip = "预览",
            enabled = linkState.text.isNotEmpty()
        ) {
            val parser = PlatformMusicParser.build(platformType)
            val result = parser.parseLink(linkState.text)
            if (result != null) items = result
            else slot.tip.warning("解析失败")
        }
        ActionSuspend(
            icon = Icons.Outlined.Download,
            tip = "下载",
            enabled = items.isNotEmpty()
        ) {
            downloadMusic()
        }
    }

    @Composable
    override fun Content(device: Device) {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(CustomTheme.padding.equalValue),
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlatformMusicType.entries.fastForEach {
                    Radio(
                        checked = platformType == it,
                        text = it.description,
                        enabled = items.isEmpty(),
                        onCheck = { platformType = it }
                    )
                }
            }
            TextInput(
                state = linkState,
                hint = "ID/链接/歌单",
                modifier = Modifier.fillMaxWidth()
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(CustomTheme.size.cardWidth),
                contentPadding = CustomTheme.padding.equalValue,
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(
                    items = items,
                    key = { it.id }
                ) {
                    PlatformMusicInfoCard(
                        info = it,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                }
            }
        }
    }
}