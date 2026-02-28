package love.yinlin.compose.ui.floating

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import love.yinlin.compose.Device
import love.yinlin.compose.Theme
import love.yinlin.compose.ui.container.Surface
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.SyncFuture
import love.yinlin.extension.catchingNull

@Stable
abstract class Dialog<R : Any> : Floating<Unit>() {
    override fun alignment(device: Device): Alignment = Alignment.Center

    override fun enter(device: Device, animationSpeed: Int): EnterTransition = scaleIn(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing),
        initialScale = 0.0001f // 什么鬼bug, 反正不能为0, 否则对话框弹出时静止移动会失去焦点无法点击
    ) + fadeIn(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing)
    )

    override fun exit(device: Device, animationSpeed: Int): ExitTransition = scaleOut(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing)
    )

    override val zIndex: Float = Z_INDEX_DIALOG

    protected var future: SyncFuture<R>? = null

    override fun close() {
        future?.send()
        future = null
        super.close()
    }

    protected suspend fun awaitResult(): R? {
        future?.cancel()
        future = null
        val result = catchingNull {
            Coroutines.sync {
                future = it
                openFloating(Unit)
            }
        }
        close()
        return result
    }

    protected suspend fun <D : Dialog<R>> awaitResult(self: D, block: suspend D.() -> R): R? {
        future?.cancel()
        future = null
        // 结构化并发
        val result = catchingNull {
            coroutineScope {
                // 回调监听任务
                val syncTask = async {
                    Coroutines.sync {
                        future = it
                        openFloating(Unit)
                    }
                }

                // 自定义任务
                val blockTask = async {
                    self.block()
                }

                select {
                    // 回调监听任务结束必须立即终止自定义任务, 例如点击对话框退出按钮, 离开Composable作用域等
                    syncTask.onAwait {
                        blockTask.cancel()
                        it
                    }

                    // 自定义任务结束触发SyncFuture.send, 等待回调监听
                    blockTask.onAwait { result ->
                        future?.send(result)?.let { syncTask.await() }
                    }
                }
            }
        }
        close()
        return result
    }

    @Composable
    protected fun LandDialog(block: @Composable () -> Unit) {
        LandFloating {
            Surface(
                modifier = Modifier.padding(Theme.padding.eValue9),
                shadowElevation = Theme.shadow.v1,
                shape = Theme.shape.v1,
                border = BorderStroke(Theme.border.v10, Theme.color.outline)
            ) {
                block()
            }
        }
    }

    @Composable
    abstract fun Land()
}