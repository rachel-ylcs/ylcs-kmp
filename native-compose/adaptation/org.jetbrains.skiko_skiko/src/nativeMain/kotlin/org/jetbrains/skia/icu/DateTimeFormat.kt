package org.jetbrains.skia.icu

import org.jetbrains.skia.impl.Managed
import org.jetbrains.skia.impl.NativePointer
import org.jetbrains.skia.impl.interopScope
import org.jetbrains.skia.impl.reachabilityBarrier
import org.jetbrains.skia.impl.withStringResult

/** Owns a cached ICU formatter in UTC. Close must not race an operation; GC also releases abandoned formatters. */
class DateTimeFormat private constructor(pointer: NativePointer) : Managed(pointer, Finalizer.pointer) {
    companion object {
        fun make(pattern: String, languageTag: String): DateTimeFormat {
            require(pattern.length <= 1024 && '\u0000' !in pattern) { "Invalid date pattern" }
            DateTimeUtils.validateLocale(languageTag)
            return interopScope {
                val error = IntArray(1)
                val status = toInteropForResult(error)
                val pointer = _nMakeDateTimeFormat(toInterop(pattern), toInterop(languageTag), status)
                status.fromInterop(error)
                check(error[0] <= 0) { "ICU date formatter failed: ${error[0]}" }
                DateTimeFormat(pointer)
            }
        }
    }

    fun format(utcTimeMillis: Long): String {
        check(!isClosed) { "Date formatter is closed" }
        require(utcTimeMillis in -8640000000000000L .. 8640000000000000L) { "Date is outside ICU's supported range" }
        return try {
            interopScope {
                val error = IntArray(1)
                val status = toInteropForResult(error)
                val result = _nFormatDateTime(_ptr, utcTimeMillis, status)
                status.fromInterop(error)
                check(error[0] <= 0) { "ICU date format failed: ${error[0]}" }
                withStringResult(result)
            }
        }
        finally { reachabilityBarrier(this) }
    }

    fun parse(text: String): Long? {
        check(!isClosed) { "Date formatter is closed" }
        if (text.length > 1024 || '\u0000' in text) return null
        return try {
            interopScope {
                val error = IntArray(1)
                val status = toInteropForResult(error)
                val result = _nParseDateTime(_ptr, toInterop(text), status)
                status.fromInterop(error)
                if (error[0] == 9) null
                else {
                    check(error[0] <= 0) { "ICU date parse failed: ${error[0]}" }
                    if (result.isFinite() && result in -8640000000000000.0 .. 8640000000000000.0) result.toLong() else null
                }
            }
        }
        finally { reachabilityBarrier(this) }
    }

    private object Finalizer {
        val pointer = _nDateTimeFormatFinalizer()
    }
}

