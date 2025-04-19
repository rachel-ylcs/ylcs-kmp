package love.yinlin.ui.screen.music

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.ktor.utils.io.*
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.Colors
import love.yinlin.common.ExtraIcons
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicResourceType
import love.yinlin.extension.fileSizeString
import love.yinlin.extension.rememberState
import love.yinlin.extension.replaceAll
import love.yinlin.platform.*
import love.yinlin.resources.Res
import love.yinlin.resources.no_audio_source
import love.yinlin.ui.component.image.ClickIcon
import love.yinlin.ui.component.image.LocalFileImage
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.input.LoadingRachelButton
import love.yinlin.ui.component.input.RachelText
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.ExpandableLayout
import love.yinlin.ui.component.lyrics.LyricsLrc
import love.yinlin.ui.component.screen.SubScreen
import love.yinlin.ui.screen.Screen
import org.jetbrains.compose.resources.stringResource

@Stable
class ScreenMusicDetails(model: AppModel, val args: Args) : Screen<ScreenMusicDetails.Args>(model) {
    @Stable
    @Serializable
    data class Args(val id: String) : Screen.Args

    data class MusicResourceItem(
        val id: Int,
        val type: MusicResourceType?,
        val name: String,
        val size: Long
    )

    private val musicInfo by derivedStateOf { app.musicFactory.musicLibrary[args.id] }
    private var lyricsText by mutableStateOf("")
    private val resources = mutableStateListOf<MusicResourceItem>()

    @Composable
    private fun MusicMetadataLayout(
        info: MusicInfo,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 5.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        LoadingRachelButton(
                            text = info.name,
                            icon = Icons.Outlined.MusicNote,
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = {}
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
                        modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(MaterialTheme.shapes.large)
                    )
                }
                LoadingRachelButton(
                    text = "演唱: ${info.singer}",
                    icon = ExtraIcons.Artist,
                    onClick = {}
                )
                LoadingRachelButton(
                    text = "作词: ${info.lyricist}",
                    icon = Icons.Outlined.Lyrics,
                    onClick = {}
                )
                LoadingRachelButton(
                    text = "作曲: ${info.composer}",
                    icon = Icons.Outlined.Lyrics,
                    onClick = {}
                )
                LoadingRachelButton(
                    text = "专辑: ${info.album}",
                    icon = Icons.Outlined.Album,
                    onClick = {}
                )
            }
        }
    }

    @Composable
    private fun MusicLyricsLayout(modifier: Modifier = Modifier) {
        var isExpanded by rememberState { false }

        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 5.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                ExpandableLayout(
                    isExpanded = isExpanded
                ) {
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
    private fun MusicResourceCard(
        item: MusicResourceItem,
        modifier: Modifier = Modifier
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            color = item.type?.background ?: MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniIcon(
                        icon = item.type?.icon ?: Icons.Outlined.QuestionMark,
                        color = Colors.White
                    )
                    Text(
                        text = item.type?.description ?: "未知资源",
                        color = Colors.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = item.name,
                    color = Colors.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = item.size.fileSizeString,
                    color = Colors.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun MusicResourceLayout(modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 5.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (resource in resources) {
                        MusicResourceCard(
                            item = resource,
                            modifier = Modifier.width(150.dp)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Portrait(info: MusicInfo) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            MusicMetadataLayout(info = info, modifier = Modifier.fillMaxWidth().padding(10.dp))
            MusicLyricsLayout(modifier = Modifier.fillMaxWidth().padding(10.dp))
            MusicResourceLayout(modifier = Modifier.fillMaxWidth().padding(10.dp))
        }
    }

    @Composable
    private fun Landscape(info: MusicInfo) {

    }

    override suspend fun initialize() {
        musicInfo?.let { info ->
            launch {
                lyricsText = Coroutines.io {
                    try {
                        val lyrics = SystemFileSystem.source(info.lyricsPath).buffered().use {
                            it.readText()
                        }
                        LyricsLrc.parseLrcPlainText(lyrics)
                    }
                    catch (_: Throwable) { "" }
                }
            }
            launch {
                val result = Coroutines.io {
                    try {
                        SystemFileSystem.list(info.path).map { path ->
                            val (idString, name) = path.name.split("-")
                            val id = idString.toInt()
                            MusicResourceItem(
                                id = id,
                                type = MusicResourceType.fromInt(id),
                                name = name,
                                size = SystemFileSystem.metadataOrNull(path)!!.size
                            )
                        }
                    }
                    catch (_: Throwable) { emptyList() }
                }
                resources.replaceAll(result)
            }
        }
    }

    @Composable
    override fun content() {
        SubScreen(
            modifier = Modifier.fillMaxSize(),
            title = musicInfo?.name ?: stringResource(Res.string.no_audio_source),
            onBack = { pop() },
            slot = slot
        ) {
            musicInfo?.let {
                if (app.isPortrait) Portrait(info = it)
                else Landscape(info = it)
            } ?: EmptyBox()
        }
    }
}