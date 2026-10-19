// Copyright 2023-2026, Christopher Banes and the Haze project contributors
// SPDX-License-Identifier: Apache-2.0
// Adapted for Rachel: uniform blur, bitmap noise and a single SrcOver color tint.

package love.yinlin.compose.ui.node

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.DisposableHandle
import love.yinlin.compose.components.blur.R
import kotlin.math.abs


@RequiresApi(31)
internal class AndroidBlurRenderer(private val context: Context) : BlurRenderer {
    private var noiseTexture: Bitmap? = null

    override fun createEffect(blurRadiusPx: Float, tintColor: Color, noiseFactor: Float, inputScale: Float): RenderEffect {
        var effect = if (blurRadiusPx > 0f) {
            AndroidRenderEffect.createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP)
        }
        else AndroidRenderEffect.createOffsetEffect(0f, 0f)

        if (noiseFactor > 0f) {
            val noise = createNoiseShader(inputScale)
            if (Build.VERSION.SDK_INT >= 33 && tintColor.alpha >= 0.005f) {
                return AndroidBlurShader.create(effect, noise, noiseFactor, tintColor.toArgb()).asComposeRenderEffect()
            }

            val noiseInput = AndroidRenderEffect.createShaderEffect(noise)
            val noiseEffect = if (noiseFactor < 1f) {
                val matrix = ColorMatrix().apply { setScale(1f, 1f, 1f, noiseFactor) }
                AndroidRenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix), noiseInput)
            }
            else noiseInput

            effect = AndroidRenderEffect.createBlendModeEffect(effect, noiseEffect, BlendMode.SOFT_LIGHT)
        }

        if (tintColor.alpha >= 0.005f) {
            effect = AndroidRenderEffect.createColorFilterEffect(BlendModeColorFilter(tintColor.toArgb(), BlendMode.SRC_OVER), effect)
        }
        return effect.asComposeRenderEffect()
    }

    private fun createNoiseShader(inputScale: Float): BitmapShader {
        val texture = noiseTexture ?: run {
            val options = BitmapFactory.Options().apply { inScaled = false }
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.blur_noise, options)
            checkNotNull(bitmap) { "Unable to decode the blur noise texture." }.also { noiseTexture = it }
        }

        return BitmapShader(texture, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
            if (abs(inputScale - 1f) >= 0.001f) {
                setLocalMatrix(Matrix().apply {
                    val reciprocal = 1f / inputScale
                    setScale(reciprocal, reciprocal)
                })
            }
        }
    }

    override fun release() {
        noiseTexture = null
    }
}

internal actual fun createBlurRenderer(node: CompositionLocalConsumerModifierNode): BlurRenderer =
    if (Build.VERSION.SDK_INT >= 31) AndroidBlurRenderer(node.currentValueOf(LocalContext).applicationContext) else error("The Android blur renderer requires API 31.")

internal actual fun DrawScope.supportsBlur(): Boolean = Build.VERSION.SDK_INT >= 31 && drawContext.canvas.nativeCanvas.isHardwareAccelerated

internal actual fun needsBlurInvalidationWorkaround(): Boolean = Build.VERSION.SDK_INT == 31

@Suppress("OVERRIDE_DEPRECATION")
internal actual fun registerBlurMemoryCallback(node: CompositionLocalConsumerModifierNode, onTrim: () -> Unit): DisposableHandle {
    val context = node.currentValueOf(LocalContext).applicationContext
    val callback = object : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) = Unit
        override fun onLowMemory() = onTrim()
        override fun onTrimMemory(level: Int) = onTrim()
    }
    context.registerComponentCallbacks(callback)
    return DisposableHandle { context.unregisterComponentCallbacks(callback) }
}
