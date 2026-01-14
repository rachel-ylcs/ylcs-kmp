package love.yinlin.compose.ui.floating

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import love.yinlin.compose.Device
import love.yinlin.compose.LocalAnimationSpeed
import love.yinlin.compose.LocalDevice
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.layout.NavigationBack
import love.yinlin.compose.ui.node.clickableNoRipple

@OptIn(ExperimentalComposeUiApi::class)
@Stable
abstract class Floating<A : Any> {
    companion object {
        const val Z_INDEX_COMMON = 5f
        const val Z_INDEX_FAB = 8f
        const val Z_INDEX_SHEET = 10f
        const val Z_INDEX_DIALOG = 20f
        const val Z_INDEX_TIP = 30f
    }

    protected abstract fun alignment(device: Device): Alignment // 对齐方式
    protected abstract fun enter(device: Device, animationSpeed: Int): EnterTransition // 开始动画
    protected abstract fun exit(device: Device, animationSpeed: Int): ExitTransition // 结束动画
    protected open val scrim: Float = 0.4f // 遮罩透明度
    protected open val zIndex: Float = Z_INDEX_COMMON // 高度
    protected open val dismissOnBackPress: Boolean = true // 返回键结束
    protected open val dismissOnClickOutside: Boolean = true // 点击遮罩结束

    private var state: A? by mutableRefStateOf(null)
    private var visible: Boolean by mutableStateOf(false)
    val isOpen: Boolean by derivedStateOf { state != null }
    fun open(args: A) { state = args }
    open fun close() { visible = false }

    private fun hide() {
        state = null
        visible = false
    }

    protected open suspend fun initialize(args: A) {}

    @Composable
    protected open fun Wrapper(block: @Composable () -> Unit) = block()

    @Composable
    protected fun AnimatedFloatingContent(
        visible: Boolean,
        content: @Composable () -> Unit
    ) {
        val device = LocalDevice.current
        val animationSpeed = LocalAnimationSpeed.current

        val alpha by animateFloatAsState(
            targetValue = if (visible) (1 - scrim) else 0f,
            animationSpec = tween(
                durationMillis = animationSpeed,
                easing = LinearOutSlowInEasing
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = alpha))
                .zIndex(zIndex)
                .clickableNoRipple {
                    if (dismissOnClickOutside) close()
                },
            contentAlignment = remember(device) { alignment(device) }
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = remember(device) { enter(device, animationSpeed) },
                exit = remember(device) { exit(device, animationSpeed) }
            ) {
                Box(modifier = Modifier.clickableNoRipple { }) {
                    content()
                }
            }
        }
    }

    @Composable
    fun Land(block: @Composable (args: A) -> Unit) {
        state?.let { args ->
            val animationSpeed = LocalAnimationSpeed.current

            LaunchedEffect(Unit) { visible = true }

            LaunchedEffect(visible) {
                if (!visible) {
                    delay(animationSpeed.toLong())
                    hide()
                }
            }

            NavigationBack {
                if (dismissOnBackPress) close()
            }

            AnimatedFloatingContent(visible = visible) {
                 Wrapper {
                    block(args)
                }

                LaunchedEffect(Unit) {
                    initialize(args)
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose { hide() }
        }
    }
}