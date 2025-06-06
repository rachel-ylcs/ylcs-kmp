package love.yinlin.ui.screen.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.GifBox
import androidx.compose.material.icons.outlined.MusicVideo
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.AppModel
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.Device
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.common.ThemeValue
import love.yinlin.data.Data
import love.yinlin.data.rachel.song.Song
import love.yinlin.platform.app
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.layout.EmptyBox
import love.yinlin.ui.component.layout.Pagination
import love.yinlin.ui.component.layout.PaginationGrid
import love.yinlin.ui.component.screen.CommonSubScreen

@Composable
private fun SongCard(
    song: Song,
    status: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(ThemeValue.Padding.ExtraValue),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ThemeValue.Padding.VerticalSpace)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = song.name,
                style = MaterialTheme.typography.labelMedium,
                color = if (status) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.weight(3f)
            )
            Text(
                text = song.id,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ThemeValue.Padding.HorizontalExtraSpace),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = song.version,
                color = if (status) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
            if (song.bgd) {
                MiniIcon(
                    icon = Icons.Outlined.GifBox,
                    color = if (status) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    size = ThemeValue.Size.MicroIcon
                )
            }
            if (song.video) {
                MiniIcon(
                    icon = Icons.Outlined.MusicVideo,
                    color = if (status) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    size = ThemeValue.Size.MicroIcon
                )
            }
        }
    }
}

@Stable
class ScreenMusicModFactory(model: AppModel) : CommonSubScreen(model) {
    private val pageSongs = object : Pagination<Song, Int, Int>(0) {
        override fun distinctValue(item: Song): Int = item.sid
        override fun offset(item: Song): Int = item.sid
    }

    private val gridState = LazyGridState()

    private suspend fun requestNewData() {
        val result = ClientAPI.request(
            route = API.User.Song.GetSongs,
            data = API.User.Song.GetSongs.Request(num = pageSongs.pageNum)
        )
        when (result) {
            is Data.Success -> pageSongs.newData(result.data)
            is Data.Error -> slot.tip.error(result.message)
        }
    }

    private suspend fun requestMoreData() {
        val result = ClientAPI.request(
            route = API.User.Song.GetSongs,
            data = API.User.Song.GetSongs.Request(
                sid = pageSongs.offset,
                num = pageSongs.pageNum
            )
        )
        if (result is Data.Success) pageSongs.moreData(result.data)
    }

    override val title: String = "MOD工坊"

    override suspend fun initialize() {
        requestNewData()
    }

    @Composable
    override fun SubContent(device: Device) {
        if (pageSongs.items.isEmpty()) EmptyBox()
        else {
            PaginationGrid(
                items = pageSongs.items,
                key = { it.sid },
                columns = GridCells.Adaptive(ThemeValue.Size.CardWidth),
                state = gridState,
                canRefresh = true,
                canLoading = pageSongs.canLoading,
                onRefresh = { requestNewData() },
                onLoading = { requestMoreData() },
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
            ) {
                SongCard(
                    song = it,
                    status = app.musicFactory.musicLibrary.contains(it.id),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        navigate(ScreenSongDetails.Args(it))
                    }
                )
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabCanExpand: Boolean = false

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestNewData() }
        else gridState.animateScrollToItem(0)
    }
}