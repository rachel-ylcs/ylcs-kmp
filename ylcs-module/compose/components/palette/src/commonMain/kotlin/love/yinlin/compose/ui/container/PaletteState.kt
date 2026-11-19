package love.yinlin.compose.ui.container

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class PaletteStatus {
    Loading,
    Ready,
    Refreshing,
    Error,
}

/**
 * 与采样图像关联的调色板状态
 */
@Stable
class PaletteState internal constructor() {
    var palette: Palette? by mutableStateOf(null)
        private set
    var status: PaletteStatus by mutableStateOf(PaletteStatus.Loading)
        private set
    var error: Throwable? by mutableStateOf(null)
        private set

    val isLoading: Boolean get() = status == PaletteStatus.Loading || status == PaletteStatus.Refreshing
    val isReady: Boolean get() = status == PaletteStatus.Ready

    val swatches: List<Palette.Swatch> get() = palette?.swatches.orEmpty()
    val targets: List<PaletteTarget> get() = palette?.targets.orEmpty()
    val maxPopulation: Int get() = palette?.maxPopulation ?: 0

    val dominantSwatch: Palette.Swatch? get() = palette?.dominantSwatch
    val vibrantSwatch: Palette.Swatch? get() = palette?.vibrantSwatch
    val lightVibrantSwatch: Palette.Swatch? get() = palette?.lightVibrantSwatch
    val darkVibrantSwatch: Palette.Swatch? get() = palette?.darkVibrantSwatch
    val mutedSwatch: Palette.Swatch? get() = palette?.mutedSwatch
    val lightMutedSwatch: Palette.Swatch? get() = palette?.lightMutedSwatch
    val darkMutedSwatch: Palette.Swatch? get() = palette?.darkMutedSwatch

    val dominantColor: Color? get() = dominantSwatch?.color
    val vibrantColor: Color? get() = vibrantSwatch?.color
    val lightVibrantColor: Color? get() = lightVibrantSwatch?.color
    val darkVibrantColor: Color? get() = darkVibrantSwatch?.color
    val mutedColor: Color? get() = mutedSwatch?.color
    val lightMutedColor: Color? get() = lightMutedSwatch?.color
    val darkMutedColor: Color? get() = darkMutedSwatch?.color

    fun getSwatchForTarget(target: PaletteTarget): Palette.Swatch? = palette?.getSwatchForTarget(target)

    fun getColorForTarget(target: PaletteTarget, defaultColor: Color = Color.Unspecified): Color = palette?.getColorForTarget(target, defaultColor) ?: defaultColor

    fun getColorForTarget(target: PaletteTarget, defaultColor: Int): Int = palette?.getColorForTarget(target, defaultColor) ?: defaultColor

    internal fun beginGeneration() {
        status = if (palette == null) PaletteStatus.Loading else PaletteStatus.Refreshing
        error = null
    }

    internal fun publish(newPalette: Palette) {
        palette = newPalette
        status = PaletteStatus.Ready
        error = null
    }

    internal fun fail(newError: Throwable) {
        status = PaletteStatus.Error
        error = newError
    }
}
