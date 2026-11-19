/*
 * Copyright 2018 The Android Open Source Project
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

/**
 * 描述调色板中目标颜色的 HSL 范围与评分权重。
 *
 * 可直接使用六种内置目标，也可以通过 [invoke] 或 [copy] 使用 DSL 创建自定义目标。
 */
@Stable
class Target private constructor(
    private val saturation: FloatArray,
    private val lightness: FloatArray,
    private val weights: FloatArray,
    val isExclusive: Boolean,
) {
    val minimumSaturation: Float get() = saturation[0]
    val targetSaturation: Float get() = saturation[1]
    val maximumSaturation: Float get() = saturation[2]

    val minimumLightness: Float get() = lightness[0]
    val targetLightness: Float get() = lightness[1]
    val maximumLightness: Float get() = lightness[2]

    val saturationWeight: Float get() = weights[0]
    val lightnessWeight: Float get() = weights[1]
    val populationWeight: Float get() = weights[2]

    internal fun normalizedWeights(): FloatArray {
        val total = weights.sum()
        return if (total > 0f) floatArrayOf(weights[0] / total, weights[1] / total, weights[2] / total)
        else floatArrayOf(0f, 0f, 0f)
    }

    /** 基于当前目标创建自定义副本。 */
    fun copy(block: Builder.() -> Unit): Target = Builder(this).apply(block).build()

    /**
     * 目标色配置 DSL。
     *
     * 饱和度、明度和评分权重均使用 0..1 范围；权重会在评分时自动归一化。
     */
    @PaletteDsl
    class Builder internal constructor(target: Target? = null) {
        var minimumSaturation = target?.minimumSaturation ?: 0f
        var targetSaturation = target?.targetSaturation ?: 0.5f
        var maximumSaturation = target?.maximumSaturation ?: 1f

        var minimumLightness = target?.minimumLightness ?: 0f
        var targetLightness = target?.targetLightness ?: 0.5f
        var maximumLightness = target?.maximumLightness ?: 1f

        var saturationWeight = target?.saturationWeight ?: 0.24f
        var lightnessWeight = target?.lightnessWeight ?: 0.52f
        var populationWeight = target?.populationWeight ?: 0.24f

        var exclusive = target?.isExclusive ?: true

        fun build(): Target {
            require(minimumSaturation in 0f..1f && targetSaturation in 0f..1f && maximumSaturation in 0f..1f)
            require(minimumSaturation <= targetSaturation && targetSaturation <= maximumSaturation)
            require(minimumLightness in 0f..1f && targetLightness in 0f..1f && maximumLightness in 0f..1f)
            require(minimumLightness <= targetLightness && targetLightness <= maximumLightness)
            require(saturationWeight >= 0f && lightnessWeight >= 0f && populationWeight >= 0f)

            return Target(
                floatArrayOf(minimumSaturation, targetSaturation, maximumSaturation),
                floatArrayOf(minimumLightness, targetLightness, maximumLightness),
                floatArrayOf(saturationWeight, lightnessWeight, populationWeight),
                exclusive,
            )
        }
    }

    companion object {
        private const val TARGET_DARK_LIGHTNESS = 0.26f
        private const val MAX_DARK_LIGHTNESS = 0.45f
        private const val MIN_LIGHT_LIGHTNESS = 0.55f
        private const val TARGET_LIGHT_LIGHTNESS = 0.74f
        private const val MIN_NORMAL_LIGHTNESS = 0.3f
        private const val TARGET_NORMAL_LIGHTNESS = 0.5f
        private const val MAX_NORMAL_LIGHTNESS = 0.7f
        private const val TARGET_MUTED_SATURATION = 0.3f
        private const val MAX_MUTED_SATURATION = 0.4f
        private const val MIN_VIBRANT_SATURATION = 0.35f

        private fun target(
            minimumSaturation: Float,
            targetSaturation: Float,
            maximumSaturation: Float,
            minimumLightness: Float,
            targetLightness: Float,
            maximumLightness: Float,
        ) = Target(
            floatArrayOf(minimumSaturation, targetSaturation, maximumSaturation),
            floatArrayOf(minimumLightness, targetLightness, maximumLightness),
            floatArrayOf(0.24f, 0.52f, 0.24f),
            true,
        )

        /** 明亮且高饱和的颜色。 */
        val LIGHT_VIBRANT = target(MIN_VIBRANT_SATURATION, 1f, 1f, MIN_LIGHT_LIGHTNESS, TARGET_LIGHT_LIGHTNESS, 1f)

        /** 中等明度且高饱和的颜色。 */
        val VIBRANT = target(MIN_VIBRANT_SATURATION, 1f, 1f, MIN_NORMAL_LIGHTNESS, TARGET_NORMAL_LIGHTNESS, MAX_NORMAL_LIGHTNESS)

        /** 暗色且高饱和的颜色。 */
        val DARK_VIBRANT = target(MIN_VIBRANT_SATURATION, 1f, 1f, 0f, TARGET_DARK_LIGHTNESS, MAX_DARK_LIGHTNESS)

        /** 明亮且低饱和的颜色。 */
        val LIGHT_MUTED = target(0f, TARGET_MUTED_SATURATION, MAX_MUTED_SATURATION, MIN_LIGHT_LIGHTNESS, TARGET_LIGHT_LIGHTNESS, 1f)

        /** 中等明度且低饱和的颜色。 */
        val MUTED = target(0f, TARGET_MUTED_SATURATION, MAX_MUTED_SATURATION, MIN_NORMAL_LIGHTNESS, TARGET_NORMAL_LIGHTNESS, MAX_NORMAL_LIGHTNESS)

        /** 暗色且低饱和的颜色。 */
        val DARK_MUTED = target(0f, TARGET_MUTED_SATURATION, MAX_MUTED_SATURATION, 0f, TARGET_DARK_LIGHTNESS, MAX_DARK_LIGHTNESS)

        internal val DEFAULT_TARGETS = [
            LIGHT_VIBRANT,
            VIBRANT,
            DARK_VIBRANT,
            LIGHT_MUTED,
            MUTED,
            DARK_MUTED,
        ]

        /** 使用 DSL 创建自定义目标色。 */
        operator fun invoke(block: Builder.() -> Unit): Target = Builder().apply(block).build()
    }
}
