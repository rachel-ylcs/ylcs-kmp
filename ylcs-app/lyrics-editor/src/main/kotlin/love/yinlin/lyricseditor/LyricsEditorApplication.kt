package love.yinlin.lyricseditor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import love.yinlin.compose.PlatformApplication
import love.yinlin.compose.extension.LazyStateReference
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.foundation.PlatformContext

@Stable
class LyricsEditorApplication(context: PlatformContext) : PlatformApplication<LyricsEditorApplication>(mApp, context) {
    @Composable
    override fun Content() {
        ScreenManager.Navigation<ScreenMain>(modifier = Modifier.fillMaxSize()) {
            screen(::ScreenMain)
        }
    }
}

@Stable
internal val mApp = LazyStateReference<LyricsEditorApplication>()

@Stable
val app by mApp