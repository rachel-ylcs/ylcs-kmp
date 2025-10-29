package love.yinlin.platform

//import android.content.Context
//import android.content.Intent
//import android.graphics.PixelFormat
//import android.net.Uri
//import android.provider.Settings
//import android.view.Gravity
//import android.view.WindowManager
//import androidx.activity.ComponentActivity
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.ComposeView
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.Density
//import androidx.compose.ui.unit.Dp
//import androidx.lifecycle.setViewTreeLifecycleOwner
//import androidx.lifecycle.setViewTreeViewModelStoreOwner
//import androidx.savedstate.setViewTreeSavedStateRegistryOwner
//import love.yinlin.uri.Scheme
//import love.yinlin.compose.*
//import love.yinlin.extension.catching
//import java.util.UUID
//
//@Stable
//class ActualFloatingLyrics(private val activity: ComponentActivity) : FloatingLyrics() {
//    private val view = ComposeView(activity).apply {
//        setViewTreeLifecycleOwner(activity)
//        setViewTreeSavedStateRegistryOwner(activity)
//        setViewTreeViewModelStoreOwner(activity)
//        setContent {
//            AppEntry(
//                fill = false,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1f)) {
//                    Content(maxWidth)
//                }
//            }
//        }
//    }
//
//    private var currentLyrics: String? by mutableStateOf(null)
//
//    val canAttached: Boolean get() = Settings.canDrawOverlays(activity)
//
//    override val isAttached: Boolean get() = view.isAttachedToWindow
//
//    fun applyPermission(onResult: (Boolean) -> Unit) = catching {
//        activity.activityResultRegistry.register(
//            key = UUID.randomUUID().toString(),
//            contract = ActivityResultContracts.StartActivityForResult()
//        ) {
//            onResult(canAttached)
//        }.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
//            setData(Uri.fromParts(Scheme.Package.toString(), activity.packageName, null))
//        })
//    }
//
//    fun attach() {
//        if (canAttached && !isAttached) {
//            val manager = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
//            val params = WindowManager.LayoutParams(
//                WindowManager.LayoutParams.MATCH_PARENT,
//                WindowManager.LayoutParams.WRAP_CONTENT,
//                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
//                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
//                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
//                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
//                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
//                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
//                PixelFormat.TRANSLUCENT
//            ).apply { gravity = Gravity.TOP }
//            manager?.addView(view, params)
//        }
//    }
//
//    fun detach() {
//        if (isAttached) {
//            val manager = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
//            manager?.removeViewImmediate(view)
//        }
//    }
//
//    override fun updateLyrics(lyrics: String?) {
//        currentLyrics = lyrics
//    }
//
//    @Composable
//    private fun Content(maxWidth: Dp) {
//        currentLyrics?.let { lyrics ->
//            val config = service.config.floatingLyricsAndroidConfig
//
//            Box(
//                modifier = Modifier.padding(
//                    start = maxWidth * config.left.coerceIn(0f, 1f),
//                    end = maxWidth * (1 - config.right).coerceIn(0f, 1f),
//                    top = CustomTheme.padding.verticalExtraSpace * 4f * config.top
//                ).fillMaxWidth(),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = lyrics,
//                    style = MaterialTheme.typography.labelLarge.copy(
//                        fontSize = MaterialTheme.typography.labelLarge.fontSize * config.textSize
//                    ),
//                    color = Colors(config.textColor),
//                    textAlign = TextAlign.Center,
//                    maxLines = 2,
//                    overflow = TextOverflow.Ellipsis,
//                    modifier = Modifier.wrapContentSize(unbounded = true)
//                        .background(color = Colors(config.backgroundColor))
//                        .padding(CustomTheme.padding.value)
//                )
//            }
//        }
//    }
//}