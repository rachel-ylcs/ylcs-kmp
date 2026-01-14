package love.yinlin.compose.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import love.yinlin.extension.BaseLazyReference
import kotlin.reflect.KProperty

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

class LazyStateReference<T : Any> : BaseLazyReference<T> {
    private var mValue: T? by mutableStateOf(null)
    override val isInit: Boolean by derivedStateOf { mValue != null }
    override fun init(value: T) {
        if (mValue == null) mValue = value
    }
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = mValue!!
}