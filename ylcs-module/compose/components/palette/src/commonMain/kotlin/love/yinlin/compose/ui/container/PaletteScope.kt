package love.yinlin.compose.ui.container

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color

/**
 * [PaletteBox] 内容槽可访问的调色板作用域。
 *
 * 读取 [palette]、[swatches] 或颜色属性时会订阅对应的 Compose 快照状态；图像分析完成后，
 * 读取这些属性的内容会自动重组。状态对象可直接通过 [paletteState]、[statusState] 访问。
 */
@Stable
class PaletteScope internal constructor(
    private val state: PaletteState,
) {
    val paletteState: State<Palette?> get() = state.paletteState
    val statusState: State<PaletteStatus> get() = state.statusState
    val errorState: State<Throwable?> get() = state.errorState

    val palette: Palette? get() = paletteState.value
    val status: PaletteStatus get() = statusState.value
    val error: Throwable? get() = errorState.value
    val isLoading: Boolean get() = status == PaletteStatus.Loading || status == PaletteStatus.Refreshing
    val swatches: List<Palette.Swatch> get() = palette?.swatches.orEmpty()
    val targets: List<Target> get() = palette?.targets.orEmpty()
    val maxPopulation: Int get() = palette?.maxPopulation ?: 0
    val isReady: Boolean get() = status == PaletteStatus.Ready

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
}
