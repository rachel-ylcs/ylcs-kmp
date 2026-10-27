package org.jetbrains.skia.icu

import org.jetbrains.skia.impl.interopScope
import org.jetbrains.skia.impl.withStringResult

/** Uses Skia's static ICU and the complete packaged locale data; no default locale is modified. */
object DateTimeUtils {
    fun localeInfo(languageTag: String): DateTimeInfo {
        validateLocale(languageTag)
        val fields = interopScope {
            val error = IntArray(1)
            val status = toInteropForResult(error)
            val result = _nDateTimeInfo(toInterop(languageTag), status)
            status.fromInterop(error)
            check(error[0] <= 0) { "ICU date locale failed: ${error[0]}" }
            withStringResult(result)
        }.split('\n')
        check(fields.size == 17) { "Invalid ICU calendar information" }
        val firstDay = fields[0].toInt()
        check(firstDay in 1 .. 7 && (fields[2] == "0" || fields[2] == "1")) { "Invalid ICU calendar conventions" }
        return DateTimeInfo(firstDay, fields[1], fields[2] == "1", List(7) { fields[3 + it * 2] to fields[4 + it * 2] })
    }

    fun bestPattern(skeleton: String, languageTag: String): String {
        require(skeleton.isNotEmpty() && skeleton.length <= 1024 && '\u0000' !in skeleton) { "Invalid date skeleton" }
        validateLocale(languageTag)
        return interopScope {
            val error = IntArray(1)
            val status = toInteropForResult(error)
            val result = _nBestDatePattern(toInterop(skeleton), toInterop(languageTag), status)
            status.fromInterop(error)
            check(error[0] <= 0) { "ICU date pattern failed: ${error[0]}" }
            withStringResult(result)
        }
    }

    fun formatNumber(value: Long, minDigits: Int, maxDigits: Int, grouping: Boolean, languageTag: String): String {
        require(minDigits in 0 .. 100 && maxDigits in 0 .. 100 && minDigits <= maxDigits) { "Invalid integer digit bounds" }
        validateLocale(languageTag)
        return interopScope {
            val error = IntArray(1)
            val status = toInteropForResult(error)
            val result = _nFormatLocaleInteger(value, minDigits, maxDigits, grouping, toInterop(languageTag), status)
            status.fromInterop(error)
            check(error[0] <= 0) { "ICU number format failed: ${error[0]}" }
            withStringResult(result)
        }
    }

    internal fun validateLocale(languageTag: String) {
        require(languageTag.length <= 256 && languageTag.none { it == '\u0000' || it == '\r' || it == '\n' }) { "Invalid date locale" }
        IcuData.ensureLoaded()
    }
}

