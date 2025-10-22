package love.yinlin.ui.screen.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.*
import love.yinlin.compose.*
import love.yinlin.compose.data.ImageQuality
import love.yinlin.data.MimeType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicResource
import love.yinlin.data.music.MusicResourceType
import love.yinlin.extension.*
import love.yinlin.platform.*
import love.yinlin.compose.ui.image.ClickIcon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.LocalFileImage
import love.yinlin.compose.ui.image.MiniIcon
import love.yinlin.compose.ui.text.TextInput
import love.yinlin.compose.ui.text.rememberTextInputState
import love.yinlin.compose.ui.input.LoadingClickText
import love.yinlin.compose.ui.input.NormalText
import love.yinlin.ui.component.layout.ExpandableLayout
import love.yinlin.ui.component.lyrics.LyricsLrc
import love.yinlin.compose.ui.floating.FloatingArgsSheet
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.component.screen.dialog.FloatingDialogCrop

@Stable
class ScreenMusicDetails(model: AppModel, val args: Args) : SubScreen<ScreenMusicDetails.Args>(model) {
    @Stable
    @Serializable
    data class Args(val id: String)

    abstract class ResourceStrategy(
        val enabled: Boolean
    )

    @Stable
    sealed class DeleteStrategy(enabled: Boolean = true): ResourceStrategy(enabled) {
        suspend fun ScreenMusicDetails.invoke(item: ResourceItem) {
            if (!enabled) return
            if (!slot.confirm.openSuspend(content = "删除资源")) return
            catching {
                val resourceFile = Path(OS.Storage.musicPath, args.id, item.resource.toString())
                Coroutines.io { SystemFileSystem.delete(resourceFile) }
                resources.removeAll { it == item }
            }
        }

        @Stable
        data object Disabled : DeleteStrategy(false)

        @Stable
        data object NoOption : DeleteStrategy()
    }

    @Stable
    sealed class ReplaceStrategy(
        enabled: Boolean = true,
        protected val needUpdateInfo: Boolean
    ): ResourceStrategy(enabled) {
        open suspend fun ScreenMusicDetails.openSource(): Source? = null

        suspend fun ScreenMusicDetails.invoke(item: ResourceItem) {
            val id = args.id
            val musicPath = musicInfo?.path
            if (!enabled || musicPath == null) return
            catching {
                Coroutines.io {
                    openSource()?.use { source ->
                        val resourceFile = Path(OS.Storage.musicPath, id, item.resource.toString())
                        SystemFileSystem.sink(resourceFile).buffered().use { sink ->
                            source.transferTo(sink)
                        }
                        Coroutines.main {
                            if (needUpdateInfo) {
                                app.musicFactory.musicLibrary.findAssign(id) {
                                    it.copy(modification = it.modification + 1)
                                }
                            }
                            val newLength = SystemFileSystem.metadataOrNull(resourceFile)?.size?.toInt() ?: 0
                            resources.findAssign(predicate = { it == item }) {
                                makeResourceItem(it.resource, newLength)
                            }
                        }
                    }
                }
            }
        }

        @Stable
        data object Disabled : ReplaceStrategy(enabled = false, needUpdateInfo = false)

        @Stable
        data class Picture(val aspectRatio: Float = 0f) : ReplaceStrategy(needUpdateInfo = true) {
            override suspend fun ScreenMusicDetails.openSource(): Source? = Picker.pickPicture()?.use { source ->
                OS.Storage.createTempFile { sink -> source.transferTo(sink) > 0L }
            }?.let { path ->
                cropDialog.openSuspend(url = path.toString(), aspectRatio = aspectRatio)?.let { rect ->
                    OS.Storage.createTempFile { sink ->
                        SystemFileSystem.source(path).buffered().use { source ->
                            ImageProcessor(ImageCrop(rect), ImageCompress, quality = ImageQuality.Full).process(source, sink)
                        }
                    }?.let { SystemFileSystem.source(it).buffered() }
                }
            }
        }

        @Stable
        data class File(
            val mimeType: List<String> = emptyList(),
            val filter: List<String> = emptyList()
        ) : ReplaceStrategy(needUpdateInfo = false) {
            override suspend fun ScreenMusicDetails.openSource(): Source? = Picker.pickFile(mimeType, filter)
        }
    }

    @Stable
    sealed class ModifyStrategy(enabled: Boolean = true): ResourceStrategy(enabled) {
        @Composable
        open fun ScreenMusicDetails.ModifyLayout(item: ResourceItem) {}

        @Stable
        data object Disabled : ModifyStrategy(false)

