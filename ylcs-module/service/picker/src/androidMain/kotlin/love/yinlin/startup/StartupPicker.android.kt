package love.yinlin.startup

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import love.yinlin.Context
import love.yinlin.data.MimeType
import love.yinlin.io.Sources
import love.yinlin.io.safeToSources
import love.yinlin.platform.Coroutines
import love.yinlin.StartupArgs
import love.yinlin.SyncStartup
import love.yinlin.uri.ContentUri
import love.yinlin.uri.ImplicitUri
import java.util.UUID

actual class StartupPicker : SyncStartup {
    private lateinit var activity: ComponentActivity
    private lateinit var resolver: ContentResolver
    private lateinit var activityResultRegistry: ActivityResultRegistry

    actual override fun init(context: Context, args: StartupArgs) {}

    override fun initDelay(context: Context, args: StartupArgs) {
        activity = context.activity
        resolver = activity.contentResolver
        activityResultRegistry = activity.activityResultRegistry
    }

    actual suspend fun pickPicture(): Source? = Coroutines.sync { future ->
        future.catching {
            activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                future.send { resolver.openInputStream(uri!!)!!.asSource().buffered() }
            }.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = Coroutines.sync { future ->
        future.catching {
            activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.PickMultipleVisualMedia(maxNum)
            ) { result ->
                future.send(result.safeToSources { resolver.openInputStream(it)!!.asSource().buffered() })
            }.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = Coroutines.sync { future ->
        future.catching {
            activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                future.send { resolver.openInputStream(uri!!)!!.asSource().buffered() }
            }.launch(mimeType.toTypedArray())
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = Coroutines.sync { future ->
        future.catching {
            activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                future.send { ContentUri(activity, uri!!.toString()) }
            }.launch(mimeType.toTypedArray())
        }
    }


    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? = Coroutines.sync { future ->
        future.catching {
            activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.CreateDocument(mimeType)
            ) { uri ->
                future.send { ContentUri(activity, uri!!.toString()) }
            }.launch(filename)
        }
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = Coroutines.sync { future ->
        future.send {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            values.put(MediaStore.Images.Media.MIME_TYPE, MimeType.IMAGE)
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
            uri to resolver.openOutputStream(uri)!!.asSink().buffered()
        }
    }

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = Coroutines.sync { future ->
        future.send {
            val values = ContentValues()
            values.put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            values.put(MediaStore.Video.Media.MIME_TYPE, MimeType.VIDEO)
            values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
            uri to resolver.openOutputStream(uri)!!.asSink().buffered()
        }
    }

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = Unit

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) resolver.delete(origin as Uri, null, null)
    }
}