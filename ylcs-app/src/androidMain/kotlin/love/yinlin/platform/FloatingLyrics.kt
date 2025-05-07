package love.yinlin.platform

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

@Stable
class ActualFloatingLyrics(private val activity: ComponentActivity) : FloatingLyrics() {
    private val view = ComposeView(activity).apply {
        setViewTreeLifecycleOwner(activity)
        setViewTreeSavedStateRegistryOwner(activity)
        setContent { FloatingContent() }
    }

    private val isDrawOverlays: Boolean get() = Settings.canDrawOverlays(activity)
    private val isAttached: Boolean get() = view.isAttachedToWindow

    fun attach() {
        if (isDrawOverlays && !isAttached) {
            val manager = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP }
            manager?.addView(view, params)
        }
    }

    fun detach() {
        if (isAttached) {
            val manager = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            manager?.removeViewImmediate(view)
        }
    }

    override fun updateLyrics(lyrics: String) {
        if (isAttached) currentLyrics = lyrics
    }

    @Composable
    fun FloatingContent() {
        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            Text(
                text = currentLyrics,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}