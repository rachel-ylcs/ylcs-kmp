@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.compose.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import love.yinlin.compatible.FloatArrayCompatible
import love.yinlin.compatible.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

private var pagInstance: PAG? = null
private val pagMutex = Mutex()
internal suspend fun pag(): PAG? = pagInstance ?: pagMutex.withLock {
    pagInstance ?: PAGInit().await().also { pagInstance = it }
}

internal val defaultPAGViewOptions: PAGViewOptions = js("({})")

internal fun PAGRect.asComposeRect(): Rect = Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

internal fun Matrix.asPAGMatrix(): PAGMatrix {
    val v = FloatArrayCompatible(this.values)
    return PAGMatrix.makeAll(
        v[Matrix.ScaleX].toDouble(),
        v[Matrix.SkewX].toDouble(),
        v[Matrix.TranslateX].toDouble(),
        v[Matrix.SkewY].toDouble(),
        v[Matrix.ScaleY].toDouble(),
        v[Matrix.TranslateY].toDouble()
    )
}

internal fun PAGMatrix.asComposeMatrix(): Matrix = Matrix(floatArrayOf(
    this.a.toFloat(), this.b.toFloat(), 0f, 0f, // Column 0: ScaleX, SkewY, 0, Persp0
    this.c.toFloat(), this.d.toFloat(), 0f, 0f, // Column 1: SkewX, ScaleY, 0, Persp1
    0f, 0f, 1f, 0f,   // Column 2: 0, 0, ScaleZ, 0
    this.tx.toFloat(), this.ty.toFloat(), 0f, 1f  // Column 3: TransX, TransY, 0, Persp2
))