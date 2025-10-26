package love.yinlin

import androidx.compose.runtime.Stable
import love.yinlin.api.API
import love.yinlin.api.ClientAPI
import love.yinlin.common.uri.Scheme
import love.yinlin.common.uri.Uri
import love.yinlin.compose.screen.DeepLink
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.Data
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.screen.community.ScreenUserCard
import love.yinlin.screen.music.ScreenSongDetails
import love.yinlin.screen.music.loader.ScreenImportMusic
import love.yinlin.screen.music.loader.ScreenPlatformMusic

@Stable
data object AppDeepLink : DeepLink {
    private fun schemeContent(manager: ScreenManager, uri: Uri) {
        if (!service.musicFactory.instance.isReady) manager.navigate(ScreenImportMusic.Args(uri.toString()))
        else manager.top.slot.tip.warning("请先停止播放器")
    }

    private fun schemeRachel(manager: ScreenManager, uri: Uri) {
        when (uri.path) {
            "/openProfile" -> {
                uri.params["uid"]?.toIntOrNull()?.let { uid ->
                    manager.navigate(ScreenUserCard.Args(uid))
                }
            }
            "/openSong" -> {
                uri.params["id"]?.let { id ->
                    manager.top.launch {
                        val result = ClientAPI.request(
                            route = API.User.Song.GetSong,
                            data = id
                        )
                        if (result is Data.Success) manager.navigate(ScreenSongDetails.Args(result.data))
                    }
                }
            }
        }
    }

    override fun process(manager: ScreenManager, uri: Uri) {
        when (uri.scheme) {
            Scheme.File -> schemeContent(manager, uri)
            Scheme.Content -> schemeContent(manager, uri)
            Scheme.Rachel -> schemeRachel(manager, uri)
            Scheme.QQMusic -> {
                if (!service.musicFactory.instance.isReady) manager.navigate(ScreenPlatformMusic.Args(
                    deeplink = uri.copy(scheme = Scheme.Https).toString(),
                    type = PlatformMusicType.QQMusic
                ))
                else manager.top.slot.tip.warning("请先停止播放器")
            }
            Scheme.NetEaseCloud -> {
                if (!service.musicFactory.instance.isReady) manager.navigate(ScreenPlatformMusic.Args(
                    deeplink = uri.copy(scheme = Scheme.Https).toString(),
                    type = PlatformMusicType.NetEaseCloud
                ))
                else manager.top.slot.tip.warning("请先停止播放器")
            }
        }
    }
}