        @Stable
        data object Config : ModifyStrategy() {
            @Composable
            override fun ScreenMusicDetails.ModifyLayout(item: ResourceItem) {
                val name = rememberTextInputState(init = musicInfo?.name ?: "")
                val singer = rememberTextInputState(init = musicInfo?.singer ?: "")
                val lyricist = rememberTextInputState(init = musicInfo?.lyricist ?: "")
                val composer = rememberTextInputState(init = musicInfo?.composer ?: "")
                val album = rememberTextInputState(init = musicInfo?.album ?: "")

                Column(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                ) {
                    LoadingClickText(
                        text = "修改",
                        icon = Icons.Outlined.Check,
                        enabled = name.ok && singer.ok && lyricist.ok && composer.ok && album.ok,
                        onClick = {
                            val id = args.id
                            app.musicFactory.musicLibrary.findAssign(id) {
                                it.copy(
                                    name = name.text,
                                    singer = singer.text,
                                    lyricist = lyricist.text,
                                    composer = composer.text,
                                    album = album.text,
                                    modification = it.modification + 1
                                )
                            }?.let { newInfo ->
                                catching {
                                    val configPath = newInfo.configPath
                                    Coroutines.io {
                                        SystemFileSystem.sink(configPath).buffered().use { sink ->
                                            sink.writeString(newInfo.toJsonString())
                                        }
                                    }
                                    resources.findAssign(predicate = { it == item }) {
                                        makeResourceItem(it.resource, SystemFileSystem.metadataOrNull(configPath)!!.size.toInt())
                                    }
                                }
                            }
                            modifySheet.close()
                        }
                    )
                    TextInput(
                        state = name,
                        hint = "歌曲名",
                        maxLength = 16,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextInput(
                        state = singer,
                        hint = "演唱",
                        maxLength = 16,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextInput(
                        state = lyricist,
                        hint = "作词",
                        maxLength = 16,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextInput(
                        state = composer,
                        hint = "作曲",
                        maxLength = 16,
                        imeAction = ImeAction.Next,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextInput(
                        state = album,
                        hint = "专辑",
                        maxLength = 16,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        @Stable
        data object LineLyrics : ModifyStrategy() {
            @Composable
            override fun ScreenMusicDetails.ModifyLayout(item: ResourceItem) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.sheetValue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "暂未开放")
                }
            }
        }
    }

    @Stable
    data class ResourceItem(
        val resource: MusicResource,
        val length: Int,
        val onDelete: DeleteStrategy,
        val onReplace: ReplaceStrategy,
        val onModify: ModifyStrategy
    )

    companion object {
        private fun makeResourceItem(resource: MusicResource, length: Int): ResourceItem {
            val type = resource.type
            val onDelete: DeleteStrategy = when (type) {
                MusicResourceType.Config, MusicResourceType.Record, MusicResourceType.Background -> DeleteStrategy.Disabled
                MusicResourceType.Audio, MusicResourceType.LineLyrics -> if (type.defaultName == resource.name) DeleteStrategy.Disabled else DeleteStrategy.NoOption
                MusicResourceType.Animation, MusicResourceType.Video, MusicResourceType.Rhyme, null -> DeleteStrategy.NoOption
            }
            val onReplace: ReplaceStrategy = when (type) {
                MusicResourceType.Config, MusicResourceType.LineLyrics, MusicResourceType.Rhyme, null -> ReplaceStrategy.Disabled
                MusicResourceType.Audio -> ReplaceStrategy.File(mimeType = listOf(MimeType.MP3, MimeType.FLAC), filter = listOf("*.mp3", "*.flac"))
                MusicResourceType.Record -> ReplaceStrategy.Picture(aspectRatio = 1f)
                MusicResourceType.Background -> ReplaceStrategy.Picture(aspectRatio = 0.5625f)
                MusicResourceType.Animation -> ReplaceStrategy.File(mimeType = listOf(MimeType.WEBP), filter = listOf("*.webp"))
                MusicResourceType.Video -> ReplaceStrategy.File(mimeType = listOf(MimeType.MP4), filter = listOf("*.mp4"))
            }
            val onModify: ModifyStrategy = when (type) {
                MusicResourceType.Config -> ModifyStrategy.Config
                MusicResourceType.LineLyrics -> ModifyStrategy.LineLyrics
                else -> ModifyStrategy.Disabled
            }
            return ResourceItem(
                resource = resource,
                length = length,
                onDelete = onDelete,
                onReplace = onReplace,
                onModify = onModify
            )
        }

        private suspend fun loadLyrics(info: MusicInfo): String = Coroutines.io {
            catchingDefault("") {
                val lyrics = SystemFileSystem.source(info.lyricsPath).buffered().use { it.readString() }
                LyricsLrc.Parser(lyrics).plainText
            }
        }

        private suspend fun loadResources(info: MusicInfo): List<ResourceItem> = Coroutines.io {
            catchingDefault({ emptyList() }) {
                SystemFileSystem.list(info.path).map { path ->
                    makeResourceItem(
                        resource = MusicResource.fromString(path.name)!!,
                        length = SystemFileSystem.metadataOrNull(path)!!.size.toInt()
                    )
                }
            }
        }
    }

    private val musicInfo by derivedStateOf { app.musicFactory.musicLibrary[args.id] }
    private var lyricsText by mutableStateOf("")
    private val resources = mutableStateListOf<ResourceItem>()

    @Composable
    private fun MusicMetadataLayout(modifier: Modifier = Modifier) {
        musicInfo?.let { info ->
            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = CustomTheme.shadow.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(5f),
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
                                text = "来源: ${info.author}",
                                icon = Icons.Outlined.Person
                            )
                        }
                        LocalFileImage(
                            path = { info.recordPath },
                            info,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(3f).aspectRatio(1f).clip(MaterialTheme.shapes.large)
                        )
                    }
                    NormalText(
                        text = "演唱: ${info.singer}",
                        icon = ExtraIcons.Artist
                    )
                    NormalText(
                        text = "作词: ${info.lyricist}",
                        icon = Icons.Outlined.Lyrics
                    )
                    NormalText(
                        text = "作曲: ${info.composer}",
                        icon = Icons.Outlined.Lyrics
                    )
                    NormalText(
                        text = "专辑: ${info.album}",
                        icon = Icons.Outlined.Album
                    )
                }
            }
        } ?: Box(modifier = modifier) { EmptyBox() }
    }

