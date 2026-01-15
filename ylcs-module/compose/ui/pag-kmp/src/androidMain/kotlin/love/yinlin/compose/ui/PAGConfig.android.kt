package love.yinlin.compose.ui

import org.libpag.PAGScaleMode

internal val PAGConfig.ScaleMode.asPAGScaleMode: Int get() = when (this) {
    PAGConfig.ScaleMode.None -> PAGScaleMode.None
    PAGConfig.ScaleMode.Stretch -> PAGScaleMode.Stretch
    PAGConfig.ScaleMode.LetterBox -> PAGScaleMode.LetterBox
    PAGConfig.ScaleMode.Zoom -> PAGScaleMode.Zoom
}