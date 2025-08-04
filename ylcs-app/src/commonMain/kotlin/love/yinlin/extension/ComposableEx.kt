package love.yinlin.extension

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlin.math.roundToInt

// BaseValue

fun Float.toRadian() = this * 3.141592f / 180
fun Float.toDegree() = this * 180 / 3.141592f

fun Offset.translate(x: Float = 0f, y: Float = 0f) = this.copy(x = this.x + x, y = this.y + y)
fun Offset.scale(dx: Float = 1f, dy: Float = 1f) = this.copy(x = this.x * dx, y = this.y * dy)
fun Offset.roundToIntOffset() = IntOffset(x = this.x.roundToInt(), y = this.y.roundToInt())
fun Offset.onLine(other: Offset, ratio: Float): Offset = Offset(x = this.x + (other.x - this.x) * ratio, y = this.y + (other.y - this.y) * ratio)
fun Offset.onCenter(other: Offset): Offset = Offset(x = (this.x + other.x) / 2, y = (this.y + other.y) / 2)
fun Offset.slope(other: Offset): Float = (other.x - this.x).let { if (it == 0f) Float.POSITIVE_INFINITY else (other.y - this.y) / it }
fun Offset.distance(other: Offset): Float = (this - other).getDistance()

fun Size.translate(x: Float = 0f, y: Float = 0f) = this.copy(width = this.width + x, height = this.height + y)
fun Size.scale(dx: Float = 1f, dy: Float = 1f) = this.copy(width = this.width * dx, height = this.height * dy)

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

class LaunchFlag(val value: AtomicBoolean = atomic(false)) {
	inline operator fun invoke(update: () -> Unit = {}, init: () -> Unit) {
		if (value.compareAndSet(expect = false, update = true)) init()
		else update()
	}
}

// Composition Local

fun <T> localComposition() = staticCompositionLocalOf<T> { error("CompositionLocal not present") }