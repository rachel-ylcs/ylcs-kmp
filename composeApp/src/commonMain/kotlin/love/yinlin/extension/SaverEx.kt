package love.yinlin.extension

import androidx.compose.runtime.saveable.Saver
import love.yinlin.ui.component.text.TextInputState

object Saver {
	val TextInputState: Saver<TextInputState, Pair<String, Boolean>> = Saver(
		save = { it.text to it.overflow },
		restore = {
			TextInputState().apply {
				text = it.first
				overflow = it.second
			}
		}
	)

	val Instant: Saver<kotlinx.datetime.Instant, Long> = Saver(
		save = { it.toEpochMilliseconds() },
		restore = { kotlinx.datetime.Instant.fromEpochMilliseconds(it) }
	)
}