@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransform
import platform.CoreGraphics.CGAffineTransformMake
import platform.CoreGraphics.CGRect
import platform.darwin.Rect as DarwinRect

fun DarwinRect.asComposeRect(): Rect = Rect(this.left.toFloat(), this.top.toFloat(), this.right.toFloat(), this.bottom.toFloat())

fun CGRect.asComposeRect(): Rect {
    val p = this.origin
    val s = this.size
    return Rect(Offset(p.x.toFloat(), p.y.toFloat()), Size(s.width.toFloat(), s.height.toFloat()))
}

//fun CGAffineTransform.asComposeMatrix(): Matrix {
//
//}
//
//fun Matrix.asCGAffineTransform(): CGAffineTransform {
//    val v = this.values
//    // 0:m00, 4:m01, 8:m02, 12:m03 (tx)
//    // 1:m10, 5:m11, 9:m12, 13:m13 (ty)
//    return CGAffineTransformMake(
//        a = v[0].toDouble(),  // m00
//        b = v[1].toDouble(),  // m10
//        c = v[4].toDouble(),  // m01
//        d = v[5].toDouble(),  // m11
//        tx = v[12].toDouble(), // m03
//        ty = v[13].toDouble()  // m13
//    )
//}