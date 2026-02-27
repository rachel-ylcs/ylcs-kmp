package love.yinlin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import kotlinx.io.files.Path
import love.yinlin.app
import love.yinlin.common.PathMod
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.data.ImageQuality
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.graphics.PlatformImage
import love.yinlin.compose.graphics.crop
import love.yinlin.compose.graphics.decode
import love.yinlin.compose.graphics.encode
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.ReplaceableBox
import love.yinlin.compose.ui.floating.DialogCrop
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.node.dashBorder
import love.yinlin.compose.ui.text.Input
import love.yinlin.compose.ui.text.InputState
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.compose.ui.text.Text
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.MimeType
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.catchingError
import love.yinlin.extension.lazyProvider
import love.yinlin.extension.mkdir
import love.yinlin.extension.readByteArray
import love.yinlin.extension.toJsonString
import love.yinlin.extension.write
import love.yinlin.extension.writeText
import love.yinlin.extension.writeTo
import love.yinlin.startup.StartupMusicPlayer
import love.yinlin.tpl.lyrics.LrcParser
import love.yinlin.uri.ImplicitUri

@Stable
class ScreenCreateMusic : Screen() {
    @Stable
    private class MusicInfoState {
        val id = InputState(DateEx.CurrentLong.toString(), maxLength = 32)
        val name = InputState(maxLength = 32)
        val author = InputState(app.config.userProfile?.name ?: "", maxLength = 32)
        val singer = InputState(maxLength = 32)
        val lyricist = InputState(maxLength = 32)
        val composer = InputState(maxLength = 32)
        val album = InputState(maxLength = 32)
        val lyrics = InputState("")
        var record: String? by mutableStateOf(null)
        var background: String? by mutableStateOf(null)
        var audioUri: ImplicitUri? by mutableRefStateOf(null)

        val canSubmit by derivedStateOf {
            id.isSafe && name.isSafe && singer.isSafe && lyricist.isSafe && composer.isSafe &&
                    album.isSafe && lyrics.isSafe && record != null && background != null && audioUri != null
        }
    }

    private val mp by lazyProvider { app.startup<StartupMusicPlayer>() }

    private val input = MusicInfoState()

    private suspend fun pickPicture(aspectRatio: Float, onPicAdd: (Path) -> Unit) {
        val path = app.picker.pickPicture()?.use { source ->
            app.os.storage.createTempFile { sink -> source.transferTo(sink) > 0L }
        }
        if (path != null) {
            cropDialog.open(url = path.toString(), aspectRatio = aspectRatio)?.let { rect ->
                app.os.storage.createTempFile { sink ->
                    val image = PlatformImage.decode(path.readByteArray()!!)!!
                    image.crop(rect)
                    sink.write(image.encode(quality = ImageQuality.Full)!!)
                    true
                }?.let { onPicAdd(it) }
            }
        }
    }

    private suspend fun submit() {
        val player = mp ?: return
        slot.loading.open {
            catchingError {
                // 1. 检查ID
                val id = input.id.text
                val name = input.name.text
                require(id !in player.library) { "ID已存在" }
                require(id.all { it.isLetterOrDigit() }) { "ID仅能由字母或数字构成" }
                // 2. 检查歌词
                val lyrics = LrcParser(input.lyrics.text)
                require(lyrics.ok) { "歌词格式非法" }
                // 3. 检查文件
                val audioFile = input.audioUri
                val recordFile = input.record
                val backgroundFile = input.background
                require(audioFile != null && recordFile != null && backgroundFile != null) { "资源文件异常" }
                Coroutines.io {
                    // 4. 生成目录
                    val musicPath = Path(PathMod, id)
                    musicPath.mkdir()
                    // 5. 写入配置
                    val info = MusicInfo(
                        version = "1.0",
                        author = input.author.text,
                        id = id,
                        name = name,
                        singer = input.singer.text,
                        lyricist = input.lyricist.text,
                        composer = input.composer.text,
                        album = input.album.text,
                        chorus = null
                    )
                    info.path(PathMod, ModResourceType.Config).writeText(info.toJsonString())
                    // 6. 写入音频
                    info.path(PathMod, ModResourceType.Audio).write { sink ->
                        audioFile.read { source ->
                            source.transferTo(sink)
                        }
                    }
                    // 7. 写入封面
                    Path(recordFile).writeTo(info.path(PathMod, ModResourceType.Record))
                    // 8. 写入壁纸
                    Path(backgroundFile).writeTo(info.path(PathMod, ModResourceType.Background))
                    // 9. 写入歌词
                    info.path(PathMod, ModResourceType.LineLyrics).writeText(lyrics.toString())
                }
                // 10. 更新曲库
                player.updateMusicLibraryInfo(listOf(id))
                pop()
            }.warningTip
        }
    }

    override val title: String = "创建MOD"

    @Composable
    override fun RowScope.RightActions() {
        LoadingIcon(icon = Icons.Check, tip = "创建", enabled = input.canSubmit, onClick = {
            if (mp?.isReady == true) slot.tip.warning("请先停止播放器")
            else submit()
        })
    }

    @Composable
    override fun Content() {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(Theme.padding.eValue)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.v)
        ) {
            Input(
                state = input.id,
                hint = "唯一ID(仅字母或数字)",
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            Input(
                state = input.author,
                hint = "作者",
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            Input(
                state = input.name,
                hint = "歌名",
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            Input(
                state = input.singer,
                hint = "演唱",
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            Input(
                state = input.lyricist,
                hint = "作词",
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            Input(
                state = input.composer,
                hint = "作曲",
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            Input(
                state = input.album,
                hint = "专辑",
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            Input(
                state = input.lyrics,
                hint = "LRC格式歌词",
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            SimpleEllipsisText(text = "封面", style = Theme.typography.v6.bold)
            ReplaceableBox(
                value = input.record,
                onReplace = {
                    launch {
                        pickPicture(1f) { input.record = it.toString() }
                    }
                },
                onDelete = { input.record = null }
            ) { uri ->
                WebImage(
                    uri = uri,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(Theme.size.image5)
                )
            }

            SimpleEllipsisText(text = "背景", style = Theme.typography.v6.bold)
            ReplaceableBox(
                value = input.background,
                onReplace = {
                    launch {
                        pickPicture(0.5625f) { input.background = it.toString() }
                    }
                },
                onDelete = { input.background = null }
            ) { uri ->
                WebImage(
                    uri = uri,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(Theme.size.image5).aspectRatio(0.5625f)
                )
            }

            SimpleEllipsisText(text = "音源", style = Theme.typography.v6.bold)
            ReplaceableBox(
                value = input.audioUri,
                onReplace = {
                    launch {
                        input.audioUri = app.picker.pickPath(
                            mimeType = listOf(MimeType.AUDIO),
                            filter = listOf("*.mp3", "*.flac", "*.m4a", "*.wav")
                        )
                    }
                },
                onDelete = { input.audioUri = null }
            ) { uri ->
                Box(
                    modifier = Modifier.dashBorder(Theme.border.v5, Theme.color.primary, Theme.shape.v7).padding(Theme.padding.value9),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = uri.path)
                }
            }
        }
    }

    private val cropDialog = this land DialogCrop()
}