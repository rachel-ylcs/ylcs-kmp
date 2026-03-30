package love.yinlin.compose.game.event

import androidx.compose.runtime.Stable
import kotlin.reflect.KClass

@Stable
sealed interface EventListener {
    val target: Array<KClass<out Event>>

    fun onEvent(tick: Long, event: Event): Boolean
}