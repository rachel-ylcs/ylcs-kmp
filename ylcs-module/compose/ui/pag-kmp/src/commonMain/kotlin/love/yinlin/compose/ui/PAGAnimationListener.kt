package love.yinlin.compose.ui

import androidx.compose.runtime.Stable

@Stable
interface PAGAnimationListener {
    fun onAnimationStart() { }
    fun onAnimationEnd() { }
    fun onAnimationCancel() { }
    fun onAnimationRepeat() { }
}