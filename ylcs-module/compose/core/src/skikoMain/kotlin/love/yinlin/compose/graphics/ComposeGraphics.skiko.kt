package love.yinlin.compose.graphics

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Matrix
import org.jetbrains.skia.Matrix33
import org.jetbrains.skia.Matrix44

typealias SkiaBlendMode = org.jetbrains.skia.BlendMode

fun BlendMode.asSkiaBlendMode(): SkiaBlendMode = when (this) {
    BlendMode.Clear -> SkiaBlendMode.CLEAR
    BlendMode.Src -> SkiaBlendMode.SRC
    BlendMode.Dst -> SkiaBlendMode.DST
    BlendMode.SrcOver -> SkiaBlendMode.SRC_OVER
    BlendMode.DstOver -> SkiaBlendMode.DST_OVER
    BlendMode.SrcIn -> SkiaBlendMode.SRC_IN
    BlendMode.DstIn -> SkiaBlendMode.DST_IN
    BlendMode.SrcOut -> SkiaBlendMode.SRC_OUT
    BlendMode.DstOut -> SkiaBlendMode.DST_OUT
    BlendMode.SrcAtop -> SkiaBlendMode.SRC_ATOP
    BlendMode.DstAtop -> SkiaBlendMode.DST_ATOP
    BlendMode.Xor -> SkiaBlendMode.XOR
    BlendMode.Plus -> SkiaBlendMode.PLUS
    BlendMode.Modulate -> SkiaBlendMode.MODULATE
    BlendMode.Screen -> SkiaBlendMode.SCREEN
    BlendMode.Overlay -> SkiaBlendMode.OVERLAY
    BlendMode.Darken -> SkiaBlendMode.DARKEN
    BlendMode.Lighten -> SkiaBlendMode.LIGHTEN
    BlendMode.ColorDodge -> SkiaBlendMode.COLOR_DODGE
    BlendMode.ColorBurn -> SkiaBlendMode.COLOR_BURN
    BlendMode.Hardlight -> SkiaBlendMode.HARD_LIGHT
    BlendMode.Softlight -> SkiaBlendMode.SOFT_LIGHT
    BlendMode.Difference -> SkiaBlendMode.DIFFERENCE
    BlendMode.Exclusion -> SkiaBlendMode.EXCLUSION
    BlendMode.Multiply -> SkiaBlendMode.MULTIPLY
    BlendMode.Hue -> SkiaBlendMode.HUE
    BlendMode.Saturation -> SkiaBlendMode.SATURATION
    BlendMode.Color -> SkiaBlendMode.COLOR
    BlendMode.Luminosity -> SkiaBlendMode.LUMINOSITY
    else -> error("Unknown BlendMode $this")
}

fun SkiaBlendMode.asComposeBlendMode(): BlendMode = when (this) {
    SkiaBlendMode.CLEAR -> BlendMode.Clear
    SkiaBlendMode.SRC -> BlendMode.Src
    SkiaBlendMode.DST -> BlendMode.Dst
    SkiaBlendMode.SRC_OVER -> BlendMode.SrcOver
    SkiaBlendMode.DST_OVER -> BlendMode.DstOver
    SkiaBlendMode.SRC_IN -> BlendMode.SrcIn
    SkiaBlendMode.DST_IN -> BlendMode.DstIn
    SkiaBlendMode.SRC_OUT -> BlendMode.SrcOut
    SkiaBlendMode.DST_OUT -> BlendMode.DstOut
    SkiaBlendMode.SRC_ATOP -> BlendMode.SrcAtop
    SkiaBlendMode.DST_ATOP -> BlendMode.DstAtop
    SkiaBlendMode.XOR -> BlendMode.Xor
    SkiaBlendMode.PLUS -> BlendMode.Plus
    SkiaBlendMode.MODULATE -> BlendMode.Modulate
    SkiaBlendMode.SCREEN -> BlendMode.Screen
    SkiaBlendMode.OVERLAY -> BlendMode.Overlay
    SkiaBlendMode.DARKEN -> BlendMode.Color
    SkiaBlendMode.LIGHTEN -> BlendMode.Color
    SkiaBlendMode.COLOR_DODGE -> BlendMode.ColorDodge
    SkiaBlendMode.COLOR_BURN -> BlendMode.ColorBurn
    SkiaBlendMode.HARD_LIGHT -> BlendMode.Hardlight
    SkiaBlendMode.SOFT_LIGHT -> BlendMode.Softlight
    SkiaBlendMode.DIFFERENCE -> BlendMode.Difference
    SkiaBlendMode.EXCLUSION -> BlendMode.Exclusion
    SkiaBlendMode.MULTIPLY -> BlendMode.Multiply
    SkiaBlendMode.HUE -> BlendMode.Hue
    SkiaBlendMode.SATURATION -> BlendMode.Saturation
    SkiaBlendMode.COLOR -> BlendMode.Color
    SkiaBlendMode.LUMINOSITY -> BlendMode.Luminosity
}

fun Matrix.asSkiaMatrix33(): Matrix33 {
    val v = this.values
    return Matrix33(
        v[0], v[4], v[12], // Skia Row0: ScaleX, SkewX, TransX
        v[1], v[5], v[13], // Skia Row1: SkewY, ScaleY, TransY
        v[3], v[7], v[15]  // Skia Row2: Persp0, Persp1, Persp2
    )
}

fun Matrix.asSkiaMatrix44(): Matrix44 {
    val v = this.values
    return Matrix44(
        v[0], v[4], v[8],  v[12], // Skia Row 0
        v[1], v[5], v[9],  v[13], // Skia Row 1
        v[2], v[6], v[10], v[14], // Skia Row 2
        v[3], v[7], v[11], v[15]  // Skia Row 3
    )
}

fun Matrix33.asComposeMatrix(): Matrix {
    val s = this.mat
    return Matrix(floatArrayOf(
        s[0], s[3], 0f, s[6], // Col 0: ScaleX, SkewY, 0, Persp0
        s[1], s[4], 0f, s[7], // Col 1: SkewX, ScaleY, 0, Persp1
        0f,   0f,   1f, 0f,   // Col 2: 0, 0, 1, 0 (Identity Z)
        s[2], s[5], 0f, s[8]  // Col 3: TransX, TransY, 0, Persp2
    ))
}

fun Matrix44.asComposeMatrix(): Matrix {
    val s = this.mat
    return Matrix(floatArrayOf(
        s[0], s[4], s[8],  s[12], // Compose Col 0
        s[1], s[5], s[9],  s[13], // Compose Col 1
        s[2], s[6], s[10], s[14], // Compose Col 2
        s[3], s[7], s[11], s[15]  // Compose Col 3
    ))
}