@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.jetbrains.skia.icu

import org.jetbrains.skia.ExternalSymbolName
import org.jetbrains.skia.impl.InteropPointer
import org.jetbrains.skia.impl.NativePointer

@ExternalSymbolName("org_jetbrains_skia_icu_LocaleUtils__1nLocaleInfo")
internal external fun _nLocaleInfo(locale: InteropPointer, error: InteropPointer): NativePointer

@ExternalSymbolName("org_jetbrains_skia_icu_LocaleUtils__1nMapCase")
internal external fun _nMapCase(source: InteropPointer, length: Int, locale: InteropPointer, mode: Int, result: InteropPointer, capacity: Int, error: InteropPointer): Int
