package love.yinlin.common

import love.yinlin.AppModel
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.platform.app
import love.yinlin.ui.screen.community.ScreenUserCard
import love.yinlin.ui.screen.music.loader.ScreenImportMusic
import love.yinlin.ui.screen.music.loader.ScreenPlatformMusic

class DeepLink(private val model: AppModel) {
    private fun schemeContent(uri: Uri) {
        if (!app.musicFactory.isReady) model.navigate(ScreenImportMusic.Args(uri.toString()))
        else model.slot.tip.warning("请先停止播放器")
    }

    private fun schemeRachel(uri: Uri) {
        when (uri.path) {
            "/openProfile" -> {
                uri.params["uid"]?.toIntOrNull()?.let { uid ->
                    model.navigate(ScreenUserCard.Args(uid))
                }
            }
        }
    }

    fun process(uri: Uri) {
        when (uri.scheme) {
            Scheme.File -> schemeContent(uri)
            Scheme.Content -> schemeContent(uri)
            Scheme.Rachel -> schemeRachel(uri)
            Scheme.QQMusic -> {
                if (!app.musicFactory.isReady) model.navigate(ScreenPlatformMusic.Args(
                    deeplink = uri.copy(scheme = Scheme.Https).toString(),
                    type = PlatformMusicType.QQMusic
                ))
                else model.slot.tip.warning("请先停止播放器")
            }
            Scheme.NetEaseCloud -> {
                if (!app.musicFactory.isReady) model.navigate(ScreenPlatformMusic.Args(
                    deeplink = uri.copy(scheme = Scheme.Https).toString(),
                    type = PlatformMusicType.NetEaseCloud
                ))
                else model.slot.tip.warning("请先停止播放器")
            }
        }
    }
}