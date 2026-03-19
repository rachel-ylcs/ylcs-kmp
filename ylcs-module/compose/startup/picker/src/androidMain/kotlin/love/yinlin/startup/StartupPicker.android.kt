package love.yinlin.startup

import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import love.yinlin.data.MimeType
import love.yinlin.io.Sources
import love.yinlin.io.safeToSources
import love.yinlin.coroutines.Coroutines
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.SyncStartup
import love.yinlin.uri.ContentUri
import love.yinlin.uri.ImplicitUri
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

actual class StartupPicker actual constructor(context: PlatformContextProvider) : SyncStartup(context) {
    @OptIn(ExperimentalUuidApi::class)
    private val generateKey: String get() = Uuid.generateV7().toString()

    actual override fun init(scope: CoroutineScope, args: StartupArgs) { }

    actual suspend fun pickPicture(): Source? = Coroutines.sync { future ->
        future.catching {
            context.activityResultRegistry!!.register(
                key = generateKey,
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                future.send { context.contentResolver.openInputStream(uri!!)!!.asSource().buffered() }
            }.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = Coroutines.sync { future ->
        future.catching {
            context.activityResultRegistry!!.register(
                key = generateKey,
                contract = ActivityResultContracts.PickMultipleVisualMedia(maxNum)
            ) { result ->
                future.send(result.safeToSources { context.contentResolver.openInputStream(it)!!.asSource().buffered() })
            }.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = Coroutines.sync { future ->
        future.catching {
            context.activityResultRegistry!!.register(
                key = generateKey,
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                future.send { context.contentResolver.openInputStream(uri!!)!!.asSource().buffered() }
            }.launch(mimeType.toTypedArray())
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = Coroutines.sync { future ->
        future.catching {
            context.activityResultRegistry!!.register(
                key = generateKey,
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                future.send { ContentUri(context.raw, uri!!.toString()) }
            }.launch(mimeType.toTypedArray())
        }
    }


    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? = Coroutines.sync { future ->
        future.catching {
            context.activityResultRegistry!!.register(
                key = generateKey,
                contract = ActivityResultContracts.CreateDocument(mimeType)
            ) { uri ->
                future.send { ContentUri(context.raw, uri!!.toString()) }
            }.launch(filename)
        }
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = Coroutines.sync { future ->
        future.send {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            values.put(MediaStore.Images.Media.MIME_TYPE, MimeType.IMAGE)
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
            uri to contentResolver.openOutputStream(uri)!!.asSink().buffered()
        }
    }

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = Coroutines.sync { future ->
        future.send {
            val values = ContentValues()
            values.put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            values.put(MediaStore.Video.Media.MIME_TYPE, MimeType.VIDEO)
            values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
            uri to contentResolver.openOutputStream(uri)!!.asSink().buffered()
        }
    }

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = Unit

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) context.contentResolver.delete(origin as Uri, null, null)
    }
}