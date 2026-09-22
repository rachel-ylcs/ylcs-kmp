package love.yinlin.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import love.yinlin.compose.extension.mutableRefStateOf
import kotlin.time.Duration.Companion.seconds

/**
 * 睡眠定时器
 */
@Stable
class SleepTimer(
    private val scope: CoroutineScope,
    private val onEnd: suspend () -> Unit
) {
    private var job: Job? by mutableRefStateOf(null)

    var remainSeconds: Int by mutableIntStateOf(0)
        private set

    val isRunning: Boolean get() = job != null

    fun start(seconds: Int) {
        job?.cancel()
        job = scope.launch {
            remainSeconds = seconds
            repeat(seconds) {
                delay(1.seconds)
                --remainSeconds
            }
            onEnd()
            job = null
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}