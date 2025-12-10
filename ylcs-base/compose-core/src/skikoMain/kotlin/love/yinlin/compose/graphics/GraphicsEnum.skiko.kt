package love.yinlin.compose.graphics

import androidx.compose.ui.graphics.BlendMode

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