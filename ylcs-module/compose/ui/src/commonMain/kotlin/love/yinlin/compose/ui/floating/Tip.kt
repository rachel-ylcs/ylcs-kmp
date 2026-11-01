package love.yinlin.compose.ui.floating

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import love.yinlin.compose.Device
import love.yinlin.compose.LocalImmersivePadding
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.animation.AnimationContent
import love.yinlin.compose.ui.container.ThemeContainer
import love.yinlin.compose.ui.icon.Icons
import love.yinlin.compose.ui.image.Icon
import love.yinlin.compose.ui.node.shadow
import love.yinlin.compose.ui.text.Text
import love.yinlin.coroutines.mainContext
import love.yinlin.extension.catching
import kotlin.time.Duration.Companion.milliseconds

@Stable
class Tip(private val scope: CoroutineScope) : Floating<Tip.Data>() {
    @Stable
    enum class Type {
        Info, Success, Warning, Error;
    }

    @Stable
    data class Data(val type: Type, val text: String)

    companion object {
        private const val DEFAULT_DURATION = 3000L
    }

    override val useBack: Boolean = false

    override val showScrim: Boolean = false

    override fun alignment(device: Device): Alignment = Alignment.TopCenter

    override fun enter(device: Device, animationDuration: Int): EnterTransition = fadeIn(
        animationSpec = tween(animationDuration, easing = FastOutSlowInEasing)
    ) + slideInVertically(
        initialOffsetY = { -it / 2 },
        animationSpec = tween(animationDuration, easing = FastOutSlowInEasing)
    ) + scaleIn(
        initialScale = 0.94f,
        animationSpec = tween(animationDuration, easing = FastOutSlowInEasing)
    )

    override fun exit(device: Device, animationDuration: Int): ExitTransition = fadeOut(
        animationSpec = tween(animationDuration / 2, easing = FastOutSlowInEasing)
    ) + slideOutVertically(
        targetOffsetY = { -it / 4 },
        animationSpec = tween(animationDuration / 2, easing = FastOutSlowInEasing)
    ) + scaleOut(
        targetScale = 0.98f,
        animationSpec = tween(animationDuration / 2, easing = FastOutSlowInEasing)
    )

    override val zIndex: Float = Z_INDEX_TIP

    private var job: Job? = null

    fun open(text: String?, type: Type, duration: Long = DEFAULT_DURATION) {
        job?.cancel()
        job = scope.launch(mainContext) {
            val self = coroutineContext[Job]
            openFloating(Data(type, text ?: ""))
            catching { delay(duration.milliseconds) }
            // 防止并发时旧 tip 将新 tip 给置 null
            if (job === self) {
                close()
                job = null
            }
        }
    }

    fun info(text: String?, duration: Long = DEFAULT_DURATION) = open(text, Type.Info, duration)
    fun success(text: String?, duration: Long = DEFAULT_DURATION) = open(text, Type.Success, duration)
    fun warning(text: String?, duration: Long = DEFAULT_DURATION) = open(text, Type.Warning, duration)
    fun error(text: String?, duration: Long = DEFAULT_DURATION) = open(text, Type.Error, duration)

    @Composable
    fun Land() {
        LandFloating { _, data ->
            val accent = when (data.type) {
                Type.Info -> Theme.color.primary
                Type.Success -> Theme.color.secondary
                Type.Warning -> Theme.color.warning
                Type.Error -> Theme.color.error
            }
            val surface = Theme.color.surface
            val shape = Theme.shape.v5
            val padding = LocalImmersivePadding.current +
                PaddingValues(horizontal = Theme.padding.h10, vertical = Theme.padding.v4)
            val duration = Theme.animation.duration.v7

            Box(
                modifier = Modifier
                    .padding(padding)
                    .defaultMinSize(minWidth = Theme.size.cell1)
                    .widthIn(max = 440.dp)
                    .shadow(shape, Theme.shadow.v7)
                    .clip(shape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accent.copy(alpha = 0.12f).compositeOver(surface),
                                accent.copy(alpha = 0.04f).compositeOver(surface),
                                surface
                            )
                        )
                    )
                    .border(1.dp, accent.copy(alpha = 0.28f), shape)
            ) {
                AnimationContent(
                    state = data,
                    enter = {
                        fadeIn(tween(duration, easing = FastOutSlowInEasing)) +
                            slideInVertically(
                                initialOffsetY = { -it / 4 },
                                animationSpec = tween(duration, easing = FastOutSlowInEasing)
                            )
                    },
                    exit = {
                        fadeOut(tween(duration / 2)) +
                            slideOutVertically(
                                targetOffsetY = { -it / 5 },
                                animationSpec = tween(duration / 2)
                            )
                    }
                ) { current ->
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(30.dp)
                                .clip(Theme.shape.circle)
                                .background(accent)
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(Theme.shape.circle)
                                .background(accent.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ThemeContainer(accent) {
                                Icon(icon = when (current.type) {
                                    Type.Info -> Icons.Lightbulb
                                    Type.Success -> Icons.Check
                                    Type.Warning -> Icons.Warning
                                    Type.Error -> Icons.Error
                                })
                            }
                        }
                        Text(
                            text = current.text,
                            color = Theme.color.onSurface,
                            style = Theme.typography.v6.bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}