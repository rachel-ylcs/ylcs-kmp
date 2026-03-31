package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import love.yinlin.compose.game.plugin.ScenePlugin

/**
 * 加入场景的实体
 */
@Stable
open class Entity : Unique() {
    /**
     * 附加场景
     */
    open fun onAttached(scene: ScenePlugin) { }

    /**
     * 离开场景
     */
    open fun onDetached(scene: ScenePlugin) { }
}