package love.yinlin.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.utils.io.core.readText
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode
import love.yinlin.data.music.MusicPlaylist
import love.yinlin.data.music.MusicResourceType
import love.yinlin.extension.parseJsonValue

abstract class MusicFactory {
    // 初始化
    abstract val isInit: Boolean
    abstract suspend fun init()

    // 库
    val musicLibrary = mutableStateMapOf<String, MusicInfo>()
    val playlistLibrary = mutableStateMapOf<String, MusicPlaylist>()

    protected suspend fun initLibrary() {
        Coroutines.io {
            val musicPath = OS.Storage.musicPath
            SystemFileSystem.list(musicPath).map { it.name }.forEach { id ->
                try {
                    val configPath = Path(musicPath, id, MusicResourceType.Config.defaultFilename)
                    val info = SystemFileSystem.source(configPath).buffered().use { it.readText().parseJsonValue<MusicInfo>() }!!
                    musicLibrary.put(info.id, info)
                }
                catch (_: Throwable) { }
            }
            playlistLibrary.put("测试", MusicPlaylist("测试", listOf("1", "2", "3", "4")))
        }
    }

    // 当前状态
    var currentPlayMode: MusicPlayMode by mutableStateOf(MusicPlayMode.ORDER)
        protected set
    var currentPlaylist: MusicPlaylist? by mutableStateOf(null)
        protected set
    var currentMusic: MusicInfo? by mutableStateOf(null)
        protected set

    // 接口
    abstract suspend fun start(playlist: MusicPlaylist, startId: String? = null)
    abstract suspend fun play()
    abstract suspend fun pause()
    abstract suspend fun stop()
}