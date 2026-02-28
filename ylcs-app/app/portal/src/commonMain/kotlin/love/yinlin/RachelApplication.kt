package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.common.PathMod
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.foundation.useNotPlatformStartupLazyFetcher
import love.yinlin.platform.Platform
import love.yinlin.screen.*
import love.yinlin.startup.StartupMusicPlayer
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri

@Stable
abstract class RachelApplication(delegate: PlatformContextDelegate) : AbstractRachelApplication(delegate) {
    private val mp by service(
        useNotPlatformStartupLazyFetcher(*Platform.Web) { PathMod },
        factory = ::StartupMusicPlayer
    )

    @Composable
    override fun Content() {
        ScreenManager.Navigation<ScreenMain>(deeplink = this) {
            // common
            screen(::ScreenMain)

            // music
            screen(::ScreenMusicLibrary)
            screen(::ScreenPlaylistLibrary)
            screen(::ScreenLyricsSettings)
            screen(::ScreenMusicDetails)
            screen(::ScreenModCenter)
            screen(::ScreenCreateMusic)
            screen(::ScreenImportMusic)
            screen(::ScreenPlatformMusic)

            // information
            screen(::ScreenAlbum)
            screen(::ScreenActivityDetails)
            screen(::ScreenModifyActivity)
            screen(::ScreenActivityLink)

            // account
            screen(::ScreenSettings)
            screen(::ScreenLogin)
            screen(::ScreenMail)
            screen(::ScreenPrice)

            // community
            screen(::ScreenUserCard)
            screen(::ScreenTopic)
            screen(::ScreenAddTopic)
            screen(::ScreenFollows)

            // game
            screen(::ScreenCreateGame)
            screen(::ScreenGameHall)
            screen(::ScreenGameHistory)
            screen(::ScreenGameRecordHistory)
            screen(::ScreenPlayGame)

            screen(::ScreenGuessLyrics)
            screen(::ScreenRhyme)

            // third-party
            screen(::ScreenWeibo)
            screen(::ScreenWeiboAlbum)
            screen(::ScreenWeiboDetails)
            screen(::ScreenWeiboFollows)
            screen(::ScreenWeiboUser)
            screen(::ScreenChaohua)
            screen(::ScreenDouyin)

            // viewer
            screen(::ScreenTest)
            screen(::ScreenImagePreview)
            screen(::ScreenWebpage)
            screen(::ScreenVideo)
        }
    }

    override fun onDeepLink(manager: ScreenManager, uri: Uri) {
        when (uri.scheme) {
            Scheme.File, Scheme.Content -> {
                if (!mp.isReady) manager.navigate(::ScreenImportMusic, uri)
                else manager.topScreen.slot.tip.warning("请先停止播放器")
            }
            Scheme.Rachel -> {
                when (uri.path) {
                    "/openProfile" -> {
                        uri.params["uid"]?.toIntOrNull()?.let { uid ->
                            manager.navigate(::ScreenUserCard, uid)
                        }
                    }
                    "/openSong" -> {
                        uri.params["id"]?.let { id ->
                            manager.navigate(::ScreenMusicDetails, id)
                        }
                    }
                }
            }
            Scheme.QQMusic -> {
                if (mp.isReady) manager.topScreen.slot.tip.warning("请先停止播放器")
                else manager.navigate(::ScreenPlatformMusic, uri.copy(scheme = Scheme.Https), PlatformMusicType.QQMusic)
            }
            Scheme.NetEaseCloud -> {
                if (mp.isReady) manager.topScreen.slot.tip.warning("请先停止播放器")
                else manager.navigate(::ScreenPlatformMusic, uri.copy(scheme = Scheme.Https), PlatformMusicType.NetEaseCloud)
            }
        }
    }
}