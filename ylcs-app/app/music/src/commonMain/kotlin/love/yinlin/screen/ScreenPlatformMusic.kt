package love.yinlin.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import love.yinlin.compose.screen.Screen
import love.yinlin.data.music.PlatformMusicType
import love.yinlin.uri.Uri

@Stable
class ScreenPlatformMusic(deeplink: Uri?, type: PlatformMusicType) : Screen() {
    @Composable
    override fun Content() {

    }
}