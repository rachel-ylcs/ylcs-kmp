package love.yinlin.compose.ui.floating

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
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

    override fun enter(device: Device, animationSpeed: Int): EnterTransition = scaleIn(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing),
        initialScale = 0.0001f
    ) + fadeIn(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing)
    )

    override fun exit(device: Device, animationSpeed: Int): ExitTransition = scaleOut(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing)
    ) + fadeOut(
        animationSpec = tween(durationMillis = animationSpeed, easing = LinearOutSlowInEasing)
    )

    override val zIndex: Float = Z_INDEX_TIP

    private var job: Job? = null

    fun open(text: String?, type: Type, duration: Long = DEFAULT_DURATION) {
        job?.cancel()
        job = scope.launch(mainContext) {
            val self = coroutineContext[Job]
            openFloating(Data(type, text ?: ""))
            catching { delay(duration) }
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
        LandFloating { data ->
            val type = data.type
            val shape = Theme.shape.v4

            val backgroundColor by animateColorAsState(when (type) {
                Type.Info -> Theme.color.secondaryContainer
                Type.Success -> Theme.color.primaryContainer
                Type.Warning -> Theme.color.warning
                Type.Error -> Theme.color.error
            })

            val padding = LocalImmersivePadding.current + PaddingValues(horizontal = Theme.padding.h10, vertical = Theme.padding.v4)

            Box(modifier = Modifier
                .padding(padding)
                .defaultMinSize(minWidth = Theme.size.cell1)
                .shadow(shape, Theme.shadow.v7)
                .clip(shape)
                .background(backgroundColor)
            ) {
                val contentColor by animateColorAsState(when (type) {
                    Type.Info, Type.Success -> Theme.color.onContainer
                    Type.Warning -> Theme.color.onWarning
                    Type.Error -> Theme.color.onError
                })

                val duration = Theme.animation.duration.v7

                ThemeContainer(contentColor) {
                    AnimationContent(
                        state = data,
                        enter = { fadeIn(tween(duration, delayMillis = (duration * 0.3f).toInt())) + slideInVertically { it / 2 } },
                        exit = { fadeOut(tween(duration)) + slideOutVertically { -it / 2 } }
                    ) {
                        Row(
                            modifier = Modifier.padding(Theme.padding.value9),
                            horizontalArrangement = Arrangement.spacedBy(Theme.padding.h10),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon = when (it.type) {
                                Type.Info -> Icons.Lightbulb
                                Type.Success -> Icons.Check
                                Type.Warning -> Icons.Warning
                                Type.Error -> Icons.Error
                            })
                            Text(
                                text = it.text,
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
}