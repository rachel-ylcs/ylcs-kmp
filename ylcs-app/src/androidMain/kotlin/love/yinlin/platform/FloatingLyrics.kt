package love.yinlin.platform

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import love.yinlin.DeviceWrapper
import love.yinlin.common.Device
import love.yinlin.common.Scheme
import love.yinlin.common.ThemeValue
import java.util.*

@Stable
class ActualFloatingLyrics(private val activity: ComponentActivity) : FloatingLyrics() {
    private val view = ComposeView(activity).apply {
        setViewTreeLifecycleOwner(activity)
        setViewTreeSavedStateRegistryOwner(activity)
        setContent {
            ContentWrapper {
                FloatingContent()
            }
        }
    }

    override val canAttached: Boolean get() = Settings.canDrawOverlays(activity)

    override val isAttached: Boolean get() = view.isAttachedToWindow

    override fun applyPermission(onResult: (Boolean) -> Unit) {
        try {
            activity.activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                onResult(canAttached)
            }.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                setData(Uri.fromParts(Scheme.Package.toString(), activity.packageName, null))
            })
        }
        catch (_: Throwable) {}
    }

    override fun attach() {
        if (canAttached && !isAttached) {
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

    override fun detach() {
        if (isAttached) {
            val manager = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            manager?.removeViewImmediate(view)
        }
    }

    override fun updateLyrics(lyrics: String?) {
        currentLyrics = lyrics
    }

    @Composable
    fun FloatingContent() {
        val config = app.config.floatingLyricsConfig
        currentLyrics?.let { lyrics ->
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.padding(
                        start = this.maxWidth * config.left.coerceIn(0f, 1f),
                        end = this.maxWidth * (1 - config.right).coerceIn(0f, 1f),
                        top = ThemeValue.Padding.VerticalExtraSpace * 4f * config.top
                    ).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lyrics,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = MaterialTheme.typography.labelLarge.fontSize * config.textSize
                        ),
                        color = Color(config.textColor),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.background(color = Color(config.backgroundColor)).padding(ThemeValue.Padding.Value)
                    )
                }
            }
        }
    }

    @Composable
    fun ContentWrapper(content: @Composable () -> Unit) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            DeviceWrapper(
                device = remember(this.maxWidth) { Device(this.maxWidth) },
                themeMode = app.config.themeMode,
                fontScale = 1f,
                content = content
            )
        }
    }
}