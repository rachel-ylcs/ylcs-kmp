package love.yinlin.platform

import io.ktor.utils.io.core.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.toNSData
import love.yinlin.common.toPath
import love.yinlin.extension.Sources
import love.yinlin.extension.safeToSources
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
                results.forEach {
                    it.itemProvider.loadFileRepresentationForTypeIdentifier(UTTypeImage.identifier) {
                            url, error ->
                        val tempUrl = copyToTempDir(url)
                        tempUrl?.toPath()?.let { path -> images.add(path) }
                        processedImages++
                        if (processedImages == results.size) {
                            continuation.resume(images.safeToSources {
                                SystemFileSystem.source(it).buffered()
                            })
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

    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = suspendCoroutine { continuation ->
        openPicker(mimeType, filter) { url ->
            continuation.safeResume {
                continuation.resume(url?.let { SystemFileSystem.source(it.toPath()!!).buffered() })
            }
        }
    }

    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitPath? = suspendCoroutine { continuation ->
        openPicker(mimeType, filter) { url ->
            continuation.safeResume {
                continuation.resume(url?.let { NormalPath(it.path!!) })
            }
        }
    }

    private inline fun openPicker(mimeType: List<String>, filter: List<String>, crossinline callback: (NSURL?) -> Unit) {
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = filter
                .map { it.substringAfterLast(".") }
                .mapNotNull { UTType.typeWithFilenameExtension(it) }
                .ifEmpty { mimeType.mapNotNull {
                    UTType.typeWithMIMEType(it)
                } }
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

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? {
        val buffer = Buffer()
        return buffer to buffer
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun actualSavePicture(filename: String, origin: Any, sink: Sink) {
        Coroutines.io {
            val bytes = (origin as Buffer).readBytes()
            val data = bytes.toNSData()
            val image = UIImage(data)
            UIImageWriteToSavedPhotosAlbum(image, null, null, null)
        }
    }

    actual suspend fun cleanSavePicture(origin: Any, result: Boolean) = Unit
}