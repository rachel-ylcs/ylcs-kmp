package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import kotlinx.serialization.Serializable
import love.yinlin.AppModel
import love.yinlin.common.Colors
import love.yinlin.ui.component.image.ClickIcon
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

        VideoPlayer(
            url = args.url,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                ClickIcon(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    color = Colors.White,
                    onClick = { pop() }
                )
            }
        )
    }
}