package love.yinlin.screen.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.yinlin.compose.Colors
import love.yinlin.compose.Device
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.PAGImageAnimation
import love.yinlin.compose.ui.PAGSource
import love.yinlin.compose.ui.PAGState
import love.yinlin.shared.resources.Res

@Stable
class ScreenTest(manager: ScreenManager) : Screen(manager) {
    override val title: String = "测试页"

    var a by mutableRefStateOf(byteArrayOf())

    val state = PAGState()

    override suspend fun initialize() {
        a = Res.readBytes("files/1.pag")
    }

    @Composable
    override fun Content(device: Device) {
        val composition by rememberDerivedState { PAGSource.Data(a) }

        PAGImageAnimation(composition, modifier = Modifier.size(300.dp).background(Colors.Black), isPlaying = true)
    }
}