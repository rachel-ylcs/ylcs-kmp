package love.yinlin.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastForEach
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

@Stable
actual object Fixup {
    // See https://github.com/androidx/media/issues/327
    // See https://github.com/PaulWoitaschek/Voice/blob/9b04e27feb27a32b3e660e7091e877bc2be93ece/playback/src/main/kotlin/voice/playback/session/MediaItemProvider.kt
    fun updateLocalFileProviderPermission(context: Context, authority: String, file: File): Uri {
        val uri = FileProvider.getUriForFile(context, authority, file)
        listOf(
            "com.android.systemui",
            "com.google.android.autosimulator",
            "com.google.android.carassistant",
            "com.google.android.googlequicksearchbox",
            "com.google.android.projection.gearhead",
            "com.google.android.wearable.app",
        ).fastForEach { grantedPackage ->
            context.grantUriPermission(grantedPackage, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return uri
    }

    // See https://stackoverflow.com/questions/38382283/change-status-bar-text-color-when-primarydark-is-white/66197063#66197063
    @Composable
    fun StatusBarAutoTheme(window: Window, isDarkMode: Boolean) {
        LaunchedEffect(window, isDarkMode) {
            WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
        }
    }
}