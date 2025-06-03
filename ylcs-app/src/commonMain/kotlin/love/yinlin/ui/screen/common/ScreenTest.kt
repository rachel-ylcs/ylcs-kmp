package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.resources.Res
import love.yinlin.ui.component.platform.PAGAnimation
import love.yinlin.ui.component.platform.PAGState
import love.yinlin.ui.component.screen.CommonSubScreen

@Stable
class ScreenTest(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "测试页"

    private val state = PAGState()

    override suspend fun initialize() {
        state.data = Res.readBytes("files/test.pag")
        while(true) {
            delay(2900 / 29)
            state.progress = (state.progress + 1 / 29.0)
            if (state.progress > 1.0) state.progress = 0.0
        }
    }

    @Composable
    override fun SubContent(device: Device) {
        PAGAnimation(
            state = state,
            modifier = Modifier.fillMaxSize()
        )
    }
}