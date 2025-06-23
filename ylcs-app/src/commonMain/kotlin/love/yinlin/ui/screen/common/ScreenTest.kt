package love.yinlin.ui.screen.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.delay
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import love.yinlin.AppModel
import love.yinlin.common.Device
import love.yinlin.extension.rememberValueState
import love.yinlin.ui.component.image.MiniImage
import love.yinlin.ui.component.screen.CommonSubScreen
import kotlin.time.DurationUnit

@Stable
class ScreenTest(model: AppModel) : CommonSubScreen(model) {
    override val title: String = "测试页"

    override suspend fun initialize() {

    }

    @Composable
    override fun SubContent(device: Device) {
        Column(modifier = Modifier.fillMaxSize()) {
            val composition by rememberLottieComposition {
                LottieCompositionSpec.JsonString(
                    SystemFileSystem.source(Path("C:\\Users\\Administrator\\Desktop\\2036.json")).buffered().use {
                        it.readByteArray()
                    }.decodeToString()
                )
            }
            var progress by rememberValueState(0f)
            MiniImage(
                painter = rememberLottiePainter(
                    composition = composition,
                    progress = { progress },
                ),
                modifier = Modifier.size(300.dp)
            )
            LaunchedEffect(composition) {
                composition?.let {
                    val duration = it.duration.toLong(DurationUnit.MILLISECONDS)
                    val fps = 60
                    val perValue = 1 / (duration / 1000f * fps)
                    while (true) {
                        println(progress)
                        progress += perValue
                        delay(1000L / fps)
                        if (progress >= 1f) progress = 0f
                    }
                }
            }
        }
    }
}