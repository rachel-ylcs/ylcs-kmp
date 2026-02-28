package love.yinlin.compose.ui.container

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import love.yinlin.compose.LocalColor
import love.yinlin.compose.Theme
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.rememberOffScreenState
import love.yinlin.compose.ui.animation.AnimationVisibility
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.keepSize
import love.yinlin.extension.catching

@Composable
private fun BannerArrow(
    visible: Boolean,
    icon: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    AnimationVisibility(visible = visible, modifier = modifier) {
        SurfaceContainer {
            val shape = Theme.shape.circle

            Box(
                modifier = Modifier
                    .border(Theme.border.v10, LocalColor.current.copy(alpha = 0.25f), shape)
                    .clip(shape)
                    .background(Theme.color.surface.copy(alpha = 0.75f))
                    .clickable(onClick = onClick)
                    .padding(Theme.padding.e),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon = icon)
            }
        }
    }
}

@Composable
private fun BannerIndicator(
    state: PagerState,
    size: Int,
    indicatorSize: Dp,
    modifier: Modifier,
    onClick: (Int) -> Unit
) {
    if (indicatorSize != Dp.Unspecified && indicatorSize != 0.dp) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.g4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val actualIndex = state.currentPage % size
            repeat(size) { index ->
                val isCurrent = actualIndex == index
                val radius by animateDpAsState(
                    targetValue = if (isCurrent) indicatorSize else indicatorSize * 0.75f,
                    animationSpec = tween(Theme.animation.duration.default)
                )

                val shape = Theme.shape.circle
                val borderColor = if (isCurrent) Theme.color.onContainer else Theme.color.onSurface
                val backgroundColor = if (isCurrent) Theme.color.primaryContainer else Theme.color.surface

                Box(modifier = Modifier.size(radius)
                    .border(Theme.border.v10, borderColor.copy(alpha = 0.25f), shape)
                    .clip(shape)
                    .background(backgroundColor.copy(alpha = 0.9f))
                    .clickable(enabled = !isCurrent) { onClick(index) }
                )
            }
        }
    }
}

@Composable
fun Banner(
    size: Int,
    modifier: Modifier = Modifier,
    initIndex: Int = 0,
    interval: Long = 0L,
    indicatorSize: Dp = Theme.size.box2,
    content: @Composable (Int) -> Unit
) {
    val initialPage = if (size > 0) {
        val halfWay = Int.MAX_VALUE / 2
        (halfWay - (halfWay % size)) + initIndex
    } else 0

    val state = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }
    val scope = rememberCoroutineScope()

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isForeground = rememberOffScreenState()

    Box(modifier = modifier.hoverable(interactionSource)) {
        if (size > 0) {
            // Content
            HorizontalPager(
                state = state,
                beyondViewportPageCount = 1,
                modifier = Modifier.keepSize().matchParentSize().zIndex(1f)
            ) { virtualIndex ->
                content(virtualIndex % size)
            }

            if (size > 1) {
                // Indicator
                BannerIndicator(
                    state = state,
                    size = size,
                    indicatorSize = indicatorSize,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(Theme.padding.e).zIndex(2f),
                    onClick = { actualIndex ->
                        val currentMainPage = state.currentPage
                        val currentOffset = currentMainPage % size
                        val virtualIndex = currentMainPage + (actualIndex - currentOffset)
                        scope.launch { state.animateScrollToPage(virtualIndex) }
                    }
                )

                // Switcher
                BannerArrow(
                    visible = isHovered,
                    icon = Icons.KeyboardArrowLeft,
                    modifier = Modifier.padding(start = Theme.padding.e).align(Alignment.CenterStart).zIndex(3f),
                    onClick = {
                        scope.launch { state.animateScrollToPage(state.currentPage - 1) }
                    }
                )
                BannerArrow(
                    visible = isHovered,
                    icon = Icons.KeyboardArrowRight,
                    modifier = Modifier.padding(end = Theme.padding.e).align(Alignment.CenterEnd).zIndex(3f),
                    onClick = {
                        scope.launch { state.animateScrollToPage(state.currentPage + 1) }
                    }
                )
            }
        }
    }

    val autoPlay by rememberDerivedState(size, interval, isForeground) {
        size > 1 && interval > 0 && !isDragged && !isHovered && isForeground
    }

    LaunchedEffect(state, interval, autoPlay) {
        if (autoPlay) {
            while (true) {
                delay(interval)
                catching { state.animateScrollToPage(state.currentPage + 1) }
            }
        }
    }
}