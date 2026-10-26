@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package love.yinlin.nativecompose.consumer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.platform.DefaultArchitectureComponentsOwner
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.enableSavedStateHandles
import kotlinx.coroutines.yield
import org.jetbrains.skia.Surface
import kotlin.coroutines.CoroutineContext

internal class PublishedSceneFixture(context: CoroutineContext) : AutoCloseable {
    private val owner = DefaultArchitectureComponentsOwner(enforceMainThread = false).apply {
        enableSavedStateHandles()
        setLifecycleState(Lifecycle.State.RESUMED)
    }
    private val platform = object : PlatformContext.Empty() {
        override val architectureComponentsOwner: PlatformArchitectureComponentsOwner get() = owner
    }
    private val recomposer = FrameRecomposer(context)
    private val scene = CanvasLayersComposeScene(recomposer, size = IntSize(240, 160), platformContext = platform)
    private val rendering = SingleComposeSceneRenderingScope {}
    private val surface = Surface.makeRasterN32Premul(240, 160)
    private var time = 0L

    fun content(content: @Composable () -> Unit) = scene.setContent(content = content)

    suspend fun settle() {
        repeat(8) {
            Snapshot.sendApplyNotifications()
            time += 100_000_000L
            with(rendering) { scene.render(recomposer, surface.canvas.asComposeCanvas(), time) }
            yield()
        }
    }

    override fun close() {
        try { scene.close() }
        finally {
            recomposer.close()
            owner.setLifecycleState(Lifecycle.State.DESTROYED)
            owner.navigationEventDispatcher.dispose()
            surface.close()
        }
    }
}
