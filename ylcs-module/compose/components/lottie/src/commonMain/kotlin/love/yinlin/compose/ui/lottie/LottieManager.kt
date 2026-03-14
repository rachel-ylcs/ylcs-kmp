package love.yinlin.compose.ui.lottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier

@Stable
class LottieManager {
    private val lottieMap = mutableStateMapOf<String, String>()

    operator fun get(key: String): String? = lottieMap[key]
    operator fun set(id: String, data: String) { lottieMap[id] = data }

    @Composable
    fun Content(id: String, modifier: Modifier = Modifier) {
        lottieMap[id]?.let { Lottie(it, modifier = modifier) }
    }
}