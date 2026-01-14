package love.yinlin.compose.ui.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import love.yinlin.extension.DateEx
import kotlin.time.Duration

@Composable
fun debounce(delay: Duration = Duration.ZERO, onClick: () -> Unit): () -> Unit {
    var lastTime = remember { DateEx.Zero }
    return {
        val currentTime = DateEx.CurrentInstant
        val diff = currentTime - lastTime
        if (diff >= delay) {
            lastTime = currentTime
            onClick()
        }
    }
}