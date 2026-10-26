@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skia.icu

import org.jetbrains.skia.ExternalSymbolName
import org.jetbrains.skia.impl.InteropPointer
import org.jetbrains.skia.impl.NativePointer

@ExternalSymbolName("org_jetbrains_skia_icu_DateTimeFormat__1nMake")
internal external fun _nMakeDateTimeFormat(pattern: InteropPointer, locale: InteropPointer, error: InteropPointer): NativePointer
@ExternalSymbolName("org_jetbrains_skia_icu_DateTimeFormat__1nGetFinalizer")
internal external fun _nDateTimeFormatFinalizer(): NativePointer
@ExternalSymbolName("org_jetbrains_skia_icu_DateTimeFormat__1nFormat")
internal external fun _nFormatDateTime(pointer: NativePointer, utcTimeMillis: Long, error: InteropPointer): NativePointer
@ExternalSymbolName("org_jetbrains_skia_icu_DateTimeFormat__1nParse")
internal external fun _nParseDateTime(pointer: NativePointer, text: InteropPointer, error: InteropPointer): Double
