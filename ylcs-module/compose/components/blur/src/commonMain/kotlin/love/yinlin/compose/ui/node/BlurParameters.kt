// Copyright 2023-2026, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for Rachel: fixed source/target blur without the generic effects runtime.

package love.yinlin.compose.ui.node

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * 背景模糊的固定参数。可共享为模块内的预设，通过 [copy] 调整单个参数。
 *
 * @param blurRadius 有限且非负的模糊半径，支持零半径；按实际密度和采样比例计算。
 * @param backgroundColor 在来源下方填充的底色，支持任意已指定颜色和整个透明度范围。
 * 半透明底色（0 < alpha < 0.9）将采样范围裁剪到 source，保持原来的边缘行为。
 * @param tintColor 在模糊和噪点后以 SrcOver 叠加的颜色，支持任意已指定颜色和整个透明度范围。
 * 真模糊路径忽略 alpha 小于 0.005 的 tint，与原实现一致。
 * @param noiseFactor Softlight 噪点强度，完整支持 0..1；零值不创建噪点资源。
 * @param qualityFraction 采样质量，完整支持 0..1；采样宽高比例为 sqrt(0.25 + 0.75 * qualityFraction)。
 *
 * 构造及 [copy] 均校验参数，越界或未指定的输入抛出 [IllegalArgumentException]。
 */
@Stable
data class BlurParameters(
    val blurRadius: Dp = 20.dp,
    val backgroundColor: Color = Color.Transparent,
    val tintColor: Color = Color.Transparent,
    val noiseFactor: Float = 0.15f,
    val qualityFraction: Float = 0.5f,
) {
    init {
        require(blurRadius.value.isFinite() && blurRadius >= 0.dp) { "blurRadius must be finite and non-negative." }
        require(backgroundColor.isSpecified && tintColor.isSpecified) { "Blur colors must be specified." }
        require(noiseFactor.isFinite() && noiseFactor in 0f .. 1f) { "noiseFactor must be in 0..1." }
        require(qualityFraction.isFinite() && qualityFraction in 0f .. 1f) { "qualityFraction must be in 0..1." }
    }

    internal val inputScale: Float get() = sqrt(0.25f + (1f - 0.25f) * qualityFraction)

    internal val clipToSource: Boolean get() = backgroundColor.alpha > 0f && backgroundColor.alpha < 0.9f

    internal val effectTint: Color get() = if (tintColor.alpha >= 0.005f) tintColor else Color.Transparent

    internal val fallbackTint: Color get() = tintColor.copy(alpha = (tintColor.alpha * (1f + blurRadius.value / 72f)).coerceAtMost(1f))
}
