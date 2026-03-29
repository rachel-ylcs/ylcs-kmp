package love.yinlin.compose.extension

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.StateFactoryMarker
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.util.*
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * google的API大概就是这么保持一致的吧 ❤️
 */
@Target(CLASS, FUNCTION)
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
annotation class GoogleCreativeConsistencyAPI

// MutableBiasAlignmentState

@Stable
interface MutableBiasAlignmentState : MutableState<BiasAlignment>

@Stable
private class DelegateBiasAlignmentState(initValue: BiasAlignment) : MutableBiasAlignmentState {
    val delegate = mutableLongStateOf(packFloats(initValue.horizontalBias, initValue.verticalBias))

    override var value: BiasAlignment
        get() = delegate.value.let { BiasAlignment(unpackFloat1(it), unpackFloat2(it)) }
        set(initValue) { delegate.value = packFloats(initValue.horizontalBias, initValue.verticalBias) }

    override fun component1(): BiasAlignment = value
    override fun component2(): (BiasAlignment) -> Unit = { value = it }
}

@StateFactoryMarker
fun mutableBiasAlignmentStateOf(value: BiasAlignment): MutableBiasAlignmentState = DelegateBiasAlignmentState(value)

@Composable
fun rememberValueState(value: BiasAlignment, vararg keys: Any?) = remember(*keys) { mutableBiasAlignmentStateOf(value) }

@Composable
inline fun rememberBiasAlignmentState(vararg keys: Any?, crossinline init: () -> BiasAlignment) = remember(*keys) { mutableBiasAlignmentStateOf(init()) }

// MutableColorState

@Stable
interface MutableColorState : MutableState<Color>

@Stable
private class DelegateColorState(initValue: Color) : MutableColorState {
    val delegate = mutableLongStateOf(initValue.value.toLong())

    override var value: Color
        get() = Color(delegate.value)
        set(initValue) { delegate.value = initValue.value.toLong() }

    override fun component1(): Color = value
    override fun component2(): (Color) -> Unit = { value = it }
}

@StateFactoryMarker
fun mutableColorStateOf(value: Color): MutableColorState = DelegateColorState(value)

@Composable
fun rememberValueState(value: Color, vararg keys: Any?) = remember(*keys) { mutableColorStateOf(value) }

@Composable
inline fun rememberColorState(vararg keys: Any?, crossinline init: () -> Color) = remember(*keys) { mutableColorStateOf(init()) }

// MutableDpState

@Stable
interface MutableDpState : MutableState<Dp>

@Stable
private class DelegateDpState(initValue: Dp) : MutableDpState {
    val delegate = mutableFloatStateOf(initValue.value)

    override var value: Dp
        get() = Dp(delegate.value)
        set(initValue) { delegate.value = initValue.value }

    override fun component1(): Dp = value
    override fun component2(): (Dp) -> Unit = { value = it }
}

@StateFactoryMarker
fun mutableDpStateOf(value: Dp): MutableDpState = DelegateDpState(value)

@Composable
fun rememberValueState(value: Dp, vararg keys: Any?) = remember(*keys) { mutableDpStateOf(value) }

@Composable
inline fun rememberDpState(vararg keys: Any?, crossinline init: () -> Dp) = remember(*keys) { mutableDpStateOf(init()) }

// MutableOffsetState

@Stable
interface MutableOffsetState : MutableState<Offset>

@Stable
private class DelegateOffsetState(initValue: Offset) : MutableOffsetState {
    val delegate = mutableLongStateOf(initValue.packedValue)

    override var value: Offset
        get() = Offset(delegate.value)
        set(initValue) { delegate.value = initValue.packedValue }

    override fun component1(): Offset = value
    override fun component2(): (Offset) -> Unit = { value = it }
}

@StateFactoryMarker
fun mutableOffsetStateOf(value: Offset): MutableOffsetState = DelegateOffsetState(value)

@Composable
fun rememberValueState(value: Offset, vararg keys: Any?) = remember(*keys) { mutableOffsetStateOf(value) }

@Composable
inline fun rememberOffsetState(vararg keys: Any?, crossinline init: () -> Offset) = remember(*keys) { mutableOffsetStateOf(init()) }

// MutableIntOffsetState

@Stable
interface MutableIntOffsetState : MutableState<IntOffset>

@Stable
private class DelegateIntOffsetState(initValue: IntOffset) : MutableIntOffsetState {
    val delegate = mutableLongStateOf(initValue.packedValue)

    override var value: IntOffset
        get() = IntOffset(delegate.value)
        set(initValue) { delegate.value = initValue.packedValue }

    override fun component1(): IntOffset = value
    override fun component2(): (IntOffset) -> Unit = { value = it }
}

