package love.yinlin.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

@Composable
fun Debounce(delay: Duration = Duration.ZERO, onClick: () -> Unit): () -> Unit {
	var lastTime by remember { mutableStateOf(Instant.fromEpochMilliseconds(0L)) }
	return {
		val currentTime = Clock.System.now()
		val diff = currentTime - lastTime
		if (diff >= delay) {
			lastTime = currentTime
			onClick()
		}
	}
}