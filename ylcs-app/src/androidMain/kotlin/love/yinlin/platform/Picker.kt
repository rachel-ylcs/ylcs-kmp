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
import love.yinlin.data.MimeType
import love.yinlin.extension.Sources
import love.yinlin.extension.safeToSources
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual object Picker {
    actual suspend fun pickPicture(): Source? = suspendCoroutine { continuation ->
        continuation.safeResume {
            val resolver = appNative.context.contentResolver
            appNative.activityResultRegistry!!.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                continuation.safeResume {
                    continuation.resume(resolver.openInputStream(uri!!)!!.asSource().buffered())
                }
            }.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = suspendCoroutine { continuation ->
        continuation.safeResume {
            val resolver = appNative.context.contentResolver
            appNative.activityResultRegistry!!.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.PickMultipleVisualMedia(maxNum)
            ) { result ->
                continuation.resume(result.safeToSources { resolver.openInputStream(it)!!.asSource().buffered() })
            }.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = suspendCoroutine { continuation ->
        continuation.safeResume {
            val resolver = appNative.context.contentResolver
            appNative.activityResultRegistry!!.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                continuation.safeResume {
                    continuation.resume(resolver.openInputStream(uri!!)!!.asSource().buffered())
                }
            }.launch(mimeType.toTypedArray())
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitPath? = suspendCoroutine { continuation ->
        continuation.safeResume {
            appNative.activityResultRegistry!!.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                continuation.safeResume {
                    continuation.resume(ContentPath(uri!!.toString()))
                }
            }.launch(mimeType.toTypedArray())
        }
    }

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitPath? = suspendCoroutine { continuation ->
        continuation.safeResume {
            appNative.activityResultRegistry!!.register(
                key = UUID.randomUUID().toString(),
                contract = ActivityResultContracts.CreateDocument(mimeType)
            ) { uri ->
                continuation.safeResume {
                    continuation.resume(ContentPath(uri!!.toString()))
                }
            }.launch(filename)
        }
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = suspendCoroutine { continuation ->
        continuation.safeResume {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            values.put(MediaStore.Images.Media.MIME_TYPE, MimeType.IMAGE)
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            val resolver = appNative.context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
            continuation.resume(uri to resolver.openOutputStream(uri)!!.asSink().buffered())
        }
    }

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = suspendCoroutine { continuation ->
        continuation.safeResume {
            val values = ContentValues()
            values.put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            values.put(MediaStore.Video.Media.MIME_TYPE, MimeType.VIDEO)
            values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            val resolver = appNative.context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
            continuation.resume(uri to resolver.openOutputStream(uri)!!.asSink().buffered())
        }
    }

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = Unit

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) appNative.context.contentResolver.delete(origin as Uri, null, null)
    }
}