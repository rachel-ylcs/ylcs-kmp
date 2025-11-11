package love.yinlin.screen.music

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import love.yinlin.Local
import love.yinlin.api.API
import love.yinlin.api.APIConfig
import love.yinlin.api.ClientAPI
import love.yinlin.api.ServerRes
import love.yinlin.app
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.floating.FloatingDialogInput
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.EmptyBox
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationGrid
import love.yinlin.data.Data
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.rachel.song.SongPreview

@Stable
class ScreenModCenter(manager: ScreenManager) : Screen(manager) {
    private val pageSongs = object : Pagination<SongPreview, String, String>(
        default = "0",
        pageNum = APIConfig.MAX_PAGE_NUM
    ) {
        override fun distinctValue(item: SongPreview): String = item.sid
        override fun offset(item: SongPreview): String = item.sid
    }

    private val gridState = LazyGridState()

    private suspend fun requestNewData() {
        val result = ClientAPI.request(
            route = API.User.Song.GetSongs,
            data = API.User.Song.GetSongs.Request(num = pageSongs.pageNum)
        )
        when (result) {
            is Data.Success -> {
                pageSongs.newData(result.data)
                gridState.scrollToItem(0)
            }
            is Data.Failure -> slot.tip.error(result.message)
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

    private suspend fun searchNewData(key: String) {
        val result = ClientAPI.request(
            route = API.User.Song.SearchSongs,
            data = key
        )
        when (result) {
            is Data.Success -> {
                pageSongs.newData(result.data)
                pageSongs.canLoading = false
                gridState.scrollToItem(0)
            }
            is Data.Failure -> slot.tip.error(result.message)
        }
    }

    override val title: String = "工坊"

    override suspend fun initialize() {
        requestNewData()
    }

    @Composable
    override fun ActionScope.RightActions() {
        ActionSuspend(Icons.Outlined.Search, "搜索") {
            val result = searchDialog.openSuspend()
            if (result != null) searchNewData(result)
        }
    }

    @Composable
    private fun SongCard(
        song: SongPreview,
        status: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Row(
            modifier = modifier.clickable(onClick = onClick).padding(CustomTheme.padding.extraValue),
            horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalSpace),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WebImage(
                uri = remember(song) { "${Local.API_BASE_URL}/${ServerRes.Mod.Song(song.sid).res(ModResourceType.Record.filename)}" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(CustomTheme.size.image).clip(MaterialTheme.shapes.large)
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.verticalSpace)
            ) {
                Text(
                    text = song.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (status) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CustomTheme.padding.horizontalExtraSpace),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = song.version,
                        color = if (status) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis
                    )
                }
            }
            Text(
                text = song.sid,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(alignment = Alignment.Top)
            )
        }
    }

    @Composable
    override fun Content(device: Device) {
        if (pageSongs.items.isEmpty()) EmptyBox()
        else {
            PauseLoading(gridState)

            PaginationGrid(
                items = pageSongs.items,
                key = { it.sid },
                columns = GridCells.Adaptive(CustomTheme.size.cardWidth),
                state = gridState,
                canRefresh = true,
                canLoading = pageSongs.canLoading,
                onRefresh = { requestNewData() },
                onLoading = { requestMoreData() },
                modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
            ) {
                SongCard(
                    song = it,
                    status = it.sid in app.mp.library,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        navigate(::ScreenMusicDetails, it.sid)
                    }
                )
            }
        }
    }

    private val isScrollTop: Boolean by derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }

    override val fabIcon: ImageVector get() = if (isScrollTop) Icons.Outlined.Refresh else Icons.Outlined.ArrowUpward

    override suspend fun onFabClick() {
        if (isScrollTop) launch { requestNewData() }
        else gridState.animateScrollToItem(0)
    }

    private val searchDialog = this land FloatingDialogInput(hint = "歌曲名", maxLength = 32)
}