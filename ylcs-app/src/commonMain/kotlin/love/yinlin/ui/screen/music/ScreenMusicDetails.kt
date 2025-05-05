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
import androidx.compose.ui.text.style.TextOverflow
import io.ktor.utils.io.*
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.ExtraIcons
import love.yinlin.common.ThemeValue
import love.yinlin.data.MimeType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicResource
import love.yinlin.data.music.MusicResourceType
import love.yinlin.extension.fileSizeString
import love.yinlin.extension.rememberState
import love.yinlin.extension.replaceAll
import love.yinlin.platform.*
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.LoadingIcon
import love.yinlin.ui.component.image.LocalFileImage
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.ExpandableLayout
import love.yinlin.ui.component.lyrics.LyricsLrc
import love.yinlin.ui.component.screen.SubScreen

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
        suspend operator fun invoke(id: String, resource: MusicResource): Boolean {
            if (enabled) {
                try {
                    val resourceFile = Path(OS.Storage.musicPath, id, resource.toString())
                    Coroutines.io { SystemFileSystem.delete(resourceFile) }
                    return true
                }
                catch (_: Throwable) {}
            }
            return false
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
        open suspend fun openSource(): Source? = null

        suspend operator fun invoke(id: String, resource: MusicResource): Boolean {
            if (enabled) {
                try {
                    openSource()?.use { source ->
                        val resourceFile = Path(OS.Storage.musicPath, id, resource.toString())
                        SystemFileSystem.sink(resourceFile).buffered().use { sink ->
                            source.transferTo(sink)
                        }
                        if (needUpdateInfo) {
                            val musicLibrary =  app.musicFactory.musicLibrary
                            musicLibrary[id]?.let { info ->
                                musicLibrary[id] = info.copy(modification = info.modification + 1)
                            }
                        }
                        return true
                    }
                }
                catch (_: Throwable) { }
            }
            return false
        }

        @Stable
        data object Disabled : ReplaceStrategy(enabled = false, needUpdateInfo = false)

        @Stable
        data object Picture : ReplaceStrategy(needUpdateInfo = true) {
            override suspend fun openSource(): Source? = Picker.pickPicture()
        }

        @Stable
        data class File(
            val mimeType: List<String> = emptyList(),
            val filter: List<String> = emptyList()
        ) : ReplaceStrategy(needUpdateInfo = false) {
            override suspend fun openSource(): Source? = Picker.pickFile(mimeType, filter)
        }
    }

    @Stable
    sealed class ModifyStrategy(enabled: Boolean = true): ResourceStrategy(enabled) {
        @Composable
        open fun ScreenMusicDetails.ModifyLayout(modifier: Modifier = Modifier) {}

        @Stable
        data object Disabled : ModifyStrategy(false)

        @Stable
        data object Config : ModifyStrategy() {
            @Composable
            override fun ScreenMusicDetails.ModifyLayout(modifier: Modifier) {

            }
        }

        @Stable
        data object LineLyrics : ModifyStrategy() {
            @Composable
            override fun ScreenMusicDetails.ModifyLayout(modifier: Modifier) {

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

    private val musicInfo by derivedStateOf { app.musicFactory.musicLibrary[args.id] }
    private var lyricsText by mutableStateOf("")
    private val resources = mutableStateListOf<ResourceItem>()

    fun makeResourceItem(resource: MusicResource, length: Int): ResourceItem {
        val type = resource.type
        val onDelete = when (type) {
            MusicResourceType.Config, MusicResourceType.Record, MusicResourceType.Background -> DeleteStrategy.Disabled
            MusicResourceType.Audio, MusicResourceType.LineLyrics -> if (type.defaultName == resource.name) DeleteStrategy.Disabled else DeleteStrategy.NoOption
            MusicResourceType.Animation, MusicResourceType.Video, null -> DeleteStrategy.NoOption
        }
        val onReplace = when (type) {
            MusicResourceType.Config, MusicResourceType.LineLyrics, null -> ReplaceStrategy.Disabled
            MusicResourceType.Audio -> ReplaceStrategy.File(mimeType = listOf(MimeType.MP3, MimeType.FLAC), filter = listOf("*.mp3", "*.flac"))
            MusicResourceType.Record -> ReplaceStrategy.Picture
            MusicResourceType.Background -> ReplaceStrategy.Picture
            MusicResourceType.Animation -> ReplaceStrategy.File(mimeType = listOf(MimeType.WEBP), filter = listOf("*.webp"))
            MusicResourceType.Video -> ReplaceStrategy.File(mimeType = listOf(MimeType.MP4), filter = listOf("*.mp4"))
        }
        val onModify = when (type) {
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
        try {
            val lyrics = SystemFileSystem.source(info.lyricsPath).buffered().use {
                it.readText()
            }
            LyricsLrc.parseLrcPlainText(lyrics)
        }
        catch (_: Throwable) { "" }
    }

    private suspend fun loadResources(info: MusicInfo): List<ResourceItem> = Coroutines.io {
        try {
            SystemFileSystem.list(info.path).map { path ->
                makeResourceItem(
                    resource = MusicResource.fromString(path.name)!!,
                    length = SystemFileSystem.metadataOrNull(path)!!.size.toInt()
                )
            }
        }
        catch (_: Throwable) { emptyList() }
    }

    @Composable
    private fun MusicMetadataLayout(modifier: Modifier = Modifier) {
        musicInfo?.let { info ->
            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = ThemeValue.Shadow.Surface
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue),
                    verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(5f),
                            verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
                        ) {
                            RachelText(
                                text = info.name,
                                icon = Icons.Outlined.MusicNote,
                                color = MaterialTheme.colorScheme.primary
                            )
                            RachelText(
                                text = "ID: ${info.id}",
                                icon = Icons.Outlined.Badge
                            )
                            RachelText(
                                text = "来源: ${info.author}",
                                icon = Icons.Outlined.Person
                            )
                        }
                        LocalFileImage(
                            path = info.recordPath,
                            quality = ImageQuality.Full,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(3f).aspectRatio(1f).clip(MaterialTheme.shapes.large)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RachelText(
                            text = "演唱: ${info.singer}",
                            icon = ExtraIcons.Artist
                        )
                        RachelText(
                            text = "作词: ${info.lyricist}",
                            icon = Icons.Outlined.Lyrics
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RachelText(
                            text = "作曲: ${info.composer}",
                            icon = Icons.Outlined.Lyrics
                        )
                        RachelText(
                            text = "专辑: ${info.album}",
                            icon = Icons.Outlined.Album
                        )
                    }
                }
            }
        } ?: Box(modifier = modifier) { EmptyBox() }
    }

    @Composable
    private fun MusicLyricsLayout(modifier: Modifier = Modifier) {
        var isExpanded by rememberState { false }

        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = ThemeValue.Shadow.Surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualExtraValue),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
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
            modifier = Modifier.width(ThemeValue.Size.CellWidth)
                .shadow(
                    elevation = ThemeValue.Shadow.Surface,
                    shape = MaterialTheme.shapes.extraLarge
                )
                .background(
                    brush = remember(item) { item.resource.type.background },
                    shape = MaterialTheme.shapes.extraLarge
                )
                .clickable {}
                .padding(ThemeValue.Padding.EqualValue)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace),
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
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalSpace, Alignment.End)
                ) {
                    if (item.onDelete.enabled) {
                        LoadingIcon(
                            icon = Icons.Outlined.Delete,
                            color = Colors.White,
                            onClick = { }
                        )
                    }
                    if (item.onReplace.enabled) {
                        LoadingIcon(
                            icon = Icons.Outlined.FindReplace,
                            color = Colors.White,
                            onClick = { }
                        )
                    }
                    if (item.onModify.enabled) {
                        LoadingIcon(
                            icon = Icons.Outlined.Edit,
                            color = Colors.White,
                            onClick = { }
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
            shadowElevation = ThemeValue.Shadow.Surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue),
                verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ThemeValue.Padding.HorizontalSpace),
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
                        onClick = { }
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.EqualSpace),
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
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MusicMetadataLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue))
            MusicLyricsLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue))
            MusicResourceLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue))
        }
    }

    @Composable
    private fun Landscape() {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.width(ThemeValue.Size.PanelWidth).fillMaxHeight().verticalScroll(rememberScrollState())) {
                MusicMetadataLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue))
                MusicLyricsLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue))
            }
            Box(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                MusicResourceLayout(modifier = Modifier.fillMaxWidth().padding(ThemeValue.Padding.EqualValue))
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
}