package org.jetbrains.skia.icu

import org.jetbrains.skia.ExternalSymbolName
import org.jetbrains.skia.impl.InteropPointer
import org.jetbrains.skia.impl.NativePointer
import org.jetbrains.skia.impl.interopScope
import org.jetbrains.skia.impl.withStringResult

/** Locale operations backed by the same static ICU archive and data as Skia text shaping. */
object LocaleUtils {
    /** Accepts a BCP 47 tag or the POSIX locale identifier supplied by the operating system. */
    fun localeInfo(languageTag: String): LocaleInfo {
        require(languageTag.none { it == '\u0000' || it == '\n' || it == '\r' }) { "Invalid locale identifier" }
        val error = IntArray(1)
        val fields = interopScope {
            val errorPointer = toInteropForResult(error)
            val pointer = _nLocaleInfo(toInterop(languageTag), errorPointer)
            errorPointer.fromInterop(error)
            check(error[0] <= 0) { "ICU locale conversion failed: ${error[0]}" }
            withStringResult(pointer)
        }.split('\n')
        check(fields.size == 5) { "Invalid ICU locale result" }
        return LocaleInfo(fields[0], fields[1], fields[2], fields[3], fields[4] == "rtl")
    }

    fun uppercase(text: String, languageTag: String): String = mapCase(text, languageTag, 0)

    fun lowercase(text: String, languageTag: String): String = mapCase(text, languageTag, 1)

    /** Title-cases a supplied initial code point; the caller preserves the remainder of the text. */
    fun titlecase(text: String, languageTag: String): String = mapCase(text, languageTag, 2)

    private fun mapCase(text: String, languageTag: String, mode: Int): String {
        if (text.isEmpty()) return text
        require('\u0000' !in languageTag) { "Invalid locale identifier" }
        return interopScope {
            val source = toInterop(ShortArray(text.length) { text[it].code.toShort() })
            val locale = toInterop(languageTag)
            val error = IntArray(1)
            val errorPointer = toInteropForResult(error)
            val size = _nMapCase(source, text.length, locale, mode, toInterop(ShortArray(0)), 0, errorPointer)
            errorPointer.fromInterop(error)
            check(error[0] <= 0 || error[0] == 15) { "ICU case conversion failed: ${error[0]}" }
            check(size >= 0) { "Invalid ICU case conversion length" }
            error[0] = 0
            val result = ShortArray(size)
            val resultPointer = toInteropForResult(result)
            val secondErrorPointer = toInteropForResult(error)
            val written = _nMapCase(source, text.length, locale, mode, resultPointer, size, secondErrorPointer)
            secondErrorPointer.fromInterop(error)
            resultPointer.fromInterop(result)
            check(error[0] <= 0 && written == size) { "ICU case conversion failed: ${error[0]}" }
            CharArray(size) { result[it].toInt().toChar() }.concatToString()
        }
    }
}

@ExternalSymbolName("org_jetbrains_skia_icu_LocaleUtils__1nLocaleInfo")
private external fun _nLocaleInfo(locale: InteropPointer, error: InteropPointer): NativePointer

@ExternalSymbolName("org_jetbrains_skia_icu_LocaleUtils__1nMapCase")
private external fun _nMapCase(source: InteropPointer, length: Int, locale: InteropPointer, mode: Int, result: InteropPointer, capacity: Int, error: InteropPointer): Int
