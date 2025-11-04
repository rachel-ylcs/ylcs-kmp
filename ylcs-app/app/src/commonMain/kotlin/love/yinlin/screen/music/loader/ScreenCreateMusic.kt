package love.yinlin.screen.music.loader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import love.yinlin.app
import love.yinlin.common.Paths
import love.yinlin.uri.ImplicitUri
import love.yinlin.compose.*
import love.yinlin.data.compose.ImageQuality
import love.yinlin.compose.graphics.ImageCrop
import love.yinlin.compose.graphics.ImageProcessor
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.MimeType
import love.yinlin.data.music.MusicInfo
import love.yinlin.extension.DateEx
import love.yinlin.extension.toJsonString
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.TextInputState
import love.yinlin.compose.ui.floating.FloatingDialogCrop
import love.yinlin.compose.ui.image.ReplaceableImage
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.data.mod.ModResourceType
import love.yinlin.extension.catching
import love.yinlin.extension.catchingError
import love.yinlin.platform.Coroutines
import love.yinlin.platform.lyrics.LrcParser

@Stable
private class MusicInfoState {
    val id = TextInputState(DateEx.CurrentLong.toString())
    val name = TextInputState()
    val author = TextInputState(app.config.userProfile?.name ?: "")
    val singer = TextInputState("未知")
    val lyricist = TextInputState("未知")
    val composer = TextInputState("未知")
    val album = TextInputState("未知")
    val lyrics = TextInputState("")
    var record: String? by mutableStateOf(null)
    var background: String? by mutableStateOf(null)
    var audioUri: ImplicitUri? by mutableRefStateOf(null)

    val canSubmit by derivedStateOf {
        id.ok && name.ok && singer.ok && lyricist.ok && composer.ok &&
        album.ok && lyrics.ok && record != null && background != null && audioUri != null
    }
}

@Stable
class ScreenCreateMusic(manager: ScreenManager) : Screen(manager) {
    private val input = MusicInfoState()

    override val title: String = "创建MOD"

    private suspend fun pickPicture(aspectRatio: Float, onPicAdd: (Path) -> Unit) {
        val path = app.picker.pickPicture()?.use { source ->
            app.os.storage.createTempFile { sink -> source.transferTo(sink) > 0L }
        }
        if (path != null) {
            cropDialog.openSuspend(url = path.toString(), aspectRatio = aspectRatio)?.let { rect ->
                app.os.storage.createTempFile { sink ->
                    SystemFileSystem.source(path).buffered().use { source ->
                        ImageProcessor(ImageCrop(rect), quality = ImageQuality.Full).process(source, sink)
                    }
                }?.let { onPicAdd(it) }
            }
        }
    }

    private suspend fun submit() {
        slot.loading.openSuspend()
        Coroutines.io {
            catching {
                // 1. 检查ID
                val id = input.id.text
                val name = input.name.text
                if (id in app.mp.library) {
                    slot.tip.warning("ID已存在")
                    return@catching
                }
                if (!id.all { it.isLetterOrDigit() }) {
                    slot.tip.warning("ID仅能由字母或数字构成")
                    return@catching
                }
                // 2. 检查歌词
                val lyrics = LrcParser(input.lyrics.text)
                if (!lyrics.ok) {
                    slot.tip.warning("歌词格式非法")
                    return@catching
                }
                // 3. 检查文件
                val audioFile = input.audioUri
                val recordFile = input.record
                val backgroundFile = input.background
                if (audioFile == null || recordFile == null || backgroundFile == null) {
                    slot.tip.warning("资源文件异常")
                    return@catching
                }
                // 4. 生成目录
                val musicPath = Path(Paths.modPath, id)
                SystemFileSystem.createDirectories(musicPath)
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
                SystemFileSystem.sink(info.path(Paths.modPath, ModResourceType.Config)).buffered().use { sink ->
                    sink.writeString(info.toJsonString())
                }
                // 6. 写入音频
                SystemFileSystem.sink(info.path(Paths.modPath, ModResourceType.Audio)).buffered().use { sink ->
                    audioFile.read { source ->
                        source.transferTo(sink)
                    }
                }
                // 7. 写入封面
                SystemFileSystem.sink(info.path(Paths.modPath, ModResourceType.Record)).buffered().use { sink ->
                    SystemFileSystem.source(Path(recordFile)).buffered().use { source ->
                        source.transferTo(sink)
                    }
                }
                // 8. 写入壁纸
                SystemFileSystem.sink(info.path(Paths.modPath, ModResourceType.Background)).buffered().use { sink ->
                    SystemFileSystem.source(Path(backgroundFile)).buffered().use { source ->
                        source.transferTo(sink)
                    }
                }
                // 9. 写入歌词
                SystemFileSystem.sink(info.path(Paths.modPath, ModResourceType.LineLyrics)).buffered().use { sink ->
                    sink.writeString(lyrics.toString())
                }
                // 10. 更新曲库
                app.mp.updateMusicLibraryInfo(listOf(id))
                pop()
            }
        }
        slot.loading.close()
    }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(
            icon = Icons.Outlined.Check,
            tip = "创建",
            enabled = input.canSubmit
        ) {
            if (app.mp.isReady) slot.tip.warning("请先停止播放器")
            else submit()
        }
    }

    @Composable
    override fun Content(device: Device) {
        Column(
            modifier = Modifier
                .padding(LocalImmersivePadding.current)
                .fillMaxSize()
                .padding(CustomTheme.padding.equalValue)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
        ) {
            TextInput(
                state = input.id,
                hint = "唯一ID(仅字母或数字)",
                maxLength = 32,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            TextInput(
                state = input.author,
                hint = "作者",
                maxLength = 32,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            TextInput(
                state = input.name,
                hint = "歌名",
                maxLength = 32,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            TextInput(
                state = input.singer,
                hint = "演唱",
                maxLength = 32,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            TextInput(
                state = input.lyricist,
                hint = "作词",
                maxLength = 32,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            TextInput(
                state = input.composer,
                hint = "作曲",
                maxLength = 32,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            TextInput(
                state = input.album,
                hint = "专辑",
                maxLength = 32,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            TextInput(
                state = input.lyrics,
                hint = "歌词",
                maxLines = 5,
                clearButton = false,
                modifier = Modifier.fillMaxWidth()
            )
            Text(text = "封面 & 背景", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReplaceableImage(
                    pic = input.record,
                    modifier = Modifier.weight(0.64f).aspectRatio(1f),
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ReplaceableImage(
                    pic = input.background,
                    modifier = Modifier.weight(0.36f).aspectRatio(0.5625f),
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
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = remember(input.audioUri) { input.audioUri?.path ?: "" },
                    onValueChange = { },
                    label = { Text(text = "音源", style = MaterialTheme.typography.titleMedium) },
                    readOnly = true,
                    trailingIcon = {
                        Row(
                            modifier = Modifier.padding(end = CustomTheme.padding.horizontalSpace),
                            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ClickIcon(
                                icon = Icons.Outlined.Add,
                                onClick = {
                                    launch {
                                        input.audioUri = app.picker.pickPath(
                                            mimeType = listOf(MimeType.AUDIO),
                                            filter = listOf("*.mp3", "*.flac", "*.m4a", "*.wav")
                                        )
                                    }
                                }
                            )
                            ClickIcon(
                                icon = Icons.Outlined.Clear,
                                onClick = { input.audioUri = null }
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    private val cropDialog = this land FloatingDialogCrop()
}