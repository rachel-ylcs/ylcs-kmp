// Copyright 2023-2026, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for Rachel: uniform blur, fixed fractal noise and a single SrcOver color tint.

package love.yinlin.compose.ui.node

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import org.jetbrains.skia.BlendMode
import org.jetbrains.skia.ColorFilter
import org.jetbrains.skia.ColorMatrix
import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.Shader

internal actual fun createBlurRenderer(node: CompositionLocalConsumerModifierNode): BlurRenderer = SkiaBlurRenderer()

internal actual fun DrawScope.supportsBlur(): Boolean = true

internal actual fun needsBlurInvalidationWorkaround(): Boolean = false

internal class SkiaBlurRenderer : BlurRenderer {
    private var currentFilter: ImageFilter? = null
    private var noiseShader: Shader? = null

    override fun createEffect(blurRadiusPx: Float, tintColor: Color, noiseFactor: Float, inputScale: Float): RenderEffect {
        currentFilter?.close()
        currentFilter = null

        var effect = if (blurRadiusPx > 0f) {
            val sigma = blurRadiusPx * 0.57735f + 0.5f
            ImageFilter.makeBlur(sigma, sigma, FilterTileMode.CLAMP)
        }
        else ImageFilter.makeOffset(0f, 0f, input = null, crop = null)

        if (noiseFactor > 0f) {
            val shader = noiseShader ?: Shader.makeFractalNoise(0.45f, 0.45f, 4, 2f).also { noiseShader = it }
            val noiseInput = ImageFilter.makeShader(shader, crop = null)
            val noiseEffect = if (noiseFactor < 1f) {
                val colorFilter = ColorFilter.makeMatrix(ColorMatrix(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, noiseFactor, 0f,
                ))
                ImageFilter.makeColorFilter(colorFilter, noiseInput, crop = null).also {
                    colorFilter.close()
                    noiseInput.close()
                }
            }
            else noiseInput

            val blended = ImageFilter.makeBlend(BlendMode.SOFT_LIGHT, effect, noiseEffect, crop = null)
            effect.close()
            noiseEffect.close()
            effect = blended
        }

        if (tintColor.alpha >= 0.005f) {
            val colorFilter = ColorFilter.makeBlend(tintColor.toArgb(), BlendMode.SRC_OVER)
            val tinted = ImageFilter.makeColorFilter(colorFilter, effect, crop = null)
            colorFilter?.close()
            effect.close()
            effect = tinted
        }

        currentFilter = effect
        return effect.asComposeRenderEffect()
    }

    override fun release() {
        currentFilter?.close()
        currentFilter = null
        noiseShader?.close()
        noiseShader = null
    }
}
