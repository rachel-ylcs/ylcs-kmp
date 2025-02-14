package love.yinlin.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

// condition Modifier

inline fun Modifier.condition(value: Boolean, callback: Modifier.() -> Modifier): Modifier =
	if (value) this.callback() else this

inline fun Modifier.condition(value: Boolean, ifTrue: Modifier.() -> Modifier, ifFalse: Modifier.() -> Modifier): Modifier =
	if (value) this.ifTrue() else this.ifFalse()


// rememberState

@Composable
inline fun <T> rememberState(crossinline init: () -> T) =
	remember { mutableStateOf(init()) }
@Composable
inline fun <T> rememberState(key1: Any?, crossinline init: () -> T) =
	remember(key1) { mutableStateOf(init()) }
@Composable
inline fun <T> rememberState(key1: Any?, key2: Any?, crossinline init: () -> T) =
	remember(key1, key2) { mutableStateOf(init()) }
@Composable
inline fun <T> rememberState(key1: Any?, key2: Any?, key3: Any?, crossinline init: () -> T) =
	remember(key1, key2, key3) { mutableStateOf(init()) }
@Composable
inline fun <T> rememberState(vararg keys: Any?, crossinline init: () -> T) =
	remember(*keys) { mutableStateOf(init()) }
@Composable
inline fun <T> rememberStateSaveable(vararg keys: Any?, saver: Saver<T, out Any>? = null, crossinline init: () -> T) =
	if (saver == null) rememberSaveable(*keys, saver = autoSaver()) { mutableStateOf(init()) }
	else rememberSaveable(*keys, stateSaver = saver) { mutableStateOf(init()) }


// Reference

@Stable
class Reference<T> {
	var value: T? = null
}


// LaunchFlag

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


// Debounce

@Stable
@Composable
fun Debounce(delay: Duration = Duration.ZERO, onClick: () -> Unit): () -> Unit {
	var lastTime by rememberStateSaveable(saver = love.yinlin.extension.Saver.Instant) { Instant.fromEpochMilliseconds(0L) }
	return {
		val currentTime = Clock.System.now()
		val diff = currentTime - lastTime
		if (diff >= delay) {
			lastTime = currentTime
			onClick()
		}
	}
}