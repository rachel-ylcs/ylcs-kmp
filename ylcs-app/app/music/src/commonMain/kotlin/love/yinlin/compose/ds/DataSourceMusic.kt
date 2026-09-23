package love.yinlin.compose.ds

import androidx.compose.runtime.*
import androidx.compose.ui.util.fastFilter
import love.yinlin.app
import love.yinlin.coroutines.Coroutines
import love.yinlin.data.mod.ModResourceType
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.Playlist
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue
import love.yinlin.fs.File

@Stable
object DataSourceMusic {
    private val modPath by lazy { app.modPath }

    // 当前歌单
    var playlist: Playlist by mutableStateOf(Playlist.None)
        private set

    // 曲库
    val library = mutableStateMapOf<String, MusicInfo>()

    // 初始化曲库
    suspend fun initLibrary() {
        val items = Coroutines.io {
            modPath.list().mapNotNull {
                val configPath = File(modPath, it.name, ModResourceType.Config.filename)
                catchingNull { configPath.readText()!!.parseJsonValue<MusicInfo>() }
            }
        }
        for (item in items) library[item.id] = item
    }

    // 更新歌单
    fun updatePlaylist(newPlaylist: Playlist) { playlist = newPlaylist }

    /**
     * 提取当前播放列表的媒体
     *
     * 可能会随着媒体变动从而更新与当前播放的列表不一致的结果
     */
    fun fetchCurrentPlaylist(list: Playlist): List<String> = when (list) {
        is Playlist.None -> []
        is Playlist.Default -> library.values.map { it.id }
        is Playlist.User -> app.config.playlistLibrary[list.name]?.items?.fastFilter { it in library } ?: []
    }

    /**
     * 载入指定媒体的配置 并更新修改标记
     */
    suspend fun reloadMusicInfo(id: String): MusicInfo? {
        val modification = library[id]?.modification ?: 0
        val configPath = File(modPath, id, ModResourceType.Config.filename)
        val info = catchingNull { configPath.readText()!!.parseJsonValue<MusicInfo>() }
        return info?.copy(modification = modification + 1)
    }

    /**
     * 载入所有指定媒体的配置 并更新修改标记
     */
    suspend fun reloadMusicInfoMap(ids: Iterable<String>): Map<String, MusicInfo> = Coroutines.io {
        buildMap {
            for (id in ids) {
                val info = reloadMusicInfo(id)
                if (info != null) put(id, info)
            }
        }
    }
}