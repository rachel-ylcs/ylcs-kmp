package love.yinlin.compose.ui.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.ui.animation.CircleLoading
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.extension.catching

@Stable
class PullState(
    private val scope: CoroutineScope,
    private val canRefresh: State<Boolean>,
    private val canLoading: State<Boolean>,
    private val onRefresh: State<(suspend () -> Unit)?>,
    private val onLoading: State<(suspend () -> Unit)?>,
    indicatorPadding: Float,
    private val thresholdRatio: Float,
) {
    private val maxDistance = indicatorPadding * 6
    private val threshold = maxDistance * thresholdRatio
    private val friction = 0.5f // 拖拽阻力感

    private var isPointerPressed by mutableStateOf(false) // 阻止电脑端滚动溢出
    internal var isProcessing by mutableStateOf(false)
    private val dragOffset = Animatable(0f)

    fun calcFraction(isTop: Boolean): Float {
        val offset = dragOffset.value
        val absOffset = (if (isTop) offset else -offset).coerceAtLeast(0f)
        return (absOffset / maxDistance).coerceIn(0f, 1f)
    }

    fun calcLerpColor(fraction: Float, color1: Color, color2: Color) = when {
        fraction >= 1f -> color2
        fraction >= thresholdRatio -> lerp(color1, color2, (fraction - thresholdRatio) / (1f - thresholdRatio))
        else -> lerp(Colors.Transparent, color1, fraction / thresholdRatio)
    }

    private fun sendResult(): Boolean {
        val current = dragOffset.value
        return if (current != 0f && !isProcessing) {
            scope.launch {
                isProcessing = true

                catching {
                    // 检查是否达到阈值
                    if (current >= threshold) {
                        dragOffset.animateTo(threshold, tween(300))
                        onRefresh.value?.invoke()
                    }
                    else if (current <= -threshold) {
                        dragOffset.animateTo(-threshold, tween(300))
                        onLoading.value?.invoke()
                    }
                }

                // 无论是否触发回调，都通过动画平滑恢复到原位
                isProcessing = false
                dragOffset.animateTo(0f, animationSpec = tween(durationMillis = 300))
            }
            true
        }
        else false
    }

    internal val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // 如果在刷新或加载中，直接消耗掉所有的 Y 轴滑动事件，彻底锁死内部容器
            if (isProcessing) return Offset(0f, available.y)

            val current = dragOffset.value

            // 如果当前处于下拉状态，但用户向上滑动，优先消耗事件来回退拖拽距离
            if (current > 0f && available.y < 0f) {
                val newOffset = (current + available.y).coerceAtLeast(0f)
                scope.launch { dragOffset.snapTo(newOffset) }
                return Offset(0f, newOffset - current)
            }

            // 如果当前处于上拉状态，但用户向下滑动，优先消耗事件来回退拖拽距离
            if (current < 0f && available.y > 0f) {
                val newOffset = (current + available.y).coerceAtMost(0f)
                scope.launch { dragOffset.snapTo(newOffset) }
                return Offset(0f, newOffset - current)
            }

            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (isProcessing || !isPointerPressed) return Offset.Zero

            // 忽略 Fling 惯性滑动带来的越界
            if (source == NestedScrollSource.UserInput) {
                val current = dragOffset.value

                // 下拉刷新
                if (available.y > 0f && canRefresh.value) {
                    val newOffset = (current + available.y * friction).coerceAtMost(maxDistance)
                    scope.launch { dragOffset.snapTo(newOffset) }
                    return Offset(0f, (newOffset - current) / friction)
                }

                // 上拉加载
                if (available.y < 0f && canLoading.value) {
                    val newOffset = (current + available.y * friction).coerceAtLeast(-maxDistance)
                    scope.launch { dragOffset.snapTo(newOffset) }
                    return Offset(0f, (newOffset - current) / friction)
                }
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity = if (isProcessing || sendResult()) Velocity(0f, available.y) else Velocity.Zero
    }

    // 单独处理电脑端拖拽逻辑

    suspend fun PointerInputScope.fixOverflowScroll() {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                isPointerPressed = event.changes.any { it.pressed }
            }
        }
    }

    suspend fun PointerInputScope.awaitDrag() {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            // 如果不是鼠标直接放行，让嵌套滚动去处理触屏拖拽
            if (down.type != PointerType.Mouse) return@awaitEachGesture

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull() ?: break

                if (change.changedToUp()) {
                    sendResult()
                    break // 结束当前拖拽手势
                }

                // 鼠标按住拖拽中
                if (change.pressed && !isProcessing) {
                    val delta = change.position.y - change.previousPosition.y
                    val current = dragOffset.value

                    if (delta != 0f) {
                        // 模拟与 NestedScroll 完全一致的阻力与极值限制
                        val newOffset = when {
                            delta > 0f && canRefresh.value && current >= 0f -> (current + delta * friction).coerceAtMost(maxDistance)
                            delta < 0f && canLoading.value && current <= 0f -> (current + delta * friction).coerceAtLeast(-maxDistance)
                            current > 0f && delta < 0f -> (current + delta).coerceAtLeast(0f)
                            current < 0f && delta > 0f -> (current + delta).coerceAtMost(0f)
                            else -> null
                        }

                        if (newOffset != null && newOffset != current) {
                            scope.launch { dragOffset.snapTo(newOffset) }
                            change.consume()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PullIndicator(
    pullState: PullState,
    isTop: Boolean,
    indicatorPadding: Dp ,
    modifier: Modifier
) {
    val fraction by rememberDerivedState(isTop) { pullState.calcFraction(isTop) }
    val color1 = Theme.color.primaryContainer
    val color2 = Theme.color.secondaryContainer

    Box(
        modifier = modifier
            .size(Theme.size.input8)
            .offset {
                IntOffset(x = 0, y = if (fraction > 0f) {
                    val travel = (indicatorPadding.toPx() * (1 + fraction)).toInt()
                    if (isTop) travel else -travel
                } else 0)
            }
            .drawWithContent {
                if (fraction > 0f) {
                    scale(1 + fraction / 4) {
                        val radius = size.width / 2
                        drawCircle(color = pullState.calcLerpColor(fraction, color1, color2), radius = radius)
                        drawCircle(color = Colors.Gray5, radius = radius, style = Stroke(width = radius / 16f), alpha = fraction)
                        this@drawWithContent.drawContent()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        ThemeContainer(Theme.color.onContainer.copy(alpha = fraction)) {
            if (pullState.isProcessing) CircleLoading.Content(modifier = Modifier.fillMaxSize(fraction = 0.7f))
            else Icon(icon = if (isTop) Icons.KeyboardArrowDown else Icons.KeyboardArrowUp, modifier = Modifier.fillMaxSize(fraction = 0.8f))
        }
    }
}

@Composable
internal fun PullLayout(
    canContainerRefresh: () -> Boolean,
    canContainerLoading: () -> Boolean,
    canRefresh: Boolean,
    canLoading: Boolean,
    onRefresh: (suspend () -> Unit)?,
    onLoading: (suspend () -> Unit)?,
    modifier: Modifier = Modifier,
    indicatorPadding: Dp = Theme.padding.v4,
    thresholdRatio: Float = 0.75f,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val canContainerRefreshUpdate by rememberUpdatedState(canContainerRefresh)
    val canContainerLoadingUpdate by rememberUpdatedState(canContainerLoading)
    val canRefreshUpdate = rememberDerivedState { canRefresh && canContainerRefreshUpdate() }
    val canLoadingUpdate = rememberDerivedState { canLoading && canContainerLoadingUpdate() }
    val onRefreshUpdate = rememberUpdatedState(onRefresh)
    val onLoadingUpdate = rememberUpdatedState(onLoading)
    val pullState = remember(density, scope, indicatorPadding, thresholdRatio) {
        PullState(
            scope = scope,
            canRefresh = canRefreshUpdate,
            canLoading = canLoadingUpdate,
            onRefresh = onRefreshUpdate,
            onLoading = onLoadingUpdate,
            indicatorPadding = with(density) { indicatorPadding.toPx() },
            thresholdRatio = thresholdRatio
        )
    }

    val pullModifier = Modifier.pointerInput(Unit) {
        with(pullState) { fixOverflowScroll() }
    }.nestedScroll(pullState.connection).pointerInput(pullState) {
        with(pullState) { awaitDrag() }
    }.clipToBounds() // 确保指示器不会绘制到容器外部

    Box(modifier = modifier then pullModifier) {
        Box(modifier = Modifier.zIndex(1f), content = content)
        if (canRefresh) {
            PullIndicator(
                pullState = pullState,
                isTop = true,
                indicatorPadding = indicatorPadding,
                modifier = Modifier.align(Alignment.TopCenter).zIndex(2f)
            )
        }
        if (canLoading) {
            PullIndicator(
                pullState = pullState,
                isTop = false,
                indicatorPadding = indicatorPadding,
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(2f)
            )
        }
    }
}