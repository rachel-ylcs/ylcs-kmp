package love.yinlin.compose.game.event

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.traits.Visible
import kotlin.reflect.KClass

@Stable
sealed interface EventListener {
    val target: Array<KClass<out Event>>

    fun onEvent(event: Event, source: Visible?): Boolean
}