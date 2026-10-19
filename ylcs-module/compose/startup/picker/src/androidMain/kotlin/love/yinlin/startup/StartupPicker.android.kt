package love.yinlin.startup

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
import androidx.activity.result.contract.ActivityResultContracts
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
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartup
import love.yinlin.uri.ContentUri
import love.yinlin.uri.ImplicitUri
import java.io.File
import java.io.FileOutputStream
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

actual class StartupPicker actual constructor(pool: StartupPool) : SyncStartup(pool) {
    @OptIn(ExperimentalUuidApi::class)
    private val generateKey: String get() = Uuid.generateV7().toString()

    private val isLegacy: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    private suspend fun requestLegacyWritePermission(): Boolean {
        if (!isLegacy) return true

        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (pool.rawContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) return true

        return Coroutines.main {
            val registry = pool.activityResultRegistry ?: return@main false
            var launcher: ActivityResultLauncher<String>? = null
            cleaning({
                Coroutines.sync { future ->
                    future.catching {
                        launcher = registry.register(
                            key = generateKey,
                            contract = ActivityResultContracts.RequestPermission()
                        ) { future.send(it) }
                        launcher.launch(permission)
                    }
                } == true
            }) {
                launcher?.unregister()
            }
        }
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

    actual suspend fun pickPicture(): Source? = Coroutines.sync { future ->
        future.catching {
            pool.activityResultRegistry!!.register(
                key = generateKey,
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                future.send { pool.contentResolver.openInputStream(uri!!)!!.asSource().buffered() }
            }.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = Coroutines.sync { future ->
        future.catching {
            pool.activityResultRegistry!!.register(
                key = generateKey,
                contract = ActivityResultContracts.PickMultipleVisualMedia(maxNum)
            ) { result ->
                future.send(result.safeToSources { pool.contentResolver.openInputStream(it)!!.asSource().buffered() })
            }.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = Coroutines.sync { future ->
        future.catching {
            pool.activityResultRegistry!!.register(
                key = generateKey,
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                future.send { pool.contentResolver.openInputStream(uri!!)!!.asSource().buffered() }
            }.launch(mimeType.toTypedArray())
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = Coroutines.sync { future ->
        future.catching {
            pool.activityResultRegistry!!.register(
                key = generateKey,
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                future.send { ContentUri(pool.rawContext, uri!!.toString()) }
            }.launch(mimeType.toTypedArray())
        }
    }


    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? = Coroutines.sync { future ->
        future.catching {
            pool.activityResultRegistry!!.register(
                key = generateKey,
                contract = ActivityResultContracts.CreateDocument(mimeType)
            ) { uri ->
                future.send { ContentUri(pool.rawContext, uri!!.toString()) }
            }.launch(filename)
        }
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? {
        if (isLegacy) return prepareLegacySave(filename, Environment.DIRECTORY_PICTURES)

        return Coroutines.sync { future ->
            future.send {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                values.put(MediaStore.Images.Media.MIME_TYPE, MimeType.IMAGE)
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                val contentResolver = pool.contentResolver
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
                val contentResolver = pool.contentResolver
                val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)!!
                uri to contentResolver.openOutputStream(uri)!!.asSink().buffered()
            }
        }
    }

    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) {
        if (isLegacy && origin is File) {
            sink.flush()
            MediaScannerConnection.scanFile(pool.rawContext, [origin.absolutePath], null, null)
        }
    }

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (!result) {
            if (origin is File) origin.delete()
            else pool.contentResolver.delete(origin as Uri, null, null)
        }
    }
}