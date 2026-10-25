@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use

internal class NativeSceneFixture(val window: TestNativeWindowScope = TestNativeWindowScope(), onCloseRequest: () -> Boolean = { true }) : AutoCloseable {
    val host: NativeSceneHost
    private val surface: Surface
    private var time = 0L

    init {
        registerSkikoComposeImplementation()
        host = NativeSceneHost(onCloseRequest)
        host.attach(window)
        surface = Surface.makeRasterN32Premul(96, 64)
    }

    fun content(content: @Composable () -> Unit) = host.setContent(content)

    fun render() {
        Snapshot.sendApplyNotifications()
        time += 16_666_667L
        surface.canvas.clear(org.jetbrains.skia.Color.TRANSPARENT)
        host.render(surface.canvas, 96, 64, time)
    }

    fun pixel(x: Int, y: Int): Int = Bitmap().use { bitmap ->
        check(bitmap.allocPixels(ImageInfo.makeN32Premul(96, 64)))
        check(surface.readPixels(bitmap, 0, 0))
        bitmap.getColor(x, y)
    }

    override fun close() {
        try { host.close() }
        finally { surface.close() }
    }
}