@StateFactoryMarker
fun mutableIntOffsetStateOf(value: IntOffset): MutableIntOffsetState = DelegateIntOffsetState(value)

@Composable
fun rememberValueState(value: IntOffset, vararg keys: Any?) = remember(*keys) { mutableIntOffsetStateOf(value) }

@Composable
inline fun rememberIntOffsetState(vararg keys: Any?, crossinline init: () -> IntOffset) = remember(*keys) { mutableIntOffsetStateOf(init()) }

// MutableDpOffsetState

@Stable
interface MutableDpOffsetState : MutableState<DpOffset>

@Stable
private class DelegateDpOffsetState(initValue: DpOffset) : MutableDpOffsetState {
    val delegate = mutableLongStateOf(initValue.packedValue)

    override var value: DpOffset
        get() = DpOffset(delegate.value)
        set(initValue) { delegate.value = initValue.packedValue }

    override fun component1(): DpOffset = value
    override fun component2(): (DpOffset) -> Unit = { value = it }
}

@StateFactoryMarker
fun mutableDpOffsetStateOf(value: DpOffset): MutableDpOffsetState = DelegateDpOffsetState(value)

@Composable
fun rememberValueState(value: DpOffset, vararg keys: Any?) = remember(*keys) { mutableDpOffsetStateOf(value) }

@Composable
inline fun rememberDpOffsetState(vararg keys: Any?, crossinline init: () -> DpOffset) = remember(*keys) { mutableDpOffsetStateOf(init()) }

// MutableSizeState

@Stable
interface MutableSizeState : MutableState<Size>

@Stable
private class DelegateSizeState(initValue: Size) : MutableSizeState {
    val delegate = mutableLongStateOf(initValue.packedValue)

    override var value: Size
        get() = Size(delegate.value)
        set(initValue) { delegate.value = initValue.packedValue }

    override fun component1(): Size = value
    override fun component2(): (Size) -> Unit = { value = it }
}

@StateFactoryMarker
fun mutableSizeStateOf(value: Size): MutableSizeState = DelegateSizeState(value)

@Composable
fun rememberValueState(value: Size, vararg keys: Any?) = remember(*keys) { mutableSizeStateOf(value) }

@Composable
inline fun rememberSizeState(vararg keys: Any?, crossinline init: () -> Size) = remember(*keys) { mutableSizeStateOf(init()) }

// MutableIntSizeState

@Stable
interface MutableIntSizeState : MutableState<IntSize>

@Stable
@GoogleCreativeConsistencyAPI
private class DelegateIntSizeState(initValue: IntSize) : MutableIntSizeState {
    val delegate = mutableLongStateOf(initValue.packedValue)

    override var value: IntSize
        get() = delegate.value.let { IntSize(unpackInt1(it), unpackInt2(it)) }
        set(initValue) { delegate.value = initValue.packedValue }

    override fun component1(): IntSize = value
    override fun component2(): (IntSize) -> Unit = { value = it }
}

@StateFactoryMarker
@GoogleCreativeConsistencyAPI
fun mutableIntSizeStateOf(value: IntSize): MutableIntSizeState = DelegateIntSizeState(value)

@Composable
fun rememberValueState(value: IntSize, vararg keys: Any?) = remember(*keys) { mutableIntSizeStateOf(value) }

@Composable
inline fun rememberIntSizeState(vararg keys: Any?, crossinline init: () -> IntSize) = remember(*keys) { mutableIntSizeStateOf(init()) }

// MutableDpSizeState

@Stable
interface MutableDpSizeState : MutableState<DpSize>

@Stable
@GoogleCreativeConsistencyAPI
private class DelegateDpSizeState(initValue: DpSize) : MutableDpSizeState {
    val delegate = mutableLongStateOf(packFloats(initValue.width.value, initValue.height.value))

    override var value: DpSize
        get() = delegate.value.let { DpSize(unpackFloat1(it).dp, unpackFloat1(it).dp) }
        set(initValue) { delegate.value = packFloats(initValue.width.value, initValue.height.value) }

    override fun component1(): DpSize = value
    override fun component2(): (DpSize) -> Unit = { value = it }
}

@StateFactoryMarker
@GoogleCreativeConsistencyAPI
fun mutableDpSizeStateOf(value: DpSize): MutableDpSizeState = DelegateDpSizeState(value)

@Composable
fun rememberValueState(value: DpSize, vararg keys: Any?) = remember(*keys) { mutableDpSizeStateOf(value) }

@Composable
inline fun rememberDpSizeState(vararg keys: Any?, crossinline init: () -> DpSize) = remember(*keys) { mutableDpSizeStateOf(init()) }