package org.libpag

import love.yinlin.extension.NativeLib
import love.yinlin.platform.NativeLibLoader

@NativeLib
object PAGDiskCache {
    init {
        NativeLibLoader.resource("pag_kmp")
    }

    @JvmStatic
    private external fun nativeMaxDiskSize(): Long
    @JvmStatic
    private external fun nativeSetMaxDiskSize(size: Long)
    @JvmStatic
    private external fun nativeRemoveAll()
    @JvmStatic
    private external fun nativeReadFile(key: String): ByteArray
    @JvmStatic
    private external fun nativeWriteFile(key: String, bytes: ByteArray): Boolean

    var maxDiskSize: Long get() = nativeMaxDiskSize()
        set(value) { nativeSetMaxDiskSize(value) }

    fun removeAll() = nativeRemoveAll()

    fun readFile(key: String): ByteArray = nativeReadFile(key)

    fun writeFile(key: String, bytes: ByteArray): Boolean = nativeWriteFile(key, bytes)
}