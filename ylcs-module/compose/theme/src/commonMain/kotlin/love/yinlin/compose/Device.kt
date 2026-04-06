package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import love.yinlin.compose.extension.rememberDerivedState

@Stable
@Serializable
data class Device(val size: Size, val type: Type) {
    @Stable
    @Serializable
    enum class Size {
        SMALL, MEDIUM, LARGE;
    }

    @Stable
    @Serializable
    enum class Type {
        PORTRAIT, LANDSCAPE, SQUARE;
    }

    constructor(width: Dp) : this(
        size = when {
            width <= 420.dp -> Size.SMALL
            width <= 900.dp -> Size.MEDIUM
            else -> Size.LARGE
        },
        type = when {
            width <= 420.dp -> Type.PORTRAIT
            width <= 900.dp -> Type.SQUARE
            else -> Type.LANDSCAPE
        }
    )

    constructor(width: Dp, height: Dp) : this(
        size = when {
            width <= 420.dp -> Size.SMALL
            width <= 900.dp -> Size.MEDIUM
            else -> Size.LARGE
        },
        type = when {
            height >= width * 1.5f -> Type.PORTRAIT
            width >= height * 1.3333f -> Type.LANDSCAPE
            else -> Type.SQUARE
        }
    )
}

@Composable
fun rememberDevice(): State<Device> {
    val windowInfo = LocalWindowInfo.current
    val device = rememberDerivedState {
        val containerSize = windowInfo.containerDpSize
        Device(containerSize.width, containerSize.height)
    }
    return device
}

@Composable
fun rememberDeviceSize(): State<Device.Size> {
    val windowInfo = LocalWindowInfo.current
    val deviceSize = rememberDerivedState {
        val containerSize = windowInfo.containerDpSize
        Device(containerSize.width, containerSize.height).size
    }
    return deviceSize
}

@Composable
fun rememberDeviceType(): State<Device.Type> {
    val windowInfo = LocalWindowInfo.current
    val deviceType = rememberDerivedState {
        val containerSize = windowInfo.containerDpSize
        Device(containerSize.width, containerSize.height).type
    }
    return deviceType
}