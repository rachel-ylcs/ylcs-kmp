package love.yinlin.ui.component.node

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

@Composable
fun Debounce(delay: Duration = Duration.ZERO, onClick: () -> Unit): () -> Unit {
    var lastTime = remember { Instant.fromEpochMilliseconds(0L) }
    return {
        val currentTime = Clock.System.now()
        val diff = currentTime - lastTime
        if (diff >= delay) {
            lastTime = currentTime
            onClick()
        }
    }
}