@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package love.yinlin.nativecompose.consumer

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.SingleComposeSceneRenderingScope
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use
import kotlin.test.*

/** Exercises only public API, using published KLIBs without source/composite substitutions. */
class ComposeScenePublicationTest {
    @Test
    fun publishedUiComposesDrawsRecomposesAndDisposes() = runBlocking {
        registerSkikoComposeImplementation()
        val recomposer = FrameRecomposer(coroutineContext)
        val scene = CanvasLayersComposeScene(recomposer, size = IntSize(32, 24))
        val rendering = SingleComposeSceneRenderingScope {}
        var color by mutableStateOf(Color.Red)
        var disposed = false
        try {
            scene.setContent(content = {
                PublishedUiContent(color) { disposed = true }
            })
            Surface.makeRasterN32Premul(32, 24).use { surface ->
                with(rendering) { scene.render(recomposer, surface.canvas.asComposeCanvas(), 1L) }
                color = Color.Blue
                Snapshot.sendApplyNotifications()
                with(rendering) { scene.render(recomposer, surface.canvas.asComposeCanvas(), 16_666_667L) }
                Bitmap().use { bitmap ->
                    assertTrue(bitmap.allocPixels(ImageInfo.makeN32Premul(32, 24)))
                    assertTrue(surface.readPixels(bitmap, 0, 0))
                    assertEquals(Color.Blue.toArgb(), bitmap.getColor(16, 12))
                }
            }
        }
        finally {
            scene.close()
            recomposer.close()
        }
        assertTrue(disposed)
    }

    @Test
    fun publishedNativeKeysAndClipboardMetadataRemainUsable() {
        assertNotEquals(androidx.compose.ui.input.key.Key.A, androidx.compose.ui.input.key.Key.B)
        val entry = androidx.compose.ui.platform.ClipEntry.withPlainText("中文 😀")
        assertEquals("中文 😀", entry.getPlainText())
        assertTrue(entry.clipMetadata.hasMimeType("text/*"))
        assertFalse(entry.clipMetadata.hasMimeType("image/png"))
    }
}
