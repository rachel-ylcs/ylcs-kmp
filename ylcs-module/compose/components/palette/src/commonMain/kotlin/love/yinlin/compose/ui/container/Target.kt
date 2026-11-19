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
 * 可通过 [Builder] 创建自定义目标，也可以直接使用六种内置目标。
 */
@Stable
class Target internal constructor(
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

    /**
     * 自定义目标的构建器。权重会在调色板评分时自动归一化。
     */
    class Builder {
        private val saturation = floatArrayOf(0f, 0.5f, 1f)
        private val lightness = floatArrayOf(0f, 0.5f, 1f)
        private val weights = floatArrayOf(0.24f, 0.52f, 0.24f)
        private var exclusive = true

        constructor()

        constructor(target: Target) {
            target.saturation.copyInto(saturation)
            target.lightness.copyInto(lightness)
            target.weights.copyInto(weights)
            exclusive = target.isExclusive
        }

        fun setMinimumSaturation(value: Float) = apply {
            require(value in 0f..1f)
            saturation[0] = value
        }

        fun setTargetSaturation(value: Float) = apply {
            require(value in 0f..1f)
            saturation[1] = value
        }

        fun setMaximumSaturation(value: Float) = apply {
            require(value in 0f..1f)
            saturation[2] = value
        }

        fun setMinimumLightness(value: Float) = apply {
            require(value in 0f..1f)
            lightness[0] = value
        }

        fun setTargetLightness(value: Float) = apply {
            require(value in 0f..1f)
            lightness[1] = value
        }

        fun setMaximumLightness(value: Float) = apply {
            require(value in 0f..1f)
            lightness[2] = value
        }

        fun setSaturationWeight(weight: Float) = apply {
            require(weight >= 0f)
            weights[0] = weight
        }

        fun setLightnessWeight(weight: Float) = apply {
            require(weight >= 0f)
            weights[1] = weight
        }

        fun setPopulationWeight(weight: Float) = apply {
            require(weight >= 0f)
            weights[2] = weight
        }

        fun setExclusive(exclusive: Boolean) = apply {
            this.exclusive = exclusive
        }

        fun build(): Target {
            require(saturation[0] <= saturation[1] && saturation[1] <= saturation[2])
            require(lightness[0] <= lightness[1] && lightness[1] <= lightness[2])
            return Target(saturation.copyOf(), lightness.copyOf(), weights.copyOf(), exclusive)
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
    }
}
