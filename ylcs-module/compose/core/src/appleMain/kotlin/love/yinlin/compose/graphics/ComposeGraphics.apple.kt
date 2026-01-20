@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import kotlinx.cinterop.*
import platform.CoreGraphics.CGAffineTransform
import platform.CoreGraphics.CGAffineTransformMake
import platform.CoreGraphics.CGFloatVar
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.darwin.Rect as DarwinRect

fun UIColor.asComposeColor(): Color = memScoped {
    val red = alloc<CGFloatVar>()
    val green = alloc<CGFloatVar>()
    val blue = alloc<CGFloatVar>()
    val alpha = alloc<CGFloatVar>()
    this@asComposeColor.getRed(red.ptr, green.ptr, blue.ptr, alpha.ptr)
    Color(red = red.value.toFloat(), green = green.value.toFloat(), blue = blue.value.toFloat(), alpha = alpha.value.toFloat())
}

fun Color.asUIColor(): UIColor = UIColor(red = red.toDouble(), green = green.toDouble(), blue = blue.toDouble(), alpha = alpha.toDouble())

fun DarwinRect.asComposeRect(): Rect = Rect(this.left.toFloat(), this.top.toFloat(), this.right.toFloat(), this.bottom.toFloat())

fun CGRect.asComposeRect(): Rect {
    val p = this.origin
    val s = this.size
    return Rect(Offset(p.x.toFloat(), p.y.toFloat()), Size(s.width.toFloat(), s.height.toFloat()))
}

fun CValue<CGRect>.asComposeRect(): Rect = this.useContents { asComposeRect() }

fun Rect.asCGRect(): CValue<CGRect> = CGRectMake(this.left.toDouble(), this.top.toDouble(), this.width.toDouble(), this.height.toDouble())

fun CGAffineTransform.asComposeMatrix(): Matrix = Matrix(floatArrayOf(
    this.a.toFloat(), this.b.toFloat(), 0f, 0f, // Column 0: ScaleX, SkewY, 0, Persp0
    this.c.toFloat(), this.d.toFloat(), 0f, 0f, // Column 1: SkewX, ScaleY, 0, Persp1
    0f, 0f, 1f, 0f,   // Column 2: 0, 0, ScaleZ, 0
    this.tx.toFloat(), this.ty.toFloat(), 0f, 1f  // Column 3: TransX, TransY, 0, Persp2
))

fun CValue<CGAffineTransform>.asComposeMatrix(): Matrix = this.useContents { asComposeMatrix() }

fun Matrix.asCGAffineTransform(): CValue<CGAffineTransform> {
    val v = this.values
    // 0:m00, 4:m01, 8:m02, 12:m03 (tx)
    // 1:m10, 5:m11, 9:m12, 13:m13 (ty)
    return CGAffineTransformMake(
        a = v[Matrix.ScaleX].toDouble(),  // m00
        b = v[Matrix.SkewY].toDouble(),  // m10
        c = v[Matrix.SkewX].toDouble(),  // m01
        d = v[Matrix.ScaleY].toDouble(),  // m11
        tx = v[Matrix.TranslateX].toDouble(), // m03
        ty = v[Matrix.TranslateY].toDouble()  // m13
    )
}