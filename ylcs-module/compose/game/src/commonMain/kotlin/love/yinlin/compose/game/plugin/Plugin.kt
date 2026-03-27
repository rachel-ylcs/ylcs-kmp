package love.yinlin.compose.game.plugin

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compose.game.Engine
import love.yinlin.reflect.metaClassName
import kotlin.reflect.KClass

@Stable
abstract class Plugin(val engine: Engine) {
    var isInitialized: Boolean by mutableStateOf(false)
        protected set

    /**
     * 前置依赖插件
     */
    open val dependencies: List<KClass<out Plugin>> = emptyList()

    open suspend fun onInitialize() { isInitialized = true }
    open fun onRelease() { isInitialized = false }
    open fun onUpdate(tick: Long) { }

    @Composable
    open fun BoxScope.Content() { }

    @OptIn(CompatibleRachelApi::class)
    val id: String = this.metaClassName

    final override fun hashCode(): Int = id.hashCode()
    final override fun equals(other: Any?): Boolean = (other as? Plugin)?.id == this.id
    final override fun toString(): String = id
}