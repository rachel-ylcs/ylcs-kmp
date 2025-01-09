package love.yinlin.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.CoroutineScope

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