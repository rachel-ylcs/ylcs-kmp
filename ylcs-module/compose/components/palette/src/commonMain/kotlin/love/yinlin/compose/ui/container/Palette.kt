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
import love.yinlin.coroutines.Coroutines
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * 从图像中提取的代表性色彩集合
 */
@Stable
class Palette internal constructor(swatches: List<Swatch>, targets: List<PaletteTarget>) {
    val swatches: List<Swatch> = swatches.toList()
    val targets: List<PaletteTarget> = targets.toList()
    val dominantSwatch: Swatch? = this.swatches.maxByOrNull { it.population }
    val maxPopulation: Int = dominantSwatch?.population ?: 0

    private val selectedSwatches = mutableMapOf<PaletteTarget, Swatch>()

    init {
        generateTargets()
    }

    val vibrantSwatch: Swatch? get() = getSwatchForTarget(PaletteTarget.VIBRANT)
    val lightVibrantSwatch: Swatch? get() = getSwatchForTarget(PaletteTarget.LIGHT_VIBRANT)
    val darkVibrantSwatch: Swatch? get() = getSwatchForTarget(PaletteTarget.DARK_VIBRANT)
    val mutedSwatch: Swatch? get() = getSwatchForTarget(PaletteTarget.MUTED)
    val lightMutedSwatch: Swatch? get() = getSwatchForTarget(PaletteTarget.LIGHT_MUTED)
    val darkMutedSwatch: Swatch? get() = getSwatchForTarget(PaletteTarget.DARK_MUTED)

    val vibrantColor: Color? get() = vibrantSwatch?.color
    val lightVibrantColor: Color? get() = lightVibrantSwatch?.color
    val darkVibrantColor: Color? get() = darkVibrantSwatch?.color
    val mutedColor: Color? get() = mutedSwatch?.color
    val lightMutedColor: Color? get() = lightMutedSwatch?.color
    val darkMutedColor: Color? get() = darkMutedSwatch?.color
    val dominantColor: Color? get() = dominantSwatch?.color

    fun getSwatchForTarget(target: PaletteTarget): Swatch? = selectedSwatches[target]

    fun getColorForTarget(target: PaletteTarget, defaultColor: Int): Int = getSwatchForTarget(target)?.rgb ?: defaultColor

    fun getColorForTarget(target: PaletteTarget, defaultColor: Color = Color.Unspecified): Color = getSwatchForTarget(target)?.color ?: defaultColor

