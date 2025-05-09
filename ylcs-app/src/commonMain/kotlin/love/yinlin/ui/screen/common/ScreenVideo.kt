package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.ImmersivePadding
import love.yinlin.common.LocalImmersivePadding
import love.yinlin.ui.component.platform.VideoPlayer
import love.yinlin.ui.screen.Screen

@Stable
class ScreenVideo(model: AppModel, val args: Args) : Screen<ScreenVideo.Args>(model) {
    @Stable
    @Serializable
    data class Args(val url: String)

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Content() {
        BackHandler { pop() }

        CompositionLocalProvider(LocalImmersivePadding provides ImmersivePadding(WindowInsets.navigationBars.asPaddingValues())) {
            VideoPlayer(
                url = args.url,
                modifier = Modifier.fillMaxSize(),
                onBack = { pop() }
            )
        }
    }
}