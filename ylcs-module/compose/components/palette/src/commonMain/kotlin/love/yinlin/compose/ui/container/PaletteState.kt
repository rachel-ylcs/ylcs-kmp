package love.yinlin.compose.ui.container

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf

/** 调色板生成状态。 */
enum class PaletteStatus {
    Loading,
    Ready,
    Refreshing,
    Error,
}

@Stable
internal class PaletteState {
    val paletteState = mutableStateOf<Palette?>(null)
    val statusState = mutableStateOf(PaletteStatus.Loading)
    val errorState = mutableStateOf<Throwable?>(null)

    fun beginGeneration() {
        statusState.value = if (paletteState.value == null) PaletteStatus.Loading else PaletteStatus.Refreshing
        errorState.value = null
    }

    fun publish(palette: Palette) {
        paletteState.value = palette
        statusState.value = PaletteStatus.Ready
        errorState.value = null
    }

    fun fail(error: Throwable) {
        statusState.value = PaletteStatus.Error
        errorState.value = error
    }
}
