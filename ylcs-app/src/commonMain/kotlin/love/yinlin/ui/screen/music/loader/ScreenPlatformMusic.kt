package love.yinlin.ui.screen.music.loader

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
import androidx.compose.ui.zIndex
import io.ktor.utils.io.core.writeText
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.api.NetEaseCloudAPI
import love.yinlin.api.QQMusicAPI
import love.yinlin.common.Device
import love.yinlin.common.ExtraIcons
import love.yinlin.common.ThemeValue
import love.yinlin.common.Uri
import love.yinlin.data.Data
import love.yinlin.data.map
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.PlatformMusicInfo
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.extension.toJsonString
import love.yinlin.platform.Coroutines
import love.yinlin.platform.OS
import love.yinlin.platform.app
import love.yinlin.platform.safeDownload
import love.yinlin.resources.Res
import love.yinlin.ui.component.image.WebImage
import love.yinlin.ui.component.input.RachelRadioButton
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.lyrics.LyricsLrc
import love.yinlin.ui.component.screen.ActionScope
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.text.TextInput
import love.yinlin.ui.component.text.TextInputState
import love.yinlin.ui.screen.music.*

@Composable
private fun PlatformMusicInfoCard(
    info: PlatformMusicInfo,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        shadowElevation = ThemeValue.Shadow.Surface
    ) {
        WebImage(
            uri = info.pic,
            contentScale = ContentScale.Crop,
            alpha = 0.1f,
            modifier = Modifier.fillMaxSize().zIndex(1f)
        )
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(ThemeValue.Padding.EqualValue)
                .zIndex(2f),
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
        ) {
            RachelText(
                text = info.name,
                style = MaterialTheme.typography.labelLarge,
                icon = Icons.Outlined.MusicNote,
                color = MaterialTheme.colorScheme.primary
            )
            RachelText(
                text = "ID: ${info.id}",
                icon = Icons.Outlined.Badge
            )
            RachelText(
                text = "演唱: ${info.singer}",
                icon = ExtraIcons.Artist
            )
            RachelText(
                text = "时长: ${info.time}",
                icon = Icons.Outlined.Schedule
            )
            Text(
                text = remember(info) { LyricsLrc.Parser(info.lyrics).plainText },
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private interface PlatformMusicParser {
    suspend fun parseLink(link: String): Data<List<PlatformMusicInfo>>

    companion object {
        fun build(type: PlatformMusicType): PlatformMusicParser = when (type) {
            PlatformMusicType.QQMusic -> QQMusicParser()
            PlatformMusicType.NetEaseCloud -> NetEaseCloudParser()
            PlatformMusicType.Kugou -> KugouParser()
        }
    }
}

private class QQMusicParser : PlatformMusicParser {
    override suspend fun parseLink(link: String): Data<List<PlatformMusicInfo>> = when {
        // 歌曲 https://c6.y.qq.com/base/fcgi-bin/u?__=8e1SWwxbKv0F
        link.contains("c6.y.qq.com") -> Coroutines.io {
            when (val tmp = QQMusicAPI.requestMusicId(link)) {
                is Data.Success -> QQMusicAPI.requestMusic(tmp.data)
                is Data.Error -> tmp
            }
        }.map { listOf(it) }
        // 歌曲 https://y.qq.com/n/ryqq/songDetail/003yJ3Ba1bDVJc
        link.contains("y.qq.com") && link.contains("songDetail") -> Coroutines.io {
            QQMusicAPI.requestMusic(link.substringAfterLast("/"))
        }.map { listOf(it) }
        // 歌单 https://i2.y.qq.com/n3/other/pages/share/personalized_playlist_v2/index.html?id=9094549201
        link.contains("i2.y.qq.com") && link.contains("playlist") -> Coroutines.io {
            val id = Uri.parse(link)?.params["id"]
            if (id != null) QQMusicAPI.requestPlaylist(id) else Data.Error()
        }
        // 歌单 https://y.qq.com/n/ryqq/playlist/9094549201
        link.contains("y.qq.com") && link.contains("playlist") -> Coroutines.io {
            QQMusicAPI.requestPlaylist(link.substringAfterLast("/"))
        }
        // 歌曲 003yJ3Ba1bDVJc
        else -> Coroutines.io {
            QQMusicAPI.requestMusic(link)
        }.map { listOf(it) }
    }
}

private class NetEaseCloudParser : PlatformMusicParser {
    override suspend fun parseLink(link: String): Data<List<PlatformMusicInfo>> = when {
        // 歌曲 http://163cn.tv/EElG0jr
        link.contains("163cn.tv") -> Coroutines.io {
            when (val tmp = NetEaseCloudAPI.requestMusicId(link)) {
                is Data.Success -> NetEaseCloudAPI.requestMusic(tmp.data)
                is Data.Error -> tmp
            }
        }.map { listOf(it) }
        // 歌单 https://y.music.163.com/m/playlist?id=13674538430&userid=10015279209&creatorId=10015279209
        link.contains("music.163.com") && link.contains("playlist") -> Coroutines.io {
            val id = Uri.parse(link)?.params["id"]
            if (id != null) NetEaseCloudAPI.requestPlaylist(id) else Data.Error()
        }
        // 歌曲 https://music.163.com/#/song?textid=1064008&id=504686858
        link.contains("music.163.com") && link.contains("song") -> Coroutines.io {
            val id = Uri.parse(link)?.params["id"]
            if (id != null) NetEaseCloudAPI.requestMusic(id) else Data.Error()
        }.map { listOf(it) }
        // 歌曲 504686858
        else -> Coroutines.io {
            NetEaseCloudAPI.requestMusic(link)
        }.map { listOf(it) }
    }
}

private class KugouParser : PlatformMusicParser {
    override suspend fun parseLink(link: String): Data<List<PlatformMusicInfo>> {
        return Data.Error()
    }
}

@Stable
class ScreenPlatformMusic(model: AppModel, args: Args) : SubScreen<ScreenPlatformMusic.Args>(model) {
    @Stable
    @Serializable
    data class Args(val deeplink: String?, val type: PlatformMusicType)

    private var platformType by mutableStateOf(args.type)
    private var linkState = TextInputState(args.deeplink ?: "")
    private var items by mutableStateOf(emptyList<PlatformMusicInfo>())

    private suspend fun downloadMusic() {
        try {
            slot.loading.openSuspend()
            val ids = mutableListOf<String>()
            Coroutines.io {
                for (item in items) {
                    // 1. 下载音频
                    val audioFile = OS.Storage.createTempFile { sink ->
                        app.fileClient.safeDownload(
                            url = item.audioUrl,
                            sink = sink,
                            isCancel = { false },
                            onGetSize = { },
                            onTick = { _, _ -> }
                        )
                    }
                    // 2. 下载封面
                    val recordFile = OS.Storage.createTempFile { sink ->
                        app.fileClient.safeDownload(
                            url = item.pic,
                            sink = sink,
                            isCancel = { false },
                            onGetSize = { },
                            onTick = { _, _ -> }
                        )
                    }
                    if (audioFile == null || recordFile == null) continue
                    if ((SystemFileSystem.metadataOrNull(audioFile)?.size ?: 0L) <= 1024 * 1024L) continue
                    if ((SystemFileSystem.metadataOrNull(recordFile)?.size ?: 0L) <= 1024 * 10L) continue
                    // 3. 生成目录
                    val id = "${platformType.prefix}${item.id}"
                    val musicPath = Path(OS.Storage.musicPath, id)
                    SystemFileSystem.createDirectories(musicPath)
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
                    SystemFileSystem.sink(info.configPath).buffered().use { sink ->
                        sink.writeText(info.toJsonString())
                    }
                    // 5. 写入音频
                    SystemFileSystem.sink(info.audioPath).buffered().use { sink ->
                        SystemFileSystem.source(audioFile).buffered().use { source ->
                            source.transferTo(sink)
                        }
                    }
                    // 6. 写入封面
                    SystemFileSystem.sink(info.recordPath).buffered().use { sink ->
                        SystemFileSystem.source(recordFile).buffered().use { source ->
                            source.transferTo(sink)
                        }
                    }
                    // 7. 写入壁纸
                    SystemFileSystem.sink(info.backgroundPath).buffered().use { sink ->
                        sink.write(Res.readBytes("files/black_background.webp"))
                    }
                    // 8. 写入歌词
                    SystemFileSystem.sink(info.lyricsPath).buffered().use { sink ->
                        sink.writeText(item.lyrics)
                    }
                    // 9. 更新曲库
                    ids += id
                }
            }
            require(ids.isNotEmpty())
            app.musicFactory.updateMusicLibraryInfo(ids)
            slot.tip.success("已成功导入${ids.size}首歌曲")
        }
        catch (_: Throwable) {
            slot.tip.warning("下载失败")
        }
        finally {
            slot.loading.close()
        }
    }

    override val title: String by derivedStateOf { platformType.description }

    @Composable
    override fun ActionScope.LeftActions() {
        if (linkState.text.isNotEmpty() || items.isNotEmpty()) {
            Action(Icons.Outlined.Refresh) {
                linkState.text = ""
                items = emptyList()
            }
        }
    }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(
            icon = Icons.Outlined.Preview,
            enabled = linkState.text.isNotEmpty()
        ) {
            val parser = PlatformMusicParser.build(platformType)
            val result = parser.parseLink(linkState.text)
            when (result) {
                is Data.Success -> items = result.data
                is Data.Error -> slot.tip.warning("解析失败")
            }
        }
        ActionSuspend(
            icon = Icons.Outlined.Download,
            enabled = items.isNotEmpty()
        ) {
            downloadMusic()
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        Column(
            modifier = Modifier.fillMaxSize().padding(ThemeValue.Padding.EqualValue),
            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlatformMusicType.entries.forEach {
                    RachelRadioButton(
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
                columns = GridCells.Adaptive(ThemeValue.Size.CardWidth),
                contentPadding = ThemeValue.Padding.EqualValue,
                horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
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