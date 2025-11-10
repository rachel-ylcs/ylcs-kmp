package love.yinlin.screen.music

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import love.yinlin.api.APIConfig
import love.yinlin.app
import love.yinlin.common.Paths
import love.yinlin.compose.CustomTheme
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.image.PauseLoading
import love.yinlin.compose.ui.layout.ActionScope
import love.yinlin.compose.ui.layout.Pagination
import love.yinlin.compose.ui.layout.PaginationColumn
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.rachel.song.Song
import love.yinlin.data.rachel.song.SongComment
import love.yinlin.extension.exists

@Stable
class ScreenMusicDetails(manager: ScreenManager, private val id: String) : Screen(manager) {
    private var clientSong: Song? by mutableStateOf(updateClientSong())
    private var remoteSong: Song? by mutableStateOf(null)

    private val pageComments = object : Pagination<SongComment, Long, Long>(
        default = 0L,
        pageNum = APIConfig.MIN_PAGE_NUM
    ) {
        override fun distinctValue(item: SongComment): Long = item.cid
        override fun offset(item: SongComment): Long = item.cid
    }

    override val title: String by derivedStateOf { clientSong?.name ?: remoteSong?.name ?: "未知歌曲" }

    private fun updateClientSong(): Song? = app.mp.library[id]?.let { musicInfo ->
        Song(
            id = musicInfo.id,
            version = musicInfo.version,
            name = musicInfo.name,
            singer = musicInfo.singer,
            lyricist = musicInfo.lyricist,
            composer = musicInfo.composer,
            album = musicInfo.album,
            animation = musicInfo.path(Paths.modPath, ModResourceType.Animation).exists,
            video = musicInfo.path(Paths.modPath, ModResourceType.Video).exists,
            rhyme = musicInfo.path(Paths.modPath, ModResourceType.Rhyme).exists
        )
    }

    override suspend fun initialize() {

    }

    @Composable
    private fun DetailsLayout() {

    }

    @Composable
    private fun ResourceLayout() {

    }

    @Composable
    override fun ActionScope.RightActions() {

    }

    @Composable
    override fun BottomBar() {
        if (app.config.userProfile != null) {

        }
    }

    @Composable
    private fun Portrait() {
        val listState = rememberLazyListState()
        PauseLoading(listState)
    }

    @Composable
    private fun Landscape() {

    }

    @Composable
    override fun Content(device: Device) {
        when (device.type) {
            Device.Type.PORTRAIT, Device.Type.SQUARE -> Portrait()
            Device.Type.LANDSCAPE -> Landscape()
        }
    }
}