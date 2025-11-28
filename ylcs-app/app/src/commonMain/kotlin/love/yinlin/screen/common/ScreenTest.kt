package love.yinlin.screen.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.delay
import love.yinlin.compose.Device
import love.yinlin.compose.graphics.AnimatedWebp
import love.yinlin.compose.screen.Screen
import love.yinlin.compose.screen.ScreenManager
import love.yinlin.compose.ui.input.PrimaryButton
import love.yinlin.platform.Coroutines
import love.yinlin.resources.Res
import love.yinlin.resources.animation
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getSystemResourceEnvironment

@Stable
class ScreenTest(manager: ScreenManager) : Screen(manager) {
    override val title: String = "测试页"

    var webp: AnimatedWebp? by mutableStateOf(null)
    var frame: Int by mutableIntStateOf(-1)

    override suspend fun initialize() {
        Coroutines.io {
            webp = AnimatedWebp.decode(getDrawableResourceBytes(getSystemResourceEnvironment(), Res.drawable.animation))
        }
    }

    @Composable
    override fun Content(device: Device) {
        Canvas(modifier = Modifier.fillMaxSize()) {
//            webp?.let {
//                it.apply { drawFrame(frame, Rect(Offset.Zero, Size(it.width / 2f, it.height / 2f))) }
//            }
        }
        PrimaryButton("开始") {
//            launch {
//                while (true) {
//                    delay(100)
//                    webp?.let {
//                        if (frame >= it.frameCount - 1) frame = 0
//                        else frame++
//                    }
//                }
//            }
        }
    }
}