package love.yinlin.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import love.yinlin.app
import love.yinlin.app.world.resources.Res
import love.yinlin.app.world.resources.card_concert_memory
import love.yinlin.compose.Colors
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.extension.rememberDerivedState
import love.yinlin.compose.extension.rememberFalse
import love.yinlin.compose.interaction.collectState
import love.yinlin.compose.screen.BasicScreen
import love.yinlin.data.common.WorldActivity
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.image.Image
import love.yinlin.compose.ui.node.pointerIcon
import love.yinlin.compose.ui.text.SimpleEllipsisText
import love.yinlin.extension.cleaning

@Stable
class ScreenWorld : BasicScreen() {
    private val activities: List<WorldActivity> = [
        WorldActivity(
            id = "concert-memory",
            title = "演出记忆",
            subtitle = "收藏每一次现场，让听过的歌有迹可循。",
            cover = Res.drawable.card_concert_memory,
            keywords = ["记录现场", "听歌图鉴", "专属足迹"],
            onClick = {
                if (app.config.userProfile != null) navigate(::ScreenConcert)
                else slot.tip.warning("请先登录")
            },
        ),
    ]

    private val scrimBrush = Brush.verticalGradient(
        0f to Colors.Black.copy(alpha = 0.2f),
        0.3f to Colors.Black.copy(alpha = 0.24f),
        0.6f to Colors.Black.copy(alpha = 0.72f),
        1f to Colors.Black.copy(alpha = 0.94f),
    )

    @Composable
    fun WorldActivityCard(
        activity: WorldActivity,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        isVisible: Boolean = true,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val interaction by interactionSource.collectState(useDragged = false)
        var pointerPressed by rememberFalse()
        val pressed = pointerPressed || interaction.isPressed
        val highlighted = pressed || interaction.isHovered || interaction.isFocused
        val interactionScale by animateFloatAsState(
            targetValue = if (highlighted) 1.035f else 1f,
            animationSpec = tween(if (pressed) Theme.animation.duration.v10 else Theme.animation.duration.v9),
        )
        val borderColor by animateColorAsState(
            targetValue = if (highlighted) Theme.color.primary else Theme.color.outline.copy(alpha = 0.5f),
            animationSpec = tween(Theme.animation.duration.v10),
        )
        var hasAppeared by rememberFalse(activity.id)
        val appearanceScale = remember(activity.id) { Animatable(if (hasAppeared) 1f else 1.1f) }
        val appearanceDuration = Theme.animation.duration.v3
        val shape = Theme.shape.v3

        LaunchedEffect(activity.id, isVisible) {
            if (hasAppeared) appearanceScale.snapTo(1f)
            else if (isVisible) {
                hasAppeared = true
                appearanceScale.animateTo(1f, tween(appearanceDuration, easing = LinearOutSlowInEasing))
            }
        }

        Box(
            modifier = modifier
                .clip(shape)
                .background(Theme.color.surface)
                .border(Theme.border.v5, borderColor, shape)
                .pointerIcon(PointerIcon.Hand)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        pointerPressed = true
                        cleaning({ pointerPressed = false }) {
                            waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        }
                    }
                }.clickable(interactionSource = interactionSource, onClick = onClick)
        ) {
            Image(
                res = activity.cover,
                modifier = Modifier.matchParentSize().graphicsLayer {
                    val scale = maxOf(appearanceScale.value, interactionScale)
                    scaleX = scale
                    scaleY = scale
                }.zIndex(1f),
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop
            )

            Box(modifier = Modifier.matchParentSize().background(scrimBrush).zIndex(2f))

            Column(
                modifier = Modifier.fillMaxSize().padding(Theme.padding.eValue7).zIndex(3f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h)
                ) {
                    for (keyword in activity.keywords) {
                        SimpleEllipsisText(
                            text = keyword,
                            color = Colors.White,
                            style = Theme.typography.v9.bold,
                            modifier = Modifier.weight(1f, fill = false)
                                .background(Colors.Black.copy(alpha = 0.6f), Theme.shape.circle)
                                .padding(Theme.padding.value)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Theme.padding.h),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Theme.padding.v),
                    ) {
                        SimpleEllipsisText(
                            text = activity.title,
                            color = Colors.White,
                            style = Theme.typography.v4.bold
                        )
                        SimpleEllipsisText(
                            text = activity.subtitle,
                            color = Colors.White.copy(alpha = 0.92f),
                            style = Theme.typography.v8
                        )
                    }
                    Icon(icon = Icons.KeyboardArrowRight, color = Colors.White)
                }
            }
        }
    }

    @Composable
    override fun BasicContent() {
        val gridState = rememberLazyGridState()
        val visibleActivities: Set<String> by rememberDerivedState { gridState.layoutInfo.visibleItemsInfo.mapTo(mutableSetOf()) { it.key as String } }

        LazyVerticalGrid(
            modifier = Modifier.padding(LocalImmersivePadding.current).fillMaxSize(),
            columns = GridCells.Adaptive(Theme.size.cell1),
            state = gridState,
            contentPadding = Theme.padding.eValue7,
            horizontalArrangement = Arrangement.spacedBy(Theme.padding.e7),
            verticalArrangement = Arrangement.spacedBy(Theme.padding.e7),
        ) {
            items(items = activities, key = { it.id }) { activity ->
                WorldActivityCard(
                    activity = activity,
                    onClick = activity.onClick,
                    isVisible = activity.id in visibleActivities,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f)
                )
            }
        }
    }
}
