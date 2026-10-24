@file:OptIn(ExperimentalForeignApi::class)

package org.jetbrains.skiko

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.reinterpret
import platform.posix._fseeki64
import platform.posix._ftelli64

internal actual fun seekResourceFile(file: COpaquePointer, offset: Long, origin: Int): Int = _fseeki64(file.reinterpret(), offset, origin)

internal actual fun tellResourceFile(file: COpaquePointer): Long = _ftelli64(file.reinterpret())