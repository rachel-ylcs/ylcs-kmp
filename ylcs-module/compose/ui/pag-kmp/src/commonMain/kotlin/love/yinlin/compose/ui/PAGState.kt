package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
expect class PAGState(
    initComposition: PAGSourceComposition? = null,
    initIsPlaying: Boolean = false,
    initProgress: Double = 0.0,
    listener: PAGAnimationListener? = null,
)