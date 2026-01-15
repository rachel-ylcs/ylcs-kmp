package love.yinlin.compose.graphics

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Matrix

typealias AndroidBlendMode = android.graphics.BlendMode

fun BlendMode.asAndroidBlendMode(): AndroidBlendMode = when (this) {
    BlendMode.Clear -> AndroidBlendMode.CLEAR
    BlendMode.Src -> AndroidBlendMode.SRC
    BlendMode.Dst -> AndroidBlendMode.DST
    BlendMode.SrcOver -> AndroidBlendMode.SRC_OVER
    BlendMode.DstOver -> AndroidBlendMode.DST_OVER
    BlendMode.SrcIn -> AndroidBlendMode.SRC_IN
    BlendMode.DstIn -> AndroidBlendMode.DST_IN
    BlendMode.SrcOut -> AndroidBlendMode.SRC_OUT
    BlendMode.DstOut -> AndroidBlendMode.DST_OUT
    BlendMode.SrcAtop -> AndroidBlendMode.SRC_ATOP
    BlendMode.DstAtop -> AndroidBlendMode.DST_ATOP
    BlendMode.Xor -> AndroidBlendMode.XOR
    BlendMode.Plus -> AndroidBlendMode.PLUS
    BlendMode.Modulate -> AndroidBlendMode.MODULATE
    BlendMode.Screen -> AndroidBlendMode.SCREEN
    BlendMode.Overlay -> AndroidBlendMode.OVERLAY
    BlendMode.Darken -> AndroidBlendMode.DARKEN
    BlendMode.Lighten -> AndroidBlendMode.LIGHTEN
    BlendMode.ColorDodge -> AndroidBlendMode.COLOR_DODGE
    BlendMode.ColorBurn -> AndroidBlendMode.COLOR_BURN
    BlendMode.Hardlight -> AndroidBlendMode.HARD_LIGHT
    BlendMode.Softlight -> AndroidBlendMode.SOFT_LIGHT
    BlendMode.Difference -> AndroidBlendMode.DIFFERENCE
    BlendMode.Exclusion -> AndroidBlendMode.EXCLUSION
    BlendMode.Multiply -> AndroidBlendMode.MULTIPLY
    BlendMode.Hue -> AndroidBlendMode.HUE
    BlendMode.Saturation -> AndroidBlendMode.SATURATION
    BlendMode.Color -> AndroidBlendMode.COLOR
    BlendMode.Luminosity -> AndroidBlendMode.LUMINOSITY
    else -> error("Unknown BlendMode $this")
}

fun AndroidBlendMode.asComposeBlendMode(): BlendMode = when (this) {
    AndroidBlendMode.CLEAR -> BlendMode.Clear
    AndroidBlendMode.SRC -> BlendMode.Src
    AndroidBlendMode.DST -> BlendMode.Dst
    AndroidBlendMode.SRC_OVER -> BlendMode.SrcOver
    AndroidBlendMode.DST_OVER -> BlendMode.DstOver
    AndroidBlendMode.SRC_IN -> BlendMode.SrcIn
    AndroidBlendMode.DST_IN -> BlendMode.DstIn
    AndroidBlendMode.SRC_OUT -> BlendMode.SrcOut
    AndroidBlendMode.DST_OUT -> BlendMode.DstOut
    AndroidBlendMode.SRC_ATOP -> BlendMode.SrcAtop
    AndroidBlendMode.DST_ATOP -> BlendMode.DstAtop
    AndroidBlendMode.XOR -> BlendMode.Xor
    AndroidBlendMode.PLUS -> BlendMode.Plus
    AndroidBlendMode.MODULATE -> BlendMode.Modulate
    AndroidBlendMode.SCREEN -> BlendMode.Screen
    AndroidBlendMode.OVERLAY -> BlendMode.Overlay
    AndroidBlendMode.DARKEN -> BlendMode.Color
    AndroidBlendMode.LIGHTEN -> BlendMode.Color
    AndroidBlendMode.COLOR_DODGE -> BlendMode.ColorDodge
    AndroidBlendMode.COLOR_BURN -> BlendMode.ColorBurn
    AndroidBlendMode.HARD_LIGHT -> BlendMode.Hardlight
    AndroidBlendMode.SOFT_LIGHT -> BlendMode.Softlight
    AndroidBlendMode.DIFFERENCE -> BlendMode.Difference
    AndroidBlendMode.EXCLUSION -> BlendMode.Exclusion
    AndroidBlendMode.MULTIPLY -> BlendMode.Multiply
    AndroidBlendMode.HUE -> BlendMode.Hue
    AndroidBlendMode.SATURATION -> BlendMode.Saturation
    AndroidBlendMode.COLOR -> BlendMode.Color
    AndroidBlendMode.LUMINOSITY -> BlendMode.Luminosity
}

typealias AndroidMatrix = android.graphics.Matrix

fun Matrix.asAndroidMatrix(): AndroidMatrix {
    // 4 x 4 降维到 3 x 3
    val v = this.values
    val androidMatrix = AndroidMatrix()
    // ScaleX(0), SkewX(4), TransX(12)
    // SkewY(1), ScaleY(5), TransY(13)
    // Persp0(3), Persp1(7), Persp2(15)
    androidMatrix.setValues(floatArrayOf(
        v[Matrix.ScaleX], v[Matrix.SkewX], v[Matrix.TranslateX],
        v[Matrix.SkewY], v[Matrix.ScaleY], v[Matrix.TranslateY],
        v[Matrix.Perspective0], v[Matrix.Perspective1], v[Matrix.Perspective2]
    ))
    return androidMatrix
}

fun AndroidMatrix.asComposeMatrix(): Matrix {
    val v = FloatArray(9)
    this.getValues(v)
    // 3 x 3 升维到 4 x 4
    return Matrix(floatArrayOf(
        v[AndroidMatrix.MSCALE_X], v[AndroidMatrix.MSKEW_Y], 0f, v[AndroidMatrix.MPERSP_0], // Column 0: ScaleX, SkewY, 0, Persp0
        v[AndroidMatrix.MSKEW_X], v[AndroidMatrix.MSCALE_Y], 0f, v[AndroidMatrix.MPERSP_1], // Column 1: SkewX, ScaleY, 0, Persp1
        0f, 0f, 1f, 0f,   // Column 2: 0, 0, ScaleZ, 0
        v[AndroidMatrix.MTRANS_X], v[AndroidMatrix.MTRANS_Y], 0f, v[AndroidMatrix.MPERSP_2]  // Column 3: TransX, TransY, 0, Persp2
    ))
}