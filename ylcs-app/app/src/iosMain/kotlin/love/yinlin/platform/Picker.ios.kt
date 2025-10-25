package love.yinlin.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import love.yinlin.common.uri.toPath
import love.yinlin.extension.Sources
import love.yinlin.extension.safeToSources
import love.yinlin.extension.toNSData
import love.yinlin.service
import platform.darwin.*
import platform.Foundation.*
import platform.UniformTypeIdentifiers.*
import platform.UIKit.*
import platform.Photos.*
import platform.PhotosUI.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual object Picker {
    // 全局引用, 避免被gc
    lateinit var phPickerDelegate: PHPickerViewControllerDelegateProtocol
    lateinit var phPickerDismissDelegate: UIAdaptivePresentationControllerDelegateProtocol
    lateinit var documentPickerDelegate: UIDocumentPickerDelegateProtocol

    actual suspend fun pickPicture(): Source? = pickPicture(1)?.getOrNull(0)

    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = suspendCoroutine { continuation ->
        Coroutines.startMain {
            continuation.safeResume {
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
                        pickerResult.itemProvider.loadFileRepresentationForTypeIdentifier(UTTypeImage.identifier) { url, _ ->
                            // TODO: 此处已被修改, 需要review
                            val srcUrl = url?.toPath()
                            if (srcUrl != null) {
                                Coroutines.startIO {
                                    val tempUrl = service.os.storage.createTempFile { sink ->
                                        SystemFileSystem.source(srcUrl).buffered().use { it.transferTo(sink) } > 0L
                                    }
                                    if (tempUrl != null) images += tempUrl
                                    processedImages++
                                    if (processedImages == results.size) {
                                        continuation.resume(images.safeToSources {
                                            SystemFileSystem.source(it).buffered()
                                        })
                                    }
                                }
                            }
                            else processedImages++
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

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = suspendCoroutine { continuation ->
        openPicker(mimeType, filter) { url ->
            continuation.safeResume {
                continuation.resume(url?.let { SandboxSource(it).buffered() })
            }
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitPath? = suspendCoroutine { continuation ->
        openPicker(mimeType, filter) { url ->
            continuation.safeResume {
                continuation.resume(url?.let { SandboxPath(it) })
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

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun savePath(filename: String, mimeType: String, filter: String): ImplicitPath? = suspendCoroutine { continuation ->
        Coroutines.startMain {
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeFolder))
            documentPickerDelegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentAtURL: NSURL) {
                    val fileUrl = didPickDocumentAtURL.URLByAppendingPathComponent(filename)
                    continuation.safeResume {
                        continuation.resume(fileUrl?.let { SandboxPath(it, didPickDocumentAtURL) })
                    }
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    continuation.resume(null)
                }
            }
            picker.delegate = documentPickerDelegate
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootViewController?.presentViewController(picker, true, null)
        }
    }

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? {
        val buffer = Buffer()
        return Photo(filename, buffer) to buffer
    }

    actual suspend fun prepareSaveVideo(filename: String): Pair<Any, Sink>? {
        val path = Path(osStorageTempPath, filename)
        val url = NSURL.fileURLWithPath(path.toString())
        return Video(filename, url) to SystemFileSystem.sink(path, false).buffered()
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) = Coroutines.io {
        when (origin) {
            is Photo -> {
                val bytes = origin.buffer.readByteArray()
                val data = bytes.toNSData()
                val image = UIImage(data)
                UIImageWriteToSavedPhotosAlbum(image, null, null, null)
            }
            is Video -> {
                val isCompatible = UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(origin.url.path!!)
                if (isCompatible) {
                    UISaveVideoAtPathToSavedPhotosAlbum(origin.url.path!!, null, null, null)
                }
            }
        }
    }

    actual suspend fun cleanSave(origin: Any, result: Boolean) {
        if (origin is Video) {
            // 此时保存到相册的动作未完成，还不能删除临时文件
            // origin.url.toPath()?.let { SystemFileSystem.delete(it) }
        }
    }
}

internal data class Photo(val filename: String, val buffer: Buffer)
internal data class Video(val filename: String, val url: NSURL)