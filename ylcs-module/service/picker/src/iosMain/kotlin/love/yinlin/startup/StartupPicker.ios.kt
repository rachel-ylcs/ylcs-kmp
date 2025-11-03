package love.yinlin.startup

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.Buffer
import kotlinx.io.InternalIoApi
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import love.yinlin.extension.toNSData
import love.yinlin.io.safeToSources
import love.yinlin.io.SandboxSource
import love.yinlin.io.Sources
import love.yinlin.platform.Coroutines
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.SyncStartup
import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.SandboxUri
import love.yinlin.uri.toPath
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.temporaryDirectory
import platform.Photos.PHPhotoLibrary
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerConfigurationSelectionOrdered
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIAdaptivePresentationControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import platform.UIKit.UIPresentationController
import platform.UIKit.UISaveVideoAtPathToSavedPhotosAlbum
import platform.UIKit.UIVideoAtPathIsCompatibleWithSavedPhotosAlbum
import platform.UIKit.presentationController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeContent
import platform.UniformTypeIdentifiers.UTTypeFolder
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject

actual class StartupPicker : SyncStartup {
    // 全局引用, 避免被gc
    lateinit var phPickerDelegate: PHPickerViewControllerDelegateProtocol
    lateinit var phPickerDismissDelegate: UIAdaptivePresentationControllerDelegateProtocol
    lateinit var documentPickerDelegate: UIDocumentPickerDelegateProtocol

    actual override fun init(context: Context, args: StartupArgs) {}

    @OptIn(ExperimentalForeignApi::class)
    private fun copyToTempDir(url: NSURL?): NSURL? {
        if (url == null)
            return null
        val fileManager = NSFileManager.defaultManager
        val tempPath = fileManager.temporaryDirectory.pathComponents?.plus(url.lastPathComponent) ?: return null
        val tempUrl = NSURL.fileURLWithPathComponents(tempPath) ?: return null
        fileManager.removeItemAtURL(tempUrl, null)
        return if (fileManager.copyItemAtURL(url, tempUrl, null)) tempUrl else null
    }

    actual suspend fun pickPicture(): Source? = pickPicture(1)?.getOrNull(0)

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = Coroutines.sync { future ->
        Coroutines.startMain {
            future.catching {
                val configuration = PHPickerConfiguration(PHPhotoLibrary.sharedPhotoLibrary()).apply {
                    selectionLimit = maxNum.toLong()
                    selection = PHPickerConfigurationSelectionOrdered
                    filter = PHPickerFilter.imagesFilter()
                }
                val picker = PHPickerViewController(configuration)
                val onImagesPicked: (List<PHPickerResult>) -> Unit = { results ->
                    val images = mutableListOf<Path>()
                    var processedImages = 0
                    results.forEach { pickerResult ->
                        pickerResult.itemProvider.loadFileRepresentationForTypeIdentifier(UTTypeImage.identifier) {
                                url, _ ->
                            val tempUrl = copyToTempDir(url)
                            tempUrl?.toPath()?.let { path -> images.add(path) }
                            processedImages++
                            if (processedImages == results.size) {
                                future.send(images.safeToSources { SystemFileSystem.source(it).buffered() })
                            }
                        }
                    }
                }
                phPickerDelegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                        picker.dismissViewControllerAnimated(true, null)
                        val results = didFinishPicking.mapNotNull { it as PHPickerResult }
                        onImagesPicked(results)
                    }
                }
                phPickerDismissDelegate = object : NSObject(), UIAdaptivePresentationControllerDelegateProtocol {
                    override fun presentationControllerDidDismiss(presentationController: UIPresentationController) {
                        onImagesPicked(emptyList())
                    }
                }
                picker.delegate = phPickerDelegate
                picker.presentationController?.delegate = phPickerDismissDelegate
                val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                rootViewController?.presentViewController(picker, true, null)
            }
        }
    }

    private inline fun openPicker(mimeType: List<String>, filter: List<String>, crossinline callback: (NSURL?) -> Unit) {
        Coroutines.startMain {
            val picker = UIDocumentPickerViewController(
                forOpeningContentTypes = filter
                    .map { it.substringAfterLast(".") }
                    .mapNotNull { UTType.typeWithFilenameExtension(it) }
                    .ifEmpty {
                        mimeType.mapNotNull {
                            UTType.typeWithMIMEType(it)
                        }
                    }
                    .ifEmpty { listOf(UTTypeContent) }
            ).apply {
                allowsMultipleSelection = false
            }
            documentPickerDelegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentAtURL: NSURL) {
                    callback(didPickDocumentAtURL)
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    callback(null)
                }
            }
            picker.delegate = documentPickerDelegate
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootViewController?.presentViewController(picker, true, null)
        }
    }

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = Coroutines.sync { future ->
        future.catching {
            openPicker(mimeType, filter) { url ->
                future.send { url?.let { SandboxSource(it).buffered() } }
            }
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitUri? = Coroutines.sync { future ->
        future.catching {
            openPicker(mimeType, filter) { url ->
                future.send { url?.let { SandboxUri(it) } }
            }
        }
    }

    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitUri? = Coroutines.sync { future ->
        Coroutines.startMain {
            future.catching {
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeFolder))
                documentPickerDelegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentAtURL: NSURL) {
                        val fileUrl = didPickDocumentAtURL.URLByAppendingPathComponent(filename)
                        future.send { fileUrl?.let { SandboxUri(it, didPickDocumentAtURL) } }
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        future.send()
                    }
                }
                picker.delegate = documentPickerDelegate
                val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                rootViewController?.presentViewController(picker, true, null)
            }
        }
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? = SaveType.Photo(filename) to Buffer()

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? {
        val fileManager = NSFileManager.defaultManager
        val tempPath = fileManager.temporaryDirectory.pathComponents?.plus(filename) ?: return null
        val url = NSURL.fileURLWithPathComponents(tempPath) ?: return null
        val path = url.toPath() ?: return null
        return SaveType.Video(filename, url) to SystemFileSystem.sink(path, false).buffered()
    }

    @OptIn(InternalIoApi::class, ExperimentalForeignApi::class)
    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = Coroutines.io {
        // TODO: 需要review
        when (val origin = origin as SaveType) {
            is SaveType.Photo -> {
                val bytes = sink.buffer.readByteArray()
                val data = bytes.toNSData()
                val image = UIImage(data)
                UIImageWriteToSavedPhotosAlbum(image, null, null, null)
            }
            is SaveType.Video -> {
                val isCompatible = UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(origin.url.path!!)
                if (isCompatible) {
                    UISaveVideoAtPathToSavedPhotosAlbum(origin.url.path!!, null, null, null)
                }
            }
        }
    }

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (origin is SaveType.Video) {
            // 此时保存到相册的动作未完成，还不能删除临时文件
            // origin.url.toPath()?.let { SystemFileSystem.delete(it) }
        }
    }
}