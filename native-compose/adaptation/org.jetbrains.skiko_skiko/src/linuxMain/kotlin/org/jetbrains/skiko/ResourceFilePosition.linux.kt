@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skiko

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.reinterpret
import platform.posix.FILE
import platform.posix.fseek
import platform.posix.ftell

internal actual fun seekResourceFile(file: COpaquePointer, offset: Long, origin: Int): Int =
    fseek(file.reinterpret<FILE>(), offset, origin)

internal actual fun tellResourceFile(file: COpaquePointer): Long =
    ftell(file.reinterpret<FILE>())
