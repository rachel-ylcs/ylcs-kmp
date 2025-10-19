package love.yinlin.common

import android.content.Context
import android.content.Intent
import androidx.compose.ui.util.fastForEach
import androidx.core.content.FileProvider
import java.io.File

object LocalFileProvider {
    // # https://github.com/androidx/media/issues/327
    // # https://github.com/PaulWoitaschek/Voice/blob/9b04e27feb27a32b3e660e7091e877bc2be93ece/playback/src/main/kotlin/voice/playback/session/MediaItemProvider.kt
    fun uri(context: Context, authority: String, file: File): android.net.Uri {
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
}