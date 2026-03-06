@file:JsModule("opfs")
@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("FunctionName", "PropertyName", "ConstPropertyName")
package love.yinlin.fs

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.w3c.files.Blob
import org.w3c.files.File
import kotlin.js.*

external interface Estimate : JsAny {
    val quota: Double
    val usage: Double
    val usageDetails: JsAny?
}

external interface StorageManager : JsAny {
    fun estimate(): Promise<Estimate>
    fun persist(): Promise<JsBoolean>
    fun persisted(): Promise<JsBoolean>
    fun getDirectory(): Promise<FileSystemDirectoryHandle>
}

external interface FileSystemHandle : JsAny {
    val kind: String
    val name: String
    fun isSameEntry(entry: FileSystemHandle): Promise<JsBoolean>
    fun remove(): Promise<JsAny?>
    fun remove(options: JsAny): Promise<JsAny?>
}

external interface FileSystemSyncAccessHandle : JsAny {
    fun close()
    fun flush()
    fun getSize(): Double
    fun read(buffer: ArrayBuffer, options: JsAny = definedExternally): Double
    fun read(buffer: ArrayBufferView, options: JsAny = definedExternally): Double
    fun truncate(newSize: Double)
    fun write(buffer: ArrayBuffer, options: JsAny = definedExternally): Double
    fun write(buffer: ArrayBufferView, options: JsAny = definedExternally): Double
}

external interface FileSystemWritableFileStream : JsAny {
    fun seek(position: Double): Promise<JsAny?>
    fun truncate(size: Double): Promise<JsAny?>
    fun write(data: ArrayBuffer): Promise<JsAny?>
    fun write(data: ArrayBufferView): Promise<JsAny?>
    fun write(data: Blob): Promise<JsAny?>
    fun write(data: JsString): Promise<JsAny?>
    fun close(): Promise<JsAny?>
}

external interface FileSystemFileHandle : FileSystemHandle {
    fun createSyncAccessHandle(options: JsAny = definedExternally): Promise<FileSystemSyncAccessHandle>
    fun createWritable(options: JsAny = definedExternally): Promise<FileSystemWritableFileStream>
    fun getFile(): Promise<File>
}

external interface FileSystemDirectoryHandle : FileSystemHandle {
    fun entries(): JsAny
    fun getDirectoryHandle(name: String, options: JsAny = definedExternally): Promise<FileSystemDirectoryHandle>
    fun getFileHandle(name: String, options: JsAny = definedExternally): Promise<FileSystemFileHandle>
    fun keys(): JsAny
    fun removeEntry(name: String, options: JsAny = definedExternally): Promise<JsAny?>
    fun resolve(possibleDescendant: FileSystemHandle): Promise<JsArray<JsString>?>
    fun values(): JsAny
}