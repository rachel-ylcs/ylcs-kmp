package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.data.ItemKey
import love.yinlin.compose.ds.DataSourceMusic
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.floating.DialogServerModFilter
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABScrollTop
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.cs.*
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.rachel.song.Song
import love.yinlin.data.rachel.song.SongFilter
import love.yinlin.extension.DateEx

@Stable
class ScreenModCenter : Screen() {
    private val gridState = LazyGridState()

    private var baseLibrary: List<Song> by mutableStateOf([])
    private val latestLibrary: List<Song> by derivedStateOf {
        baseLibrary.sortedByDescending { DateEx.Formatter.standardDate.parse(it.time) }.take(4)
    }
    private var currentLibrary: List<Song> by mutableStateOf([])
    private var currentFilter: SongFilter? by mutableStateOf(null)

    override val title: String = "银临合集"

    private fun searchSongs(filter: SongFilter): List<Song> = currentLibrary.filter { (val name, val singer, val lyricist, val composer, val album, val animation, val video, val rhyme, val accompaniment) ->
        (!filter.useAnimation || animation) &&
                (!filter.useVideo || video) &&
                (!filter.useRhyme || rhyme) &&
                (!filter.useAccompaniment || accompaniment) &&
                (filter.key?.let { name.contains(it, ignoreCase = true) } ?: true) &&
                (filter.singer?.let { singer.contains(it, ignoreCase = true) } ?: true) &&
                (filter.lyricist?.let { lyricist.contains(it, ignoreCase = true) } ?: true) &&
                (filter.composer?.let { composer.contains(it, ignoreCase = true) } ?: true) &&
                (filter.album?.let { album.contains(it, ignoreCase = true) } ?: true)
    }

    override fun initialize() {
        launch {
            val [result] = ApiSongGetSongLibrary.requestNull() ?: return@launch
            if (result.isNotEmpty()) {
                baseLibrary = result
                currentLibrary = result
            }
        }
    }

    @Composable
    override fun RowScope.LeftActions() {
        if (currentFilter != null) {
            Icon(icon = Icons.Clear, tip = "关闭搜索", onClick = {
                currentLibrary = baseLibrary
                currentFilter = null
            })
        }
    }

    @Composable
    override fun RowScope.RightActions() {
        LoadingIcon(icon = Icons.Search, tip = "搜索", onClick = {
            val result = filterDialog.open(currentFilter)
            if (result != null) {
                val targetSongs = searchSongs(result)
                if (targetSongs.isEmpty()) slot.tip.warning("未搜索到满足条件的歌曲")
                else {
                    currentLibrary = targetSongs
                    currentFilter = result
                }
            }
        })
    }

    @Composable
    private fun SongCard(modifier: Modifier, song: Song, status: Boolean) {
        Row(
            modifier = modifier.height(IntrinsicSize.Min).clickable {
                navigate(::ScreenMusicDetails, song.sid)
            }.padding(Theme.padding.value9),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WebImage(
                source = ServerRes.Mod.Song(song.sid).res(ModResourceType.Record.filename).url,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxHeight().aspectRatio(1f).clip(Theme.shape.v8)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                SimpleEllipsisText(text = song.name, style = Theme.typography.v7.bold, color = if (status) Theme.color.primary else LocalColor.current)
                SimpleEllipsisText(text = song.singer, style = Theme.typography.v8, color = LocalColorVariant.current)
            }
        }
    }

    @Composable
    override fun Content() {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(Theme.size.cell4),
            state = gridState,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
        ) {
            item(key = ItemKey("title1"), span = { GridItemSpan(maxLineSpan) }) {
                SimpleEllipsisText(
                    text = "新歌速递",
                    style = Theme.typography.v5.bold,
                    color = Theme.color.secondary,
                    modifier = Modifier.padding(Theme.padding.value9)
                )
            }

            items(items = latestLibrary, key = { it.sid to 0 }) { song ->
                SongCard(
                    modifier = Modifier.fillMaxWidth(),
                    song = song,
                    status = song.sid in DataSourceMusic.library
                )
            }

            item(key = ItemKey("title2"), span = { GridItemSpan(maxLineSpan) }) {
                SimpleEllipsisText(
                    text = "曲库 / ${baseLibrary.size} 首",
                    style = Theme.typography.v5.bold,
                    color = Theme.color.tertiary,
                    modifier = Modifier.padding(Theme.padding.value9)
                )
            }

            items(items = currentLibrary, key = { it.sid }) { song ->
                SongCard(
                    modifier = Modifier.fillMaxWidth(),
                    song = song,
                    status = song.sid in DataSourceMusic.library
                )
            }
        }
    }

    override val fab: FAB = FABScrollTop(gridState)

    private val filterDialog = this land DialogServerModFilter()
}
