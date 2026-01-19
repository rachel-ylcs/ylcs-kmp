@file:OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
package love.yinlin.compose.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.FloatArrayCompatible
import kotlin.js.ExperimentalWasmJsInterop

typealias PlatformPAG = org.libpag.PAG
typealias PlatformPAGImage = org.libpag.PAGImage
typealias PlatformRect = org.libpag.Rect
typealias PlatformMatrix = org.libpag.Matrix

internal fun PlatformRect.asComposeRect(): Rect = Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

internal fun Matrix.asPAGMatrix(): PlatformMatrix {
    val v = FloatArrayCompatible(this.values)
    return PlatformMatrix.makeAll(
        v[Matrix.ScaleX].toDouble(),
        v[Matrix.SkewX].toDouble(),
        v[Matrix.TranslateX].toDouble(),
        v[Matrix.SkewY].toDouble(),
        v[Matrix.ScaleY].toDouble(),
        v[Matrix.TranslateY].toDouble()
    )
}

internal fun PlatformMatrix.asComposeMatrix(): Matrix = Matrix(floatArrayOf(
    this.a.toFloat(), this.b.toFloat(), 0f, 0f, // Column 0: ScaleX, SkewY, 0, Persp0
    this.c.toFloat(), this.d.toFloat(), 0f, 0f, // Column 1: SkewX, ScaleY, 0, Persp1
    0f, 0f, 1f, 0f,   // Column 2: 0, 0, ScaleZ, 0
    this.tx.toFloat(), this.ty.toFloat(), 0f, 1f  // Column 3: TransX, TransY, 0, Persp2
))