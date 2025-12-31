@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.win32

import kotlinx.cinterop.*
import kotlinx.datetime.LocalDateTime
import kotlinx.io.files.Path
import love.yinlin.extension.*
import platform.windows.*

// 文件时间

internal inline fun Path.fileTime(block: (WIN32_FIND_DATAW) -> FILETIME): LocalDateTime? = memScoped {
    val path = this@fileTime.toString()
    val data = alloc<WIN32_FIND_DATAW>()
    GetFileAttributesExW(path, GET_FILEEX_INFO_LEVELS.GetFileExInfoStandard, data.ptr)
    if (data.dwFileAttributes != INVALID_FILE_ATTRIBUTES) {
        val ft = block(data)
        val value = merge64(ft.dwHighDateTime, ft.dwLowDateTime)
        ((value.toULong() - 116444736000000000UL) / 10000UL).toLong().toLocalDateTime
    }
    else null
}

internal fun Path.fileTime(time: LocalDateTime, index: Int) = memScoped {
    val path = this@fileTime.toString()
    val attr = GetFileAttributesW(path)
    val flag = if (attr != INVALID_FILE_ATTRIBUTES && (attr.toInt() and FILE_ATTRIBUTE_DIRECTORY) != 0) 0x02000000U else 0U
    val hFile = CreateFileW(path, GENERIC_WRITE.toUInt(), 0U, null, OPEN_EXISTING.toUInt(), flag, null)
    if (hFile != INVALID_HANDLE_VALUE) {
        val value = (time.toLong.toULong() * 10000UL + 116444736000000000UL).toLong()
        val ft = alloc<FILETIME>()
        ft.dwHighDateTime = value.uint1
        ft.dwLowDateTime = value.uint2
        SetFileTime(hFile, if (index == 0) ft.ptr else null, if (index == 1) ft.ptr else null, if (index == 2) ft.ptr else null)
    }
}

var Path.createTime: LocalDateTime?
    get() = this.fileTime { it.ftCreationTime }
    set(value) { value?.let { time -> this.fileTime(time, 0) } }
var Path.writeTime: LocalDateTime?
    get() = this.fileTime { it.ftLastWriteTime }
    set(value) { value?.let { time -> this.fileTime(time, 1) } }
var Path.accessTime: LocalDateTime?
    get() = this.fileTime { it.ftLastAccessTime }
    set(value) { value?.let { time -> this.fileTime(time, 2) } }