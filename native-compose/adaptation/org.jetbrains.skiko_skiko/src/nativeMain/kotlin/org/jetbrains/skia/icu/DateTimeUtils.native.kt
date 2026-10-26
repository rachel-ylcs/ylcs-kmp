@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skia.icu

import org.jetbrains.skia.ExternalSymbolName
import org.jetbrains.skia.impl.InteropPointer
import org.jetbrains.skia.impl.NativePointer

@ExternalSymbolName("org_jetbrains_skia_icu_DateTimeUtils__1nInfo")
internal external fun _nDateTimeInfo(locale: InteropPointer, error: InteropPointer): NativePointer
@ExternalSymbolName("org_jetbrains_skia_icu_DateTimeUtils__1nBestPattern")
internal external fun _nBestDatePattern(skeleton: InteropPointer, locale: InteropPointer, error: InteropPointer): NativePointer
@ExternalSymbolName("org_jetbrains_skia_icu_DateTimeUtils__1nFormatInteger")
internal external fun _nFormatLocaleInteger(value: Long, minDigits: Int, maxDigits: Int, grouping: Boolean, locale: InteropPointer, error: InteropPointer): NativePointer
