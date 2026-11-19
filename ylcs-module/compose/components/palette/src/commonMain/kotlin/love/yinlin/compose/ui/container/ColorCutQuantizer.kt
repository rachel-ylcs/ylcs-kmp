package love.yinlin.compose.ui.container

import kotlin.math.max
import kotlin.math.min

/**
 * 基于 AndroidX Palette ColorCutQuantizer 的 5-bit RGB 中值切分量化器。
 *
 * 使用 32³ 色彩直方图和按颜色空间体积优先切分的 Vbox，避免引入平台图像 API。
 */
internal class ColorCutQuantizer(
    pixels: IntArray,
    private val maximumColorCount: Int,
    private val filters: List<Palette.Filter>,
) {
    private val histogram = IntArray(1 shl (QUANTIZE_WORD_WIDTH * 3))
    private val colors: IntArray
    private val temporaryHsl = FloatArray(3)

    init {
        require(maximumColorCount > 0)

        for (pixel in pixels) {
            val quantized = quantizeFromRgb888(pixel)
            histogram[quantized]++
        }

        var distinctCount = 0
        for (color in histogram.indices) {
            if (histogram[color] > 0 && shouldIgnoreColor(approximateToRgb888(color))) histogram[color] = 0
            if (histogram[color] > 0) distinctCount++
        }

        colors = IntArray(distinctCount)
        var index = 0
        for (color in histogram.indices) {
            if (histogram[color] > 0) colors[index++] = color
        }
    }

    fun quantize(): List<Palette.Swatch> {
        if (colors.isEmpty()) return []

        if (colors.size <= maximumColorCount) {
            return colors.map { Palette.Swatch(approximateToRgb888(it), histogram[it]) }
        }

        val boxes = mutableListOf(Vbox(0, colors.lastIndex))
        while (boxes.size < maximumColorCount) {
            var largestIndex = -1
            var largestVolume = Int.MIN_VALUE
            for (index in boxes.indices) {
                val box = boxes[index]
                if (box.volume > largestVolume) {
                    largestIndex = index
                    largestVolume = box.volume
                }
            }

            if (largestIndex < 0) break
            val box = boxes.removeAt(largestIndex)
            if (!box.canSplit) {
                boxes += box
                break
            }

            boxes += box.splitBox()
            boxes += box
        }

        return boxes.mapNotNull { box ->
            box.averageColor().takeUnless { shouldIgnoreColor(it.rgb, it.hslArray) }
        }
    }

    private fun shouldIgnoreColor(color: Int): Boolean {
        if (filters.isEmpty()) return false
        PaletteColorUtils.rgbToHsl(color, temporaryHsl)
        return shouldIgnoreColor(color, temporaryHsl)
    }

    private fun shouldIgnoreColor(color: Int, hsl: FloatArray): Boolean {
        for (filter in filters) if (!filter.isAllowed(color, hsl)) return true
        return false
    }

    private inner class Vbox(
        private var lowerIndex: Int,
        private var upperIndex: Int,
    ) {
        private var population = 0
        private var minimumRed = 0
        private var maximumRed = 0
        private var minimumGreen = 0
        private var maximumGreen = 0
        private var minimumBlue = 0
        private var maximumBlue = 0

        val volume: Int
            get() = (maximumRed - minimumRed + 1) * (maximumGreen - minimumGreen + 1) * (maximumBlue - minimumBlue + 1)

        val canSplit: Boolean
            get() = upperIndex > lowerIndex

        init {
            fitBox()
        }

        private fun fitBox() {
            var minRed = Int.MAX_VALUE
            var minGreen = Int.MAX_VALUE
            var minBlue = Int.MAX_VALUE
            var maxRed = Int.MIN_VALUE
            var maxGreen = Int.MIN_VALUE
            var maxBlue = Int.MIN_VALUE
            var count = 0

            for (index in lowerIndex..upperIndex) {
                val color = colors[index]
                count += histogram[color]

                val red = quantizedRed(color)
                val green = quantizedGreen(color)
                val blue = quantizedBlue(color)

                minRed = min(minRed, red)
                minGreen = min(minGreen, green)
                minBlue = min(minBlue, blue)
                maxRed = max(maxRed, red)
                maxGreen = max(maxGreen, green)
                maxBlue = max(maxBlue, blue)
            }

            minimumRed = minRed
            minimumGreen = minGreen
            minimumBlue = minBlue
            maximumRed = maxRed
            maximumGreen = maxGreen
            maximumBlue = maxBlue
            population = count
        }

        private fun splitBox(): Vbox {
            check(canSplit) { "Cannot split a color box containing only one color." }

            val splitPoint = findSplitPoint()
            val newBox = Vbox(splitPoint + 1, upperIndex)
            upperIndex = splitPoint
            fitBox()
            return newBox
        }

        private fun findSplitPoint(): Int {
            val longestDimension = when {
                maximumRed - minimumRed >= maximumGreen - minimumGreen &&
                    maximumRed - minimumRed >= maximumBlue - minimumBlue -> COMPONENT_RED

                maximumGreen - minimumGreen >= maximumRed - minimumRed &&
                    maximumGreen - minimumGreen >= maximumBlue - minimumBlue -> COMPONENT_GREEN

                else -> COMPONENT_BLUE
            }

            modifySignificantOctet(colors, longestDimension, lowerIndex, upperIndex)
            colors.sort(lowerIndex, upperIndex + 1)
            modifySignificantOctet(colors, longestDimension, lowerIndex, upperIndex)

            val midpoint = population / 2
            var count = 0
            for (index in lowerIndex..upperIndex) {
                count += histogram[colors[index]]
                if (count >= midpoint) return min(upperIndex - 1, index)
            }
            return lowerIndex
        }

        fun averageColor(): Palette.Swatch {
            var redSum = 0
            var greenSum = 0
            var blueSum = 0
            var totalPopulation = 0

            for (index in lowerIndex..upperIndex) {
                val color = colors[index]
                val colorPopulation = histogram[color]
                totalPopulation += colorPopulation
                redSum += colorPopulation * quantizedRed(color)
                greenSum += colorPopulation * quantizedGreen(color)
                blueSum += colorPopulation * quantizedBlue(color)
            }

            val red = redSum / totalPopulation
            val green = greenSum / totalPopulation
            val blue = blueSum / totalPopulation
            return Palette.Swatch(approximateToRgb888(red, green, blue), totalPopulation)
        }
    }

    private fun quantizeFromRgb888(color: Int): Int {
        val red = ((color shr 16) and 0xFF) shr (8 - QUANTIZE_WORD_WIDTH)
        val green = ((color shr 8) and 0xFF) shr (8 - QUANTIZE_WORD_WIDTH)
        val blue = (color and 0xFF) shr (8 - QUANTIZE_WORD_WIDTH)
        return (red shl (QUANTIZE_WORD_WIDTH * 2)) or (green shl QUANTIZE_WORD_WIDTH) or blue
    }

    private fun approximateToRgb888(color: Int): Int = approximateToRgb888(
        quantizedRed(color),
        quantizedGreen(color),
        quantizedBlue(color),
    )

    private fun approximateToRgb888(red: Int, green: Int, blue: Int): Int =
        0xFF000000.toInt() or
            (modifyWordWidth(red, QUANTIZE_WORD_WIDTH, 8) shl 16) or
            (modifyWordWidth(green, QUANTIZE_WORD_WIDTH, 8) shl 8) or
            modifyWordWidth(blue, QUANTIZE_WORD_WIDTH, 8)

    private fun modifyWordWidth(value: Int, currentWidth: Int, targetWidth: Int): Int {
        val shifted = if (targetWidth > currentWidth) value shl (targetWidth - currentWidth)
        else value shr (currentWidth - targetWidth)
        return shifted and ((1 shl targetWidth) - 1)
    }

    private fun quantizedRed(color: Int): Int = (color shr (QUANTIZE_WORD_WIDTH * 2)) and QUANTIZE_WORD_MASK
    private fun quantizedGreen(color: Int): Int = (color shr QUANTIZE_WORD_WIDTH) and QUANTIZE_WORD_MASK
    private fun quantizedBlue(color: Int): Int = color and QUANTIZE_WORD_MASK

    private fun modifySignificantOctet(array: IntArray, dimension: Int, lower: Int, upper: Int) {
        when (dimension) {
            COMPONENT_GREEN -> for (index in lower..upper) {
                val color = array[index]
                array[index] = (quantizedGreen(color) shl (QUANTIZE_WORD_WIDTH * 2)) or
                    (quantizedRed(color) shl QUANTIZE_WORD_WIDTH) or quantizedBlue(color)
            }

            COMPONENT_BLUE -> for (index in lower..upper) {
                val color = array[index]
                array[index] = (quantizedBlue(color) shl (QUANTIZE_WORD_WIDTH * 2)) or
                    (quantizedGreen(color) shl QUANTIZE_WORD_WIDTH) or quantizedRed(color)
            }
        }
    }

    private companion object {
        const val QUANTIZE_WORD_WIDTH = 5
        const val QUANTIZE_WORD_MASK = (1 shl QUANTIZE_WORD_WIDTH) - 1
        const val COMPONENT_RED = -3
        const val COMPONENT_GREEN = -2
        const val COMPONENT_BLUE = -1
    }
}
