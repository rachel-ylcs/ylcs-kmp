/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package love.yinlin.compose.ui.container

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * 从图像中提取的代表性色彩集合。
 *
 * 量化使用 AndroidX Palette 的 5-bit RGB 中值切分算法；目标颜色按饱和度、明度和像素占比评分。
 * [Builder.generate] 是同步 API，图像较大时应优先使用 [Builder.generateAsync]。
 */
@Stable
class Palette internal constructor(
    swatches: List<Swatch>,
    targets: List<Target>,
) {
    val swatches: List<Swatch> = swatches.toList()
    val targets: List<Target> = targets.toList()
    val dominantSwatch: Swatch? = this.swatches.maxByOrNull { it.population }
    val maxPopulation: Int = dominantSwatch?.population ?: 0

    private val selectedSwatches = mutableMapOf<Target, Swatch>()

    init {
        generateTargets()
    }

    val vibrantSwatch: Swatch? get() = getSwatchForTarget(Target.VIBRANT)
    val lightVibrantSwatch: Swatch? get() = getSwatchForTarget(Target.LIGHT_VIBRANT)
    val darkVibrantSwatch: Swatch? get() = getSwatchForTarget(Target.DARK_VIBRANT)
    val mutedSwatch: Swatch? get() = getSwatchForTarget(Target.MUTED)
    val lightMutedSwatch: Swatch? get() = getSwatchForTarget(Target.LIGHT_MUTED)
    val darkMutedSwatch: Swatch? get() = getSwatchForTarget(Target.DARK_MUTED)

    val vibrantColor: Color? get() = vibrantSwatch?.color
    val lightVibrantColor: Color? get() = lightVibrantSwatch?.color
    val darkVibrantColor: Color? get() = darkVibrantSwatch?.color
    val mutedColor: Color? get() = mutedSwatch?.color
    val lightMutedColor: Color? get() = lightMutedSwatch?.color
    val darkMutedColor: Color? get() = darkMutedSwatch?.color
    val dominantColor: Color? get() = dominantSwatch?.color

    fun getSwatchForTarget(target: Target): Swatch? = selectedSwatches[target]

    fun getColorForTarget(target: Target, defaultColor: Int): Int =
        getSwatchForTarget(target)?.rgb ?: defaultColor

    fun getColorForTarget(target: Target, defaultColor: Color = Color.Unspecified): Color =
        getSwatchForTarget(target)?.color ?: defaultColor

    fun getVibrantColor(defaultColor: Int): Int = getColorForTarget(Target.VIBRANT, defaultColor)
    fun getLightVibrantColor(defaultColor: Int): Int = getColorForTarget(Target.LIGHT_VIBRANT, defaultColor)
    fun getDarkVibrantColor(defaultColor: Int): Int = getColorForTarget(Target.DARK_VIBRANT, defaultColor)
    fun getMutedColor(defaultColor: Int): Int = getColorForTarget(Target.MUTED, defaultColor)
    fun getLightMutedColor(defaultColor: Int): Int = getColorForTarget(Target.LIGHT_MUTED, defaultColor)
    fun getDarkMutedColor(defaultColor: Int): Int = getColorForTarget(Target.DARK_MUTED, defaultColor)
    fun getDominantColor(defaultColor: Int): Int = dominantSwatch?.rgb ?: defaultColor

    fun getVibrantColor(defaultColor: Color = Color.Unspecified): Color = vibrantSwatch?.color ?: defaultColor
    fun getLightVibrantColor(defaultColor: Color = Color.Unspecified): Color = lightVibrantSwatch?.color ?: defaultColor
    fun getDarkVibrantColor(defaultColor: Color = Color.Unspecified): Color = darkVibrantSwatch?.color ?: defaultColor
    fun getMutedColor(defaultColor: Color = Color.Unspecified): Color = mutedSwatch?.color ?: defaultColor
    fun getLightMutedColor(defaultColor: Color = Color.Unspecified): Color = lightMutedSwatch?.color ?: defaultColor
    fun getDarkMutedColor(defaultColor: Color = Color.Unspecified): Color = darkMutedSwatch?.color ?: defaultColor
    fun getDominantColor(defaultColor: Color = Color.Unspecified): Color = dominantSwatch?.color ?: defaultColor

    private fun generateTargets() {
        val usedColors = mutableSetOf<Int>()
        for (target in targets) {
            val weights = target.normalizedWeights()
            var bestSwatch: Swatch? = null
            var bestScore = 0f

            for (swatch in swatches) {
                val hsl = swatch.hslArray
                if (hsl[1] < target.minimumSaturation || hsl[1] > target.maximumSaturation ||
                    hsl[2] < target.minimumLightness || hsl[2] > target.maximumLightness ||
                    swatch.rgb in usedColors
                ) continue

                val score = weights[0] * (1f - kotlin.math.abs(hsl[1] - target.targetSaturation)) +
                    weights[1] * (1f - kotlin.math.abs(hsl[2] - target.targetLightness)) +
                    weights[2] * (swatch.population / maxPopulation.coerceAtLeast(1).toFloat())

                if (bestSwatch == null || score > bestScore) {
                    bestSwatch = swatch
                    bestScore = score
                }
            }

            if (bestSwatch != null) {
                selectedSwatches[target] = bestSwatch
                if (target.isExclusive) usedColors += bestSwatch.rgb
            }
        }
    }

    /**
     * 代表调色板中的一种颜色及其像素占比。
     *
     * [rgb] 为 ARGB 整数；[color]、[titleColor] 和 [bodyColor] 可直接用于 Compose。
     */
    @Stable
    class Swatch(
        val rgb: Int,
        val population: Int,
    ) {
        internal val hslArray: FloatArray by lazy { PaletteColorUtils.rgbToHsl(rgb) }
        val hsl: FloatArray get() = hslArray.copyOf()
        val color: Color get() = PaletteColorUtils.toColor(rgb)

        private val textColors: TextColors by lazy { generateTextColors() }

        val titleTextColor: Int get() = textColors.title
        val bodyTextColor: Int get() = textColors.body
        val titleColor: Color get() = PaletteColorUtils.toColor(titleTextColor)
        val bodyColor: Color get() = PaletteColorUtils.toColor(bodyTextColor)

        private fun generateTextColors(): TextColors {
            val lightBodyAlpha = PaletteColorUtils.calculateMinimumAlpha(WHITE, rgb, MIN_CONTRAST_BODY_TEXT)
            val lightTitleAlpha = PaletteColorUtils.calculateMinimumAlpha(WHITE, rgb, MIN_CONTRAST_TITLE_TEXT)

            if (lightBodyAlpha != -1 && lightTitleAlpha != -1) {
                return TextColors(setAlpha(WHITE, lightTitleAlpha), setAlpha(WHITE, lightBodyAlpha))
            }

            val darkBodyAlpha = PaletteColorUtils.calculateMinimumAlpha(BLACK, rgb, MIN_CONTRAST_BODY_TEXT)
            val darkTitleAlpha = PaletteColorUtils.calculateMinimumAlpha(BLACK, rgb, MIN_CONTRAST_TITLE_TEXT)

            if (darkBodyAlpha != -1 && darkTitleAlpha != -1) {
                return TextColors(setAlpha(BLACK, darkTitleAlpha), setAlpha(BLACK, darkBodyAlpha))
            }

            val title = if (lightTitleAlpha != -1) setAlpha(WHITE, lightTitleAlpha) else setAlpha(BLACK, darkTitleAlpha)
            val body = if (lightBodyAlpha != -1) setAlpha(WHITE, lightBodyAlpha) else setAlpha(BLACK, darkBodyAlpha)
            return TextColors(title, body)
        }

        override fun equals(other: Any?): Boolean =
            other is Swatch && rgb == other.rgb && population == other.population

        override fun hashCode(): Int = 31 * rgb + population

        override fun toString(): String =
            "Swatch [RGB: #${rgb.toUInt().toString(16)}] [HSL: ${hsl.contentToString()}] [Population: $population]"

        private data class TextColors(val title: Int, val body: Int)

        private companion object {
            const val WHITE = -1
            const val BLACK = -0x1000000
            const val MIN_CONTRAST_TITLE_TEXT = 3f
            const val MIN_CONTRAST_BODY_TEXT = 4.5f

            fun setAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)
        }
    }

    /** 控制量化阶段允许进入调色板的颜色。 */
    fun interface Filter {
        fun isAllowed(rgb: Int, hsl: FloatArray): Boolean
    }

    fun interface PaletteAsyncListener {
        fun onGenerated(palette: Palette?)
    }

    /**
     * 调色板配置 DSL。
     *
     * 使用 [Palette.from] 创建 Builder，在配置块中声明采样参数、过滤器和目标色。
     */
    @PaletteDsl
    class Builder private constructor(
        private val bitmap: ImageBitmap?,
        private val inputSwatches: List<Swatch>?,
    ) {
        private val targets = mutableListOf<Target>()
        private val filters = mutableListOf(DEFAULT_FILTER)
        private var resizeArea = DEFAULT_RESIZE_BITMAP_AREA
        private var selectedRegion: Region? = null

        var maximumColorCount = DEFAULT_CALCULATE_NUMBER_COLORS
            set(value) {
                require(value > 0) { "maximumColorCount must be greater than zero." }
                field = value
            }

        var resizeBitmapArea: Int
            get() = resizeArea
            set(value) {
                require(value > 0) { "resizeBitmapArea must be greater than zero." }
                resizeArea = value
            }

        internal constructor(bitmap: ImageBitmap) : this(bitmap, null) {
            targets += Target.DEFAULT_TARGETS
        }

        internal constructor(swatches: List<Swatch>) : this(null, swatches.toList()) {
            require(swatches.isNotEmpty()) { "List of swatches must not be empty." }
        }

        /** 添加颜色过滤器。 */
        fun filter(filter: Filter) {
            filters += filter
        }

        /** 移除默认过滤器及此前添加的过滤器。 */
        fun clearFilters() {
            filters.clear()
        }

        /** 限定采样区域，坐标以原始 ImageBitmap 为准。 */
        fun region(left: Int, top: Int, right: Int, bottom: Int) {
            val source = bitmap ?: return
            val clippedLeft = left.coerceAtLeast(0)
            val clippedTop = top.coerceAtLeast(0)
            val clippedRight = right.coerceAtMost(source.width)
            val clippedBottom = bottom.coerceAtMost(source.height)
            require(clippedLeft < clippedRight && clippedTop < clippedBottom) {
                "The given region must intersect with the ImageBitmap dimensions."
            }
            selectedRegion = Region(clippedLeft, clippedTop, clippedRight, clippedBottom)
        }

        /** 清除采样区域限制。 */
        fun clearRegion() {
            selectedRegion = null
        }

        /** 添加一个预定义或已构建的目标色。 */
        fun target(target: Target) {
            if (target !in targets) targets += target
        }

        /** 通过 DSL 配置并添加自定义目标色。 */
        fun target(block: Target.Builder.() -> Unit) {
            target(Target(block))
        }

        /** 清除所有目标色。 */
        fun clearTargets() {
            targets.clear()
        }

        /**
         * 同步生成调色板。图像分析是 CPU 密集操作，不应在 UI 线程调用。
         */
        fun generate(): Palette {
            val source = bitmap
            if (source == null) return Palette(inputSwatches.orEmpty(), targets)

            val pixels = extractPixels(source)
            val swatches = ColorCutQuantizer(pixels.values, maximumColorCount, filters).quantize()
            return Palette(swatches, targets)
        }

        /** 在 [Dispatchers.Default] 上生成调色板，调用方可直接从协程中使用。 */
        suspend fun generateAsync(): Palette = withContext(Dispatchers.Default) { generate() }

        /**
         * 在 [scope] 中异步生成调色板。发生生成错误时，监听器收到 null。
         */
        fun generate(scope: CoroutineScope, listener: PaletteAsyncListener): Job = scope.launch {
            val result = try {
                generateAsync()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                null
            }
            listener.onGenerated(result)
        }

        /** 与 [generate] 相同，但使用异步 API 命名。 */
        fun generateAsync(scope: CoroutineScope, listener: PaletteAsyncListener): Job = generate(scope, listener)

        private fun extractPixels(source: ImageBitmap): PixelData {
            val sourceWidth = source.width
            val sourceHeight = source.height
            val sourcePixels = IntArray(sourceWidth * sourceHeight)
            source.readPixels(sourcePixels)

            val sourceArea = sourceWidth.toLong() * sourceHeight.toLong()
            val scaleRatio = if (sourceArea > resizeArea) sqrt(resizeArea / sourceArea.toDouble()) else 1.0

            val scaledWidth = ceil(sourceWidth * scaleRatio).toInt().coerceAtLeast(1)
            val scaledHeight = ceil(sourceHeight * scaleRatio).toInt().coerceAtLeast(1)
            val region = selectedRegion
            val left = region?.let { floor(it.left * scaleRatio).toInt() } ?: 0
            val top = region?.let { floor(it.top * scaleRatio).toInt() } ?: 0
            val right = region?.let { ceil(it.right * scaleRatio).toInt().coerceAtMost(scaledWidth) } ?: scaledWidth
            val bottom = region?.let { ceil(it.bottom * scaleRatio).toInt().coerceAtMost(scaledHeight) } ?: scaledHeight
            val outputWidth = (right - left).coerceAtLeast(1)
            val outputHeight = (bottom - top).coerceAtLeast(1)
            val output = IntArray(outputWidth * outputHeight)

            var outputIndex = 0
            for (y in top until bottom) {
                val sourceY = floor(y / scaleRatio).toInt().coerceIn(0, sourceHeight - 1)
                for (x in left until right) {
                    val sourceX = floor(x / scaleRatio).toInt().coerceIn(0, sourceWidth - 1)
                    output[outputIndex++] = sourcePixels[sourceY * sourceWidth + sourceX]
                }
            }

            return PixelData(output, outputWidth, outputHeight)
        }

        private data class Region(val left: Int, val top: Int, val right: Int, val bottom: Int)
        private data class PixelData(val values: IntArray, val width: Int, val height: Int)
    }

    companion object {
        private const val DEFAULT_FILTER_BLACK_MAX_LIGHTNESS = 0.05f
        private const val DEFAULT_FILTER_WHITE_MIN_LIGHTNESS = 0.95f

        /** 默认量化采样面积，112×112。 */
        const val DEFAULT_RESIZE_BITMAP_AREA = 112 * 112

        /** 默认生成 16 种代表色。 */
        const val DEFAULT_CALCULATE_NUMBER_COLORS = 16

        private val DEFAULT_FILTER = Filter { _, hsl ->
            val isBlack = hsl[2] <= DEFAULT_FILTER_BLACK_MAX_LIGHTNESS
            val isWhite = hsl[2] >= DEFAULT_FILTER_WHITE_MIN_LIGHTNESS
            val isNearRedILine = hsl[0] in 10f..37f && hsl[1] <= 0.82f
            !isBlack && !isWhite && !isNearRedILine
        }

        fun from(bitmap: ImageBitmap, block: Builder.() -> Unit = {}): Builder = Builder(bitmap).apply(block)

        fun from(swatches: List<Swatch>): Palette = Builder(swatches).generate()

        fun generate(bitmap: ImageBitmap): Palette = from(bitmap).generate()

        fun generate(bitmap: ImageBitmap, maximumColorCount: Int): Palette =
            from(bitmap) { this.maximumColorCount = maximumColorCount }.generate()

        suspend fun generateAsync(bitmap: ImageBitmap): Palette = from(bitmap).generateAsync()

        suspend fun generateAsync(bitmap: ImageBitmap, maximumColorCount: Int): Palette =
            from(bitmap) { this.maximumColorCount = maximumColorCount }.generateAsync()
    }
}

