package love.yinlin.screen.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.delay
import kotlinx.io.files.Path
import love.yinlin.compose.Device
import love.yinlin.compose.graphics.AnimatedWebp
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.extension.readByteArray
import love.yinlin.platform.Coroutines

@Stable
class ScreenTest(manager: ScreenManager) : Screen(manager) {
    override val title: String = "测试页"

    private var animation: AnimatedWebp? by mutableStateOf(null)

    override suspend fun initialize() {
        animation = AnimatedWebp.decode(Path("C:\\Users\\Administrator\\Desktop\\animation.webp").readByteArray()!!)
        launch {
            Coroutines.cpu {
                while (true) {
                    delay(100)
                    animation?.nextFrame() ?: break
                }
            }
        }
    }

    override fun finalize() {
        animation?.release()
        animation = null
    }

    @Composable
    override fun Content(device: Device) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            animation?.apply { drawFrame(Rect(Offset.Zero, Size(324f, 576f))) }
        }
        PrimaryButton("测试") {
            animation?.resetFrame()
        }
    }
}