package love.yinlin.lyricseditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import love.yinlin.compose.Colors
import love.yinlin.compose.PlatformApplication
import love.yinlin.compose.extension.LazyStateReference
import love.yinlin.foundation.PlatformContextDelegate

@Stable
class LyricsEditorApplication(delegate: PlatformContextDelegate) :  PlatformApplication<LyricsEditorApplication>(mApp, delegate) {
    @Composable
    override fun Content() {
        Box(modifier = Modifier.fillMaxSize().background(Colors.White))
    }
}

@Stable
internal val mApp = LazyStateReference<LyricsEditorApplication>()