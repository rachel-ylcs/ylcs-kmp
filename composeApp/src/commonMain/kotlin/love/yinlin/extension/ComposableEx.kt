package love.yinlin.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope

inline fun Modifier.condition(value: Boolean, callback: Modifier.() -> Modifier): Modifier =
	if (value) this.callback() else this

inline fun Modifier.condition(value: Boolean, ifTrue: Modifier.() -> Modifier, ifFalse: Modifier.() -> Modifier): Modifier =
	if (value) this.ifTrue() else this.ifFalse()

class LaunchFlag(var flag: Unit? = null)

@Composable
inline fun LaunchOnce(ref: LaunchFlag, crossinline block: suspend CoroutineScope.() -> Unit) {
	LaunchedEffect(Unit) {
		if (ref.flag == null) {
			ref.flag = Unit
			block()
		}
	}
}