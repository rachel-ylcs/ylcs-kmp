package love.yinlin.compose.ui.lottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import love.yinlin.compose.window.rememberOffScreenWindowState

@Composable
actual fun Lottie(data: String, modifier: Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(data))
    val isForeground by rememberOffScreenWindowState()
    LottieAnimation(composition, iterations = LottieConstants.IterateForever, isPlaying = isForeground, modifier = modifier)
}