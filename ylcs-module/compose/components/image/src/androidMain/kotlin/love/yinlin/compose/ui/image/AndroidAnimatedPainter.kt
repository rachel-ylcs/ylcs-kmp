package love.yinlin.compose.ui.image

import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.roundToInt

internal class AndroidAnimatedPainter(private val drawable: AnimatedImageDrawable) : Painter(), Drawable.Callback {
    private val handler = Handler(Looper.getMainLooper())
    private var invalidation by mutableIntStateOf(0)
    private var active = false
    private var width = -1
    private var height = -1

    init {
        drawable.callback = this
    }

    override val intrinsicSize: Size = Size(drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())

    override fun DrawScope.onDraw() {
        invalidation
        val w = size.width.roundToInt()
        val h = size.height.roundToInt()
        if (width != w || height != h) {
            width = w
            height = h
            drawable.setBounds(0, 0, w, h)
        }
        drawable.draw(drawContext.canvas.nativeCanvas)
    }

    override fun applyAlpha(alpha: Float): Boolean {
        drawable.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        drawable.colorFilter = colorFilter?.asAndroidColorFilter()
        return true
    }

    fun update(playing: Boolean) {
        if (active == playing) return
        active = playing

        if (playing) drawable.start()
        else drawable.stop()
    }

    fun dispose() {
        active = false
        drawable.stop()
        drawable.callback = null
        handler.removeCallbacksAndMessages(drawable)
    }

    override fun invalidateDrawable(who: Drawable) {
        invalidation++
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, time: Long) {
        handler.postAtTime(what, drawable, time)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        handler.removeCallbacks(what, drawable)
    }
}