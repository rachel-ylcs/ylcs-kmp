package love.yinlin.foundation.picker

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import love.yinlin.coroutines.SyncFuture
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import love.yinlin.data.MimeType
import love.yinlin.io.Sources
import love.yinlin.io.safeToSources
import love.yinlin.coroutines.Coroutines
import love.yinlin.extension.catching
import love.yinlin.extension.catchingNull
import love.yinlin.extension.cleaning
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.uri.ContentUri
import love.yinlin.uri.ImplicitUri
import java.io.File
import java.io.FileOutputStream
import kotlin.uuid.Uuid

actual class Picker actual constructor(private val context: PlatformContextProvider) : AutoCloseable {
    @Volatile private var closed = false

    private val pending: MutableMap<String, () -> Unit> = mutableMapOf()

    actual override fun close() {
        closed = true
        Coroutines.dispatchUI {
            for (cancel in pending.values.toList()) cancel()
        }
    }

    private suspend fun <I, O> result(contract: ActivityResultContract<I, O>, input: I): O? = Coroutines.main {
        check(!closed) { "Picker is closed" }

        val registry = context.activityResultRegistry ?: return@main null
        val key = generateKey
        var launcher: ActivityResultLauncher<I>? = null
        cleaning({
            Coroutines.sync { future: SyncFuture<O> ->
                pending[key] = { future.cancel() }
                future.catching {
                    val registered = registry.register(key, contract) { future.send(it) }
                    launcher = registered
                    registered.launch(input)
                }
            }
        }) {
            launcher?.unregister()
            pending.remove(key)
        }
    }

    private val generateKey: String get() = Uuid.generateV7().toString()

    private val isLegacy: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    private suspend fun requestLegacyWritePermission(): Boolean {
        if (!isLegacy) return true

        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (context.rawContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) return true

        return result(ActivityResultContracts.RequestPermission(), permission) == true
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun prepareLegacySave(filename: String, directory: String): Pair<Any, Sink>? {
        if (!requestLegacyWritePermission()) return null

        var file: File? = null
        var stream: FileOutputStream? = null
        var prepared = false

        return cleaning({
            catchingNull {
                val result = Coroutines.io {
                    val name = File(filename.replace('\\', '/')).name
                    require(name.isNotBlank() && name != "." && name != "..")
                    val parent = Environment.getExternalStoragePublicDirectory(directory)
                    check(parent.isDirectory || parent.mkdirs())

                    var target = File(parent, name)
                    while (!target.createNewFile()) {
                        target = File(parent, "${generateKey}_$name")
                    }

                    file = target
                    val output = FileOutputStream(target)
                    stream = output
                    target to output.asSink().buffered()
                }
                prepared = true
                result
            }
        }) {
            if (!prepared) {
                catching { stream?.close() }
                catching { file?.delete() }
            }
        }
    }

    actual suspend fun pickPicture(): Source? {
        val uri = result(ActivityResultContracts.PickVisualMedia(), PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) ?: return null
        return pickerResource { context.contentResolver.openInputStream(uri)?.asSource()?.buffered() }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? {
        require(maxNum > 0)
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        val uris = if (maxNum == 1) result(ActivityResultContracts.PickVisualMedia(), request)?.let { [it] }
        else result(ActivityResultContracts.PickMultipleVisualMedia(maxNum), request)
        if (uris.isNullOrEmpty()) return null
        return pickerResource { uris.safeToSources { context.contentResolver.openInputStream(it)!!.asSource().buffered() } }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? {
        val uri = result(ActivityResultContracts.OpenDocument(), mimeType.toTypedArray()) ?: return null
        return pickerResource { context.contentResolver.openInputStream(uri)?.asSource()?.buffered() }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? {
        val uri = result(ActivityResultContracts.OpenDocument(), mimeType.toTypedArray()) ?: return null
        return ContentUri(context.rawContext, uri.toString())
    }

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? {
        val uri = result(ActivityResultContracts.CreateDocument(mimeType), filename) ?: return null
        return ContentUri(context.rawContext, uri.toString())
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? {
        if (isLegacy) return prepareLegacySave(filename, Environment.DIRECTORY_PICTURES)

        return Coroutines.sync { future ->
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
    }

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? {
        if (isLegacy) return prepareLegacySave(filename, Environment.DIRECTORY_MOVIES)

        return Coroutines.sync { future ->
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
    }

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) {
        if (isLegacy && origin is File) {
            sink.flush()
            MediaScannerConnection.scanFile(context.rawContext, [origin.absolutePath], null, null)
        }
    }

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) {
            if (origin is File) origin.delete()
            else context.contentResolver.delete(origin as Uri, null, null)
        }
    }
}
