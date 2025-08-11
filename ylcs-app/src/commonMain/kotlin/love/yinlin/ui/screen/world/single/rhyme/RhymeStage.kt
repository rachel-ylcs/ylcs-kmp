package love.yinlin.ui.screen.world.single.rhyme

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import love.yinlin.data.music.RhymeLyricsConfig

// 游戏舞台
@Stable
internal class RhymeStage {
    private var scene: Scene? = null
    private val lock = SynchronizedObject()

    fun onInitialize(lyrics: RhymeLyricsConfig, imageSet: ImageSet) {
        synchronized(lock) { scene = Scene(lyrics, imageSet) }
    }

    fun onClear() {
        synchronized(lock) { scene = null }
    }

    fun onUpdate(position: Long) {
        synchronized(lock) { scene?.onUpdate(position) }
    }

    fun onEvent(pointer: Pointer) {
        synchronized(lock) { scene?.onEvent(pointer) }
    }

    fun onDraw(scope: DrawScope, textManager: RhymeTextManager) {
        scene?.run { scope.draw(textManager) }
    }

    fun onResult(): RhymeResult {
        return RhymeResult(0)
    }
}