    @Composable
    private fun MusicLyricsLayout(modifier: Modifier = Modifier) {
        var isExpanded by rememberFalse()

        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = CustomTheme.shadow.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalExtraValue),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "歌词",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ClickIcon(
                        icon = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        onClick = { isExpanded = !isExpanded }
                    )
                }
                ExpandableLayout(isExpanded) {
                    SelectionContainer {
                        Text(
                            text = lyricsText,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MusicResourceCard(item: ResourceItem) {
        Box(
            modifier = Modifier.width(CustomTheme.size.cellWidth)
                .shadow(
                    elevation = CustomTheme.shadow.surface,
                    shape = MaterialTheme.shapes.extraLarge
                )
                .background(
                    brush = remember(item) { item.resource.type.background },
                    shape = MaterialTheme.shapes.extraLarge
                )
                .clickable {}
                .padding(CustomTheme.padding.equalValue)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniIcon(
                        icon = item.resource.type.icon,
                        color = Colors.White
                    )
                    Text(
                        text = item.resource.type?.description ?: "未知资源",
                        color = Colors.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = item.resource.name,
                    color = Colors.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = remember(item) { item.length.toLong().fileSizeString },
                    color = Colors.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace, Alignment.End)
                ) {
                    if (item.onDelete.enabled) {
                        LoadingIcon(
                            icon = Icons.Outlined.Delete,
                            tip = "删除",
                            color = Colors.White,
                            onClick = {
                                if (app.musicFactory.isReady) slot.tip.warning("请先停止播放器")
                                else with(item.onDelete) { invoke(item) }
                            }
                        )
                    }
                    if (item.onReplace.enabled) {
                        LoadingIcon(
                            icon = Icons.Outlined.FindReplace,
                            tip = "替换",
                            color = Colors.White,
                            onClick = {
                                if (app.musicFactory.isReady) slot.tip.warning("请先停止播放器")
                                else with(item.onReplace) { invoke(item) }
                            }
                        )
                    }
                    if (item.onModify.enabled) {
                        LoadingIcon(
                            icon = Icons.Outlined.Edit,
                            tip = "编辑",
                            color = Colors.White,
                            onClick = {
                                if (app.musicFactory.isReady) slot.tip.warning("请先停止播放器")
                                else modifySheet.open(item)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MusicResourceLayout(modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = CustomTheme.shadow.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue),
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = CustomTheme.padding.horizontalSpace),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "资源",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ClickIcon(
                        icon = Icons.Outlined.Add,
                        tip = "导入",
                        onClick = { }
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.equalSpace),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (resource in resources) {
                        MusicResourceCard(item = resource)
                    }
                }
            }
        }
    }

    @Composable
    private fun Portrait() {
        Column(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize().verticalScroll(rememberScrollState())) {
            MusicMetadataLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue))
            MusicLyricsLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue))
            MusicResourceLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue))
        }
    }

    @Composable
    private fun Landscape() {
        Row(modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()) {
            Column(modifier = Modifier.width(CustomTheme.size.panelWidth).fillMaxHeight().verticalScroll(rememberScrollState())) {
                MusicMetadataLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue))
                MusicLyricsLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue))
            }
            Box(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                MusicResourceLayout(modifier = Modifier.fillMaxWidth().padding(CustomTheme.padding.equalValue))
            }
        }
    }

    override suspend fun initialize() {
        musicInfo?.let { info ->
            launch { lyricsText = loadLyrics(info) }
            launch { resources.replaceAll(loadResources(info)) }
        }
    }

    override val title: String by derivedStateOf { musicInfo?.name ?: "" }

    @Composable
    override fun SubContent(device: Device) = when (device.type) {
        Device.Type.PORTRAIT -> Portrait()
        Device.Type.LANDSCAPE, Device.Type.SQUARE -> Landscape()
    }

    private val modifySheet = object : FloatingArgsSheet<ResourceItem>() {
        @Composable
        override fun Content(args: ResourceItem) {
            with(args.onModify) { ModifyLayout(args) }
        }
    }

    private val cropDialog = FloatingDialogCrop()

    @Composable
    override fun Floating() {
        modifySheet.Land()
        cropDialog.Land()
    }
}