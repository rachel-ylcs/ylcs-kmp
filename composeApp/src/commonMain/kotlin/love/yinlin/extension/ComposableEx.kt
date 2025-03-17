package love.yinlin.extension

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.AtomicReference
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
fun <T> rememberDerivedState(calculation: () -> T) =
	remember { derivedStateOf(calculation) }
@Composable
fun <T> rememberDerivedState(key1: Any?, calculation: () -> T) =
	remember(key1) { derivedStateOf(calculation) }
@Composable
fun <T> rememberDerivedState(key1: Any?, key2: Any?, calculation: () -> T) =
	remember(key1, key2) { derivedStateOf(calculation) }
@Composable
fun <T> rememberDerivedState(key1: Any?, key2: Any?, key3: Any?, calculation: () -> T) =
	remember(key1, key2, key3) { derivedStateOf(calculation) }
@Composable
fun <T> rememberDerivedState(vararg keys: Any?, calculation: () -> T) =
	remember(*keys) { derivedStateOf(calculation) }

// itemKey

val String.itemKey get() = Unit to this


// LaunchFlag


typealias LaunchFlag = AtomicReference<Boolean>
fun launchFlag(): LaunchFlag = AtomicReference(false)

@Composable
inline fun LaunchOnce(flag: LaunchFlag, crossinline block: suspend CoroutineScope.() -> Unit) {
	LaunchedEffect(Unit) {
		if (flag.compareAndSet(false, true)) block()
	}
}


// Debounce


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


// Composition Local


fun <T> localComposition() = staticCompositionLocalOf<T> { error("CompositionLocal not present") }