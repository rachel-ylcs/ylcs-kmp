package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
expect open class PAGState(
    initSource: PAGSource? = null,
    initIsPlaying: Boolean = false,
    initProgress: Double = 0.0,
    initRepeatCount: Int = PAGConfig.INFINITY,
    initScaleMode: PAGConfig.ScaleMode = PAGConfig.ScaleMode.LetterBox,
    listener: PAGAnimationListener = PAGAnimationListener.Default,
)