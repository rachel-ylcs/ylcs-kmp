@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.win32

import kotlinx.cinterop.*
import kotlinx.datetime.LocalDateTime
import kotlinx.io.bytestring.buildByteString
import kotlinx.io.files.Path
import love.yinlin.extension.*
import platform.posix.*
import platform.windows.*

// 文件时间

private inline fun Path.fileTime(block: (WIN32_FIND_DATAW) -> FILETIME): LocalDateTime? = memScoped {
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

private fun Path.fileTime(time: LocalDateTime, index: Int) = memScoped {
    val path = this@fileTime.toString()
    val attr = GetFileAttributesW(path)
    val flag = if (attr != INVALID_FILE_ATTRIBUTES && (attr.toInt() and FILE_ATTRIBUTE_DIRECTORY) != 0) 0x02000000U else 0U
    val hFile = CreateFileW(path, GENERIC_WRITE.convert(), 0U, null, OPEN_EXISTING.convert(), flag, null)
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

// 标准目录

enum class StandardPath(val code: Int) {
    Desktop(0x0000),
    Documents(0x0005),
    Music(0x000D),
    Video(0x000E),
    Fonts(0x0014),
    AppData(0x001A),
    Windows(0x0024),
    System(0x0025),
    ProgramFiles(0x0026),
    Picture(0x0027),

    Running(0x1211),
    Temp(0x1213);

    val path: String get() = memScoped {
        val buffer = allocArray<UShortVar>(MAX_PATH)
        when (this@StandardPath) {
            Running -> GetModuleFileNameW(null, buffer, MAX_PATH.convert())
            Temp -> GetTempPathW(MAX_PATH.convert(), buffer)
            else -> SHGetSpecialFolderPathW(null, buffer, code, FALSE)
        }
        buffer.toKString()
    }
}

// 取快捷方式目标
val Path.shorcutTarget: Path? get() = memScoped {
    val shortcutPath = this@shorcutTarget.toString()
    val hFile = CreateFileW(shortcutPath, GENERIC_READ, FILE_SHARE_READ.convert(), null, OPEN_EXISTING.convert(), FILE_ATTRIBUTE_NORMAL.convert(), null)
    if (hFile == INVALID_HANDLE_VALUE) return null
    val vFileSize = alloc<LARGE_INTEGER>()
    if (GetFileSizeEx(hFile, vFileSize.ptr) == FALSE) {
        CloseHandle(hFile)
        return null
    }
    val shortcutSize = merge64(vFileSize.HighPart.toUInt(), vFileSize.LowPart)
    val headerSize = sizeOf<LINKFILE_HEADER>()
    if (shortcutSize !in headerSize..8192L) {
        CloseHandle(hFile)
        return null
    }
    val buffer = allocArray<UByteVar>(shortcutSize)
    val result = ReadFile(hFile, buffer.reinterpret(), shortcutSize.convert(), null, null)
    CloseHandle(hFile)
    if (result == FALSE) return null
    val header = buffer.reinterpret<LINKFILE_HEADER>().pointed
    if (header.HeaderSize == 0x4CU && (header.Flags and 2U) != 0U) {
        var p = buffer.plus(headerSize)!!
        val end = buffer.plus(shortcutSize)!!
        val size16 = p.reinterpret<UShortVar>().pointed
        p = p.plus(sizeOf<UShortVar>().toInt() + size16.value.toInt() + 44)!!
        while (p.pointed.value.toInt() != 0) p = p.plus(1)!!
        if (p.rawValue.toLong() < end.rawValue.toLong()) {
            p = p.plus(1)!!
            val data = buildByteString {
                var v = p.pointed.value.toInt()
                while (v != 0) {
                    append(v.toByte())
                    p = p.plus(1)!!
                    v = p.pointed.value.toInt()
                }
            }
            val targetPath = data.toByteArray().decodeToString()
            return Path(targetPath)
        }
    }
    return null
}

// 取驱动操作
data class DriverInfo(
    val name: Path, // 驱动器路径
    val totalSpace: Long, // 总空间 Byte
    val freeSpace: Long, // 剩余空间 Byte
) {
    companion object {
        internal fun getDriversBitmap(): List<Char> {
            var value = GetLogicalDrives()
            val bm = mutableListOf<Char>()
            for (i in 0 ..< 26) {
                if ((value and 1U) == 1U) bm += 'A' + i
                value = value shr 1
            }
            return bm
        }

        val entries: List<DriverInfo> get() = memScoped {
            getDriversBitmap().map { ch ->
                val name = "$ch:"
                val data = allocArray<UIntVar>(4)
                GetDiskFreeSpaceW(name, data, data + 1, data + 2, data + 3)
                val m = (data[1] * data[0]).toLong()
                DriverInfo(
                    name = Path(name),
                    totalSpace = data[3].toLong() * m,
                    freeSpace = data[2].toLong() * m
                )
            }
        }
    }
}

// 回收站操作

object RecycleBin {
    // 删除文件或目录到回收站
    fun delete(path: Path): Boolean = memScoped {
        val pathStr = path.toString()
        val length = pathStr.length
        val dst = allocArray<UShortVar>(length + 2)
        val shDelFile = alloc<SHFILEOPSTRUCTW>()
        shDelFile.hwnd = null
        shDelFile.fFlags = (FOF_NOERRORUI or FOF_ALLOWUNDO or FOF_SILENT or FOF_NOCONFIRMATION).convert()
        shDelFile.wFunc = FO_DELETE.convert()
        shDelFile.pFrom = pathStr.usePinned { wmemcpy(dst, it.addressOf(0).reinterpret(), length.convert()) }
        dst[length] = 0U
        dst[length + 1] = 0U
        SHFileOperationW(shDelFile.ptr) == 0
    }

    // 检查回收站是否为空
    val isEmpty: Boolean get() = memScoped {
        val info = alloc<SHQUERYRBINFO>()
        for (bm in DriverInfo.getDriversBitmap()) {
            info.cbSize = sizeOf<SHQUERYRBINFO>().convert()
            SHQueryRecycleBinW($$"$$bm:\\$Recycle.Bin\\", info.ptr)
            if (info.i64NumItems > 0) return false
        }
        return true
    }
    val isNotEmpty: Boolean get() = !isEmpty

    // 清空回收站
    fun clear(tip: Boolean = false) {
        val flag = if (tip) 0 else SHERB_NOCONFIRMATION or SHERB_NOPROGRESSUI or SHERB_NOSOUND
        SHEmptyRecycleBinW(null, null, flag.convert())
    }
}