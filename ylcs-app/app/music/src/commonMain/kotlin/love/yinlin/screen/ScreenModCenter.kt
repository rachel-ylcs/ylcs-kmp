package love.yinlin.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import love.yinlin.app
import love.yinlin.compose.LocalColor
import love.yinlin.compose.LocalColorVariant
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.ui.container.RachelStatefulProvider
import love.yinlin.compose.ui.container.StatefulBox
import love.yinlin.compose.ui.floating.DialogChoice
import love.yinlin.compose.ui.floating.DialogInput
import love.yinlin.compose.ui.floating.FAB
import love.yinlin.compose.ui.floating.FABScrollTop
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.LoadingIcon
import love.yinlin.compose.ui.image.WebImage
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationGrid
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.cs.*
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.rachel.song.SongPreview
import love.yinlin.extension.lazyProvider
import love.yinlin.startup.StartupMusicPlayer

@Stable
class ScreenModCenter : Screen() {
    companion object {
        val AlbumList = listOf(
            "银临EP",
            "腐草为萤",
            "蚍蜉渡海",
            "风花雪月",
            "琉璃",
            "离地十公分·A面",
            "离地十公分·B面",
            "山色有无中",
            "粼粼",
            "单曲集",
            "影视剧OST",
            "游戏OST"
        )
    }

    private val mp by lazyProvider { app.startup<StartupMusicPlayer>() }

    private val provider = RachelStatefulProvider()

    private val pageSongs = object : Pagination<SongPreview, String, String>(
        default = "0",
        pageNum = APIConfig.MAX_PAGE_NUM
    ) {
        override fun distinctValue(item: SongPreview): String = item.sid
        override fun offset(item: SongPreview): String = item.sid
    }

    private val gridState = LazyGridState()

    private suspend fun requestNewData(loading: Boolean) {
        provider.withLoading(loading) {
            gridState.requestScrollToItem(0)
            pageSongs.newData(ApiSongGetSongs.requestNull(pageSongs.default, pageSongs.pageNum)!!.o1)
        }
    }

    private suspend fun requestMoreData() {
        ApiSongGetSongs.request(pageSongs.offset, pageSongs.pageNum) { pageSongs.moreData(it) }
    }

    private suspend fun searchNewData(key: String) {
        provider.withLoading {
            gridState.requestScrollToItem(0)
            val result = ApiSongSearchSongs.requestNull(key)!!.o1
            pageSongs.newData(result)
            pageSongs.canLoading = false
            result.isNotEmpty()
        }
    }

    private suspend fun filterAlbumNewData(album: String) {
        provider.withLoading {
            gridState.requestScrollToItem(0)
            val result = ApiSongSearchSongsByAlbum.requestNull(album)!!.o1
            pageSongs.newData(result)
            pageSongs.canLoading = false
            result.isNotEmpty()
        }
    }

    override val title: String = "工坊"

    override suspend fun initialize() {
        requestNewData(true)
    }

    @Composable
    override fun RowScope.RightActions() {
        LoadingIcon(icon = Icons.Search, tip = "搜索", onClick = {
            val result = searchDialog.open()
            if (result != null) searchNewData(result)
        })
        LoadingIcon(icon = Icons.Filter, tip = "专辑筛选", onClick = {
            val result = albumDialog.open()
            if (result != null) filterAlbumNewData(AlbumList[result])
        })
    }

    @Composable
    private fun SongCard(song: SongPreview, status: Boolean, onClick: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Theme.padding.value),
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WebImage(
                uri = ServerRes.Mod.Song(song.sid).res(ModResourceType.Record.filename).url,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(Theme.size.image8).clip(Theme.shape.v6)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Theme.padding.v9)
            ) {
                SimpleEllipsisText(text = song.name, style = Theme.typography.v7.bold, color = if (status) Theme.color.primary else LocalColor.current)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimpleEllipsisText(text = song.sid, color = LocalColorVariant.current)
                    SimpleEllipsisText(text = song.version, style = Theme.typography.v8, color = LocalColorVariant.current)
                }
            }
        }
    }

    @Composable
    override fun Content() {
        StatefulBox(
            provider = provider,
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize()
        ) {
            PaginationGrid(
                items = pageSongs.items,
                key = { it.sid },
                columns = GridCells.Adaptive(Theme.size.cell4),
                state = gridState,
                canRefresh = true,
                canLoading = pageSongs.canLoading,
                onRefresh = { requestNewData(false) },
                onLoading = ::requestMoreData,
                modifier = Modifier.fillMaxSize()
            ) { song ->
                SongCard(song = song, status = mp?.library?.let { song.sid in it } ?: false, onClick = {
                    navigate(::ScreenMusicDetails, song.sid)
                })
            }
        }
    }

    override val fab: FAB = FABScrollTop(gridState)

    private val searchDialog = this land DialogInput(hint = "歌曲名", maxLength = 32)

    private val albumDialog = this land DialogChoice.fromItems(AlbumList)
}