    private fun generateTargets() {
        val usedColors: MutableSet<Int> = []
        for (target in targets) {
            val weights = target.normalizedWeights()
            var bestSwatch: Swatch? = null
            var bestScore = 0f

            for (swatch in swatches) {
                val hsl = swatch.hslArray
                if (hsl[1] < target.minimumSaturation || hsl[1] > target.maximumSaturation || hsl[2] < target.minimumLightness || hsl[2] > target.maximumLightness || swatch.rgb in usedColors) continue

                val score = weights[0] * (1f - abs(hsl[1] - target.targetSaturation)) +
                    weights[1] * (1f - abs(hsl[2] - target.targetLightness)) +
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
     * 代表调色板中的一种颜色及其像素占比
     */
    @Stable
    class Swatch(val rgb: Int, val population: Int) {
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

            if (lightBodyAlpha != -1 && lightTitleAlpha != -1) return TextColors(setAlpha(WHITE, lightTitleAlpha), setAlpha(WHITE, lightBodyAlpha))

            val darkBodyAlpha = PaletteColorUtils.calculateMinimumAlpha(BLACK, rgb, MIN_CONTRAST_BODY_TEXT)
            val darkTitleAlpha = PaletteColorUtils.calculateMinimumAlpha(BLACK, rgb, MIN_CONTRAST_TITLE_TEXT)

            if (darkBodyAlpha != -1 && darkTitleAlpha != -1) return TextColors(setAlpha(BLACK, darkTitleAlpha), setAlpha(BLACK, darkBodyAlpha))

            val title = if (lightTitleAlpha != -1) setAlpha(WHITE, lightTitleAlpha) else setAlpha(BLACK, darkTitleAlpha)
            val body = if (lightBodyAlpha != -1) setAlpha(WHITE, lightBodyAlpha) else setAlpha(BLACK, darkBodyAlpha)
            return TextColors(title, body)
        }

        override fun equals(other: Any?): Boolean = other is Swatch && rgb == other.rgb && population == other.population

        override fun hashCode(): Int = 31 * rgb + population

        override fun toString(): String = "Swatch [RGB: #${rgb.toUInt().toString(16)}] [HSL: ${hsl.contentToString()}] [Population: $population]"

        private data class TextColors(val title: Int, val body: Int)

        private companion object {
            const val WHITE = -1
            const val BLACK = -0x1000000
            const val MIN_CONTRAST_TITLE_TEXT = 3f
            const val MIN_CONTRAST_BODY_TEXT = 4.5f

            fun setAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)
        }
    }

    /** 控制量化阶段允许进入调色板的颜色 */
    fun interface Filter {
        fun isAllowed(rgb: Int, hsl: FloatArray): Boolean
    }

    /**
     * 调色板配置 DSL
     */
    @PaletteDsl
    class Builder private constructor(
        private val bitmap: ImageBitmap?,
        private val inputSwatches: List<Swatch>?,
    ) {
        private val targets: MutableList<PaletteTarget> = []
        private val filters: MutableList<Filter> = [DEFAULT_FILTER]
        private var resizeArea = DEFAULT_RESIZE_BITMAP_AREA
        private var selectedRegion: Region? = null

        // 最大保留颜色数量，默认 16
        var maximumColorCount = DEFAULT_CALCULATE_NUMBER_COLORS
            set(value) {
                require(value > 0) { "maximumColorCount must be greater than zero." }
                field = value
            }

        // 采样像素总面积上限，默认 112×112
        var resizeBitmapArea: Int
            get() = resizeArea
            set(value) {
                require(value > 0) { "resizeBitmapArea must be greater than zero." }
                resizeArea = value
            }

        internal constructor(bitmap: ImageBitmap) : this(bitmap, null) {
            targets += PaletteTarget.DEFAULT_TARGETS
        }

        internal constructor(swatches: List<Swatch>) : this(null, swatches.toList()) {
            require(swatches.isNotEmpty()) { "List of swatches must not be empty." }
        }

        fun filter(filter: Filter) {
            filters += filter
        }

        fun clearFilters() {
            filters.clear()
        }

        fun region(left: Int, top: Int, right: Int, bottom: Int) {
            val source = bitmap ?: return
            val clippedLeft = left.coerceAtLeast(0)
            val clippedTop = top.coerceAtLeast(0)
            val clippedRight = right.coerceAtMost(source.width)
            val clippedBottom = bottom.coerceAtMost(source.height)

            require(clippedLeft < clippedRight && clippedTop < clippedBottom) { "The given region must intersect with the ImageBitmap dimensions." }

            selectedRegion = Region(clippedLeft, clippedTop, clippedRight, clippedBottom)
        }

        fun clearRegion() {
            selectedRegion = null
        }

        fun target(target: PaletteTarget) {
            if (target !in targets) targets += target
        }

        fun target(block: PaletteTarget.Builder.() -> Unit) {
            target(PaletteTarget(block))
        }

        fun clearTargets() {
            targets.clear()
        }

        suspend fun generate(): Palette = Coroutines.cpu {
            val source = bitmap ?: return@cpu Palette(inputSwatches.orEmpty(), targets)

            val pixels = extractPixels(source)
            val swatches = ColorCutQuantizer(pixels.values, maximumColorCount, filters).quantize()
            Palette(swatches, targets)
        }

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

        private class PixelData(val values: IntArray, val width: Int, val height: Int)
    }

    companion object {
        private const val DEFAULT_FILTER_BLACK_MAX_LIGHTNESS = 0.05f
        private const val DEFAULT_FILTER_WHITE_MIN_LIGHTNESS = 0.95f
        const val DEFAULT_RESIZE_BITMAP_AREA = 112 * 112 // 默认量化采样面积，112×112
        const val DEFAULT_CALCULATE_NUMBER_COLORS = 16 // 默认生成 16 种代表色

        private val DEFAULT_FILTER = Filter { _, hsl ->
            val isBlack = hsl[2] <= DEFAULT_FILTER_BLACK_MAX_LIGHTNESS
            val isWhite = hsl[2] >= DEFAULT_FILTER_WHITE_MIN_LIGHTNESS
            val isNearRedILine = hsl[0] in 10f .. 37f && hsl[1] <= 0.82f
            !isBlack && !isWhite && !isNearRedILine
        }

        fun from(bitmap: ImageBitmap, block: Builder.() -> Unit = { }): Builder = Builder(bitmap).apply(block)
    }
}

