package love.yinlin.ui.component.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import love.yinlin.compose.*
import love.yinlin.platform.app
import love.yinlin.ui.component.image.MiniIcon
import love.yinlin.ui.component.node.clickableNoRipple
import love.yinlin.ui.component.node.condition

@Stable
data class FABAction(
    val icon: ImageVector,
    val tip: String? = null,
    val onClick: () -> Unit = {}
)

@Composable
private fun FABIcon(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(CustomTheme.size.fab)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        MiniIcon(
            icon = icon,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun FABIcon(
    icon: ImageVector,
    tip: String,
    onClick: () -> Unit
) {
    BallonTip(text = tip) {
        Box(
            modifier = Modifier
                .size(CustomTheme.size.fab)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            MiniIcon(
                icon = icon,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun FABMainIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: suspend () -> Unit
) {
    Box(modifier = modifier) {
        Box(modifier = Modifier.padding(bottom = CustomTheme.shadow.icon).shadow(CustomTheme.shadow.icon, CircleShape)) {
            val scope = rememberCoroutineScope()

            FABIcon(icon = icon) {
                scope.launch { onClick() }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FABLayout(
    icon: ImageVector,
    canExpand: Boolean,
    onClick: suspend () -> Unit,
    menus: Array<FABAction>
) {
    Box(
        modifier = Modifier
            .padding(LocalImmersivePadding.current)
            .fillMaxSize()
            .zIndex(Floating.Z_INDEX_FAB),
        contentAlignment = Alignment.BottomEnd
    ) {
        if (menus.isEmpty()) {
            FABMainIcon(
                icon = icon,
                modifier = Modifier.padding(CustomTheme.padding.fabSpace),
                onClick = onClick
            )
        }
        else {
            var expanded by rememberFalse()
            val duration = app.config.animationSpeed
            val alpha by animateFloatAsState(
                targetValue = if (expanded) 0.6f else 0f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = LinearOutSlowInEasing
                )
            )

            BackHandler(enabled = expanded) { expanded = false }

            DisposableEffect(Unit) {
                onDispose { expanded = false }
            }

            Box(modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = alpha))
                .condition(expanded) { clickableNoRipple { expanded = false } },
                contentAlignment = Alignment.BottomEnd
            ) {
                Column(
                    modifier = Modifier.padding(CustomTheme.padding.fabSpace).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CustomTheme.padding.fabSpace / 2)
                ) {
                    menus.forEachIndexed { index, action ->
                        val delay = (menus.size - 1 - index) * duration / menus.size
                        AnimatedVisibility(
                            visible = expanded,
                            enter = fadeIn(tween(delayMillis = delay)) +
                                    slideInVertically(tween(delayMillis = delay, durationMillis = duration - delay)) { it },
                            exit = fadeOut(tween(duration)) +
                                    slideOutHorizontally(tween(durationMillis = duration))
                        ) {
                            action.tip?.let { tip ->
                                FABIcon(icon = action.icon, tip = tip) {
                                    expanded = false
                                    action.onClick()
                                }
                            } ?: FABIcon(icon = action.icon) {
                                expanded = false
                                action.onClick()
                            }
                        }
                    }

                    FABMainIcon(icon = icon) {
                        if (canExpand) expanded = !expanded
                        else onClick()
                    }
                }
            }
        }
    }
}