package love.yinlin.platform

import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import love.yinlin.uri.ContentUri
import love.yinlin.uri.ImplicitUri
import love.yinlin.data.MimeType
import love.yinlin.extension.Sources
import love.yinlin.extension.safeToSources
import love.yinlin.service
import java.util.UUID

actual object Picker {
    actual suspend fun pickPicture(): Source? = Coroutines.sync { future ->
        future.catching {
            val context = service.context
            val resolver = context.platformContext.contentResolver
            context.activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                future.send { resolver.openInputStream(uri!!)!!.asSource().buffered() }
            }.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = Coroutines.sync { future ->
        future.catching {
            val context = service.context
            val resolver = context.platformContext.contentResolver
            context.activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.PickMultipleVisualMedia(maxNum)
            ) { result ->
                future.send(result.safeToSources { resolver.openInputStream(it)!!.asSource().buffered() })
            }.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = Coroutines.sync { future ->
        future.catching {
            val context = service.context
            val resolver = context.platformContext.contentResolver
            context.activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                future.send { resolver.openInputStream(uri!!)!!.asSource().buffered() }
            }.launch(mimeType.toTypedArray())
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = Coroutines.sync { future ->
        future.catching {
            service.context.activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                future.send { ContentUri(service.context.platformContext, uri!!.toString()) }
            }.launch(mimeType.toTypedArray())
        }
    }

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? = Coroutines.sync { future ->
        future.catching {
            service.context.activityResultRegistry.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.CreateDocument(mimeType)
            ) { uri ->
                future.send { ContentUri(service.context.platformContext, uri!!.toString()) }
            }.launch(filename)
        }
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = Coroutines.sync { future ->
        future.send {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            values.put(MediaStore.Images.Media.MIME_TYPE, MimeType.IMAGE)
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            val resolver = service.context.platformContext.contentResolver
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
            val resolver = service.context.platformContext.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
            uri to resolver.openOutputStream(uri)!!.asSink().buffered()
        }
    }

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = Unit

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) service.context.platformContext.contentResolver.delete(origin as Uri, null, null)
    }
}