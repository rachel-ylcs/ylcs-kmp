package love.yinlin.foundation.picker

import kotlinx.cinterop.*
import kotlinx.io.*
import kotlin.concurrent.Volatile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.SyncFuture
import love.yinlin.extension.catching
import love.yinlin.extension.cleaning
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.fs.*
import love.yinlin.io.SandboxSource
import love.yinlin.io.Sources
import love.yinlin.io.safeToSources
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.SandboxUri
import platform.Foundation.*
import platform.Photos.*
import platform.PhotosUI.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class Picker actual constructor(private val context: PlatformContextProvider) : AutoCloseable {
    private val dialogMutex = Mutex()
    @Volatile private var closed = false
    private var pending: SyncFuture<*>? = null
    private var activeController: UIViewController? = null

    actual override fun close() {
        closed = true
        Coroutines.dispatchUI {
            pending?.cancel()
            activeController?.dismissViewControllerAnimated(false, null)
            activeController = null
            phPickerDelegate = null
            phPickerDismissDelegate = null
            documentPickerDelegate = null
        }
    }

    private suspend fun <T> await(onCancel: () -> Unit = {}, block: (SyncFuture<T>) -> Unit): T? = dialogMutex.withLock {
        Coroutines.main {
            check(!closed) { "Picker is closed" }

            cleaning({
                Coroutines.sync { future: SyncFuture<T> ->
                    pending = future
                    future.clean {
                        onCancel()
                        val controller = activeController
                        Coroutines.dispatchUI { controller?.dismissViewControllerAnimated(false, null) }
                    }
                    future.catching { block(future) }
                }
            }) {
                pending = null
                activeController = null
                phPickerDelegate = null
                phPickerDismissDelegate = null
                documentPickerDelegate = null
            }
        }
    }

    private var phPickerDelegate: PHPickerViewControllerDelegateProtocol? = null
    private var phPickerDismissDelegate: UIAdaptivePresentationControllerDelegateProtocol? = null
    private var documentPickerDelegate: UIDocumentPickerDelegateProtocol? = null

    private fun copyToTempDir(url: NSURL?): File? {
        if (url == null) return null
        val fileManager = NSFileManager.defaultManager
        val extension = url.pathExtension?.takeIf { it.isNotEmpty() } ?: "tmp"
        val tempUrl = fileManager.temporaryDirectory.URLByAppendingPathComponent("picker-${NSUUID().UUIDString}.$extension") ?: return null

        if (!fileManager.copyItemAtURL(url, tempUrl, null)) {
            fileManager.removeItemAtURL(tempUrl, null)
            return null
        }

        return tempUrl.path?.let(File::invoke) ?: run {
            fileManager.removeItemAtURL(tempUrl, null)
            null
        }
    }

    actual suspend fun pickPicture(): Source? = pickPicture(1)?.firstOrNull()

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = Coroutines.main {
        val pictures = PendingPictures()

        val selected = await<List<File>>(onCancel = pictures::cancel) { future ->
            future.catching {
                require(maxNum > 0)

                val configuration = PHPickerConfiguration(PHPhotoLibrary.sharedPhotoLibrary()).apply {
                    selectionLimit = maxNum.toLong()
                    selection = PHPickerConfigurationSelectionOrdered
                    filter = PHPickerFilter.imagesFilter()
                }

                val picker = PHPickerViewController(configuration)
                var didFinish = false
                val onImagesPicked: (List<PHPickerResult>) -> Unit = onImagesPicked@{ results ->
                    if (results.isEmpty()) {
                        future.send()
                        return@onImagesPicked
                    }
                    pictures.start(results.size)
                    results.forEachIndexed { index, result ->
                        result.itemProvider.loadFileRepresentationForTypeIdentifier(UTTypeImage.identifier) { url, _ ->
                            val file = copyToTempDir(url)
                            pictures.complete(index, file)?.let { future.send(it) }
                        }
                    }
                }

                phPickerDelegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                        if (didFinish) return
                        didFinish = true
                        picker.dismissViewControllerAnimated(true, null)
                        onImagesPicked(didFinishPicking.filterIsInstance<PHPickerResult>())
                    }
                }

                phPickerDismissDelegate = object : NSObject(), UIAdaptivePresentationControllerDelegateProtocol {
                    override fun presentationControllerDidDismiss(presentationController: UIPresentationController) {
                        if (!didFinish) future.send()
                    }
                }

                picker.delegate = phPickerDelegate
                val rootViewController = checkNotNull(context.controller ?: UIApplication.sharedApplication.keyWindow?.rootViewController) {
                    "No view controller is available for the picker"
                }
                activeController = picker
                rootViewController.presentViewController(picker, true, null)
                picker.presentationController?.delegate = phPickerDismissDelegate
            }
        } ?: return@main null

        var transferred = false
        cleaning({
            pickerResource {
                selected.safeToSources { file ->
                    TemporaryPictureSource(file, file.rawSourceSync()).buffered()
                }
            }.also { transferred = it != null }
        }) {
            if (!transferred) selected.forEach { file -> catching { file.deleteSync() } }
        }
    }

    private fun openPicker(mimeType: List<String>, filter: List<String>, callback: (NSURL?) -> Unit) {
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = filter
            .map { it.substringAfterLast(".") }
            .mapNotNull { UTType.typeWithFilenameExtension(it) }
            .ifEmpty { mimeType.mapNotNull { UTType.typeWithMIMEType(it) } }
            .ifEmpty { [UTTypeContent] }
        )
        picker.allowsMultipleSelection = false

        documentPickerDelegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentAtURL: NSURL) {
                callback(didPickDocumentAtURL)
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                callback(null)
            }
        }

        picker.delegate = documentPickerDelegate
        val rootViewController = checkNotNull(context.controller ?: UIApplication.sharedApplication.keyWindow?.rootViewController) {
            "No view controller is available for the picker"
        }
        activeController = picker
        rootViewController.presentViewController(picker, true, null)
    }

    private suspend fun selectDocument(mimeType: List<String>, filter: List<String>): NSURL? = await { future ->
        future.catching {
            openPicker(mimeType, filter) { url -> future.send(url) }
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = Coroutines.main {
        val url = selectDocument(mimeType, filter) ?: return@main null
        pickerResource { SandboxSource(url).buffered() }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = Coroutines.main {
        selectDocument(mimeType, filter)?.let(::SandboxUri)
    }

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? = Coroutines.main {
        val folder = await<NSURL> { future ->
            future.catching {
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = [UTTypeFolder])

                documentPickerDelegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentAtURL: NSURL) {
                        future.send(didPickDocumentAtURL)
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        future.send()
                    }
                }

                picker.delegate = documentPickerDelegate

                val rootViewController = checkNotNull(context.controller ?: UIApplication.sharedApplication.keyWindow?.rootViewController) {
                    "No view controller is available for the picker"
                }
                activeController = picker
                rootViewController.presentViewController(picker, true, null)
            }
        } ?: return@main null

        folder.URLByAppendingPathComponent(filename)?.let { SandboxUri(it, folder) }
    }

    private suspend fun prepareSave(filename: String, photo: Boolean): Pair<Any, Sink>? = Coroutines.io {
        val name = filename.replace('\\', '/').substringAfterLast('/')
        require(name.isNotBlank() && name != "." && name != "..")

        val suffix = name.substringAfterLast('.', "").takeIf {
            it.isNotEmpty() && it.length <= 12 && it.all(Char::isLetterOrDigit)
        } ?: "bin"

        val url = NSFileManager.defaultManager.temporaryDirectory.URLByAppendingPathComponent("picker-${NSUUID().UUIDString}.$suffix") ?: return@io null
        val file = url.path?.let(File::invoke) ?: return@io null
        var prepared = false

        cleaning({
            val sink = file.bufferedSink()
            val origin = if (photo) SaveType.Photo(name, url) else SaveType.Video(name, url)
            prepared = true
            origin to sink
        }) {
            if (!prepared) {
                catching { file.delete() }
            }
        }
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = prepareSave(filename, true)

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? = prepareSave(filename, false)

    @OptIn(BetaInteropApi::class)
    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = Coroutines.io {
        val resource = origin as? SaveType ?: return@io
        cleaning({
            sink.flush()
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                error.value = null
                val success = PHPhotoLibrary.sharedPhotoLibrary().performChangesAndWait({
                    val options = PHAssetResourceCreationOptions().apply {
                        originalFilename = resource.filename
                        val extension = resource.filename.substringAfterLast('.', "")
                        if (extension.isNotEmpty()) {
                            uniformTypeIdentifier = if (extension.equals("webp", ignoreCase = true)) "org.webmproject.webp"
                            else UTType.typeWithFilenameExtension(extension)?.identifier
                        }
                        shouldMoveFile = false
                    }

                    PHAssetCreationRequest.creationRequestForAsset().addResourceWithType(
                        if (resource is SaveType.Photo) PHAssetResourceTypePhoto else PHAssetResourceTypeVideo,
                        fileURL = resource.url,
                        options = options
                    )
                }, error.ptr)

                if (!success) {
                    throw IOException(error.value?.localizedDescription ?: ("Failed to import ${resource.filename} into Photos"))
                }
            }
        }) {
            NSFileManager.defaultManager.removeItemAtURL(resource.url, null)
        }
    }

    actual suspend fun cleanSave(origin: Any, result: Boolean) = Coroutines.io {
        if (origin is SaveType) NSFileManager.defaultManager.removeItemAtURL(origin.url, null)
    }
}
