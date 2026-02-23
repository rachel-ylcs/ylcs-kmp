package love.yinlin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.foundation.PlatformContextDelegate
import love.yinlin.screen.*
import love.yinlin.uri.Uri

@Stable
abstract class RachelApplication(delegate: PlatformContextDelegate) : AbstractRachelApplication(delegate) {
    @Composable
    override fun Content() {
        ScreenManager.Navigation<ScreenMain>(deeplink = this) {
            // common
            screen(::ScreenMain)

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

    }
}