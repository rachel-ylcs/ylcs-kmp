package love.yinlin.compose.ui.floating

import androidx.annotation.CallSuper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import love.yinlin.compose.Device
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.node.silentClick
import love.yinlin.compose.ui.tool.NavigationBack

@Stable
abstract class Floating<A : Any> {
    companion object {
        const val Z_INDEX_COMMON = 5f
        const val Z_INDEX_FAB = 8f
        const val Z_INDEX_SHEET = 10f
        const val Z_INDEX_DIALOG = 20f
        const val Z_INDEX_TIP = 30f
    }

    /**
     * 对齐方式
     */
    protected abstract fun alignment(device: Device): Alignment
    /**
     * 开始动画
     */
    protected abstract fun enter(device: Device, animationSpeed: Int): EnterTransition
    /**
     * 结束动画
     */
    protected abstract fun exit(device: Device, animationSpeed: Int): ExitTransition

    /**
     * 遮罩透明度
     */
    protected open val scrimAlpha: Float = 0.6f

    /**
     * 层级
     */
    protected open val zIndex: Float = Z_INDEX_COMMON

    /**
     * 响应返回键
     */
    protected open val useBack: Boolean = true

    /**
     * back 关闭
     */
    protected open val dismissOnBackPress: Boolean = true

    /**
     * 点击遮罩关闭
     */
    protected open val dismissOnClickOutside: Boolean = true

    /**
     * 显示遮罩
     */
    protected open val showScrim: Boolean = true

    /**
     * 初始化
     *
     * 仅在打开时执行一次
     */
    protected open suspend fun initialize(args: A) {}

    private var currentArgs: A? by mutableRefStateOf(null)

    /**
     * 是否打开
     */
    var isOpen: Boolean by mutableStateOf(false)
        private set

    /**
     * 打开
     */
    protected fun openFloating(args: A) {
        currentArgs = args
        isOpen = true
    }

    /**
     * 关闭
     */
    @CallSuper
    open fun close() { isOpen = false }

    @Composable
    protected fun LandFloating(block: @Composable (args: A) -> Unit) {
        val animationSpeed = Theme.animation.duration.default
        val device = LocalDevice.current

        val transition = updateTransition(targetState = isOpen)
        if (transition.currentState || transition.targetState) {
            Box(modifier = Modifier.fillMaxSize().zIndex(zIndex)) {
                if (showScrim) {
                    val alpha by transition.animateFloat(
                        transitionSpec = { tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing) },
                        targetValueByState = { if (it) scrimAlpha else 0f }
                    )

                    Box(modifier = Modifier.fillMaxSize().background(Theme.color.scrim.copy(alpha = alpha)).silentClick {
                        if (dismissOnClickOutside) close()
                    }.zIndex(1f))
                }

                transition.AnimatedVisibility(
                    visible = { it },
                    enter = enter(device, animationSpeed),
                    exit = exit(device, animationSpeed),
                    modifier = Modifier.align(alignment(device)).zIndex(2f)
                ) {
                    currentArgs?.let { args ->
                        block(args)

                        LaunchedEffect(Unit) {
                            initialize(args)
                        }
                    }
                }
            }
        }

        LaunchedEffect(transition.currentState) {
            if (!transition.currentState && !transition.targetState) currentArgs = null
        }

        NavigationBack(enabled = useBack && isOpen) {
            if (dismissOnBackPress) close()
        }

        DisposableEffect(Unit) {
            onDispose(::close)
        }
    }
}