package love.yinlin.compose.graphics

import androidx.compose.ui.graphics.BlendMode

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