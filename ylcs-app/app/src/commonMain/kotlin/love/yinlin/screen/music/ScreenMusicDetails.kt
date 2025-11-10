package love.yinlin.screen.music

import androidx.compose.runtime.*
import love.yinlin.app
import love.yinlin.compose.Device
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.music.Song

@Stable
class ScreenMusicDetails(manager: ScreenManager, id: String) : Screen(manager) {
    private val clientSong: Song? by mutableStateOf(app.mp.library[id]?.let { musicInfo ->
        Song(
            id = musicInfo.id,
            version = musicInfo.version,
            name = musicInfo.name,
            singer = musicInfo.singer,
            lyricist = musicInfo.lyricist,
            composer = musicInfo.composer,
            album = musicInfo.album,
        )
    })
    private val remoteSong: Song? by mutableStateOf(null)

    override val title: String by derivedStateOf { clientSong?.name ?: remoteSong?.name ?: "未知歌曲" }

    override suspend fun initialize() {

    }

    @Composable
    override fun Content(device: Device) {

    }
}