@file:OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
package love.yinlin.compose.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.FloatArrayCompatible
import kotlin.js.ExperimentalWasmJsInterop

internal typealias PlatformPAG = org.libpag.PAG
internal typealias PlatformPAGFont = org.libpag.PAGFont
internal typealias PlatformPAGImage = org.libpag.PAGImage
internal typealias PlatformPAGRect = org.libpag.Rect
internal typealias PlatformPAGLayer = org.libpag.PAGLayer
internal typealias PlatformPAGMarker = org.libpag.Marker
internal typealias PlatformPAGMatrix = org.libpag.Matrix
internal typealias PlatformPAGVideoRange = org.libpag.PAGVideoRange

internal fun PlatformPAGRect.asComposeRect(): Rect = Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

internal fun Matrix.asPAGMatrix(): PlatformPAGMatrix {
    val v = FloatArrayCompatible(this.values)
    return PlatformPAGMatrix.makeAll(
        v[Matrix.ScaleX].toDouble(),
        v[Matrix.SkewX].toDouble(),
        v[Matrix.TranslateX].toDouble(),
        v[Matrix.SkewY].toDouble(),
        v[Matrix.ScaleY].toDouble(),
        v[Matrix.TranslateY].toDouble()
    )
}

internal fun PlatformPAGMatrix.asComposeMatrix(): Matrix = Matrix(floatArrayOf(
    this.a.toFloat(), this.b.toFloat(), 0f, 0f, // Column 0: ScaleX, SkewY, 0, Persp0
    this.c.toFloat(), this.d.toFloat(), 0f, 0f, // Column 1: SkewX, ScaleY, 0, Persp1
    0f, 0f, 1f, 0f,   // Column 2: 0, 0, ScaleZ, 0
    this.tx.toFloat(), this.ty.toFloat(), 0f, 1f  // Column 3: TransX, TransY, 0, Persp2
))