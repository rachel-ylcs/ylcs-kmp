package love.yinlin.compose.ui.lottie

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import love.yinlin.compose.extension.rememberRefNull
import love.yinlin.compose.extension.rememberValueState
import love.yinlin.compose.window.rememberOffScreenWindowState
import love.yinlin.extension.catchingNull
import org.jetbrains.skia.skottie.Animation
import org.jetbrains.skia.sksg.InvalidationController
import kotlin.time.Duration.Companion.milliseconds

@Composable
actual fun Lottie(data: String, modifier: Modifier) {
    var animation: Animation? by rememberRefNull()
    var currentTime: Float by rememberValueState(0f)
    val invalidationController = remember { InvalidationController() }

    val isForeground by rememberOffScreenWindowState()

    LaunchedEffect(data) {
        animation = if (data.isEmpty()) null else catchingNull { Animation.makeFromString(data) }
        animation?.let { anim ->
            val fps = anim.fPS.let { if (it <= 0f) 60f else it }
            val duration = anim.duration
            if (duration > 0f) {
                currentTime = 0f
                while (isActive) {
                    delay((1000 / fps).toInt().milliseconds)
                    currentTime += 1 / fps
                    if (currentTime >= duration) currentTime = 0f
                }
            }
        }
    }

    Box(modifier = modifier.drawBehind {
        if (isForeground) {
            animation?.let { anim ->
                drawIntoCanvas { canvas ->
                    anim.seekFrameTime(currentTime, invalidationController)
                    anim.render(canvas.nativeCanvas, 0f, 0f, size.width, size.height)
                }
            }
        }
    })
}