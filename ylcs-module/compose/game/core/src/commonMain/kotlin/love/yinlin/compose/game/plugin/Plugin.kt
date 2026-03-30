package love.yinlin.compose.game.plugin

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.LayerOrder
import love.yinlin.reflect.metaClassName
import kotlin.reflect.KClass

@Stable
abstract class Plugin(val engine: Engine) {
    @PublishedApi
    internal var isInitialized: Boolean by mutableStateOf(false)

    /**
     * 前置依赖插件
     */
    open val dependencies: List<KClass<out Plugin>> = emptyList()

    /**
     * 初始化
     */
    open suspend fun onInitialize(): Boolean = true

    /**
     * 销毁
     */
    open fun onRelease() { }

    /**
     * 层级
     */
    open val layerOrder: Int = LayerOrder.Default

    /**
     * 内容
     */
    @Composable
    open fun BoxScope.Content() { }

    @OptIn(CompatibleRachelApi::class)
    val id: String = this.metaClassName

    final override fun hashCode(): Int = id.hashCode()
    final override fun equals(other: Any?): Boolean = (other as? Plugin)?.id == this.id
    final override fun toString(): String = id
}