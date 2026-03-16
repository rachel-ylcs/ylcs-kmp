package love.yinlin.compose.extension

import androidx.compose.runtime.*
import love.yinlin.extension.BaseLazyReference
import kotlin.jvm.JvmName
import kotlin.reflect.KProperty

class LazyStateReference<T : Any> : BaseLazyReference<T> {
    private var mValue: T? by mutableStateOf(null)
    override val isInit: Boolean by derivedStateOf { mValue != null }
    override fun init(value: T) {
        if (mValue == null) mValue = value
    }
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = mValue!!
}

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

@Composable
@JvmName("rememberMovableContent0")
fun rememberMovableContent(content: @Composable () -> Unit) =
    remember(content) { movableContentOf(content) }

@Composable
@JvmName("rememberMovableContent1")
fun <T1> rememberMovableContent(content: @Composable (T1) -> Unit) =
    remember(content) { movableContentOf(content) }

@Composable
@JvmName("rememberMovableContent2")
fun <T1, T2> rememberMovableContent(content: @Composable (T1, T2) -> Unit) =
    remember(content) { movableContentOf(content) }

@Composable
@JvmName("rememberMovableContent3")
fun <T1, T2, T3> rememberMovableContent(content: @Composable (T1, T2, T3) -> Unit) =
    remember(content) { movableContentOf(content) }

@Composable
@JvmName("rememberMovableContent4")
fun <T1, T2, T3, T4> rememberMovableContent(content: @Composable (T1, T2, T3, T4) -> Unit) =
    remember(content) { movableContentOf(content) }

@Suppress("UnusedReceiverParameter")
@JvmName("movableComposable0")
fun <R> R.movableComposable(content: @Composable R.() -> Unit) = movableContentWithReceiverOf(content)

@Suppress("UnusedReceiverParameter")
@JvmName("movableComposable1")
fun <R, T1> R.movableComposable(content: @Composable R.(T1) -> Unit) = movableContentWithReceiverOf(content)

@Suppress("UnusedReceiverParameter")
@JvmName("movableComposable2")
fun <R, T1, T2> R.movableComposable(content: @Composable R.(T1, T2) -> Unit) = movableContentWithReceiverOf(content)

@Suppress("UnusedReceiverParameter")
@JvmName("movableComposable3")
fun <R, T1, T2, T3> R.movableComposable(content: @Composable R.(T1, T2, T3) -> Unit) = movableContentWithReceiverOf(content)