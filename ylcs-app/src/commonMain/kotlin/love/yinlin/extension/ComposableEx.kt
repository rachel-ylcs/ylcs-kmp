package love.yinlin.extension

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.jvm.JvmInline
import kotlin.time.Duration


// BaseValue


fun Offset.translate(x: Float = 0f, y: Float = 0f) = copy(x = this.x + x, y = this.y + y)


// condition Modifier


inline fun Modifier.condition(value: Boolean, callback: Modifier.() -> Modifier): Modifier =
	if (value) this.callback() else this

inline fun Modifier.condition(value: Boolean, ifTrue: Modifier.() -> Modifier, ifFalse: Modifier.() -> Modifier): Modifier =
	if (value) this.ifTrue() else this.ifFalse()


// clickableNoRipple


@Composable
fun Modifier.clickableNoRipple(enabled: Boolean = true, onClick: () -> Unit): Modifier = this.clickable(
	interactionSource = remember { MutableInteractionSource() },
	indication = null,
	enabled = enabled,
	onClick = onClick
)


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
fun rememberValueState(value: Int, vararg keys: Any?) = remember(*keys) { mutableIntStateOf(value) }

@Composable
fun rememberValueState(value: Long, vararg keys: Any?) = remember(*keys) { mutableLongStateOf(value) }

@Composable
fun rememberValueState(value: Float, vararg keys: Any?) = remember(*keys) { mutableFloatStateOf(value) }

@Composable
fun rememberValueState(value: Double, vararg keys: Any?) = remember(*keys) { mutableDoubleStateOf(value) }

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


// rememberOffScreenState

@Composable
fun rememberOffScreenState(): Boolean {
	var value by rememberState { false }
	LifecycleStartEffect(Unit) {
		value = true
		onStopOrDispose {
			value = false
		}
	}
	return value
}

@Composable
inline fun OffScreenEffect(crossinline block: (isForeground: Boolean) -> Unit) {
	LifecycleStartEffect(Unit) {
		block(true)
		onStopOrDispose {
			block(false)
		}
	}
}

// LaunchFlag

@OptIn(ExperimentalAtomicApi::class)
@JvmInline
value class LaunchFlag(val value: AtomicBoolean = AtomicBoolean(false)) {
	suspend inline fun update(scope: CoroutineScope, crossinline block: suspend CoroutineScope.() -> Unit) {
		if (value.compareAndSet(expectedValue = false, newValue = true)) scope.block()
	}
}

@OptIn(ExperimentalAtomicApi::class)
fun launchFlag(): LaunchFlag = LaunchFlag()

@OptIn(ExperimentalAtomicApi::class)
@Composable
inline fun LaunchOnce(flag: LaunchFlag, crossinline block: suspend CoroutineScope.() -> Unit) {
	LaunchedEffect(Unit) {
		flag.update(this, block)
	}
}


// Composition Local


fun <T> localComposition() = staticCompositionLocalOf<T> { error("CompositionLocal not present") }


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