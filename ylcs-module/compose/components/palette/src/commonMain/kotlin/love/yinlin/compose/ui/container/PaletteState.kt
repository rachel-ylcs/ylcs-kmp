package love.yinlin.compose.ui.container

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

/** 调色板生成状态。 */
enum class PaletteStatus {
    Loading,
    Ready,
    Refreshing,
    Error,
}

/**
 * 与采样图像关联的调色板状态。
 *
 * 通过 [rememberPalette] 创建。生成完成后可直接读取主色、目标色和全部 Swatch；
 * 读取这些属性会订阅对应的 Compose 快照状态，并在生成状态变化时触发重组。
 */
@Stable
class PaletteState internal constructor() {
    /** 最近一次成功生成的调色板；重新采样期间保留上一份结果。 */
    val paletteState: State<Palette?> field = mutableStateOf(null)
    val statusState: State<PaletteStatus> field = mutableStateOf(PaletteStatus.Loading)
    val errorState: State<Throwable?> field = mutableStateOf(null)

    val palette: Palette? get() = paletteState.value
    val status: PaletteStatus get() = statusState.value
    val error: Throwable? get() = errorState.value
    val isLoading: Boolean get() = status == PaletteStatus.Loading || status == PaletteStatus.Refreshing
    val isReady: Boolean get() = status == PaletteStatus.Ready

    val swatches: List<Palette.Swatch> get() = palette?.swatches.orEmpty()
    val targets: List<Target> get() = palette?.targets.orEmpty()
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

    fun getSwatchForTarget(target: Target): Palette.Swatch? = palette?.getSwatchForTarget(target)

    fun getColorForTarget(target: Target, defaultColor: Color = Color.Unspecified): Color =
        palette?.getColorForTarget(target, defaultColor) ?: defaultColor

    fun getColorForTarget(target: Target, defaultColor: Int): Int =
        palette?.getColorForTarget(target, defaultColor) ?: defaultColor

    fun getVibrantColor(defaultColor: Int): Int = palette?.getVibrantColor(defaultColor) ?: defaultColor
    fun getLightVibrantColor(defaultColor: Int): Int = palette?.getLightVibrantColor(defaultColor) ?: defaultColor
    fun getDarkVibrantColor(defaultColor: Int): Int = palette?.getDarkVibrantColor(defaultColor) ?: defaultColor
    fun getMutedColor(defaultColor: Int): Int = palette?.getMutedColor(defaultColor) ?: defaultColor
    fun getLightMutedColor(defaultColor: Int): Int = palette?.getLightMutedColor(defaultColor) ?: defaultColor
    fun getDarkMutedColor(defaultColor: Int): Int = palette?.getDarkMutedColor(defaultColor) ?: defaultColor
    fun getDominantColor(defaultColor: Int): Int = palette?.getDominantColor(defaultColor) ?: defaultColor

    fun getVibrantColor(defaultColor: Color = Color.Unspecified): Color = vibrantColor ?: defaultColor
    fun getLightVibrantColor(defaultColor: Color = Color.Unspecified): Color = lightVibrantColor ?: defaultColor
    fun getDarkVibrantColor(defaultColor: Color = Color.Unspecified): Color = darkVibrantColor ?: defaultColor
    fun getMutedColor(defaultColor: Color = Color.Unspecified): Color = mutedColor ?: defaultColor
    fun getLightMutedColor(defaultColor: Color = Color.Unspecified): Color = lightMutedColor ?: defaultColor
    fun getDarkMutedColor(defaultColor: Color = Color.Unspecified): Color = darkMutedColor ?: defaultColor
    fun getDominantColor(defaultColor: Color = Color.Unspecified): Color = dominantColor ?: defaultColor

    internal fun beginGeneration() {
        statusState.value = if (paletteState.value == null) PaletteStatus.Loading else PaletteStatus.Refreshing
        errorState.value = null
    }

    internal fun publish(palette: Palette) {
        paletteState.value = palette
        statusState.value = PaletteStatus.Ready
        errorState.value = null
    }

    internal fun fail(error: Throwable) {
        statusState.value = PaletteStatus.Error
        errorState.value = error
    }
}
