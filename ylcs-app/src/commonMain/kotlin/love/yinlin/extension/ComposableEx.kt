package love.yinlin.extension

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.jvm.JvmInline
import kotlin.math.roundToInt

// BaseValue

fun Offset.translate(x: Float = 0f, y: Float = 0f) = this.copy(x = this.x + x, y = this.y + y)
fun Offset.roundToIntOffset() = IntOffset(x = this.x.roundToInt(), y = this.y.roundToInt())

fun Size.translate(x: Float = 0f, y: Float = 0f) = this.copy(width = this.width + x, height = this.height + y)

// State

fun <T> mutableRefStateOf(value: T) = mutableStateOf(value, referentialEqualityPolicy())

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
inline fun <T> rememberRefState(crossinline init: () -> T) =
    remember { mutableStateOf(init(), referentialEqualityPolicy()) }
@Composable
inline fun <T> rememberRefState(key1: Any?, crossinline init: () -> T) =
    remember(key1) { mutableStateOf(init(), referentialEqualityPolicy()) }
@Composable
inline fun <T> rememberRefState(key1: Any?, key2: Any?, crossinline init: () -> T) =
    remember(key1, key2) { mutableStateOf(init(), referentialEqualityPolicy()) }
@Composable
inline fun <T> rememberRefState(key1: Any?, key2: Any?, key3: Any?, crossinline init: () -> T) =
    remember(key1, key2, key3) { mutableStateOf(init(), referentialEqualityPolicy()) }
@Composable
inline fun <T> rememberRefState(vararg keys: Any?, crossinline init: () -> T) =
    remember(*keys) { mutableStateOf(init(), referentialEqualityPolicy()) }

@Composable
fun rememberFalse(vararg keys: Any?) = remember(*keys) { mutableStateOf(false) }
@Composable
fun rememberTrue(vararg keys: Any?) = remember(*keys) { mutableStateOf(true) }
@Composable
fun rememberValueState(value: Int, vararg keys: Any?) = remember(*keys) { mutableIntStateOf(value) }
@Composable
inline fun rememberIntState(vararg keys: Any?, crossinline init: () -> Int) = remember(*keys) { mutableIntStateOf(init()) }
@Composable
fun rememberValueState(value: Long, vararg keys: Any?) = remember(*keys) { mutableLongStateOf(value) }
@Composable
inline fun rememberLongState(vararg keys: Any?, crossinline init: () -> Long) = remember(*keys) { mutableLongStateOf(init()) }
@Composable
fun rememberValueState(value: Float, vararg keys: Any?) = remember(*keys) { mutableFloatStateOf(value) }
@Composable
inline fun rememberFloatState(vararg keys: Any?, crossinline init: () -> Float) = remember(*keys) { mutableFloatStateOf(init()) }
@Composable
fun rememberValueState(value: Double, vararg keys: Any?) = remember(*keys) { mutableDoubleStateOf(value) }
@Composable
inline fun rememberDoubleState(vararg keys: Any?, crossinline init: () -> Double) = remember(*keys) { mutableDoubleStateOf(init()) }

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
	var value by rememberFalse()
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
	inline operator fun invoke(update: () -> Unit = {}, init: () -> Unit) {
		if (value.compareAndSet(expectedValue = false, newValue = true)) init()
		else update()
	}
}

@OptIn(ExperimentalAtomicApi::class)
fun launchFlag(): LaunchFlag = LaunchFlag()

// Composition Local

fun <T> localComposition() = staticCompositionLocalOf<T> { error("CompositionLocal not present") }