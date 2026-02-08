package love.yinlin.compose.ui.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
internal class SwipeState {
    var isReleaseEdge = false
    var refreshStatus by mutableStateOf(PaginationStatus.IDLE)
    var loadingStatus by mutableStateOf(PaginationStatus.IDLE)
    var isAnimateOver by mutableStateOf(true)
    val isRunning: Boolean get() = !isAnimateOver || refreshStatus == PaginationStatus.RUNNING || loadingStatus == PaginationStatus.RUNNING

    private val mutatorMutex = MutatorMutex()
    // TODO: 等 kotlin 2.3 稳定后用 backend field替换
    private val _indicatorOffset = Animatable(0f)
    val indicatorOffset: Float get() = _indicatorOffset.value

    suspend fun animateOffsetTo(offset: Float) = mutatorMutex.mutate {
        _indicatorOffset.animateTo(offset) {
            if (this.value == 0f) isAnimateOver = true
        }
    }

    suspend fun snapOffsetTo(headerHeightPx: Float, footerHeightPx: Float, offset: Float) = mutatorMutex.mutate(MutatePriority.UserInput) {
        _indicatorOffset.snapTo(offset)
        if (indicatorOffset >= headerHeightPx) refreshStatus = PaginationStatus.RELEASE
        else if (indicatorOffset <= -footerHeightPx) loadingStatus = PaginationStatus.RELEASE
        else {
            if (indicatorOffset > 0) refreshStatus = PaginationStatus.PULL
            if (indicatorOffset < 0) loadingStatus = PaginationStatus.PULL
        }
    }
}